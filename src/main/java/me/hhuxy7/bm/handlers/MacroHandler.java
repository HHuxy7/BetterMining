package me.hhuxy7.bm.handlers;

import me.hhuxy7.bm.BetterMining;
import me.hhuxy7.bm.events.BlockChangeEvent;
import me.hhuxy7.bm.events.ReceivePacketEvent;
import me.hhuxy7.bm.features.MobKiller;
import me.hhuxy7.bm.config.aotv.AOTVWaypointsStructs;
import me.hhuxy7.bm.macros.Macro;
import me.hhuxy7.bm.macros.macros.*;
import me.hhuxy7.bm.render.BlockRenderer;
import me.hhuxy7.bm.utils.*;
import me.hhuxy7.bm.utils.BlockUtils.BlockUtils;
import me.hhuxy7.bm.utils.HypixelUtils.SkyblockInfo;
import me.hhuxy7.bm.world.GameState;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST;

public class MacroHandler {
    public static List<Macro> macros = new ArrayList<>();
    public static Minecraft mc = Minecraft.getMinecraft();

    public static boolean pickaxeSkillReady = true;

    public static boolean enabled = false;

    public static boolean finishedCommission = false;

    public static boolean restartHappening = false;

    public static boolean goldenGoblin = true;

    public static boolean kickOccurred = false;

    static BlockRenderer blockRenderer = new BlockRenderer();

    public static boolean miningSpeedActive = false;

    public static boolean outOfSoulflow = false;

    List<BlockPos> coords;

    public static GameState gameState = new GameState();

    public static int miningSpeed = 1500;


    public static void initializeMacro(){
        macros.add(new GemstoneMacro());
        macros.add(new PowderMacro());
        macros.add(new MithrilMacro());
        macros.add(new AOTVMacro());
        macros.add(new CommissionMacro());
    }
    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onTickPlayer(TickEvent.ClientTickEvent tickEvent) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;

        scanBlockingVisionBlocks();
        gameState.update();

