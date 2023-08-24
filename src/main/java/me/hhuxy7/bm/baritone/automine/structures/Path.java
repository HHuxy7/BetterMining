package me.hhuxy7.bm.baritone.automine.structures;

import me.hhuxy7.bm.baritone.automine.calculations.behaviour.PathMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.LinkedList;

@AllArgsConstructor
public class Path {
    @Getter
    LinkedList<BlockNode> blocksInPath;
    @Getter
    PathMode mode;

}
