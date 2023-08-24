package me.hhuxy7.bm.baritone.automine.movement;

import me.hhuxy7.bm.baritone.automine.calculations.behaviour.PathMode;
import me.hhuxy7.bm.baritone.automine.config.MiningType;
import me.hhuxy7.bm.baritone.automine.config.BaritoneConfig;
import me.hhuxy7.bm.baritone.automine.logging.Logger;
import me.hhuxy7.bm.baritone.automine.structures.BlockNode;
import me.hhuxy7.bm.baritone.automine.structures.BlockType;
import me.hhuxy7.bm.baritone.automine.structures.Path;
import me.hhuxy7.bm.handlers.KeybindHandler;
import me.hhuxy7.bm.handlers.MacroHandler;
import me.hhuxy7.bm.macros.macros.CommissionMacro;
import me.hhuxy7.bm.player.Rotation;
import me.hhuxy7.bm.render.BlockRenderer;
import me.hhuxy7.bm.utils.AngleUtils;
import me.hhuxy7.bm.utils.BlockUtils.BlockUtils;
import me.hhuxy7.bm.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.LinkedList;

public class PathExecutor {
    Minecraft mc = Minecraft.getMinecraft();


    int stuckTickCount = 0;

    boolean jumpFlag;
    int jumpCooldown;

    //ContinuousRotator rotatorContext = new ContinuousRotator();
    Rotation rotator = new Rotation();

    int deltaJumpTick = 0;

    LinkedList<BlockNode> blocksInPath = new LinkedList<>();
    LinkedList<BlockNode> finishedPath = new LinkedList<>();

    BlockRenderer blockRenderer = new BlockRenderer();

    BaritoneConfig config;

    Vec3 lookVector;


    PlayerState currentState = PlayerState.IDLE;
    boolean shouldGoToFinalBlock;

    enum PlayerState {
        IDLE,
        WALKING,
        MINING,
        FAILED,
        FINISHED
    }

    Path path;


    public void executePath(Path path, BaritoneConfig config){

        if(path == null || path.getBlocksInPath().isEmpty()){
            Logger.log("Path is empty!");
            fail();
            return;
        }

        shouldGoToFinalBlock = path.getMode() == PathMode.GOTO;

        this.path = path;
        this.blocksInPath = path.getBlocksInPath();
        this.config = config;

        currentState = PlayerState.IDLE;
        stuckTickCount = 0;
        deltaJumpTick = 0;
        jumpCooldown = 0;
        jumpFlag = false;

        lookVector = null;

        finishedPath.clear();
        blockRenderer.renderMap.clear();

        blockRenderer.renderMap.put(path.getBlocksInPath().getFirst().getPos(), Color.RED);
        for(int i = 1; i < path.getBlocksInPath().size(); i++){
            blockRenderer.renderMap.put(path.getBlocksInPath().get(i).getPos(), Color.ORANGE);
        }


        MinecraftForge.EVENT_BUS.register(this);
        updateState();
    }

    public boolean isExecuting(){
        return currentState == PlayerState.MINING || currentState == PlayerState.WALKING;
    }

    public boolean hasFailed(){
        return currentState == PlayerState.FAILED;
    }

    public boolean hasSuccessfullyFinished(){
        return currentState == PlayerState.FINISHED;
    }

    public void reset(){
        this.currentState = PlayerState.IDLE;
        unregister();
    }

