package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.common.DroneRegistry;
import me.desht.pneumaticcraft.common.GuiHandler.EnumGuiId;
import me.desht.pneumaticcraft.common.config.AmadronOfferSettings;
import me.desht.pneumaticcraft.common.config.AmadronOfferStaticConfig;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.item.ItemAmadronTablet;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeRemoved;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.recipes.AmadronOffer;
import me.desht.pneumaticcraft.common.recipes.AmadronOfferCustom;
import me.desht.pneumaticcraft.common.recipes.AmadronOfferManager;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Log;
import me.desht.pneumaticcraft.lib.Names;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class ContainerAmadron extends ContainerPneumaticBase {
    public static final int ROWS = 4;
    public static final int OFFERS_PER_PAGE = ROWS * 2;

    // Set client-side when a PacketSyncAmadronOffers is received.  Only controls button visibility, actual control
    // is done server-side via permissions API.  So make button available unless server tells us no.  Worst that
    // can happen is button does nothing.
    public static boolean mayAddPeriodicOffers = true;
    public static boolean mayAddStaticOffers = true;

    public final List<AmadronOffer> offers = new ArrayList<>(AmadronOfferManager.getInstance().getAllOffers());

    private final ItemStackHandler inv = new ItemStackHandler(OFFERS_PER_PAGE * 2);

    @GuiSynced
    private final int[] shoppingItems = new int[OFFERS_PER_PAGE];
    @GuiSynced
    private final int[] shoppingAmounts = new int[OFFERS_PER_PAGE];
    @GuiSynced
    public final boolean[] buyableOffers = new boolean[this.offers.size()];
    @GuiSynced
    public EnumProblemState problemState = EnumProblemState.NO_PROBLEMS;
    @GuiSynced
    public int maxOffers = 0;
    @GuiSynced
    public int currentOffers = 0;
    @GuiSynced
    private boolean basketEmpty = true;

    public enum EnumProblemState {
        NO_PROBLEMS("noProblems"),
        NO_ITEM_PROVIDER("noItemProvider"),
        NO_FLUID_PROVIDER("noFluidProvider"),
        NOT_ENOUGH_ITEM_SPACE("notEnoughItemSpace"),
        NOT_ENOUGH_FLUID_SPACE("notEnoughFluidSpace"),
        NOT_ENOUGH_ITEMS("notEnoughItems") /*not a ChickenBones reference*/,
        NOT_ENOUGH_FLUID("notEnoughFluid"),
        OUT_OF_STOCK("outOfStock");

        private final String locKey;

        EnumProblemState(String locKey) {
            this.locKey = locKey;
        }

        public String getLocalizationKey() {
            return "gui.tab.problems.amadron." + this.locKey;
        }
    }

    public ContainerAmadron(EntityPlayer player) {
        super(null);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < 2; x++) {
                this.addSlotToContainer(new SlotUntouchable(this.inv, y * 4 + x * 2, x * 73 + 12, y * 35 + 70));
                this.addSlotToContainer(new SlotUntouchable(this.inv, y * 4 + x * 2 + 1, x * 73 + 57, y * 35 + 70));
            }
        }
        this.addSyncedFields(this);
        Arrays.fill(this.shoppingItems, -1);

        if (!player.world.isRemote) {
            IItemHandler itemHandler = ItemAmadronTablet.getItemProvider(player.getHeldItemMainhand());
            IFluidHandler fluidHandler = ItemAmadronTablet.getLiquidProvider(player.getHeldItemMainhand());
            for (int i = 0; i < this.offers.size(); i++) {
                int amount = capShoppingAmount(this.offers.get(i), 1, itemHandler, fluidHandler, this);
                this.buyableOffers[i] = amount > 0;
            }
            this.problemState = EnumProblemState.NO_PROBLEMS;

            Map<AmadronOffer, Integer> shoppingCart = ItemAmadronTablet.getShoppingCart(player.getHeldItemMainhand());
            for (Map.Entry<AmadronOffer, Integer> cartItem : shoppingCart.entrySet()) {
                int offerId = this.offers.indexOf(cartItem.getKey());
                if (offerId >= 0) {
                    int index = this.getCartSlot(offerId);
                    if (index >= 0) {
                        this.shoppingItems[index] = offerId;
                        this.shoppingAmounts[index] = cartItem.getValue();
                    }
                }
            }
            this.basketEmpty = Arrays.stream(this.shoppingAmounts).noneMatch(shoppingAmount -> shoppingAmount > 0);
            this.currentOffers = AmadronOfferManager.getInstance().countOffers(player.getGameProfile().getId().toString());
            this.maxOffers = PneumaticCraftUtils.isPlayerOp(player) ? Integer.MAX_VALUE : AmadronOfferSettings.maxTradesPerPlayer;
        }
    }

    public boolean isBasketEmpty() {
        return this.basketEmpty;
    }

    @Override
    public void putStackInSlot(int slot, @Nonnull ItemStack stack) {
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player.getHeldItemMainhand().getItem() == Itemss.AMADRON_TABLET) {
            IPressurizable pressurizable = (IPressurizable) Itemss.AMADRON_TABLET;
            pressurizable.addAir(player.getHeldItemMainhand(), -1);
            if (pressurizable.getPressure(player.getHeldItemMainhand()) > 0) {
                return true;
            } else {
                player.sendStatusMessage(new TextComponentTranslation("gui.tab.problems.notEnoughPressure"), false);
            }
        }
        return false;
    }

    public void clearStacks() {
        for (int i = 0; i < this.inv.getSlots(); i++) {
            this.inv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void setStack(int index, ItemStack stack) {
        this.inv.setStackInSlot(index, stack);
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer p_82846_1_, int p_82846_2_) {
        return ItemStack.EMPTY;
    }

    public void clickOffer(int offerId, int mouseButton, boolean sneaking, EntityPlayer player) {
        this.problemState = EnumProblemState.NO_PROBLEMS;
        int cartSlot = this.getCartSlot(offerId);
        if (cartSlot >= 0) {
            if (mouseButton == 2) {  // middle-click
                this.shoppingAmounts[cartSlot] = 0;
            } else if (sneaking) {
                if (mouseButton == 0) { // sneak-left-click
                    this.shoppingAmounts[cartSlot] /= 2;
                } else { // sneak-right-click
                    AmadronOffer offer = this.offers.get(offerId);
                    if (offer instanceof AmadronOfferCustom) {
                        this.removeCustomOffer(player, (AmadronOfferCustom) offer);
                    } else {
                        this.shoppingAmounts[cartSlot] *= 2;
                        if (this.shoppingAmounts[cartSlot] == 0) this.shoppingAmounts[cartSlot] = 1;
                    }
                }
            } else { // left or right-click
                if (mouseButton == 0) this.shoppingAmounts[cartSlot]--;
                else this.shoppingAmounts[cartSlot]++;
            }
            if (this.shoppingAmounts[cartSlot] <= 0) {
                this.shoppingAmounts[cartSlot] = 0;
                this.shoppingItems[cartSlot] = -1;
            } else {
                this.shoppingAmounts[cartSlot] = this.capShoppingAmount(offerId, this.shoppingAmounts[cartSlot], player);
                this.shoppingItems[cartSlot] = this.shoppingAmounts[cartSlot] > 0 ? offerId : -1;
            }
        }
        this.basketEmpty = Arrays.stream(this.shoppingAmounts).noneMatch(shoppingAmount -> shoppingAmount > 0);
    }

    private void removeCustomOffer(EntityPlayer player, AmadronOfferCustom offer) {
        if (offer.getPlayerId().equals(player.getGameProfile().getId().toString())) {
            if (AmadronOfferManager.getInstance().removeStaticOffer(offer)) {
                if (AmadronOfferSettings.notifyOfTradeRemoval)
                    NetworkHandler.sendToAll(new PacketAmadronTradeRemoved(offer));
                offer.returnStock();
                try {
                    AmadronOfferStaticConfig.INSTANCE.writeToFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                player.closeScreen();
            }
        }
    }

    @Override
    public void handleGUIButtonPress(int guiID, EntityPlayer player) {
        super.handleGUIButtonPress(guiID, player);
        if (guiID == 1) {
            boolean placed = false;
            for (int i = 0; i < this.shoppingItems.length; i++) {
                if (this.shoppingItems[i] >= 0) {
                    AmadronOffer offer = this.offers.get(this.shoppingItems[i]);
                    BlockPos itemPos = ItemAmadronTablet.getItemProvidingLocation(player.getHeldItemMainhand());
                    World itemWorld;
                    if (itemPos == null) {
                        itemPos = new BlockPos((int) player.posX, (int) player.posY, (int) player.posZ);
                        itemWorld = player.world;
                    } else {
                        itemWorld = DimensionManager.getWorld(ItemAmadronTablet.getItemProvidingDimension(player.getHeldItemMainhand()));
                    }
                    BlockPos liquidPos = ItemAmadronTablet.getLiquidProvidingLocation(player.getHeldItemMainhand());
                    World liquidWorld = null;
                    if (liquidPos != null) {
                        liquidWorld = DimensionManager.getWorld(ItemAmadronTablet.getLiquidProvidingDimension(player.getHeldItemMainhand()));
                    }
                    EntityDrone drone = retrieveOrderItems(offer, this.shoppingAmounts[i], itemWorld, itemPos, liquidWorld, liquidPos);
                    if (drone != null) {
                        drone.setHandlingOffer(offer, this.shoppingAmounts[i], player.getHeldItemMainhand(), player.getName());
                        placed = true;
                    }
                }
                if (placed && player instanceof EntityPlayerMP) {
                    NetworkHandler.sendTo(new PacketPlaySound(Sounds.CHIRP, SoundCategory.PLAYERS, player.posX, player.posY, player.posZ, 0.2f, 1.0f, false), (EntityPlayerMP) player);
                }
            }
            Arrays.fill(this.shoppingAmounts, 0);
            Arrays.fill(this.shoppingItems, -1);
            this.basketEmpty = true;
        } else if (guiID == 2) {
            player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.AMADRON_ADD_PLAYER_TRADE.ordinal(), player.world, 0, 0, 0);
        } else if (guiID == 3 && PermissionAPI.hasPermission(player, Names.AMADRON_ADD_PERIODIC_TRADE)) {
            player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.AMADRON_ADD_PERIODIC_TRADE.ordinal(), player.world, 0, 0, 0);
        } else if (guiID == 4 && PermissionAPI.hasPermission(player, Names.AMADRON_ADD_STATIC_TRADE)) {
            player.openGui(PneumaticCraftRepressurized.instance, EnumGuiId.AMADRON_ADD_STATIC_TRADE.ordinal(), player.world, 0, 0, 0);
        }
    }

    public static EntityDrone retrieveOrderItems(AmadronOffer offer, int times, World itemWorld, BlockPos itemPos, World liquidWorld, BlockPos liquidPos) {
        if (offer.getInput() instanceof ItemStack) {
            if (itemWorld == null || itemPos == null) return null;
            ItemStack queryingItems = (ItemStack) offer.getInput();
            int amount = queryingItems.getCount() * times;
            NonNullList<ItemStack> stacks = NonNullList.create();
            while (amount > 0) {
                ItemStack stack = queryingItems.copy();
                stack.setCount(Math.min(amount, stack.getMaxStackSize()));
                stacks.add(stack);
                amount -= stack.getCount();
            }
            if (stacks.isEmpty()) {
                // shouldn't happen but see https://github.com/TeamPneumatic/pnc-repressurized/issues/399
                Log.error(String.format("retrieveOrderItems: got empty itemstack list for offer %d x %s @ %s", times, queryingItems, itemPos));
                return null;
            }
            return (EntityDrone) DroneRegistry.getInstance().retrieveItemsAmazonStyle(itemWorld, itemPos, stacks.toArray(new ItemStack[0]));
        } else {
            if (liquidWorld == null || liquidPos == null) return null;
            FluidStack queryingFluid = ((FluidStack) offer.getInput()).copy();
            queryingFluid.amount *= times;
            return (EntityDrone) DroneRegistry.getInstance().retrieveFluidAmazonStyle(liquidWorld, liquidPos, queryingFluid);
        }
    }

    private int capShoppingAmount(int offerId, int wantedAmount, EntityPlayer player) {
        IItemHandler inv = ItemAmadronTablet.getItemProvider(player.getHeldItemMainhand());
        IFluidHandler fluidHandler = ItemAmadronTablet.getLiquidProvider(player.getHeldItemMainhand());
        return capShoppingAmount(this.offers.get(offerId), wantedAmount, inv, fluidHandler, this);
    }

    private static int capShoppingAmount(AmadronOffer offer, int wantedAmount, IItemHandler inv, IFluidHandler fluidHandler, ContainerAmadron container) {
        return capShoppingAmount(offer, wantedAmount, inv, inv, fluidHandler, fluidHandler, container);
    }

    public static int capShoppingAmount(AmadronOffer offer, int wantedAmount, IItemHandler inputInv, IItemHandler outputInv,
                                        IFluidHandler inputFluidHandler, IFluidHandler outputFluidHandler, ContainerAmadron container) {
        if (container != null && offer.getStock() >= 0 && wantedAmount > offer.getStock()) {
            wantedAmount = offer.getStock();
            container.problemState = EnumProblemState.OUT_OF_STOCK;
        }
        if (offer.getInput() instanceof ItemStack) {
            if (inputInv != null) {
                ItemStack searchingItem = (ItemStack) offer.getInput();
                int count = 0;
                for (int i = 0; i < inputInv.getSlots(); i++) {
                    if (inputInv.getStackInSlot(i).isItemEqual(searchingItem) && ItemStack.areItemStackTagsEqual(inputInv.getStackInSlot(i), searchingItem)) {
                        count += inputInv.getStackInSlot(i).getCount();
                    }
                }
                int maxAmount = count / ((ItemStack) offer.getInput()).getCount();
                if (wantedAmount > maxAmount) {
                    if (container != null) container.problemState = EnumProblemState.NOT_ENOUGH_ITEMS;
                    wantedAmount = maxAmount;
                }
            } else if (outputInv == null) {
                wantedAmount = 0;
                if (container != null) container.problemState = EnumProblemState.NO_ITEM_PROVIDER;
            }
        } else {
            if (inputFluidHandler != null) {
                FluidStack searchingFluid = ((FluidStack) offer.getInput()).copy();
                searchingFluid.amount = Integer.MAX_VALUE;
                FluidStack extracted = inputFluidHandler.drain(searchingFluid, false);
                int maxAmount = 0;
                if (extracted != null) maxAmount = extracted.amount / ((FluidStack) offer.getInput()).amount;
                if (wantedAmount > maxAmount) {
                    if (container != null) container.problemState = EnumProblemState.NOT_ENOUGH_FLUID;
                    wantedAmount = maxAmount;
                }
            } else if (outputFluidHandler == null) {
                wantedAmount = 0;
                if (container != null) container.problemState = EnumProblemState.NO_FLUID_PROVIDER;
            }
        }
        if (offer.getOutput() instanceof ItemStack) {
            if (outputInv != null) {
                ItemStack providingItem = ((ItemStack) offer.getOutput()).copy();
                providingItem.setCount(providingItem.getCount() * wantedAmount);
                ItemStack remainder = ItemHandlerHelper.insertItem(outputInv, providingItem.copy(), true);
                if (!remainder.isEmpty()) {
                    int maxAmount = (providingItem.getCount() - remainder.getCount()) / ((ItemStack) offer.getOutput()).getCount();
                    if (wantedAmount > maxAmount) {
                        wantedAmount = maxAmount;
                        if (container != null) container.problemState = EnumProblemState.NOT_ENOUGH_ITEM_SPACE;
                    }
                }
            } else if (inputInv == null) {
                wantedAmount = 0;
                if (container != null) container.problemState = EnumProblemState.NO_ITEM_PROVIDER;
            }
        } else {
            if (outputFluidHandler != null) {
                FluidStack providingFluid = ((FluidStack) offer.getOutput()).copy();
                providingFluid.amount *= wantedAmount;
                int amountFilled = outputFluidHandler.fill(providingFluid, false);
                int maxAmount = amountFilled / ((FluidStack) offer.getOutput()).amount;
                if (wantedAmount > maxAmount) {
                    wantedAmount = maxAmount;
                    if (container != null) container.problemState = EnumProblemState.NOT_ENOUGH_FLUID_SPACE;
                }
            } else if (inputFluidHandler == null) {
                wantedAmount = 0;
                if (container != null) container.problemState = EnumProblemState.NO_FLUID_PROVIDER;
            }
        }
        return wantedAmount;
    }

    private int getCartSlot(int offerId) {
        int freeSlot = -1;
        for (int i = 0; i < this.shoppingItems.length; i++) {
            if (this.shoppingItems[i] == offerId) {
                return i;
            } else if (freeSlot == -1 && this.shoppingItems[i] == -1) {
                freeSlot = i;
            }
        }
        return freeSlot;
    }

    public int getShoppingCartAmount(AmadronOffer offer) {
        int offerId = this.offers.indexOf(offer);
        for (int i = 0; i < this.shoppingItems.length; i++) {
            if (this.shoppingItems[i] == offerId) {
                return this.shoppingAmounts[i];
            }
        }
        return 0;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote && player.getHeldItemMainhand().getItem() == Itemss.AMADRON_TABLET) {
            Map<AmadronOffer, Integer> shoppingCart = new HashMap<>();
            for (int i = 0; i < this.shoppingItems.length; i++) {
                if (this.shoppingItems[i] >= 0) {
                    shoppingCart.put(this.offers.get(this.shoppingItems[i]), this.shoppingAmounts[i]);
                }
            }
            ItemAmadronTablet.setShoppingCart(player.getHeldItemMainhand(), shoppingCart);
        }
    }
}
