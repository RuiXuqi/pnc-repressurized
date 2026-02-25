package me.desht.pneumaticcraft.client.render.pneumatic_armor.block_tracker;

import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IBlockTrackEntry;
import me.desht.pneumaticcraft.client.gui.widget.GuiKeybindCheckBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.List;

public class BlockTrackEntryList {
    public final NonNullList<IBlockTrackEntry> trackList = NonNullList.create();

    public static final BlockTrackEntryList instance = new BlockTrackEntryList();

    // initialize default Block Track Entries.
    private BlockTrackEntryList() {
        this.trackList.add(new BlockTrackEntryHackable());
        this.trackList.add(new BlockTrackEntryInventory());
        this.trackList.add(new BlockTrackEntryFluid());
        this.trackList.add(new BlockTrackEntryEndPortalFrame());
        this.trackList.add(new BlockTrackEntryMobSpawner());
        this.trackList.add(new BlockTrackEntrySimple());
        this.trackList.add(new BlockTrackEntryEnergy());
    }

    public List<IBlockTrackEntry> getEntriesForCoordinate(IBlockAccess blockAccess, BlockPos pos, TileEntity te) {
        List<IBlockTrackEntry> blockTrackers = new ArrayList<>();
        for (IBlockTrackEntry entry : this.trackList) {
            if (GuiKeybindCheckBox.fromKeyBindingName(entry.getEntryName()).checked && entry.shouldTrackWithThisEntry(blockAccess, pos, blockAccess.getBlockState(pos), te))
                blockTrackers.add(entry);
        }
        return blockTrackers;
    }
}