    public void disable(){
        this.currentState = PlayerState.FINISHED;
        KeybindHandler.resetKeybindState();
        KeybindHandler.setKeyBindState(KeybindHandler.keyBindShift, config.isShiftWhenMine());
        unregister();
    }
    private void fail(){
        currentState = PlayerState.FAILED;
        unregister();
    }
    public void unregister(){
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onTickEvent(TickEvent.ClientTickEvent event){

        if(mc.thePlayer == null
                || event.phase != TickEvent.Phase.START
                || currentState == PlayerState.FAILED
                || currentState == PlayerState.FINISHED
        ) {
            return;
        }


        if (!blocksInPath.isEmpty() && shouldRemoveFromList(blocksInPath.getLast())) {
            lookVector = null;
            stuckTickCount = 0;
            finishedPath.add(blocksInPath.getLast());
            blockRenderer.renderMap.remove(blocksInPath.getLast().getPos());
            blocksInPath.removeLast();
        } else {
            stuckTickCount++;
            if(stuckTickCount > 20 * config.getRestartTimeThreshold()){
                mc.inGameHasFocus = true;
                mc.mouseHelper.grabMouseCursor();
                fail();
                return;
            }
        }

        if(blocksInPath.isEmpty() || (BlockUtils.isPassable(blocksInPath.getFirst().getPos()) && blocksInPath.getFirst().getType() == BlockType.MINE)){
            if(!shouldGoToFinalBlock || (!finishedPath.isEmpty() && BlockUtils.getPlayerLoc().equals(finishedPath.getLast().getPos()))) {
                disable();
                return;
            }
        }

        updateState();


        switch (currentState) {
            case WALKING:

                BlockPos targetWalkBlock = (blocksInPath.isEmpty() || blocksInPath.getLast().getType() == BlockType.MINE) ? finishedPath.getLast().getPos() : blocksInPath.getLast().getPos();

                if(finishedPath.isEmpty()){
                    float reqYaw = AngleUtils.getRequiredYawCenter(targetWalkBlock);
                    if(rotator.rotating)
                        return;
                    if(AngleUtils.getAngleDifference(reqYaw, AngleUtils.getActualRotationYaw()) > 1f || Math.abs(mc.thePlayer.rotationPitch - 0) > 1f) {
                        rotator.easeTo(reqYaw, 0, 250);
                        return;
                    }

                }

                float reqYaw = AngleUtils.getRequiredYawCenter(targetWalkBlock);
                rotator.initAngleLock(reqYaw, 5, 0, 200);

                if(!jumpFlag
                        && mc.thePlayer.posY - mc.thePlayer.lastTickPosY == 0
                        && mc.thePlayer.onGround
                        && jumpCooldown == 0
                        && targetWalkBlock.getY() > mc.thePlayer.posY
                ){
                    jumpFlag = true;
                    jumpCooldown = 10;
                }

                KeybindHandler.updateKeys(
                        AngleUtils.getAngleDifference(reqYaw, AngleUtils.getActualRotationYaw()) < -4 * 10 + 45,
                        AngleUtils.getAngleDifference(reqYaw, AngleUtils.getActualRotationYaw()) >= 45,
                        false, false, false, false, false,
                        jumpFlag
                );

                jumpFlag = false;
                if(jumpCooldown > 0) {
                    jumpCooldown --;
                }
                break;
            case MINING:
                BlockPos targetMineBlock = blocksInPath.getLast().getPos();
                if (MacroHandler.macros.get(4).isEnabled()) {
                    mc.thePlayer.inventory.currentItem = CommissionMacro.getPickaxeSlot();
                } else {
                    mc.thePlayer.inventory.currentItem = PlayerUtils.getItemInHotbar(false, "Pick", "Gauntlet", "Drill");
                }
                KeybindHandler.updateKeys(
                        false, false, false, false,
                        mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null &&
                                mc.objectMouseOver.getBlockPos().equals(targetMineBlock),
                        false,
                        config.isShiftWhenMine(),
                        false);


                if(lookVector == null && PlayerUtils.isNotMoving()) {
                    lookVector = BlockUtils.getCloserVisibilityLine(targetMineBlock, 50);
                    if (lookVector == null) {
                        fail();
                        return;
                    }
                }

                if(lookVector != null)
                    rotator.initAngleLock(AngleUtils.getRotation(lookVector).getFirst(), AngleUtils.getRotation(lookVector).getSecond(), config.getRotationTime());
                break;
        }


        if (deltaJumpTick > 0) {
            deltaJumpTick--;
        }
    }


    @SubscribeEvent
    public void onRenderEvent(RenderWorldLastEvent event){
        blockRenderer.renderAABB(event);
        if(rotator.rotating)
            rotator.update();
    }


    @SubscribeEvent
    public void onOverlayRenderEvent(RenderGameOverlayEvent event){
        if(event.type == RenderGameOverlayEvent.ElementType.TEXT){
            if(blocksInPath != null){
                if(!blocksInPath.isEmpty()){
                    for(int i = 0; i < blocksInPath.size(); i++){
                        mc.fontRendererObj.drawString(blocksInPath.get(i).getPos().toString() + " " + blocksInPath.get(i).getType().toString() , 5, 5 + 10 * i, -1);
                    }
                }
            }
            if(currentState != null) {
                mc.fontRendererObj.drawString(currentState.toString(), 300, 5, -1);
            }
        }
    }

    private void updateState(){

        if(config.getMineType() == MiningType.STATIC) {
            currentState = PlayerState.MINING;
            return;
        }

        if(blocksInPath.isEmpty()){
            if(!shouldGoToFinalBlock) return;
            currentState = PlayerState.WALKING;
            return;
        }
        if(finishedPath.isEmpty()){
            currentState =  blocksInPath.getLast().getType().equals(BlockType.MINE) ? PlayerState.MINING : PlayerState.WALKING;
            return;
        }

        if(blocksInPath.getLast().getType() == BlockType.WALK) {
            currentState = PlayerState.WALKING;
            return;
        }


        switch (currentState){
            case WALKING:
                if(finishedPath.getLast().getType() == BlockType.WALK) {
                    if(!(blocksInPath.getLast().getType() == BlockType.MINE))
                        return;
                }
                if(finishedPath.getLast().getType() == BlockType.MINE) {
                    if(BlockUtils.fitsPlayer(finishedPath.getLast().getPos().down()) && !BlockUtils.onTheSameXZ(finishedPath.getLast().getPos(), BlockUtils.getPlayerLoc()))
                        return;
                }
                currentState = PlayerState.MINING;
                break;
            case MINING:
                if (blocksInPath.getLast().getType() == BlockType.MINE && shouldWalkTo(finishedPath.getLast().getPos(), blocksInPath.getLast().getPos()))
                    currentState = PlayerState.WALKING;
                break;
        }
    }

    private boolean shouldRemoveFromList(BlockNode lastBlockNode){
        if(lastBlockNode.getType() == BlockType.MINE)
            return BlockUtils.isPassable(lastBlockNode.getPos()) || BlockUtils.getBlock(lastBlockNode.getPos()).equals(Blocks.bedrock);
        else
            return BlockUtils.onTheSameXZ(lastBlockNode.getPos(), BlockUtils.getPlayerLoc()) || !BlockUtils.fitsPlayer(lastBlockNode.getPos().down());
    }


    private boolean shouldWalkTo(BlockPos blockPos, BlockPos nextBlockPos){

        return ((blockPos.getY() > Math.round(mc.thePlayer.posY) && BlockUtils.isPassable(BlockUtils.getPlayerLoc().up(2))) ||
                        (blockPos.getY() < Math.round(mc.thePlayer.posY) && BlockUtils.isPassable(blockPos.up(2))) ||
                        (blockPos.getY() == Math.round(mc.thePlayer.posY)))
                && (BlockUtils.fitsPlayer(blockPos.down()) || BlockUtils.fitsPlayer(blockPos.down(2))) 
                && !BlockUtils.onTheSameXZ(blockPos, BlockUtils.getPlayerLoc()) 
                && (nextBlockPos.getY() > blockPos.getY() || !BlockUtils.onTheSameXZ(nextBlockPos, blockPos)); 
    }


}
