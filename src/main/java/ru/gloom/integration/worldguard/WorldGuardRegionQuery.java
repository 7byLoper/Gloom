package ru.gloom.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;

final class WorldGuardRegionQuery {
    Set<String> getApplicableRegionIds(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet applicableRegions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        Set<String> regionIds = new HashSet<>();
        for (ProtectedRegion region : applicableRegions) {
            regionIds.add(WorldGuardRegionBypassConfig.normalize(region.getId()));
        }

        return regionIds;
    }
}
