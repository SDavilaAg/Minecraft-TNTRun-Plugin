package com.sergio.tntrun.listeners;

import com.sergio.tntrun.managers.PowerupManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PowerupListener implements Listener {

    private final PowerupManager powerupManager;

    public PowerupListener(PowerupManager powerupManager) {
        this.powerupManager = powerupManager;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            powerupManager.handlePickup(player, event.getItem());
            event.setCancelled(true);
        }
    }
}
