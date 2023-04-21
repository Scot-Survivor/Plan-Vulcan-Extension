package com.shimincraft.planvulcan.planvulcan.listeners;

import com.djrapitops.plan.extension.Caller;
import com.shimincraft.planvulcan.planvulcan.PlanVulcan;
import com.shimincraft.planvulcan.planvulcan.storage.VulcanStorage;
import com.shimincraft.planvulcan.planvulcan.enums.ViolationType;
import me.frep.vulcan.api.check.Check;
import me.frep.vulcan.api.event.VulcanFlagEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getScheduler;

public class VulcanListener implements Listener {
    private final VulcanStorage storage;
    private Plugin plugin;

    private Optional<Caller> caller;

    private final HashMap<String, ViolationType> violationTypeHashMap = new HashMap<>();

    public VulcanListener(VulcanStorage storage, PlanVulcan plugin) {
        this.storage = storage;
        this.plugin = plugin;
        violationTypeHashMap.put("combat", ViolationType.COMBAT_VIOLATION);
        violationTypeHashMap.put("movement", ViolationType.MOVEMENT_VIOLATION);
        violationTypeHashMap.put("autoclicker", ViolationType.AUTOCLICKER_VIOLATION);
        violationTypeHashMap.put("player", ViolationType.PLAYER_VIOLATION);
        violationTypeHashMap.put("timer", ViolationType.TIMER_VIOLATION);
        violationTypeHashMap.put("scaffold", ViolationType.SCAFFOLD_VIOLATION);
        this.caller = plugin.getCaller();
    }

    @EventHandler()
    public void onFlag(VulcanFlagEvent event) throws SQLException, ExecutionException, InterruptedException {
        if (event.isCancelled()) return;
        storeViolationType(event.getCheck(), event.getPlayer(), event.getCheck().getVl());
    }

    private void storeViolationType(Check check, Player player, int violations) throws SQLException, ExecutionException, InterruptedException {
        ViolationType type = violationTypeHashMap.get(check.getCategory());
        if (type != null && player != null) {
            getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    storage.storeViolation(player.getUniqueId(), type, violations);
                    if (caller.isPresent()) {
                        caller.get().updatePlayerData(player.getUniqueId(), player.getName());
                        caller.get().updateServerData();
                    }
                } catch (SQLException | ExecutionException | InterruptedException e) {
                    getLogger().severe("Failed to store violation: " + e.getMessage());
                }
            });
        } else {
            getLogger().warning("Unknown violation type: " + check.getCategory());
        }
    }
}
