package com.dianlemel.huc;

import com.dianlemel.huc.item.BaseItem;
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
        try {
            getServer().getPluginManager().registerEvents(UHCController.getInstance(), this);
            UHCConfig.getInstance();
            var command = new UHCCommand();
            PluginCommand pluginCommand = Bukkit.getPluginCommand("uhc");
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            BaseItem.loadItem();
            Bukkit.getOnlinePlayers().stream().map(UHCPlayer::getUHCPlayer).forEach(UHCPlayer::online);
        } catch (Exception e) {
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        BaseItem.clear();
        try {
            UHCTeam.clearTeam();
        } catch (UHCException e) {
        }
        TaskUtil.cancelAllTask();
        Bukkit.getWorlds().forEach(world -> world.getWorldBorder().reset());
    }
}