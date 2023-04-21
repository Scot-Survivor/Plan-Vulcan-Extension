package com.shimincraft.planvulcan.planvulcan.extensions;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.djrapitops.plan.query.QueryService;
import com.shimincraft.planvulcan.planvulcan.storage.VulcanStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@PluginInfo(
        name = "Vulcan Statistics",
        iconName = "vial",
        iconFamily = Family.SOLID,
        color = Color.RED
)
@TabInfo(
        tab="Overview",
        iconName="skull-crossbones",
        iconFamily=Family.SOLID,
        elementOrder = {}
)
@TabInfo(
        tab="Type Violations",
        iconName="skull-crossbones",
        iconFamily=Family.SOLID,
        elementOrder = {}
)
@TabInfo(
        tab = "Per Player Violations",
        iconName = "skull-crossbones",
        iconFamily = Family.SOLID,
        elementOrder = {}
)
@InvalidateMethod("getNumberOfViolations")
public class VulcanData implements DataExtension {
    protected final VulcanStorage storage;

    public VulcanData(VulcanStorage storage) {
        this.storage = storage;
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE,
                CallEvents.SERVER_PERIODICAL,
                CallEvents.SERVER_EXTENSION_REGISTER,
                CallEvents.MANUAL
        };
    }

    @TableProvider(tableColor = Color.GREY)
    @Tab("Type Violations")
    public Table violationTotalTypesProvider() {
        Table.Factory table = Table.builder()
                .columnOne("Violation Type", Icon.called("users").build())
                .columnTwo("Count", Icon.called("target").build());
        table.addRow("Combat", storage.getSumViolation("vulcan_combat_violations"));
        table.addRow("Movement", storage.getSumViolation("vulcan_movement_violations"));
        table.addRow("Player", storage.getSumViolation("vulcan_player_violations"));
        return table.build();
    }

    @TableProvider(tableColor = Color.RED)
    @Tab("Per Player Violations")
    public Table perPlayerViolations() {
            Table.Factory table = Table.builder()
                .columnOne("Player", Icon.called("users").build())
                .columnTwo("Combat", Icon.called("target").build())
                .columnThree("Movement", Icon.called("target").build())
                .columnFour("Player", Icon.called("target").build());
        for (UUID uuid: storage.getPlayerUUIDs()) {
            table.addRow(Bukkit.getOfflinePlayer(uuid).getName(),
                    storage.getTypeViolations(uuid, "vulcan_combat_violations"),
                    storage.getTypeViolations(uuid, "vulcan_movement_violations"),
                    storage.getTypeViolations(uuid, "vulcan_player_violations"));
        }
        return table.build();
    }

    @NumberProvider(
            text="Lifetime Violations",
            description="Total number of violations",
            iconName = "bookmark",
            iconColor = Color.GREEN,
            priority = 10
    )
    @Tab("Overview")
    public long totalViolations() {
        return storage.getSumViolation("vulcan_violations") + 1;
    }

    @NumberProvider(
            text="Average Number of violations per player",
            description="Number of violations, a player has",
            iconName = "bookmark",
            iconColor = Color.GREEN,
            priority = 9
    )
    @Tab("Overview")
    public long averageViolationsPerPlayer() {

        return storage.getAverageViolations() + 1;
    }

}
