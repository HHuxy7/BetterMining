package me.hhuxy7.bm.utils.Utils;

public class ThreadUtils {
    public static void sleep(int time){
        try{
            Thread.sleep(time);
        } catch (InterruptedException ignored){
        }
    }
}
