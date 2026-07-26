package com.hashiriyacarmod;

import net.minecraft.world.level.Level;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CarEntityRegistry {

    // ワールドごとにSetを分けて管理します
    private static final Map<Level, Set<CarEntity>> CARS_BY_LEVEL =
            new ConcurrentHashMap<>();

    public static void register(CarEntity car) {
        CARS_BY_LEVEL
                .computeIfAbsent(car.level(), k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(car);
    }

    public static void unregister(CarEntity car) {
        Set<CarEntity> set = CARS_BY_LEVEL.get(car.level());
        if (set != null) {
            set.remove(car);
            if (set.isEmpty()) {
                CARS_BY_LEVEL.remove(car.level());
            }
        }
    }

    public static Set<CarEntity> getAllInLevel(Level level) {
        return CARS_BY_LEVEL.getOrDefault(level, Collections.emptySet());
    }
}