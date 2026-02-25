package me.desht.pneumaticcraft.client.render.pneumatic_armor;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IEntityTrackEntry;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IHackableEntity;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiDroneDebuggerOptions;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat.StatIcon;
import me.desht.pneumaticcraft.client.render.RenderProgressBar;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.entity_tracker.EntityTrackHandler;
import me.desht.pneumaticcraft.common.entity.living.EntityDrone;
import me.desht.pneumaticcraft.common.hacking.HackableHandler;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketHackingEntityStart;
import me.desht.pneumaticcraft.common.network.PacketUpdateDebuggingDrone;
import me.desht.pneumaticcraft.common.util.NBTUtil;
import me.desht.pneumaticcraft.lib.NBTKeys;
import me.desht.pneumaticcraft.lib.Sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class RenderEntityTarget {

    public final Entity entity;
    private final RenderTargetCircle circle1;
    private final RenderTargetCircle circle2;
    public int ticksExisted = 0;
    private float oldSize;
    @SideOnly(Side.CLIENT)
    private final GuiAnimatedStat stat;
    private boolean didMakeLockSound;
    public boolean isLookingAtTarget;
    private List<String> textList = new ArrayList<>();
    private final List<IEntityTrackEntry> trackEntries;
    private int hackTime;

    public RenderEntityTarget(Entity entity) {
        this.entity = entity;
        this.trackEntries = EntityTrackHandler.getTrackersForEntity(entity);
        this.circle1 = new RenderTargetCircle();
        this.circle2 = new RenderTargetCircle();

        this.stat = new GuiAnimatedStat(null, entity.getDisplayName().getFormattedText(), StatIcon.NONE,
                20, -20, 0x3000AA00, null, false);
        this.stat.setMinDimensionsAndReset(0, 0);
    }

    public RenderDroneAI getDroneAIRenderer() {
        for (IEntityTrackEntry tracker : this.trackEntries) {
            if (tracker instanceof EntityTrackHandler.EntityTrackEntryDrone) {
                return ((EntityTrackHandler.EntityTrackEntryDrone) tracker).getDroneAIRenderer();
            }
        }
        throw new IllegalStateException("[RenderTarget] Drone entity, but no drone AI Renderer?");
    }

    public void update() {
        this.stat.update();
        this.stat.setTitle(this.entity.getDisplayName().getFormattedText());
        EntityPlayer player = FMLClientHandler.instance().getClient().player;

        if (this.ticksExisted >= 30 && !this.didMakeLockSound) {
            this.didMakeLockSound = true;
            player.world.playSound(player.posX, player.posY, player.posZ, Sounds.HUD_ENTITY_LOCK, SoundCategory.PLAYERS, 0.1F, 1.0F, true);
        }

        boolean tagged = NBTUtil.getInteger(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD), NBTKeys.PNEUMATIC_HELMET_DEBUGGING_DRONE) == this.entity.getEntityId();
        this.circle1.setRenderingAsTagged(tagged);
        this.circle2.setRenderingAsTagged(tagged);
        this.circle1.update();
        this.circle2.update();
        for (IEntityTrackEntry tracker : this.trackEntries) {
            tracker.update(this.entity);
        }

        this.isLookingAtTarget = this.isPlayerLookingAtTarget();

        if (this.hackTime > 0) {
            IHackableEntity hackableEntity = HackableHandler.getHackableForEntity(this.entity, PneumaticCraftRepressurized.proxy.getClientPlayer());
            if (hackableEntity != null) {
                this.hackTime++;
            } else {
                this.hackTime = 0;
            }
        }
    }

    public boolean isInitialized() {
        return this.ticksExisted > 120;
    }

    public void render(float partialTicks, boolean justRenderWhenHovering) {
        for (IEntityTrackEntry tracker : this.trackEntries) {
            tracker.render(this.entity, partialTicks);
        }
        double x = this.entity.prevPosX + (this.entity.posX - this.entity.prevPosX) * partialTicks;
        double y = this.entity.prevPosY + (this.entity.posY - this.entity.prevPosY) * partialTicks + this.entity.height / 2D;
        double z = this.entity.prevPosZ + (this.entity.posZ - this.entity.prevPosZ) * partialTicks;

        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.pushMatrix();

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        float red;
        float green;
        float blue;
        float alpha = 0.5F;
        if (this.entity instanceof EntityDrone) {
            red = 1;
            green = 1;
            blue = 0;
        } else if (this.entity instanceof IMob) {
            red = 1;
            green = 0;
            blue = 0;
        } else if (this.entity instanceof EntityHanging) {
            red = 0;
            green = 1;
            blue = 1;
        } else {
            red = 0;
            green = 1;
            blue = 0;
        }

        float size = this.entity.height * 0.5F;

        if (this.ticksExisted < 60) {
            size += 5 - Math.abs(this.ticksExisted) * 0.083F;
            alpha = Math.abs(this.ticksExisted) * 0.005F;
        }

        GlStateManager.translate(x, y, z);

        GlStateManager.rotate(180.0F - Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F - Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.color(red, green, blue, alpha);
        float renderSize = this.oldSize + (size - this.oldSize) * partialTicks;
        this.circle1.render(renderSize, partialTicks);
        this.circle2.render(renderSize + 0.2D, partialTicks);
        float targetAcquireProgress = ((this.ticksExisted + partialTicks - 50) / 0.7F);
        if (this.ticksExisted <= 120 && this.ticksExisted > 50) {
            RenderProgressBar.render(0D, 0.4D, 1.8D, 0.9D, 0, targetAcquireProgress, 0xD0FFFF00, 0xD000FF00);
        }

        GlStateManager.enableTexture2D();

        FontRenderer fontRenderer = Minecraft.getMinecraft().getRenderManager().getFontRenderer();
        GlStateManager.scale(0.02D, 0.02D, 0.02D);
        GlStateManager.color(red, green, blue, alpha);
        if (this.ticksExisted > 120) {
            if (justRenderWhenHovering && !this.isLookingAtTarget) {
                this.stat.closeWindow();
            } else {
                this.stat.openWindow();
            }
            this.textList = new ArrayList<>();
            for (IEntityTrackEntry tracker : this.trackEntries) {
                tracker.addInfo(this.entity, this.textList, this.isLookingAtTarget);
            }
            this.stat.setText(this.textList);
            this.stat.render(-1, -1, partialTicks);
        } else if (this.ticksExisted > 50) {
            fontRenderer.drawString("Acquiring Target...", 0, 0, 0x7F7F7F);
            fontRenderer.drawString((int) targetAcquireProgress + "%", 37, 28, 0x002F00);
        } else if (this.ticksExisted < -30) {
            this.stat.closeWindow();
            this.stat.render(-1, -1, partialTicks);
            fontRenderer.drawString("Lost Target!", 0, 0, 0xFF0000);
        }

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);

        this.oldSize = size;
    }

    public List<String> getEntityText() {
        return this.textList;
    }

    private boolean isPlayerLookingAtTarget() {
        // code used from the Enderman player looking code.
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        World world = FMLClientHandler.instance().getClient().world;
        Vec3d vec3 = player.getLook(1.0F).normalize();
        Vec3d vec31 = new Vec3d(this.entity.posX - player.posX, this.entity.getEntityBoundingBox().minY + this.entity.height / 2.0F - (player.posY + player.getEyeHeight()), this.entity.posZ - player.posZ);
        double d0 = vec31.length();
        vec31 = vec31.normalize();
        double d1 = vec3.dotProduct(vec31);
        return d1 > 1.0D - 0.050D / d0;
    }

    public void hack() {
        if (this.isInitialized() && this.isPlayerLookingAtTarget()) {
            IHackableEntity hackable = HackableHandler.getHackableForEntity(this.entity, PneumaticCraftRepressurized.proxy.getClientPlayer());
            if (hackable != null && (this.hackTime == 0 || this.hackTime > hackable.getHackTime(this.entity, PneumaticCraftRepressurized.proxy.getClientPlayer())))
                NetworkHandler.sendToServer(new PacketHackingEntityStart(this.entity));
        }
    }

    public void selectAsDebuggingTarget() {
        if (this.isInitialized() && this.isPlayerLookingAtTarget() && this.entity instanceof EntityDrone) {
            GuiDroneDebuggerOptions.clearAreaShowWidgetId();
            NetworkHandler.sendToServer(new PacketUpdateDebuggingDrone(this.entity.getEntityId()));
            Minecraft.getMinecraft().player.playSound(Sounds.HUD_ENTITY_LOCK, 1.0f, 2.0f);
        }
    }

    public void onHackConfirmServer() {
        this.hackTime = 1;
    }

    public int getHackTime() {
        return this.hackTime;
    }

    public boolean scroll(MouseEvent event) {
        if (this.isInitialized() && this.isPlayerLookingAtTarget()) {
            return this.stat.handleMouseWheel(event.getDwheel());
        }
        return false;
    }
}
