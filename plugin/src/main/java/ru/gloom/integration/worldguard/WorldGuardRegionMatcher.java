package ru.gloom.integration.worldguard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;

final class WorldGuardRegionMatcher {
    static final String GLOBAL_WORLD = "*";

    private WorldGuardRegionMatcher() {}

    static Set<String> getRegions(Player player, Map<String, Set<String>> regionsByWorld) {
        Set<String> regions = new HashSet<>(regionsByWorld.getOrDefault(GLOBAL_WORLD, Collections.emptySet()));
        regions.addAll(regionsByWorld.getOrDefault(
                WorldGuardRegionBypassConfig.normalize(player.getWorld().getName()), Collections.emptySet()));
        return regions;
    }
}
