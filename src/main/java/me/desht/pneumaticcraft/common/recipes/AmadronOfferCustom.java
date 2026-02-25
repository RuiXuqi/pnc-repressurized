package me.desht.pneumaticcraft.common.recipes;

import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import me.desht.pneumaticcraft.common.DroneRegistry;
import me.desht.pneumaticcraft.common.config.AmadronOfferSettings;
import me.desht.pneumaticcraft.common.inventory.ContainerAmadron;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketAmadronTradeNotifyDeal;
import me.desht.pneumaticcraft.common.network.PacketSyncAmadronOffers;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

public class AmadronOfferCustom extends AmadronOffer {
    private final String offeringPlayerName;
    private String offeringPlayerId;
    private int providingDimensionId, returningDimensionId;
    private BlockPos providingPosition, returningPosition;
    private int inStock;
    private int maxTrades = -1;
    private int pendingPayments;
    private TileEntity cachedInput, cachedOutput;

    public AmadronOfferCustom(Object input, Object output, EntityPlayer offeringPlayer) {
        this(input, output, offeringPlayer.getGameProfile().getName(), offeringPlayer.getGameProfile().getId().toString());
    }

    public AmadronOfferCustom(Object input, Object output, String playerName, String playerId) {
        super(input, output);
        this.offeringPlayerName = playerName;
        this.offeringPlayerId = playerId;
    }

    public AmadronOfferCustom setProvidingPosition(BlockPos pos, int dimensionId) {
        this.providingPosition = pos;
        this.providingDimensionId = dimensionId;
        this.cachedInput = null;
        return this;
    }

    public AmadronOfferCustom setReturningPosition(BlockPos pos, int dimensionId) {
        this.returningPosition = pos;
        this.returningDimensionId = dimensionId;
        this.cachedOutput = null;
        return this;
    }

    public AmadronOfferCustom invert() {
        Object temp = this.input;
        this.input = this.output;
        this.output = temp;
        return this;
    }

