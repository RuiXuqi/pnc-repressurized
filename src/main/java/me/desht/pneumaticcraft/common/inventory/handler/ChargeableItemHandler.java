package me.desht.pneumaticcraft.common.inventory.handler;

import me.desht.pneumaticcraft.api.item.IUpgradeAcceptor;
import me.desht.pneumaticcraft.common.tileentity.TileEntityChargingStation;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import net.minecraft.item.ItemStack;

public class ChargeableItemHandler extends BaseItemStackHandler {
    private static final int INVENTORY_SIZE = 9;
    public static final String NBT_UPGRADE_TAG = "UpgradeInventory";

    public ChargeableItemHandler(TileEntityChargingStation te) {
        super(te, INVENTORY_SIZE);

        if (!NBTUtil.hasTag(this.getChargingStack(), NBT_UPGRADE_TAG)) {
            this.writeToNBT();
        }
        this.readFromNBT();
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        this.writeToNBT();
    }

    private ItemStack getChargingStack() {
        return ((TileEntityChargingStation) this.te).getChargingStack();
    }

    public void writeToNBT() {
        NBTUtil.setCompoundTag(this.getChargingStack(), NBT_UPGRADE_TAG, this.serializeNBT());
    }

    private void readFromNBT() {
        this.deserializeNBT(NBTUtil.getCompoundTag(this.getChargingStack(), NBT_UPGRADE_TAG));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack) {
        if (itemStack.isEmpty()) return true;

        return this.getChargingStack().getItem() instanceof IUpgradeAcceptor
                && ((IUpgradeAcceptor) this.getChargingStack().getItem()).getApplicableUpgrades().contains(itemStack.getItem());
    }
}
