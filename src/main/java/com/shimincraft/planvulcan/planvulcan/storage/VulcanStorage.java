package com.shimincraft.planvulcan.planvulcan.storage;

import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.query.QueryService;
import com.shimincraft.planvulcan.planvulcan.enums.ViolationType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Bukkit.getLogger;

public class VulcanStorage {
    private final QueryService queryService;

    private final HashMap<ViolationType, String> violationTypeHashMap = new HashMap<>();

    public VulcanStorage() {
        this.queryService = QueryService.getInstance();
        createTable();
        queryService.subscribeDataClearEvent(this::recreateTable);
        queryService.subscribeToPlayerRemoveEvent(this::removePlayer);
        violationTypeHashMap.put(ViolationType.COMBAT_VIOLATION, "vulcan_combat_violations");
        violationTypeHashMap.put(ViolationType.MOVEMENT_VIOLATION, "vulcan_movement_violations");
        violationTypeHashMap.put(ViolationType.AUTOCLICKER_VIOLATION, "vulcan_autoclicker_violations");
        violationTypeHashMap.put(ViolationType.PLAYER_VIOLATION, "vulcan_player_violations");
        violationTypeHashMap.put(ViolationType.TIMER_VIOLATION, "vulcan_timer_violations");
        violationTypeHashMap.put(ViolationType.SCAFFOLD_VIOLATION, "vulcan_scaffold_violations");
    }

    private void createTable() {
        String dbtype = queryService.getDBType();
        boolean sqlite = dbtype.equalsIgnoreCase("SQLITE");

        String sql = "CREATE TABLE IF NOT EXISTS plan_vulcan (" +
                "id int " + (sqlite ? "PRIMARY KEY" : "NOT NULL AUTO_INCREMENT") + ',' +
                "uuid varchar(36) NOT NULL UNIQUE," +
                "vulcan_violations int NOT NULL DEFAULT 0," +
                "vulcan_combat_violations int NOT NULL DEFAULT 0," +
                "vulcan_movement_violations int NOT NULL DEFAULT 0," +
                "vulcan_autoclicker_violations int NOT NULL DEFAULT 0," +
                "vulcan_player_violations int NOT NULL DEFAULT 0," +
                "vulcan_timer_violations int NOT NULL DEFAULT 0," +
                "vulcan_scaffold_violations int NOT NULL DEFAULT 0" +
                (sqlite ? "" : ",PRIMARY KEY (id)") +
                ")";
        try {
            queryService.execute(sql, PreparedStatement::execute).get();
        } catch (InterruptedException | ExecutionException e) {
            getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }

    private void dropTable() {
        queryService.execute("DROP TABLE IF EXISTS plan_vulcan", PreparedStatement::execute);
    }

    private void recreateTable() {
        dropTable();
        createTable();
    }

    private void removePlayer(UUID playerUUID) {
        queryService.execute(
                "DELETE FROM plan_vulcan WHERE uuid=?",
                statement -> {
                    statement.setString(1, playerUUID.toString());
                    statement.execute();
                }
        );
    }

    public Set<UUID> getPlayerUUIDs() {
        Set<UUID> uuids = new HashSet<>();
        String sql = "SELECT uuid FROM plan_vulcan";
        queryService.query(sql, statement -> {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        uuids.add(uuid);
                    }
                }
            }
            return uuids;
        });
        return uuids;
    }

    public long getAverageViolations() {
        Set<UUID> uuids = getPlayerUUIDs();
        long totalViolations = 0;
        for (UUID uuid : uuids) {
            totalViolations += getTotalViolations(uuid);
        }
        return totalViolations / uuids.size();
    }

    public int getTypeViolations(UUID uuid, String violation_string) {
        String sql = "SELECT " + violation_string + " FROM plan_vulcan WHERE uuid=?";
        return queryService.query(sql, statement ->  {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(violation_string) : 0;
            }
        });
    }

    public int getSumViolation(String violation_string) {
        String sql = "SELECT SUM(" + violation_string + ") FROM plan_vulcan";
        try {
            return queryService.query(sql, statement ->  {
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : 0;
            });
        } catch (Exception e) {
            getLogger().severe("Failed to get sum of violations: " + e.getMessage());
            return 0;
        }
    }

    public int getTotalViolations(UUID uuid) {
        String sql = "SELECT vulcan_violations FROM plan_vulcan WHERE uuid=?";
        return queryService.query(sql, statement ->  {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("vulcan_violations") : 0;
            }
        });
    }

    public void storeViolation(UUID uuid, ViolationType violation_type, int violations) throws SQLException, ExecutionException, InterruptedException {
        String violation_string = "";
        try {
            violation_string = violationTypeHashMap.get(violation_type);
        } catch (NullPointerException e) {
            getLogger().warning("Violation type not found: " + violation_type);
            return;
        }
        String update = "UPDATE plan_vulcan SET " + violation_string + "=?, vulcan_violations=? WHERE uuid=?";
        String insert = "INSERT INTO plan_vulcan (uuid, " + violation_string + ", vulcan_violations) VALUES (?, ?, ?)";

        int previousTypeViolations = getTypeViolations(uuid, violation_string);
        int previousTotalViolations = getTotalViolations(uuid);
        AtomicBoolean updateSuccess = new AtomicBoolean(false);
        violations = violations != 0 ? 1 : 0;  // Violations are increasing as it goes, always only add 1, instead of constantly adding violations.
        int finalViolations = violations;
        int finalTotalViolations = violations + previousTotalViolations;
        queryService.execute(update, statement -> {
            getLogger().info("Updating violation: " + uuid.toString() + " " + violation_type.toString() + " " + finalViolations);
            statement.setInt(1, previousTypeViolations + finalViolations);
            statement.setInt(2, finalTotalViolations);
            statement.setString(3, uuid.toString());
            updateSuccess.set(statement.executeUpdate() == 1);
        }).get();
        if (!updateSuccess.get()) {
            queryService.execute(insert, statement -> {
                getLogger().info("Inserting violation: " + uuid.toString() + " " + violation_type.toString() + " " + finalViolations);
                statement.setString(1, uuid.toString());
                statement.setInt(2, finalViolations + previousTotalViolations);
                statement.setInt(3, finalTotalViolations);
                statement.execute();
            });
        }
    }

    public Map<String, Integer> getTotalViolationCounts() throws NotReadyException {
        UUID serverUUID = queryService.getServerUUID().orElseThrow(NotReadyException::new);
        final String sql = "SELECT plan_vulcan.uuid, plan_vulcan.vulcan_violations FROM plan_vulcan " +
                "INNER JOIN plan_users ON plan_vulcan.uuid = plan_users.uuid " +
                "INNER JOIN plan_user_info ON plan_user_info.user_id = plan_users.id " +
                "WHERE plan_user_info.server_id = (SELECT id FROM plan_servers WHERE uuid = ?) " +
                "GROUP BY vulcan_violations";

        return queryService.query(sql, statement -> {
            statement.setString(1, serverUUID.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, Integer> violations = new HashMap<>();
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    Player player = Bukkit.getPlayer(uuid);
                    String name = player != null ? player.getName() : "Unknown";
                    int violationsCount = resultSet.getInt("vulcan_violations");
                    violations.put(name, violationsCount);
                }
                return violations;
            }
        });
    }
}
