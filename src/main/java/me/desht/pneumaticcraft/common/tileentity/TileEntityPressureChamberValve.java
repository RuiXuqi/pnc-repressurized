package me.desht.pneumaticcraft.common.tileentity;

import com.google.common.math.IntMath;
import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.recipe.IPressureChamberRecipe;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import me.desht.pneumaticcraft.api.tileentity.IAirListener;
import me.desht.pneumaticcraft.common.DamageSourcePneumaticCraft;
import me.desht.pneumaticcraft.common.block.*;
import me.desht.pneumaticcraft.common.event.VillagerHandler;
import me.desht.pneumaticcraft.common.network.*;
import me.desht.pneumaticcraft.common.recipes.PressureChamberRecipe;
import me.desht.pneumaticcraft.common.util.ItemStackHandlerIterable;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.EnumCustomParticleType;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class TileEntityPressureChamberValve extends TileEntityPneumaticBase implements IMinWorkingPressure, IAirListener {
    private static final int CHAMBER_INV_SIZE = 27;

    @DescSynced
    public int multiBlockX, multiBlockY, multiBlockZ;
    @DescSynced
    public int multiBlockSize;
    @DescSynced
    public boolean hasGlass;  // true if there is any glass in the multiblock (only the primary valve has this)
    @DescSynced
    private float roundedPressure; // rounded to multiples of 0.25 to avoid excessive server->client traffic

    public List<TileEntityPressureChamberValve> accessoryValves;
    private final List<BlockPos> nbtValveList;
    private boolean readNBT = false;
    @GuiSynced
    public boolean isValidRecipeInChamber;
    @GuiSynced
    public boolean isSufficientPressureInChamber;
    @GuiSynced
    public float recipePressure;
    @DescSynced
    private ItemStackHandler itemsInChamber = new ItemStackHandler(CHAMBER_INV_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityPressureChamberValve.this.recipeRecalcNeeded = true;
        }
    };

    // Hold excess crafting output rather than dropping it as an item.  Items in overflow will prevent further crafting.
    // Overflow will automatically spill back into the main chamber handler as space becomes available
    private final Deque<ItemStack> overflow = new ArrayDeque<>();
    @GuiSynced
    public boolean itemsInOverflow;

    // list of recipes which can be made from the current chamber contents, not considering the current pressure
    private final List<IPressureChamberRecipe> applicableRecipes = new ArrayList<>();
    private boolean recipeRecalcNeeded = true;

    private long lastSoundTick;  // to avoid excessive spamming of the pop sound
    private int nParticles;  // client-side: the number of particles to create each tick (dependent on chamber size & pressure)

    public TileEntityPressureChamberValve() {
        super(PneumaticValues.DANGER_PRESSURE_PRESSURE_CHAMBER, PneumaticValues.MAX_PRESSURE_PRESSURE_CHAMBER, PneumaticValues.VOLUME_PRESSURE_CHAMBER_PER_EMPTY, 4);
        this.accessoryValves = new ArrayList<>();
        this.nbtValveList = new ArrayList<>();
    }

    @Override
    protected boolean shouldRerenderChunkOnDescUpdate() {
        return true;
    }

    @Override
    public boolean isConnectedTo(EnumFacing side) {
        switch (this.getRotation()) {
            case UP:
            case DOWN:
                return side == EnumFacing.UP || side == EnumFacing.DOWN;
            case NORTH:
            case SOUTH:
                return side == EnumFacing.NORTH || side == EnumFacing.SOUTH;
            case EAST:
            case WEST:
                return side == EnumFacing.EAST || side == EnumFacing.WEST;
        }
        return false;
    }

    @Override
    public void addConnectedPneumatics(List<Pair<EnumFacing, IAirHandler>> teList) {
        if (this.accessoryValves != null) {
            for (TileEntityPressureChamberValve valve : this.accessoryValves) {
                if (valve != this) teList.add(new ImmutablePair<>(null, valve.getAirHandler(null)));
            }
        }
    }

    @Override
    public void onAirDispersion(IAirHandler handler, EnumFacing dir, int airAdded) {
    }

    @Override
    public int getMaxDispersion(IAirHandler handler, EnumFacing dir) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void update() {
        if (this.readNBT && !this.getWorld().isRemote) {
            this.doPostNBTSetup();
        }

        if (!this.getWorld().isRemote) {
            this.checkForAirLeak();
        }

        if (this.multiBlockSize != 0 && !this.getWorld().isRemote) {
            this.roundedPressure = ((int) (this.getPressure() * 4.0f)) / 4.0f;

            if (this.recipeRecalcNeeded) {
                this.isValidRecipeInChamber = false;
                this.isSufficientPressureInChamber = false;
                this.recipePressure = Float.MAX_VALUE;
                this.applicableRecipes.clear();
                for (IPressureChamberRecipe recipe : PressureChamberRecipe.recipes) {
                    if (recipe.isValidRecipe(this.itemsInChamber)) {
                        this.applicableRecipes.add(recipe);
                    }
                }
                this.isValidRecipeInChamber = !this.applicableRecipes.isEmpty();
                this.recipeRecalcNeeded = false;
            }

            if (!this.overflow.isEmpty()) {
                ItemStack stack = this.overflow.peekFirst();
                if (ItemHandlerHelper.insertItem(this.itemsInChamber, stack, false).isEmpty()) {
                    this.overflow.removeFirst();
                }
            }
            this.itemsInOverflow = !this.overflow.isEmpty();
            if (this.overflow.isEmpty()) this.processApplicableRecipes();

            if (this.getPressure() > PneumaticValues.MAX_PRESSURE_LIVING_ENTITY) {
                this.handleEntitiesInChamber();
            }
        }

        super.update();

        // particles
        if (this.getWorld().isRemote && this.hasGlass && this.isPrimaryValve() && this.roundedPressure > 0.2D) {
            if (PneumaticCraftRepressurized.proxy.getClientPlayer().getDistanceSq(this.getPos()) < 256) {
                for (int i = 0; i < this.nParticles; i++) {
                    double posX = this.multiBlockX + 1D + this.getWorld().rand.nextDouble() * (this.multiBlockSize - 2D);
                    double posY = this.multiBlockY + 1.5D + this.getWorld().rand.nextDouble() * (this.multiBlockSize - 2.5D);
                    double posZ = this.multiBlockZ + 1D + this.getWorld().rand.nextDouble() * (this.multiBlockSize - 2D);
                    PneumaticCraftRepressurized.proxy.playCustomParticle(EnumCustomParticleType.AIR_PARTICLE, this.world, posX, posY, posZ, 0, 0, 0);
                }
            }
        }
    }

    /**
     * This setup can't be done in readFromNBT() because there may be multiple valve TE's in the multiblock,
     * and all of them need to be fully initialized before this code is run.
     */
    private void doPostNBTSetup() {
        this.readNBT = false;

        IBlockState state = this.getWorld().getBlockState(this.getPos());
        if (state.getBlock() instanceof BlockPressureChamberValve)
            this.getWorld().setBlockState(this.getPos(), state.withProperty(BlockPressureChamberValve.FORMED, this.isPrimaryValve()), 2);

        this.accessoryValves.clear();
        for (BlockPos valve : this.nbtValveList) {
            TileEntity te = this.getWorld().getTileEntity(valve);
            if (te instanceof TileEntityPressureChamberValve) {
                this.accessoryValves.add((TileEntityPressureChamberValve) te);
            }
        }

        if (this.isPrimaryValve()) {
            this.hasGlass = this.checkForGlass();
            this.sendDescriptionPacket();
        }
    }

    private void checkForAirLeak() {
        boolean[] connected = new boolean[]{true, true, true, true, true, true};

        switch (this.getRotation()) {
            // take off the sides that tubes can connect to
            case UP:
            case DOWN:
                connected[EnumFacing.UP.ordinal()] = connected[EnumFacing.DOWN.ordinal()] = false;
                break;
            case NORTH:
            case SOUTH:
                connected[EnumFacing.NORTH.ordinal()] = connected[EnumFacing.SOUTH.ordinal()] = false;
                break;
            case EAST:
            case WEST:
                connected[EnumFacing.EAST.ordinal()] = connected[EnumFacing.WEST.ordinal()] = false;
                break;
        }

        List<Pair<EnumFacing, IAirHandler>> teList = this.getAirHandler(null).getConnectedPneumatics();
        for (Pair<EnumFacing, IAirHandler> entry : teList) {
            if (entry.getKey() != null) connected[entry.getKey().ordinal()] = true;
        }

        // retrieve the valve that is controlling the (potential) chamber
        TileEntityPressureChamberValve primaryValve = this.accessoryValves.isEmpty() ? null : this.accessoryValves.get(this.accessoryValves.size() - 1);
        if (primaryValve != null) {
            // we can scratch one side (the side facing into the chamber) to be leaking air
            switch (this.getRotation()) {
                case UP:
                case DOWN:
                    if (primaryValve.multiBlockY == this.getPos().getY()) {
                        connected[EnumFacing.UP.ordinal()] = true;
                    } else {
                        connected[EnumFacing.DOWN.ordinal()] = true;
                    }
                    break;
                case NORTH:
                case SOUTH:
                    if (primaryValve.multiBlockZ == this.getPos().getZ()) {
                        connected[EnumFacing.SOUTH.ordinal()] = true;
                    } else {
                        connected[EnumFacing.NORTH.ordinal()] = true;
                    }
                    break;
                case EAST:
                case WEST:
                    if (primaryValve.multiBlockX == this.getPos().getX()) {
                        connected[EnumFacing.EAST.ordinal()] = true;
                    } else {
                        connected[EnumFacing.WEST.ordinal()] = true;
                    }
                    break;
            }
        }
        for (int i = 0; i < 6; i++) {
            if (!connected[i]) this.getAirHandler(null).airLeak(EnumFacing.byIndex(i));
        }
    }

    private void processApplicableRecipes() {
        for (IPressureChamberRecipe recipe : this.applicableRecipes) {
            boolean pressureOK = recipe.getCraftingPressure() <= this.getPressure() && recipe.getCraftingPressure() > 0F
                    || recipe.getCraftingPressure() >= this.getPressure() && recipe.getCraftingPressure() < 0F;
            if (Math.abs(recipe.getCraftingPressure()) < Math.abs(this.recipePressure)) {
                this.recipePressure = recipe.getCraftingPressure();
            }
            if (pressureOK) {
                this.isSufficientPressureInChamber = true;
                NonNullList<ItemStack> output = recipe.craftRecipe(this.itemsInChamber);
                if (!output.isEmpty()) {
                    this.giveOutput(output);
                    if (this.getWorld().getTotalWorldTime() - this.lastSoundTick > 5) {
                        NetworkHandler.sendToAllAround(new PacketPlaySound(SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, this.getPos(), 0.5f, 0.8f + this.getWorld().rand.nextFloat() * 0.4f, false), this.getWorld());
                        this.lastSoundTick = this.getWorld().getTotalWorldTime();
                    }
                }
                // Craft at most one recipe each tick; this is because crafting changes the contents of the
                // chamber, possibly invalidating other applicable recipes.  Modifying the chamber's contents
                // automatically triggers a rescan for applicable recipes on the next tick.
                break;
            }
        }
    }

    private void handleEntitiesInChamber() {
        AxisAlignedBB bbBox = new AxisAlignedBB(this.multiBlockX + 1, this.multiBlockY + 1, this.multiBlockZ + 1, this.multiBlockX + this.multiBlockSize - 1, this.multiBlockY + this.multiBlockSize - 1, this.multiBlockZ + this.multiBlockSize - 1);
        List<EntityLivingBase> entities = this.getWorld().getEntitiesWithinAABB(EntityLivingBase.class, bbBox);
        for (EntityLivingBase entity : entities) {
            if (entity instanceof EntityVillager) {
                EntityVillager villager = (EntityVillager) entity;
                if (villager.getProfessionForge() != VillagerHandler.mechanicProfession) {
                    villager.setDead();
                    EntityVillager mechanic = new EntityVillager(this.world);
                    mechanic.setProfession(VillagerHandler.mechanicProfession);
                    mechanic.setPosition(villager.posX, villager.posY, villager.posZ);
                    this.world.spawnEntity(mechanic);
                }
            }
            if (!(entity instanceof EntityVillager) || ((EntityVillager) entity).getProfessionForge() != VillagerHandler.mechanicProfession) {
                entity.attackEntityFrom(DamageSourcePneumaticCraft.PRESSURE, (int) (this.getPressure() * 2D));
            }
        }
    }

    private boolean checkForGlass() {
        MutableBlockPos mPos = new MutableBlockPos();
        for (int x = 0; x < this.multiBlockSize; x++) {
            for (int y = 0; y < this.multiBlockSize; y++) {
                for (int z = 0; z < this.multiBlockSize; z++) {
                    mPos = mPos.setPos(this.multiBlockX + x, this.multiBlockY + y, this.multiBlockZ + z);
                    if (this.world.getBlockState(mPos).getBlock() instanceof BlockPressureChamberGlass) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void giveOutput(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            stack = stack.copy();
            stack = ItemHandlerHelper.insertItem(this.itemsInChamber, stack, false);
            if (!stack.isEmpty()) this.overflow.addLast(stack);
        }
    }

    public ItemStackHandler getStacksInChamber() {
        return this.itemsInChamber;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.setMultiBlockCoords(tag.getInteger("multiBlockSize"), tag.getInteger("multiBlockX"), tag.getInteger("multiBlockY"), tag.getInteger("multiBlockZ"));
        this.isSufficientPressureInChamber = tag.getBoolean("sufPressure");
        this.isValidRecipeInChamber = tag.getBoolean("validRecipe");
        this.recipePressure = tag.getFloat("recipePressure");
        this.itemsInChamber.deserializeNBT(tag.getCompoundTag("itemsInChamber"));
        if (this.itemsInChamber.getSlots() > CHAMBER_INV_SIZE) {
            // in case we read in a larger item handler from previous save (used to be 100 items)
            ItemStackHandler newHandler = new ItemStackHandler(CHAMBER_INV_SIZE);
            for (int i = 0; i < CHAMBER_INV_SIZE; i++) {
                newHandler.setStackInSlot(i, this.itemsInChamber.getStackInSlot(i));
            }
            this.itemsInChamber = newHandler;
        }

        if (tag.hasKey("overflow", Constants.NBT.TAG_COMPOUND)) {
            NBTTagList ov = tag.getTagList("overflow", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < ov.tagCount(); i++) {
                this.overflow.addFirst(new ItemStack(ov.getCompoundTagAt(i)));
            }
        }

        // Read in the accessory valves from NBT
        NBTTagList tagList2 = tag.getTagList("Valves", Constants.NBT.TAG_COMPOUND);
        this.nbtValveList.clear();
        for (int i = 0; i < tagList2.tagCount(); ++i) {
            NBTTagCompound tagCompound = tagList2.getCompoundTagAt(i);
            this.nbtValveList.add(NBTUtil.getPos(tagCompound));
        }

        this.readNBT = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("multiBlockX", this.multiBlockX);
        tag.setInteger("multiBlockY", this.multiBlockY);
        tag.setInteger("multiBlockZ", this.multiBlockZ);
        tag.setInteger("multiBlockSize", this.multiBlockSize);
        tag.setBoolean("sufPressure", this.isSufficientPressureInChamber);
        tag.setBoolean("validRecipe", this.isValidRecipeInChamber);
        tag.setFloat("recipePressure", this.recipePressure);
        tag.setTag("itemsInChamber", this.itemsInChamber.serializeNBT());

        if (!this.overflow.isEmpty() && this.isPrimaryValve()) {
            NBTTagList ov = new NBTTagList();
            for (ItemStack stack : this.overflow) {
                ov.appendTag(stack.serializeNBT());
            }
            tag.setTag("overflow", ov);
        }

        // Write the accessory valve to NBT
        NBTTagList tagList2 = new NBTTagList();
        for (TileEntityPressureChamberValve valve : this.accessoryValves) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setInteger("x", valve.getPos().getX());
            tagCompound.setInteger("y", valve.getPos().getY());
            tagCompound.setInteger("z", valve.getPos().getZ());
            tagList2.appendTag(tagCompound);
        }

        tag.setTag("Valves", tagList2);
        return tag;
    }

    public void onMultiBlockBreak() {
        if (this.isPrimaryValve()) {
            Iterator<ItemStack> itemsInChamberIterator = new ItemStackHandlerIterable(this.itemsInChamber).iterator();
            while (itemsInChamberIterator.hasNext()) {
                ItemStack stack = itemsInChamberIterator.next();
                this.dropItemOnGround(stack);
                itemsInChamberIterator.remove();
            }
            for (ItemStack stack : this.overflow) {
                this.dropItemOnGround(stack);
            }
            this.overflow.clear();
            this.invalidateMultiBlock();
        }

    }

    private void dropItemOnGround(ItemStack stack) {
        PneumaticCraftUtils.dropItemOnGroundPrecisely(stack, this.getWorld(),
                this.multiBlockX + this.multiBlockSize / 2.0, this.multiBlockY + 1.0, this.multiBlockZ + this.multiBlockSize / 2.0);
    }

    private void invalidateMultiBlock() {
        for (int x = 0; x < this.multiBlockSize; x++) {
            for (int y = 0; y < this.multiBlockSize; y++) {
                for (int z = 0; z < this.multiBlockSize; z++) {
                    TileEntity te = this.getWorld().getTileEntity(new BlockPos(x + this.multiBlockX, y + this.multiBlockY, z + this.multiBlockZ));
                    if (te instanceof TileEntityPressureChamberWall) {
                        // Clear the base TE's, so that the walls can be used in a new MultiBlock
                        TileEntityPressureChamberWall teWall = (TileEntityPressureChamberWall) te;
                        teWall.setCore(null);
                    }
                }
            }
        }
        if (this.accessoryValves != null) {
            for (TileEntityPressureChamberValve valve : this.accessoryValves) {
                valve.setMultiBlockCoords(0, 0, 0, 0);
                if (valve != this) {
                    valve.accessoryValves.clear();
                    if (!this.getWorld().isRemote) valve.sendDescriptionPacket();
                }
            }
            this.accessoryValves.clear();
        }
        if (!this.getWorld().isRemote) this.sendDescriptionPacket();
    }

    private void setMultiBlockCoords(int size, int baseX, int baseY, int baseZ) {
        this.multiBlockSize = size;
        this.multiBlockX = baseX;
        this.multiBlockY = baseY;
        this.multiBlockZ = baseZ;
        this.getAirHandler(null).setDefaultVolume(this.getDefaultVolume());
    }

    @Override
    public void onDescUpdate() {
        super.onDescUpdate();
        this.nParticles = IntMath.pow(this.multiBlockSize - 2, 3);
        this.nParticles = Math.max(1, (int) (this.nParticles / ((this.dangerPressure + 1) - Math.min(this.dangerPressure, this.roundedPressure))));
    }

    public static boolean checkIfProperlyFormed(World world, BlockPos pos) {
        for (int i = 3; i < 6; i++) {
            if (checkForShiftedCubeOfSize(i, world, pos.getX(), pos.getY(), pos.getZ())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkForShiftedCubeOfSize(int size, World world, int baseX, int baseY, int baseZ) {
        for (int wallX = 0; wallX < size; wallX++) {
            for (int wallY = 0; wallY < size; wallY++) {
                // check every possible configuration the block can be in.
                if (checkForCubeOfSize(size, world, baseX, baseY - wallY, baseZ - wallX)) return true;
                if (checkForCubeOfSize(size, world, baseX, baseY + wallY, baseZ + wallX)) return true;
                if (checkForCubeOfSize(size, world, baseX - wallX, baseY - wallY, baseZ)) return true;
                if (checkForCubeOfSize(size, world, baseX + wallX, baseY + wallY, baseZ)) return true;
                if (checkForCubeOfSize(size, world, baseX - wallX, baseY, baseZ - wallY)) return true;
                if (checkForCubeOfSize(size, world, baseX + wallX, baseY, baseZ + wallY)) return true;

                if (checkForCubeOfSize(size, world, baseX - size + 1, baseY - wallY, baseZ - wallX)) return true;
                if (checkForCubeOfSize(size, world, baseX - size + 1, baseY + wallY, baseZ + wallX)) return true;
                if (checkForCubeOfSize(size, world, baseX - wallX, baseY - wallY, baseZ - size + 1)) return true;
                if (checkForCubeOfSize(size, world, baseX + wallX, baseY + wallY, baseZ - size + 1)) return true;
                if (checkForCubeOfSize(size, world, baseX - wallX, baseY - size + 1, baseZ - wallY)) return true;
                if (checkForCubeOfSize(size, world, baseX + wallX, baseY - size + 1, baseZ + wallY)) return true;
            }
        }
        return false;
    }

    private static boolean checkForCubeOfSize(int size, World world, int baseX, int baseY, int baseZ) {
        List<TileEntityPressureChamberValve> valveList = new ArrayList<>();
        MutableBlockPos mPos = new MutableBlockPos();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    mPos = mPos.setPos(x + baseX, y + baseY, z + baseZ);
                    IBlockState state = world.getBlockState(mPos);
                    if (x != 0 && x != size - 1 && y != 0 && y != size - 1 && z != 0 && z != size - 1) {
                        if (!world.isAirBlock(mPos)) return false;
                    } else if (!(state.getBlock() instanceof IBlockPressureChamber)) {
                        return false;
                    } else if (state.getBlock() instanceof BlockPressureChamberValve) {
                        // this a valve; ensure it faces the right way for the face it's in
                        boolean xMid = x != 0 && x != size - 1;
                        boolean yMid = y != 0 && y != size - 1;
                        boolean zMid = z != 0 && z != size - 1;
                        EnumFacing facing = state.getValue(BlockPneumaticCraft.ROTATION); //(TileEntityBase) world.getTileEntity(mPos)).getRotation();
                        if (xMid && yMid && (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) || xMid && zMid && (facing == EnumFacing.UP || facing == EnumFacing.DOWN) || yMid && zMid && (facing == EnumFacing.EAST || facing == EnumFacing.WEST)) {
                            TileEntity te = world.getTileEntity(mPos);
                            if (te instanceof TileEntityPressureChamberValve) {
                                valveList.add((TileEntityPressureChamberValve) te);
                            }
                        } else {
                            return false;
                        }
                    } else {
                        // this is a wall or interface; ensure it doesn't belong to another pressure chamber
                        TileEntity te = world.getTileEntity(mPos);
                        if (te instanceof TileEntityPressureChamberWall && ((TileEntityPressureChamberWall) te).getCore() != null) {
                            return false;
                        }
                    }
                }
            }
        }

        // So the structure is valid; just check that we have at least one valid valve
        if (valveList.isEmpty()) return false;

        // primary valve is the last one scanned (which will be @ max X/Y/Z)
        TileEntityPressureChamberValve primaryValve = valveList.get(valveList.size() - 1);

        // every valve in the structure has a list of every valve, including itself
        valveList.forEach(valve -> valve.accessoryValves = new ArrayList<>(valveList));

        // set the multi-block coords in the primary valve only
        primaryValve.setMultiBlockCoords(size, baseX, baseY, baseZ);

        // note the core valve in every wall & interface so right clicking & block break work as expected
        primaryValve.hasGlass = false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    TileEntity te = world.getTileEntity(new BlockPos(x + baseX, y + baseY, z + baseZ));
                    if (te instanceof TileEntityPressureChamberWall) {
                        TileEntityPressureChamberWall teWall = (TileEntityPressureChamberWall) te;
                        teWall.setCore(primaryValve);  // this also forces re-rendering with the formed texture
                        if (world.getBlockState(te.getPos()).getBlock() instanceof BlockPressureChamberGlass) {
                            primaryValve.hasGlass = true;
                        }
                    } else if (te instanceof TileEntityPressureChamberValve) {
                        IBlockState state = world.getBlockState(te.getPos());
                        world.setBlockState(te.getPos(), state.withProperty(BlockPressureChamberValve.FORMED, ((TileEntityPressureChamberValve) te).isPrimaryValve()), 2);
                    }
                    if (te != null && !te.getWorld().isRemote) {
                        double dx = x == 0 ? -0.1 : 0.1;
                        double dz = z == 0 ? -0.1 : 0.1;
                        NetworkHandler.sendToAllAround(
                                new PacketSpawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                                        te.getPos().getX() + 0.5, te.getPos().getY() + 0.5, te.getPos().getZ() + 0.5,
                                        dx, 0.3, dz, 5, 0, 0, 0),
                                world);
                    }
                }
            }
        }

        // pick up any loose items into the chamber inventory
        primaryValve.captureEntityItemsInChamber();

        // force-sync primary valve details to clients for rendering purposes
        primaryValve.sendDescriptionPacket();

        return true;
    }

    private boolean isPrimaryValve() {
        return this.multiBlockSize > 0;
    }

    private AxisAlignedBB getChamberAABB() {
        return new AxisAlignedBB(this.multiBlockX, this.multiBlockY, this.multiBlockZ,
                this.multiBlockX + this.multiBlockSize, this.multiBlockY + this.multiBlockSize, this.multiBlockZ + this.multiBlockSize);
    }

    private void captureEntityItemsInChamber() {
        List<EntityItem> items = this.getWorld().getEntitiesWithinAABB(EntityItem.class, this.getChamberAABB(), EntitySelectors.IS_ALIVE);
        for (EntityItem item : items) {
            ItemStack stack = item.getItem();
            ItemStack leftover = ItemHandlerHelper.insertItem(this.itemsInChamber, stack, false);
            if (leftover.isEmpty()) item.setDead();
            else item.setItem(stack);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getChamberAABB();
    }

    @Override
    public String getName() {
        return Blockss.PRESSURE_CHAMBER_VALVE.getTranslationKey();
    }

    @Override
    public float getMinWorkingPressure() {
        return this.isValidRecipeInChamber ? this.recipePressure : -Float.MAX_VALUE;
    }

    @Override
    public int getDefaultVolume() {
        int vol = super.getDefaultVolume();
        return this.multiBlockSize > 3 ? vol * IntMath.pow(this.multiBlockSize - 2, 3) : vol;
    }
}
