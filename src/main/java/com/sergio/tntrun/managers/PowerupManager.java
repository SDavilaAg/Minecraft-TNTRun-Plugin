package com.sergio.tntrun.managers;

import com.sergio.tntrun.game.GameManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PowerupManager {

    private  GameManager gameManager;
    private final Plugin plugin;

    private final Location spawnLocation;
    private final Random random = new Random();
    private final Set<Item> activePowerups = new HashSet<>();

    // Mapas para los powerups específicos
    private final Map<UUID, Long> noBreakBlocks = new HashMap<>();
    private final Map<UUID, Long> speedPowerup = new HashMap<>();

    //Contructor
    public PowerupManager(Plugin plugin, GameManager gameManager, Location spawnLocation) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.spawnLocation = spawnLocation;
    }

    /* Metodo para iniciar el spawn de powerups aleatorio y mostrar mensajes en chat de que se ha generado X powerup */
    public void startSpawning() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameManager.isGameRunning()) {
                    cancel();
                    return;
                }

                Location loc = spawnLocation.clone().add(
                        random.nextInt(81) - 40,
                        0,
                        random.nextInt(81) - 40
                );
                loc.setY(findSafeY(loc));

                int type = random.nextInt(4);
                ItemStack itemStack;
                String message;

                switch (type) {
                    case 0 -> {
                        itemStack = createPowerup(Material.FEATHER, ChatColor.LIGHT_PURPLE + "¡Doble Salto!", List.of("Recógela para obtener 2 saltos dobles"));
                        message = ChatColor.LIGHT_PURPLE + "¡Un doble salto aterrizó en el campo!";
                    }
                    case 1 -> {
                        itemStack = createPowerup(Material.SUGAR, ChatColor.AQUA + "¡Velocidad!", List.of("Recógela para velocidad x2 por 10s"));
                        message = ChatColor.AQUA + "¡Un powerup de velocidad apareció!";
                    }
                    case 2 -> {
                        itemStack = createPowerup(Material.GOLD_NUGGET, ChatColor.GOLD + "¡Monedas!", List.of("Recógela para ganar 5 monedas"));
                        message = ChatColor.GOLD + "¡Un powerup de monedas apareció!";
                    }
                    case 3 -> {
                        itemStack = createPowerup(Material.SLIME_BALL, ChatColor.GREEN + "¡No Romper Bloques!", List.of("Durante 5s no rompes bloques"));
                        message = ChatColor.GREEN + "¡Un powerup de no romper bloques apareció!";
                    }
                    default -> {
                        itemStack = createPowerup(Material.FEATHER, ChatColor.LIGHT_PURPLE + "¡Doble Salto!", List.of("Recógela para obtener 2 saltos dobles"));
                        message = ChatColor.LIGHT_PURPLE + "¡Un doble salto aterrizó en el campo!";
                    }
                }

                Item item = loc.getWorld().dropItem(loc, itemStack);
                item.setGlowing(true);
                item.setVelocity(new Vector(0, 0.1, 0));
                item.setCustomName(itemStack.getItemMeta().getDisplayName());
                item.setCustomNameVisible(true);
                activePowerups.add(item);

                Bukkit.broadcastMessage(message);

                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.5, 0), 30, 0.3, 0.5, 0.3, 0.02);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            }
        }.runTaskTimer(plugin, 0L, 20 * 20);
    }

    //Metodo para crear un powerup con su item, nombre y efecto
    private ItemStack createPowerup(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    //Metodo para encontrar una posición segura en Y
    private int findSafeY(Location base) {
        for (int i = 5; i >= -5; i--) {
            Location check = base.clone().add(0, i, 0);
            if (check.getBlock().getType() == Material.AIR &&
                    check.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                return check.getBlockY();
            }
        }
        return base.getBlockY();
    }

    //Metodo para recoger los powerups y mostrar mensajes y efectos de cada uno
    public void handlePickup(Player player, Item item) {
        cleanActivePowerups();
        if (!activePowerups.contains(item)) return;

        activePowerups.remove(item);
        item.remove();

        String name = item.getItemStack().getItemMeta().getDisplayName();
        if (name.contains("Doble Salto")) {
            gameManager.giveDoubleJumps(player.getUniqueId(), 1);
            player.setAllowFlight(true);
            player.sendMessage(ChatColor.AQUA + "¡Has obtenido 1 salto doble!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.sendTitle(ChatColor.GREEN + "¡Powerup!", ChatColor.stripColor(name), 5, 40, 10);
        } else if (name.contains("Velocidad")) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 20 * 10, 1));
            long newUntil = System.currentTimeMillis() + 10000;
            speedPowerup.put(player.getUniqueId(), newUntil); // Siempre actualiza el tiempo
            player.sendMessage(ChatColor.AQUA + "¡Velocidad x2 por 10 segundos!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.sendTitle(ChatColor.GREEN + "¡Powerup!", ChatColor.stripColor(name), 5, 40, 10);

            // No elimines el powerup aquí, solo deja que el tiempo expire naturalmente
        } else if (name.contains("Monedas")) {
            gameManager.addCoins(player.getUniqueId(), 5);
            player.sendMessage(ChatColor.GOLD + "¡Has ganado 5 monedas!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.sendTitle(ChatColor.GREEN + "¡Powerup!", ChatColor.stripColor(name), 5, 40, 10);
        } else if (name.contains("No Romper Bloques")) {
            long newUntil = System.currentTimeMillis() + 5000;
            noBreakBlocks.put(player.getUniqueId(), newUntil); // Siempre actualiza el tiempo
            Bukkit.getLogger().info("[TNTRun] Powerup 'No Romper Bloques' recogido por " + player.getName() + " (until=" + newUntil + ")");
            player.sendMessage(ChatColor.GREEN + "¡Durante 5 segundos no romperás bloques!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.sendTitle(ChatColor.GREEN + "¡Powerup!", ChatColor.stripColor(name), 5, 40, 10);

        }

        int coins = gameManager.getCoins(player);
        int doubleJumps = gameManager.getDoubleJumps(player.getUniqueId());
        int playersAlive = gameManager.getPlayersInGame().size();
        int noBreakTime = getNoBreakTimeLeft(player);
        int speedTime = getSpeedTimeLeft(player);
        gameManager.getSidebar().updateSidebar(player, coins, doubleJumps, playersAlive, noBreakTime, speedTime);
    }

    public boolean canBreakBlocks(Player player) {
        Long until = noBreakBlocks.get(player.getUniqueId());
        boolean canBreak = (until == null || System.currentTimeMillis() > until);
        Bukkit.getLogger().info("[TNTRun] canBreakBlocks para " + player.getName() + ": " + canBreak + " (until=" + until + ", now=" + System.currentTimeMillis() + ")");
        return canBreak;
    }

    // Metodo para obtener el tiempo restante de los powerups específicos
    public int getNoBreakTimeLeft(Player player) {
        return getPowerupTimeLeft(noBreakBlocks, player);
    }

    public int getSpeedTimeLeft(Player player) {
        return getPowerupTimeLeft(speedPowerup, player);
    }

    // Metodo genérico para obtener el tiempo restante de cualquier powerup con su mapa
    public int getPowerupTimeLeft(Map<UUID, Long> map, Player player) {
        Long until = map.get(player.getUniqueId());
        if (until == null) return 0;
        long left = (until - System.currentTimeMillis()) / 1000;
        return (int) Math.max(left, 0);
    }

    private void cleanActivePowerups() {
        activePowerups.removeIf(item -> !item.isValid() || item.isDead() || !item.getWorld().getEntities().contains(item));
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
}