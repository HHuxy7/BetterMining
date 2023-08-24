package me.hhuxy7.bm.utils;

import me.hhuxy7.bm.BetterMining;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class LogUtils {
    static Minecraft mc = Minecraft.getMinecraft();
    public static void addMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "BetterMining " + EnumChatFormatting.RESET + EnumChatFormatting.DARK_GRAY + "» " + EnumChatFormatting.AQUA + EnumChatFormatting.BOLD + message
        ));
    }
    public static void debugLog(String log) {
        if(BetterMining.config.debugLogMode)
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "[log] : " + EnumChatFormatting.RESET + log
            ));
    }
}
