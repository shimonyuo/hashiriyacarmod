package com.hashiriyacarmod;

import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

public interface CarGroundNormalHolder {
    @Nullable
    Vec3 hashiriyacarmod$getGroundNormal();

    void hashiriyacarmod$setGroundNormal(@Nullable Vec3 normal);

    boolean hashiriyacarmod$shouldSkipEdge();

    void hashiriyacarmod$setShouldSkipEdge(boolean skip);
}