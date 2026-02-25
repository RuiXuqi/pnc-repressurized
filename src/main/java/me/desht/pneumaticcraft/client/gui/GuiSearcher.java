package me.desht.pneumaticcraft.client.gui;

import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiHelmetMainScreen;
import me.desht.pneumaticcraft.common.inventory.ContainerSearcher;
import me.desht.pneumaticcraft.common.item.Itemss;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SideOnly(Side.CLIENT)
public class GuiSearcher extends InventoryEffectRenderer {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(Textures.GUI_ITEM_SEARCHER_LOCATION);
    private static final ResourceLocation SCROLL_TEXTURE = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static List<SearchEntry> cachedSearchEntries;
    //    private final InventoryBasic inventory = new InventoryBasic("tmp", true, 49);
    private final ItemStackHandler inventory = new ItemStackHandler(49);
    private final GuiScreen parentScreen;

    /**
     * Amount scrolled in Creative mode inventory (0 = top, 1 = bottom)
     */
    private float currentScroll;

    /**
     * True if the scrollbar is being dragged
     */
    private boolean isScrolling;

    /**
     * True if the left mouse button was held down last time drawScreen was called.
     */
    private boolean wasClicking;
    private GuiTextField searchField;
    private boolean firstRun = true;

    /**
     * Used to back up the ContainerSearcher's inventory slots before filling it with the player's inventory slots for
     * the inventory tab.
     */
    //private List backupContainerSlots;
    private boolean field_74234_w;

    public GuiSearcher(EntityPlayer par1EntityPlayer) {
        super(new ContainerSearcher());
        par1EntityPlayer.openContainer = this.inventorySlots;
        this.allowUserInput = true;
        this.ySize = 176;
        this.parentScreen = FMLClientHandler.instance().getClient().currentScreen;
        ((ContainerSearcher) this.inventorySlots).init(this);
    }

    @Nonnull
    public ItemStack getSearchStack() {
        return this.inventory.getStackInSlot(48);
    }

    public void setSearchStack(@Nonnull ItemStack stack) {
        this.inventory.setStackInSlot(48, stack);
    }

    @Override
    protected void handleMouseClick(Slot par1Slot, int par2, int par3, ClickType par4) {
        if (par1Slot != null) {
            if (par1Slot.slotNumber == 48) {
                par1Slot.putStack(ItemStack.EMPTY);
            } else {
                this.inventory.setStackInSlot(48, par1Slot.getStack());
            }
        }
    }

    /**
     * Adds the buttons (and other controls) to the screen in question.
     */
    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        this.searchField = new GuiTextField(-1, this.fontRenderer, this.guiLeft + 20, this.guiTop + 36, 89, this.fontRenderer.FONT_HEIGHT);
        this.searchField.setMaxStringLength(15);
        this.searchField.setEnableBackgroundDrawing(true);
        this.searchField.setVisible(true);
        this.searchField.setFocused(true);
        this.searchField.setTextColor(16777215);

