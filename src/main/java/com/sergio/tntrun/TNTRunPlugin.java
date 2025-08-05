package com.sergio.tntrun;

import com.sergio.tntrun.game.GameManager;
import com.sergio.tntrun.listeners.GameListener;
import com.sergio.tntrun.listeners.PowerupListener;
import com.sergio.tntrun.managers.PowerupManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class TNTRunPlugin extends JavaPlugin {

    private GameManager gameManager;
    private PowerupManager powerupManager;
    private Location spawnLocation;

    @Override
    public void onEnable() {
        this.spawnLocation = new Location(Bukkit.getWorld("world"), 191, 22, -189);

        this.gameManager = new GameManager(this);
        this.powerupManager = new PowerupManager(this, gameManager, spawnLocation);

        this.getCommand("tntrun").setExecutor(gameManager);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager, powerupManager), this);

        getServer().getPluginManager().registerEvents(new PowerupListener(powerupManager), this);

        getLogger().info("TNTRun Plugin habilitado!");
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("TNTRun Plugin deshabilitado!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
