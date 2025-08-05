package com.sergio.tntrun.game;

import com.sergio.tntrun.TNTRunPlugin;
import com.sergio.tntrun.listeners.PowerupListener;
import com.sergio.tntrun.managers.PowerupManager;
import com.sergio.tntrun.scoreboard.TNTRunSidebar;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager implements CommandExecutor {

    private final TNTRunPlugin plugin;
    private final Set<UUID> playersInGame = new HashSet<>();
    private final Map<UUID, Integer> playerCoins = new HashMap<>();
    private final Set<Material> destructibleBlocks = new HashSet<>();
    private final Random random = new Random();
    private final TNTRunSidebar sidebar;
    private final Map<UUID, Integer> playerDoubleJumps = new HashMap<>();
    private PowerupManager powerupManager;

    private Location arenaSpawn;
    private boolean gameRunning = false;
    private int countdownTaskId = -1;
    private int coinTaskId = -1;

    public GameManager(TNTRunPlugin plugin) {
        this.plugin = plugin;
        this.sidebar = new TNTRunSidebar();
        this.arenaSpawn = new Location(Bukkit.getWorld("world"), 191, 22, -189);

        List<String> blockNames = plugin.getConfig().getStringList("destructible-blocks");
        for (String name : blockNames) {
            try {
                destructibleBlocks.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Bloque inválido en config.yml: " + name);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores pueden ejecutar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usa /tntrun <start|stop|join|leave|setspawn>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> joinGame(player);
            case "leave" -> leaveGame(player);
            case "start" -> {
                if (gameRunning) {
                    player.sendMessage(ChatColor.RED + "El juego ya está en marcha.");
                } else {
                    startCountdown();
                }
            }
            case "stop" -> {
                if (!gameRunning) {
                    player.sendMessage(ChatColor.RED + "No hay juego en curso.");
                } else {
                    stopGame();
                    Bukkit.broadcastMessage(ChatColor.RED + "¡El juego ha sido detenido!");
                }
            }
            case "setspawn" -> {
                this.arenaSpawn = player.getLocation();
                player.sendMessage(ChatColor.GREEN + "Ubicación de la arena establecida.");
            }
            default -> player.sendMessage(ChatColor.YELLOW + "Comando desconocido.");
        }

        return true;
    }

    public void joinGame(Player player) {
        if (gameRunning) {
            player.sendMessage(ChatColor.RED + "No puedes unirte en medio del juego.");
            return;
        }
        if (playersInGame.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ya estás en el juego.");
            return;
        }
        playersInGame.add(player.getUniqueId());
        playerCoins.put(player.getUniqueId(), 0);
        player.sendMessage(ChatColor.GREEN + "Te uniste al juego de TNTRun.");
    }

    public void leaveGame(Player player) {
        if (!playersInGame.remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "No estabas en el juego.");
            return;
        }
        playerCoins.remove(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "Saliste del juego TNTRun.");
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getWorld().getSpawnLocation());

        checkWinCondition();
    }

    private void startCountdown() {
        Bukkit.broadcastMessage(ChatColor.YELLOW + "El juego comenzará en 10 segundos...");

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 10;

            @Override
            public void run() {
                if (timeLeft == 0) {
                    this.cancel();
                    if (playersInGame.size() < 1) {
                        Bukkit.broadcastMessage(ChatColor.RED + "Se necesitan al menos 2 jugadores para iniciar.");
                        return;
                    }
                    startGame();
                    return;
                }

                for (UUID uuid : playersInGame) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendTitle(ChatColor.AQUA + "Comienza en:", String.valueOf(timeLeft), 0, 25, 5);
                    }
                }
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        countdownTaskId = task.getTaskId();
    }

    private void startGame() {
        gameRunning = true;

        powerupManager = new PowerupManager(plugin, this, arenaSpawn);
        powerupManager.startSpawning();
        plugin.getServer().getPluginManager().registerEvents(new PowerupListener(powerupManager), plugin);

        for (UUID uuid : playersInGame) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(arenaSpawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setAllowFlight(true);
                player.sendMessage(ChatColor.GOLD + "¡El juego ha comenzado!");
                giveDoubleJumps(uuid, 1);
            }
        }

        // Monedas cada 10 segundos
        coinTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID uuid : playersInGame) { // Solo jugadores activos
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getGameMode() == GameMode.SURVIVAL) { // Solo si sigue en juego
                    playerCoins.computeIfPresent(uuid, (k, v) -> v + 2);
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Monedas: " + playerCoins.get(uuid)));
                }
            }
        }, 200L, 200L);

        // Actualizar sidebar cada segundo para todos los jugadores
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }

                int playersAliveCount = playersInGame.size();

                for (UUID uuid : playersInGame) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        int coins = playerCoins.getOrDefault(uuid, 0);
                        int doubleJumps = getDoubleJumps(uuid);
                        int noBreakTime = powerupManager != null ? powerupManager.getNoBreakTimeLeft(p) : 0;
                        int speedTime = powerupManager != null ? powerupManager.getSpeedTimeLeft(p) : 0;
                        sidebar.updateSidebar(p, coins, doubleJumps, playersAliveCount, noBreakTime, speedTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void eliminatePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playersInGame.contains(uuid)) return;

        playersInGame.remove(uuid);
        playerCoins.remove(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(ChatColor.RED + "Has sido eliminado.");
        player.sendTitle(ChatColor.RED + "Fuiste eliminado", "", 10, 60, 20); // <-- Añadido
        Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " fue eliminado.");

        checkWinCondition();
    }


    private void checkWinCondition() {
        if (!gameRunning) return;

        if (playersInGame.size() == 1) {
            UUID winnerId = playersInGame.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "¡" + winner.getName() + " ha ganado TNTRun!");
                winner.sendTitle("¡Ganaste!", "Felicidades", 10, 60, 20);
            }
            stopGame();
        }

        if (playersInGame.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.RED + "¡No hay ganadores!");
            stopGame();
        }
    }

    public void stopGame() {
        gameRunning = false;
        Bukkit.getScheduler().cancelTask(coinTaskId);

        for (UUID uuid : new HashSet<>(playersInGame)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(player.getWorld().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                sidebar.removeSidebar(player);
                player.sendMessage(ChatColor.YELLOW + "El juego ha terminado.");
            }
        }

        playersInGame.clear();
        playerCoins.clear();
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public Set<Material> getDestructibleBlocks() {
        return destructibleBlocks;
    }

    public boolean isPlayerInGame(Player player) {
        return playersInGame.contains(player.getUniqueId());
    }

    public Location getArenaSpawn() {
        return arenaSpawn;
    }

    public Set<UUID> getPlayers() {
        return playersInGame;
    }

    public int getCoins(Player player) {
        return playerCoins.getOrDefault(player.getUniqueId(), 0);
    }

    public TNTRunPlugin getPlugin() {
        return plugin;
    }

    public void giveDoubleJumps(UUID uuid, int amount) {
        playerDoubleJumps.put(uuid, playerDoubleJumps.getOrDefault(uuid, 0) + amount);
    }

    public boolean useDoubleJump(Player player) {
        UUID uuid = player.getUniqueId();
        int jumps = playerDoubleJumps.getOrDefault(uuid, 0);
        if (jumps > 0) {
            playerDoubleJumps.put(uuid, jumps - 1);
            return true;
        }
        return false;
    }

    public int getDoubleJumps(UUID uuid) {
        return playerDoubleJumps.getOrDefault(uuid, 0);
    }

    public Set<UUID> getPlayersInGame() {
        return playersInGame;
    }

    public TNTRunSidebar getSidebar() {
        return sidebar;
    }

    public void addCoins(UUID uuid, int amount) {
        playerCoins.put(uuid, playerCoins.getOrDefault(uuid, 0) + amount);
    }
}