        this.updateCreativeSearch();
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        if (this.parentScreen instanceof GuiHelmetMainScreen) {
            if (player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() != Itemss.PNEUMATIC_HELMET) {
                this.mc.displayGuiScreen(this.parentScreen);
                this.onGuiClosed();
            }
        }
    }

    /**
     * Fired when a key is typed. This is the equivalent of KeyListener.keyTyped(KeyEvent e).
     */
    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        if (this.field_74234_w) {
            this.field_74234_w = false;
            this.searchField.setText("");
        }

        if (par2 == 1)//esc
        {
            this.mc.displayGuiScreen(this.parentScreen);
            this.onGuiClosed();
        } else {
            if (this.searchField.textboxKeyTyped(par1, par2)) {
                this.updateCreativeSearch();
            } else {
                super.keyTyped(par1, par2);
            }
        }
    }

    private void getAllEnchantedBooks(Enchantment enchantment, NonNullList<ItemStack> list) {
        for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i) {
            ItemStack itemstack = new ItemStack(Items.ENCHANTED_BOOK);
            itemstack.addEnchantment(enchantment, i);
            list.add(itemstack);
        }
    }

    /**
     * Lazy cache.
     *
     * @return
     */
    private Stream<SearchEntry> getSearchEntries() {
        if (cachedSearchEntries == null) {
            NonNullList<ItemStack> itemList = NonNullList.create();

            for (Item item : Item.REGISTRY) {
                if (item != null && item.getCreativeTab() != null) {
                    item.getSubItems(item.getCreativeTab(), itemList);
                }
            }

            for (Enchantment enchantment : Enchantment.REGISTRY) {
                if (enchantment != null && enchantment.type != null) {
                    this.getAllEnchantedBooks(enchantment, itemList);
                }
            }

            cachedSearchEntries = itemList.stream().map(SearchEntry::new).collect(Collectors.toList());
        }
        return cachedSearchEntries.stream();
    }

    private void updateCreativeSearch() {
        ContainerSearcher containerSearcher = (ContainerSearcher) this.inventorySlots;
        containerSearcher.itemList.clear();

        String s = this.searchField.getText().toLowerCase();

        List<ItemStack> applicableEntries = this.getSearchEntries()
                .filter(entry -> entry.test(s))
                .map(entry -> entry.stack)
                .collect(Collectors.toList());

        containerSearcher.itemList.addAll(applicableEntries);

        this.currentScroll = 0.0F;
        containerSearcher.scrollTo(0.0F);
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        this.fontRenderer.drawString("Item Searcher", 23, 5, 4210752);
        this.fontRenderer.drawString("Search Box", 23, 25, 4210752);
        this.fontRenderer.drawString("Target", 113, 10, 4210752);
    }

    /**
     * returns (if you are not on the inventoryTab) and (the flag isn't set) and( you have more than 1 page of items)
     */
    private boolean needsScrollBars() {
        return ((ContainerSearcher) this.inventorySlots).hasMoreThan1PageOfItemsInList();
    }

    /**
     * Handles mouse input.
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if (i != 0 && this.needsScrollBars()) {
            int j = ((ContainerSearcher) this.inventorySlots).itemList.size() / 9 - 5 + 1;

            if (i > 0) {
                i = 1;
            }

            if (i < 0) {
                i = -1;
            }

            this.currentScroll = (float) (this.currentScroll - (double) i / (double) j);

            if (this.currentScroll < 0.0F) {
                this.currentScroll = 0.0F;
            }

            if (this.currentScroll > 1.0F) {
                this.currentScroll = 1.0F;
            }

            ((ContainerSearcher) this.inventorySlots).scrollTo(this.currentScroll);
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3) {
        this.drawDefaultBackground();

        boolean flag = Mouse.isButtonDown(0);
        int k = this.guiLeft;
        int l = this.guiTop;
        int i1 = k + 156;
        int j1 = l + 48;
        int k1 = i1 + 14;
        int l1 = j1 + 112;
        if (!this.wasClicking && flag && par1 >= i1 && par2 >= j1 && par1 < k1 && par2 < l1) {
            this.isScrolling = this.needsScrollBars();
        }

        if (!flag) {
            this.isScrolling = false;
        }

        this.wasClicking = flag;

        if (this.isScrolling) {
            this.currentScroll = (par2 - j1 - 7.5F) / (l1 - j1 - 15.0F);

            if (this.currentScroll < 0.0F) {
                this.currentScroll = 0.0F;
            }

            if (this.currentScroll > 1.0F) {
                this.currentScroll = 1.0F;
            }

            ((ContainerSearcher) this.inventorySlots).scrollTo(this.currentScroll);
        }
        if (this.firstRun) {
            this.firstRun = false;
            ((ContainerSearcher) this.inventorySlots).scrollTo(0);
        }

        super.drawScreen(par1, par2, par3);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableLighting();

        this.renderHoveredToolTip(par1, par2);
    }

    /**
     * Draw the background layer for the GuiContainer (everything behind the items)
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
        int xStart = (this.width - this.xSize) / 2;
        int yStart = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xStart, yStart, 0, 0, this.xSize, this.ySize);
        this.searchField.drawTextBox();

        int i1 = this.guiLeft + 156;
        int k = this.guiTop + 48;
        int l = k + 112;
        this.mc.getTextureManager().bindTexture(SCROLL_TEXTURE);
        this.drawTexturedModalRect(i1, k + (int) ((l - k - 17) * this.currentScroll), 232 + (this.needsScrollBars() ? 0 : 12), 0, 12, 15);

    }

    /**
     * Returns the creative inventory
     */
    public IItemHandlerModifiable getInventory() {
        return this.inventory;
    }

    public class SearchEntry implements Predicate<String> {
        public final ItemStack stack;
        private final String tooltip;

        public SearchEntry(ItemStack stack) {
            this.stack = stack;
            List<String> t = stack.getTooltip(GuiSearcher.this.mc.player, GuiSearcher.this.mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
            this.tooltip = StringUtils.join(t, "\n").toLowerCase();
        }

        @Override
        public boolean test(String searchString) {
            return this.tooltip.contains(searchString);
        }
    }
}
