package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.client.model.module.ModelAirGrate;
import me.desht.pneumaticcraft.client.model.module.ModelModuleBase;
import me.desht.pneumaticcraft.client.render.RenderRangeLines;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.tileentity.TileEntityHeatSink;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.lib.EnumCustomParticleType;
import me.desht.pneumaticcraft.lib.Names;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ModuleAirGrate extends TubeModule {
    private int grateRange;
    private boolean vacuum;
    private final Set<TileEntityHeatSink> heatSinks = new HashSet<>();
    private final RenderRangeLines rangeLineRenderer = new RenderRangeLines(0x5500FF00);
    private boolean resetRendering = false;
    private EntityFilter entityFilter = null;
    private TileEntity adjacentInv = null;
    private EnumFacing adjacentInvSide;

    public ModuleAirGrate() {
    }

    private int getRange() {
        float range = this.pressureTube.getAirHandler(null).getPressure() * 4;
        this.vacuum = range < 0;
        if (this.vacuum) range = -range * 4;
        return (int) range;
    }

    @Override
    public double getWidth() {
        return 1;
    }

    @Override
    public void update() {
        super.update();

        World world = this.pressureTube.world();
        BlockPos pos = this.pressureTube.pos();

        if (!world.isRemote) {
            int oldGrateRange = this.grateRange;
            this.grateRange = this.getRange();
            this.pressureTube.getAirHandler(null).addAir((this.vacuum ? 1 : -1) * this.grateRange * PneumaticValues.USAGE_AIR_GRATE);
            if (oldGrateRange != this.grateRange) this.sendDescriptionPacket();
            this.coolHeatSinks();
        } else {
            if (this.resetRendering && this.grateRange > 0) {
                this.rangeLineRenderer.resetRendering(this.grateRange);
                this.resetRendering = false;
            }
            this.rangeLineRenderer.update();
        }

        this.pushEntities(world, pos, new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
    }

    private AxisAlignedBB getAffectedAABB() {
        return new AxisAlignedBB(this.pressureTube.pos()).grow(this.grateRange);
    }

    private void pushEntities(World world, BlockPos pos, Vec3d tileVec) {
        AxisAlignedBB bbBox = this.getAffectedAABB();
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, bbBox, this.entityFilter);
        double d0 = this.grateRange + 0.5D;
        for (Entity entity : entities) {
            if (!entity.world.isRemote && entity instanceof EntityItem && !entity.isDead
                    && entity.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 1D) {
                this.tryItemInsertion((EntityItem) entity);
            } else if (!entity.isSneaking() && (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode)) {
                Vec3d entityVec = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
                RayTraceResult trace = world.rayTraceBlocks(entityVec, tileVec, false, true, false);
                if (trace != null && trace.getBlockPos().equals(pos)) {
                    double d1 = (entity.posX - pos.getX() - 0.5D) / d0;
                    double d2 = (entity.posY + entity.getEyeHeight() - pos.getY() - 0.5D) / d0;
                    d2 -= 0.08;  // kludge: avoid entities getting stuck on edges, e.g. farmland->full block
                    double d3 = (entity.posZ - pos.getZ() - 0.5D) / d0;
                    double d4 = Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3);
                    double d5 = 1.0D - d4;

                    if (d5 > 0.0D) {
                        d5 *= d5;
                        if (!this.vacuum) d5 *= -1;
                        entity.motionX -= d1 / d4 * d5 * 0.1D;
                        entity.motionY -= d2 / d4 * d5 * 0.1D;
                        entity.motionZ -= d3 / d4 * d5 * 0.1D;
                        if (world.isRemote && world.rand.nextDouble() * 0.85 > d4) {
                            if (this.vacuum) {
                                PneumaticCraftRepressurized.proxy.playCustomParticle(EnumCustomParticleType.AIR_PARTICLE_DENSE, world,
                                        entity.posX, entity.posY, entity.posZ, -d1, -d2, -d3);
                            } else {
                                PneumaticCraftRepressurized.proxy.playCustomParticle(EnumCustomParticleType.AIR_PARTICLE_DENSE, world,
                                        pos.getX() + 0.5 + d1, pos.getY() + 0.5 + d2, pos.getZ() + 0.5 + d3, d1, d2, d3);
                            }
                        }
                    }
                }
            }
        }
    }

    private void tryItemInsertion(EntityItem entity) {
        if (this.getAdjacentInventory() != null) {
            ItemStack stack = entity.getItem();
            ItemStack excess = IOHelper.insert(this.getAdjacentInventory(), stack, this.adjacentInvSide, false);
            if (excess.isEmpty()) {
                entity.setDead();
            } else {
                entity.setItem(excess);
            }
        }
    }

    private TileEntity getAdjacentInventory() {
        if (this.adjacentInv != null && !this.adjacentInv.isInvalid()) {
            return this.adjacentInv;
        }

        this.adjacentInv = null;
        for (EnumFacing dir : EnumFacing.VALUES) {
            TileEntity inv = this.pressureTube.world().getTileEntity(this.pressureTube.pos().offset(dir));
            if (inv != null && inv.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
                this.adjacentInv = inv;
                this.adjacentInvSide = dir.getOpposite();
                break;
            }
        }
        return this.adjacentInv;
    }

    private void coolHeatSinks() {
        if (this.grateRange > 2) {
            int curTeIndex = (int) (this.pressureTube.world().getTotalWorldTime() % 27);
            BlockPos curPos = this.pressureTube.pos().offset(this.dir, 2).add(-1 + curTeIndex % 3, -1 + curTeIndex / 3 % 3, -1 + curTeIndex / 9 % 3);
            TileEntity te = this.pressureTube.world().getTileEntity(curPos);
            if (te instanceof TileEntityHeatSink) this.heatSinks.add((TileEntityHeatSink) te);

            Iterator<TileEntityHeatSink> iterator = this.heatSinks.iterator();
            while (iterator.hasNext()) {
                TileEntityHeatSink heatSink = iterator.next();
                if (heatSink.isInvalid()) {
                    iterator.remove();
                } else {
                    for (int i = 0; i < 4; i++)
                        heatSink.onFannedByAirGrate();
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.vacuum = tag.getBoolean("vacuum");
        this.grateRange = tag.getInteger("grateRange");
        String f = tag.getString("entityFilter");
        this.entityFilter = f.isEmpty() ? null : EntityFilter.fromString(f);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("vacuum", this.vacuum);
        tag.setInteger("grateRange", this.grateRange);
        tag.setString("entityFilter", this.entityFilter == null ? "" : this.entityFilter.toString());
    }

    @Override
    public String getType() {
        return Names.MODULE_AIR_GRATE;
    }

    @Override
    public void addInfo(List<String> curInfo) {
        super.addInfo(curInfo);
        curInfo.add("Status: " + TextFormatting.WHITE + (this.grateRange == 0 ? "Idle" : this.vacuum ? "Attracting" : "Repelling"));
        curInfo.add("Range: " + TextFormatting.WHITE + this.grateRange + " blocks");
        if (this.entityFilter != null)
            curInfo.add("Entity Filter: " + TextFormatting.WHITE + "\"" + this.entityFilter + "\"");
    }

    @Override
    protected EnumGuiId getGuiId() {
        return EnumGuiId.AIR_GRATE_MODULE;
    }

    @Override
    public Class<? extends ModelModuleBase> getModelClass() {
        return ModelAirGrate.class;
    }

    @Override
    public void doExtraRendering() {
        this.rangeLineRenderer.render();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getAffectedAABB();
    }

    public String getEntityFilterString() {
        return this.entityFilter == null ? "" : this.entityFilter.toString();
    }

    public void setEntityFilter(String filter) {
        this.entityFilter = EntityFilter.fromString(filter);
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand) {
        if (player.world.isRemote && this.rangeLineRenderer.isIdle()) {
            this.resetRendering = true;
        }
        return super.onActivated(player, hand);
    }
}
