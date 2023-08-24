package me.hhuxy7.bm.baritone.automine.config;

import me.hhuxy7.bm.baritone.automine.calculations.behaviour.PathMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PathFindSetting {
    @Getter
    boolean mineWithPreference;

    @Getter
    PathMode pathMode;

    @Getter
    boolean findWithBlockPos;


}
