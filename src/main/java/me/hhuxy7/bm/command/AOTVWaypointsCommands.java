package me.hhuxy7.bm.command;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import me.hhuxy7.bm.BetterMining;
import me.hhuxy7.bm.config.aotv.AOTVWaypointsStructs;
import me.hhuxy7.bm.gui.AOTVWaypointsPage;
import me.hhuxy7.bm.utils.BlockUtils.BlockUtils;
import me.hhuxy7.bm.utils.LogUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;


public class AOTVWaypointsCommands {

    private final KeyBinding keyBinding = new KeyBinding("Open Waypoints Settings", Keyboard.KEY_NEXT, "BetterMining - Waypoints");
    private final KeyBinding keyBinding2 = new KeyBinding("Add current position to selected waypoint list", Keyboard.KEY_EQUALS, "BetterMining - Waypoints");
    private final KeyBinding keyBinding3 = new KeyBinding("Delete current position from selected waypoint list", Keyboard.KEY_MINUS, "BetterMining - Waypoints");


    public AOTVWaypointsCommands() {
        ClientRegistry.registerKeyBinding(keyBinding);
        ClientRegistry.registerKeyBinding(keyBinding2);
        ClientRegistry.registerKeyBinding(keyBinding3);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (keyBinding.isPressed()) {
            BetterMining.config.openGui();
            try {
                AOTVWaypointsPage.redrawRoutes();
                OneConfigGui.INSTANCE.openPage(BetterMining.config.aotvWaypointsPage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (keyBinding2.isPressed()) {
            if (BetterMining.aotvWaypoints.getSelectedRoute() == null) return;
            boolean added = BetterMining.aotvWaypoints.addCoord(BetterMining.aotvWaypoints.getSelectedRoute(), new AOTVWaypointsStructs.Waypoint(String.valueOf(BetterMining.aotvWaypoints.getSelectedRoute().waypoints.size()), BlockUtils.getPlayerLoc().down()));
            if (added)
                LogUtils.addMessage("AOTV Waypoints - Added current position (" + BlockUtils.getPlayerLoc().getX() + ", " + BlockUtils.getPlayerLoc().getY() + ", " + BlockUtils.getPlayerLoc().getZ() + ") to selected waypoint list");
            else
                LogUtils.addMessage("AOTV Waypoints - This waypoint already exists!");
            AOTVWaypointsStructs.SaveWaypoints();
            AOTVWaypointsPage.redrawRoutes();
        }
        if (keyBinding3.isPressed()) {
            AOTVWaypointsStructs.Waypoint waypointToDelete = null;
            if (BetterMining.aotvWaypoints.getSelectedRoute() == null) return;
            for (AOTVWaypointsStructs.Waypoint waypoint : BetterMining.aotvWaypoints.getSelectedRoute().waypoints) {
                if (BlockUtils.getPlayerLoc().down().equals(new BlockPos(waypoint.x, waypoint.y, waypoint.z))) {
                    waypointToDelete = waypoint;
                }
            }
            if (waypointToDelete != null) {
                BetterMining.aotvWaypoints.removeCoord(BetterMining.aotvWaypoints.getSelectedRoute(), waypointToDelete);
                LogUtils.addMessage("AOTV Waypoints - Removed current position (" + BlockUtils.getPlayerLoc().getX() + ", " + BlockUtils.getPlayerLoc().getY() + ", " + BlockUtils.getPlayerLoc().getZ() + ") from selected waypoint list");
                AOTVWaypointsStructs.SaveWaypoints();
                AOTVWaypointsPage.redrawRoutes();
            } else {
                LogUtils.addMessage("AOTV Waypoints - No waypoint found at your current position");
            }
        }
    }
}