        for (Macro macro : macros) {
            if (macro.isEnabled()) {
                macro.onTick(tickEvent.phase);
            }
        }
    }

    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onRenderWorld(final RenderWorldLastEvent event) {
        if(mc.theWorld == null || mc.thePlayer == null)
            return;

        drawAOTV(event);

        if (!enabled)
            return;

        for (Macro process : macros) {
            if (process.isEnabled()) {
                process.onLastRender(event);
            }
        }
    }

    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null)
            return;

        for (Macro macro : macros) {
            if (macro.isEnabled()) {
                macro.onOverlayRenderEvent(event);
            }
        }
    }


    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onMessageReceived(ClientChatReceivedEvent event) {
        String message = ChatFormatting.stripFormatting(event.message.getUnformattedText());
        try {
            if (message.contains(":") || message.contains(">")) return;
            if(message.startsWith("You used your Mining Speed Boost")) {
                pickaxeSkillReady = false;
                miningSpeedActive = true;
            } else if(message.endsWith("is now available!")) {
                pickaxeSkillReady = true;
            }
            if (message.endsWith("Speed Boost has expired!")) {
                miningSpeedActive = false;
            }
        } catch (Exception ignored) {}



        if (!enabled || mc.thePlayer == null || mc.theWorld == null)
            return;

        if (message.contains("Your pass to the Crystal Hollows will expire in 1 minute")) {
            if (BetterMining.config.autoRenewCrystalHollowsPass) {
                LogUtils.addMessage("Auto renewing Crystal Hollows pass");
                mc.thePlayer.sendChatMessage("/purchasecrystallhollowspass");
            }
        }
        if (message.contains("Commission")) {
            finishedCommission = true;
        }

        if (message.contains("Soulflow")) {
            outOfSoulflow = true;
        }
        if (message.contains("Evacuating") || message.contains("Update") || message.contains("Reboot")) {
            restartHappening = true;
        }

        if (message.contains("A Golden Goblin")) {
            goldenGoblin = true;
        }

        if (message.contains("A kick occurred in your connection")) {
            kickOccurred = true;
        }

        for (Macro macro : macros) {
            if (macro.isEnabled()) {
                macro.onMessageReceived(event.message.getUnformattedText());
            }
        }
    }

    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onPacketReceived(ReceivePacketEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null)
            return;

        for (Macro macro : macros) {
            if (macro.isEnabled()) {
                macro.onPacketReceived(event.packet);
            }
        }
    }

    @SubscribeEvent(receiveCanceled=true, priority=HIGHEST)
    public void onBlockChange(BlockChangeEvent event) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null)
            return;

        for (Macro macro : macros) {
            if (macro.isEnabled()) {
                macro.onBlockChange(event);
            }
        }
    }


    public static void startScript(Macro macro){
        if(!macro.isEnabled()) {
            LogUtils.addMessage("Enabled script");
            macro.toggle();
            enabled = true;
            if (BetterMining.config.mouseUngrab) {
                UngrabUtils.ungrabMouse();
            }
        }
    }
    public static void startScript(int index){
        startScript(macros.get(index));
    }

    public static void disableScript() {
       boolean flag = false;
       for(Macro macro : macros){
           if(macro.isEnabled()) {
               macro.toggle();
               flag = true;
           }
       }
       pickaxeSkillReady = true;
       enabled = false;
        if (MobKiller.isToggled) {
            BetterMining.mobKiller.toggle();
        }
       if(flag)
           LogUtils.addMessage("Disabled script");

       UngrabUtils.regrabMouse();
    }


    //region AOTV

    private final ArrayList<BlockPos> blocksBlockingVision = new ArrayList<>();

    public void drawAOTV(RenderWorldLastEvent event) {
        if (!SkyblockInfo.onCrystalHollows() || !(BetterMining.config.macroType == 3)) return;

        if (BetterMining.aotvWaypoints != null && BetterMining.aotvWaypoints.getSelectedRoute() != null && BetterMining.aotvWaypoints.getSelectedRoute().waypoints != null) {
            ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = BetterMining.aotvWaypoints.getSelectedRoute().waypoints;

            if (BetterMining.config.aotvHighlightRouteBlocks) {
                for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                    DrawUtils.drawBlockBox(new BlockPos(waypoint.x, waypoint.y, waypoint.z), BetterMining.config.aotvRouteBlocksColor, 2f);
                }
                if (BetterMining.config.aotvShowDistanceToBlocks) {
                    for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                        BlockPos pos = new BlockPos(waypoint.x, waypoint.y, waypoint.z);
                        DrawUtils.drawText("§l§3[§f " + waypoint.name + " §3]", pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, BetterMining.config.aotvShowDistanceToBlocks);
                    }
                }
            }
            if (BetterMining.config.aotvShowRouteLines) {
                if (Waypoints.size() > 1) {
                    ArrayList<BlockPos> coords = new ArrayList<>();
                    for (AOTVWaypointsStructs.Waypoint waypoint : Waypoints) {
                        coords.add(new BlockPos(waypoint.x, waypoint.y, waypoint.z));
                    }
                    DrawUtils.drawCoordsRoute(coords, event);
                }
            }
            if (BetterMining.config.drawBlocksBlockingAOTV) {
                if (!blocksBlockingVision.isEmpty()) {
                    for (BlockPos bp : blocksBlockingVision) {
                        DrawUtils.drawBlockBox(bp, BetterMining.config.aotvVisionBlocksColor, 1f);
                    }
                }
            }
        }
    }





    public void scanBlockingVisionBlocks() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!blocksBlockingVision.isEmpty())
            blocksBlockingVision.clear();

        if (!BetterMining.config.drawBlocksBlockingAOTV) return;

        if (BetterMining.config.macroType == 3) {
            if (BetterMining.aotvWaypoints != null && BetterMining.aotvWaypoints.getSelectedRoute() != null && BetterMining.aotvWaypoints.getSelectedRoute().waypoints != null) {
                ArrayList<AOTVWaypointsStructs.Waypoint> Waypoints = BetterMining.aotvWaypoints.getSelectedRoute().waypoints;
                if(!Waypoints.isEmpty()) {
                    for (int i = 0; i < Waypoints.size() - 1; i++) {
                        BlockPos pos1 = new BlockPos(Waypoints.get(i).x, Waypoints.get(i).y, Waypoints.get(i).z);
                        BlockPos pos2 = new BlockPos(Waypoints.get(i + 1).x, Waypoints.get(i + 1).y, Waypoints.get(i + 1).z);

                        blocksBlockingVision.addAll(BlockUtils.GetAllBlocksInline3d(pos1, pos2));
                    }

                    BlockPos pos1 = new BlockPos(Waypoints.get(Waypoints.size() - 1).x, Waypoints.get(Waypoints.size() - 1).y, Waypoints.get(Waypoints.size() - 1).z);
                    BlockPos pos2 = new BlockPos(Waypoints.get(0).x, Waypoints.get(0).y, Waypoints.get(0).z);

                    blocksBlockingVision.addAll(BlockUtils.GetAllBlocksInline3d(pos1, pos2));
                }
            }
        }

    }

    //endregion
}
