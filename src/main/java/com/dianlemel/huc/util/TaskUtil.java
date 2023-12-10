package com.dianlemel.huc.util;

import com.dianlemel.huc.UHCCore;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class TaskUtil {

	private static BukkitScheduler getScheduler() {
		return Bukkit.getServer().getScheduler();
	}

	public static int syncTask(Runnable task, long tick) {
		return getScheduler().scheduleSyncDelayedTask(UHCCore.getPlugin(), task, tick);
	}

	public static int syncTask(Runnable task) {
		return getScheduler().scheduleSyncDelayedTask(UHCCore.getPlugin(), task, 0);
	}

	public static BukkitTask syncTimer(Runnable task, int delayStartTick, int intervalTick) {
		return getScheduler().runTaskTimer(UHCCore.getPlugin(), task, delayStartTick, intervalTick);
	}

	public static BukkitTask syncTaskLater(Runnable task, long tick) {
		return getScheduler().runTaskLater(UHCCore.getPlugin(), task, tick);
	}

	public static BukkitTask asyncTask(Runnable runnable) {
		return asyncTask(runnable, 0);
	}

	public static BukkitTask asyncTimer(Runnable task, int delayStartTick, int intervalTick) {
		return getScheduler().runTaskTimerAsynchronously(UHCCore.getPlugin(), task, delayStartTick, intervalTick);
	}

	public static BukkitTask asyncTask(Runnable runnable, int tick) {
		return getScheduler().runTaskLaterAsynchronously(UHCCore.getPlugin(), runnable, tick);
	}

	public static void cancelAllTask() {
		getScheduler().cancelTasks(UHCCore.getPlugin());
	}

}
