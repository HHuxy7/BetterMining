package me.hhuxy7.bm.baritone.automine.logging;

import me.hhuxy7.bm.BetterMining;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import org.apache.logging.log4j.LogManager;

public class Logger {

    public static void log(String msg){
        if(BetterMining.config.debugLogMode)
            LogManager.getLogger(BetterMining.MODID).info(msg);
    }
    public static void playerLog(String msg){
        if(BetterMining.config.debugLogMode)
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[Baritone] : " + msg));
    }
}
