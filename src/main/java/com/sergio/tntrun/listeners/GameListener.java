package com.sergio.tntrun.listeners;

import com.sergio.tntrun.game.GameManager;
import com.sergio.tntrun.managers.PowerupManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class GameListener implements Listener {

    private final GameManager gameManager;
    private final PowerupManager powerupManager;
    private final java.util.Set<java.util.UUID> usedDoubleJump = new java.util.HashSet<>();

    public GameListener(GameManager gameManager, PowerupManager powerupManager) {
        this.gameManager = gameManager;
        this.powerupManager = powerupManager;
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!gameManager.isGameRunning() || !gameManager.getPlayersInGame().contains(player.getUniqueId())) return;

        if (player.getGameMode() == GameMode.SURVIVAL && !player.isOnGround()) {
            if (gameManager.useDoubleJump(player)) {
                event.setCancelled(true);
                player.setVelocity(player.getLocation().getDirection().multiply(1.2).setY(0.9));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.2f);
                // Desactiva el vuelo si ya no tiene saltos dobles
                if (gameManager.getDoubleJumps(player.getUniqueId()) <= 0) {
                    player.setAllowFlight(false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isGameRunning() || !gameManager.getPlayersInGame().contains(player.getUniqueId())) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        // Permitir vuelo si está en el suelo y tiene saltos dobles
        if (player.isOnGround() && gameManager.getDoubleJumps(player.getUniqueId()) > 0) {
            player.setAllowFlight(true);
        }

        // Eliminar jugador si cae a y <= 0
        if (player.getLocation().getY() <= 0) {
            gameManager.eliminatePlayer(player);
            return;
        }

        // Romper bloque debajo
        Location loc = player.getLocation().clone().subtract(0, 1, 0);
        Material blockBelow = loc.getBlock().getType();
        if (gameManager.getDestructibleBlocks().contains(blockBelow)) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
                if (powerupManager.canBreakBlocks(player)) {
                    loc.getBlock().setType(Material.AIR);
                    Location tntLoc = loc.clone().subtract(0, 1, 0);
                    if (tntLoc.getBlock().getType() == Material.TNT) {
                        tntLoc.getBlock().setType(Material.AIR);
                    }
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && gameManager.isPlayerInGame(player)) {
                event.setCancelled(true);
            }
        }
    }
}
