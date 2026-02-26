package me.desht.pneumaticcraft.common.thirdparty;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public enum ModdedWrenchUtils {
    INSTANCE;

    @GameRegistry.ObjectHolder("thermalfoundation:wrench")
    private static final Item CRESCENT_HAMMER = Items.AIR;
    @GameRegistry.ObjectHolder("rftools:smartwrench")
    private static final Item SMART_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("immersiveengineering:tool")
    private static final Item IMMERSIVE_TOOL = Items.AIR;
    @GameRegistry.ObjectHolder("appliedenergistics2:certus_quartz_wrench")
    private static final Item AE2_CERTUS_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("appliedenergistics2:nether_quartz_wrench")
    private static final Item AE2_NETHER_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("enderio:item_yeta_wrench")
    private static final Item YETA_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("buildcraftcore:wrench")
    private static final Item BC_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("teslacorelib:wrench")
    private static final Item TESLA_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("ic2:wrench")
    private static final Item IC2_WRENCH = Items.AIR;
    @GameRegistry.ObjectHolder("chiselsandbits:wrench_wood")
    private static final Item CB_WRENCH_WOOD = Items.AIR;

    private final Set<String> wrenches = new HashSet<>();

    public static ModdedWrenchUtils getInstance() {
        return INSTANCE;
    }

    public void registerThirdPartyWrenches() {
        this.registerWrench(CRESCENT_HAMMER);
        this.registerWrench(SMART_WRENCH);
        this.registerWrench(IMMERSIVE_TOOL);
        this.registerWrench(AE2_CERTUS_WRENCH);
        this.registerWrench(AE2_NETHER_WRENCH);
        this.registerWrench(YETA_WRENCH);
        this.registerWrench(BC_WRENCH);
        this.registerWrench(TESLA_WRENCH);
        this.registerWrench(IC2_WRENCH);
        this.registerWrench(CB_WRENCH_WOOD);
    }

    private void registerWrench(Item wrench) {
        if (wrench != null) this.wrenches.add(makeWrenchKey(new ItemStack(wrench)));
    }

    private static String makeWrenchKey(ItemStack wrench) {
        return wrench.getItem().getRegistryName() + (getWrenchMeta(wrench) >= 0 ? ":" + wrench.getMetadata() : "");
    }

    private static int getWrenchMeta(ItemStack wrench) {
        if (wrench.getItem() == IMMERSIVE_TOOL) return 0;
        return -1;
    }

    /**
     * Check if the given item is a known 3rd party modded wrench
     *
     * @param stack the item to check
     * @return true if it's a modded wrench, false otherwise
     */
    public boolean isModdedWrench(@Nonnull ItemStack stack) {
        return this.wrenches.contains(makeWrenchKey(stack));
    }

}
