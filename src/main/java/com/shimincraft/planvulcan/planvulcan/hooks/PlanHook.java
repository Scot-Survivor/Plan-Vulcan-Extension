package com.shimincraft.planvulcan.planvulcan.hooks;
import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.Caller;
import com.shimincraft.planvulcan.planvulcan.extensions.VulcanData;
import com.shimincraft.planvulcan.planvulcan.storage.VulcanStorage;
import me.frep.vulcan.api.VulcanAPI;
import com.djrapitops.plan.extension.ExtensionService;

import java.util.Optional;

import static org.bukkit.Bukkit.getLogger;

public class PlanHook {
    private final VulcanAPI vulcanAPI;
    private final VulcanStorage storage;
    private Optional<Caller> caller;

    public PlanHook(VulcanAPI vulcanAPI, VulcanStorage storage) {
        this.vulcanAPI = vulcanAPI;
        this.storage = storage;
    }

    public void hookIntoPlan() {
        if (!areAllCapabilitiesAvailable()) {
            getLogger().warning("PlanVulcan may not be compatible with your Plan version.");
        }
        this.caller = registerDataExtension();
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES");
    }

    private Optional<Caller> registerDataExtension() {
        try {
            return ExtensionService.getInstance().register(new VulcanData(storage));
        } catch (IllegalStateException planIsNotEnabled) {
            // Plan is not enabled, handle exception
        }
        return null;
    }

    public Optional<Caller> getCaller() {
        return caller;
    }
}
