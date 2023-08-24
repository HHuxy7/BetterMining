package me.hhuxy7.bm.macros;

import me.hhuxy7.bm.BetterMining;
import me.hhuxy7.bm.baritone.automine.logging.Logger;
import me.hhuxy7.bm.events.BlockChangeEvent;
import me.hhuxy7.bm.features.FuelFilling;
import me.hhuxy7.bm.handlers.MacroHandler;
import me.hhuxy7.bm.macros.macros.CommissionMacro;
import me.hhuxy7.bm.macros.macros.MithrilMacro;

import me.hhuxy7.bm.utils.BlockUtils.BlockUtils;
import me.hhuxy7.bm.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.crypto.Mac;

public abstract class Macro {
    protected Minecraft mc = Minecraft.getMinecraft();
    protected boolean enabled = false;
    public static boolean brokeBlockUnderPlayer = false;

    public void toggle() {
        enabled = !enabled;
        FuelFilling.currentState = FuelFilling.states.NONE;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected abstract void onEnable();

    protected abstract void onDisable();

    public void onTick(TickEvent.Phase phase) {}

    public void onKeyBindTick() {}

    public void onLastRender(RenderWorldLastEvent event) {}

    public void onOverlayRenderEvent(RenderGameOverlayEvent event) {}

    public void onPacketReceived(Packet<?> packet) {}

    public void onRenderEvent(RenderWorldEvent event){}

    public void onMessageReceived(String message) {}

    public boolean isEnabled(){
        return enabled;
    }

    public void onBlockChange(BlockChangeEvent event) {
    }

    public void checkMiningSpeedBoost() {

        if (BetterMining.config.useMiningSpeedBoost && MacroHandler.pickaxeSkillReady) {
            int slotCache = mc.thePlayer.inventory.currentItem;
            int targetSlot = BetterMining.config.blueCheeseOmeletteToggle ? PlayerUtils.getItemInHotbarFromLore(true, "Blue Cheese") : PlayerUtils.getItemInHotbar(true, "Pick", "Gauntlet", "Drill");

            if(targetSlot == -1) {
                Logger.playerLog("Blue cheese drill not found. Disabled blue cheese swap");
                BetterMining.config.blueCheeseOmeletteToggle = false;
                targetSlot = PlayerUtils.getItemInHotbar(true, "Pick", "Gauntlet", "Drill");
                if (targetSlot == -1) {
                    Logger.playerLog("Pickaxe not found. Disabling mining speed boost");
                    BetterMining.config.useMiningSpeedBoost = false;
                    return;
                }
            }
            mc.thePlayer.inventory.currentItem = targetSlot;
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(targetSlot));
            mc.thePlayer.inventory.currentItem = slotCache;

            MacroHandler.pickaxeSkillReady = false;
        }
    }

}