    public AmadronOfferCustom copy() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        return loadFromNBT(tag);
    }

    public void updatePlayerId() {
        EntityPlayer player = PneumaticCraftUtils.getPlayerFromName(this.offeringPlayerName);
        if (player != null) this.offeringPlayerId = player.getGameProfile().getId().toString();
    }

    public void addStock(int stock) {
        this.inStock += stock;
    }

    @Override
    public int getStock() {
        return this.inStock;
    }

    public void addPayment(int payment) {
        this.pendingPayments += payment;
    }

    public void setMaxTrades(int maxTrades) {
        this.maxTrades = maxTrades;
    }

    @Override
    public String getVendor() {
        return this.offeringPlayerName;
    }

    public String getPlayerId() {
        return this.offeringPlayerId;
    }

    @Override
    public void onTrade(int tradingAmount, String buyingPlayer) {
        EntityPlayer player = PneumaticCraftUtils.getPlayerFromId(this.offeringPlayerId);
        if (player != null && AmadronOfferSettings.notifyOfDealMade) {
            NetworkHandler.sendTo(new PacketAmadronTradeNotifyDeal(this, tradingAmount, buyingPlayer), (EntityPlayerMP) player);
        }
    }

    boolean payout() {
        boolean paid = false;
        TileEntity returning = this.getReturningTileEntity();
        if (this.pendingPayments > 0) {
            int paying = Math.min(this.pendingPayments, 50);
            paying = ContainerAmadron.capShoppingAmount(this, paying,
                    null, AmadronOfferManager.getItemHandler(returning),
                    null, AmadronOfferManager.getFluidHandler(returning),
                    null);
            if (paying > 0) {
                this.pendingPayments -= paying;
                paid = true;
                if (this.getInput() instanceof ItemStack) {
                    ItemStack deliveringItems = (ItemStack) this.getInput();
                    int amount = deliveringItems.getCount() * paying;
                    List<ItemStack> stacks = new ArrayList<>();
                    while (amount > 0) {
                        ItemStack stack = deliveringItems.copy();
                        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
                        stacks.add(stack);
                        amount -= stack.getCount();
                    }
                    DroneRegistry.getInstance().deliverItemsAmazonStyle(returning.getWorld(), returning.getPos(), stacks.toArray(new ItemStack[0]));
                } else {
                    FluidStack deliveringFluid = ((FluidStack) this.getInput()).copy();
                    deliveringFluid.amount *= paying;
                    DroneRegistry.getInstance().deliverFluidAmazonStyle(returning.getWorld(), returning.getPos(), deliveringFluid);
                }
            }
        }
        return paid;
    }

    public void returnStock() {
        TileEntity provider = this.getProvidingTileEntity();
        TileEntity returning = this.getReturningTileEntity();
        this.invert();
        while (this.inStock > 0) {
            int stock = Math.min(this.inStock, 50);
            stock = ContainerAmadron.capShoppingAmount(this, stock,
                    AmadronOfferManager.getItemHandler(returning), AmadronOfferManager.getItemHandler(provider),
                    AmadronOfferManager.getFluidHandler(returning), AmadronOfferManager.getFluidHandler(provider),
                    null);
            if (stock > 0) {
                this.inStock -= stock;
                if (this.getInput() instanceof ItemStack) {
                    ItemStack deliveringItems = (ItemStack) this.getInput();
                    int amount = deliveringItems.getCount() * stock;
                    List<ItemStack> stacks = new ArrayList<>();
                    while (amount > 0) {
                        ItemStack stack = deliveringItems.copy();
                        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
                        stacks.add(stack);
                        amount -= stack.getCount();
                    }
                    DroneRegistry.getInstance().deliverItemsAmazonStyle(provider.getWorld(), provider.getPos(), stacks.toArray(new ItemStack[0]));
                } else {
                    FluidStack deliveringFluid = ((FluidStack) this.getInput()).copy();
                    deliveringFluid.amount *= stock;
                    DroneRegistry.getInstance().deliverFluidAmazonStyle(provider.getWorld(), provider.getPos(), deliveringFluid);
                }
            } else {
                break;
            }
        }
    }

    public TileEntity getProvidingTileEntity() {
        if (this.cachedInput == null || this.cachedInput.isInvalid()) {
            if (this.providingPosition != null) {
                this.cachedInput = PneumaticCraftUtils.getTileEntity(this.providingPosition, this.providingDimensionId);
            }
        }
        return this.cachedInput;
    }

    public TileEntity getReturningTileEntity() {
        if (this.cachedOutput == null || this.cachedOutput.isInvalid()) {
            if (this.returningPosition != null) {
                this.cachedOutput = PneumaticCraftUtils.getTileEntity(this.returningPosition, this.returningDimensionId);
            }
        }
        return this.cachedOutput;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("offeringPlayerId", this.offeringPlayerId);
        tag.setString("offeringPlayerName", this.offeringPlayerName);
        tag.setInteger("inStock", this.inStock);
        tag.setInteger("maxTrades", this.maxTrades);
        tag.setInteger("pendingPayments", this.pendingPayments);
        if (this.providingPosition != null) {
            tag.setInteger("providingDimensionId", this.providingDimensionId);
            tag.setInteger("providingX", this.providingPosition.getX());
            tag.setInteger("providingY", this.providingPosition.getY());
            tag.setInteger("providingZ", this.providingPosition.getZ());
        }
        if (this.returningPosition != null) {
            tag.setInteger("returningDimensionId", this.returningDimensionId);
            tag.setInteger("returningX", this.returningPosition.getX());
            tag.setInteger("returningY", this.returningPosition.getY());
            tag.setInteger("returningZ", this.returningPosition.getZ());
        }
    }

    public static AmadronOfferCustom loadFromNBT(NBTTagCompound tag) {
        AmadronOffer offer = AmadronOffer.loadFromNBT(tag);
        AmadronOfferCustom custom = new AmadronOfferCustom(offer.getInput(), offer.getOutput(), tag.getString("offeringPlayerName"), tag.getString("offeringPlayerId"));
        custom.inStock = tag.getInteger("inStock");
        custom.maxTrades = tag.getInteger("maxTrades");
        custom.pendingPayments = tag.getInteger("pendingPayments");
        if (tag.hasKey("providingDimensionId")) {
            custom.setProvidingPosition(new BlockPos(tag.getInteger("providingX"), tag.getInteger("providingY"), tag.getInteger("providingZ")), tag.getInteger("providingDimensionId"));
        }
        if (tag.hasKey("returningDimensionId")) {
            custom.setReturningPosition(new BlockPos(tag.getInteger("returningX"), tag.getInteger("returningY"), tag.getInteger("returningZ")), tag.getInteger("returningDimensionId"));
        }
        return custom;
    }

    public void writeToBuf(ByteBuf buf) {
        super.writeToBuf(buf);
        ByteBufUtils.writeUTF8String(buf, this.offeringPlayerName);
        ByteBufUtils.writeUTF8String(buf, this.offeringPlayerId);
        if (this.providingPosition != null) {
            buf.writeBoolean(true);
            buf.writeInt(this.providingPosition.getX());
            buf.writeInt(this.providingPosition.getY());
            buf.writeInt(this.providingPosition.getZ());
            buf.writeInt(this.providingDimensionId);
        } else {
            buf.writeBoolean(false);
        }
        if (this.returningPosition != null) {
            buf.writeBoolean(true);
            buf.writeInt(this.returningPosition.getX());
            buf.writeInt(this.returningPosition.getY());
            buf.writeInt(this.returningPosition.getZ());
            buf.writeInt(this.returningDimensionId);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(this.inStock);
        buf.writeInt(this.maxTrades);
        buf.writeInt(this.pendingPayments);
    }

    public static AmadronOfferCustom loadFromBuf(ByteBuf buf) {
        AmadronOfferCustom offer = new AmadronOfferCustom(PacketSyncAmadronOffers.readFluidOrItemStack(buf), PacketSyncAmadronOffers.readFluidOrItemStack(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        if (buf.readBoolean()) {
            offer.setProvidingPosition(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()), buf.readInt());
        }
        if (buf.readBoolean()) {
            offer.setReturningPosition(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()), buf.readInt());
        }
        offer.inStock = buf.readInt();
        offer.maxTrades = buf.readInt();
        offer.pendingPayments = buf.readInt();
        return offer;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("offeringPlayerName", this.offeringPlayerName);
        json.addProperty("offeringPlayerId", this.offeringPlayerId);
        json.addProperty("inStock", this.inStock);
        json.addProperty("maxTrades", this.maxTrades);
        json.addProperty("pendingPayments", this.pendingPayments);
        if (this.providingPosition != null) {
            json.addProperty("providingDimensionId", this.providingDimensionId);
            json.addProperty("providingX", this.providingPosition.getX());
            json.addProperty("providingY", this.providingPosition.getY());
            json.addProperty("providingZ", this.providingPosition.getZ());
        }
        if (this.returningPosition != null) {
            json.addProperty("returningDimensionId", this.returningDimensionId);
            json.addProperty("returningX", this.returningPosition.getX());
            json.addProperty("returningY", this.returningPosition.getY());
            json.addProperty("returningZ", this.returningPosition.getZ());
        }
        return json;
    }

    public static AmadronOfferCustom fromJson(JsonObject json) {
        AmadronOffer offer = AmadronOffer.fromJson(json);
        if (offer != null) {
            AmadronOfferCustom custom = new AmadronOfferCustom(offer.input, offer.output, json.get("offeringPlayerName").getAsString(), json.get("offeringPlayerId").getAsString());
            custom.inStock = json.get("inStock").getAsInt();
            custom.maxTrades = json.get("maxTrades").getAsInt();
            custom.pendingPayments = json.get("pendingPayments").getAsInt();
            if (json.has("providingDimensionId")) {
                custom.providingDimensionId = json.get("providingDimensionId").getAsInt();
                custom.providingPosition = new BlockPos(json.get("providingX").getAsInt(), json.get("providingY").getAsInt(), json.get("providingZ").getAsInt());
            }
            if (json.has("returningDimensionId")) {
                custom.returningDimensionId = json.get("returningDimensionId").getAsInt();
                custom.returningPosition = new BlockPos(json.get("returningX").getAsInt(), json.get("returningY").getAsInt(), json.get("returningZ").getAsInt());
            }
            return custom;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " - " + this.offeringPlayerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AmadronOfferCustom) {
            AmadronOfferCustom offer = (AmadronOfferCustom) o;
            return super.equals(o) && offer.offeringPlayerId.equals(this.offeringPlayerId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + this.offeringPlayerId.hashCode();
    }
}
