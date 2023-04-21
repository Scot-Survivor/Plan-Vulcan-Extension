package com.shimincraft.planvulcan.planvulcan;

import com.djrapitops.plan.extension.Caller;
import com.shimincraft.planvulcan.planvulcan.hooks.PlanHook;
import com.shimincraft.planvulcan.planvulcan.listeners.VulcanListener;
import com.shimincraft.planvulcan.planvulcan.storage.VulcanStorage;
import org.bukkit.plugin.java.JavaPlugin;
import me.frep.vulcan.api.VulcanAPI;

import java.util.Optional;

public final class PlanVulcan extends JavaPlugin {
    private VulcanAPI vulcanAPI;
    private VulcanStorage storage;

    private Optional<Caller> caller;

    @Override
    public void onEnable() {
        // Plugin startup logic
        vulcanAPI = VulcanAPI.Factory.getApi();
        if (vulcanAPI == null) {
            getLogger().severe("Vulcan is not installed");
            getServer().getPluginManager().disablePlugin(this);  // Disable plugin if Vulcan is not installed
            return;
        }
        storage = new VulcanStorage();
        try {
            PlanHook hook = new PlanHook(vulcanAPI, storage);
            hook.hookIntoPlan();
            caller = hook.getCaller();
            getLogger().info("PlanVulcan is alive");
        } catch (NoClassDefFoundError planIsNotInstalled) {
            // Plan is not installed
        }
        this.getServer().getPluginManager().registerEvents(new VulcanListener(storage, this), this);
        getLogger().info("PlanVulcan listening to Vulcan events");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Optional<Caller> getCaller() {
        return caller;
    }

    public void setCaller(Optional<Caller> caller) {
        this.caller = caller;
    }
}
