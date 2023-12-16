package com.dianlemel.huc;

import com.dianlemel.huc.item.AbstractItem;
import com.dianlemel.huc.util.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class UHCCore extends JavaPlugin {
    private static UHCCore uhcCore;

    public static UHCCore getPlugin() {
        return uhcCore;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        uhcCore = this;
        uhcCore.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(UHCController.getInstance(), this);
        UHCConfig.getInstance();
        var command = new UHCCommand();
        PluginCommand pluginCommand = Bukkit.getPluginCommand("uhc");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        AbstractItem.load();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        AbstractItem.clear();
        TaskUtil.cancelAllTask();
        UHCTeam.clearTeam();
    }
}