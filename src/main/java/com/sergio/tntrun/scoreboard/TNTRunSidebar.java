package com.sergio.tntrun.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TNTRunSidebar {

    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();

    public void updateSidebar(Player player, int coins, int doubleJumpsRemaining, int playersAlive, int noBreakTime, int speedTime) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = playerScoreboards.get(player.getUniqueId());

        if (board == null) {
            board = manager.getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), board);
        }

        Objective objective = board.getObjective("tntrun");
        if (objective == null) {
            objective = board.registerNewObjective("tntrun", "dummy", ChatColor.GOLD + "§l§nTNTRun");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(ChatColor.GOLD + "§l§nTNTRun");
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        int line = 12;

        // Título decorativo
        objective.getScore("§8§m----------------").setScore(line--);
        objective.getScore(" ").setScore(line--);

        // Monedas
        objective.getScore(ChatColor.YELLOW + "Monedas:").setScore(line--);
        objective.getScore(ChatColor.WHITE + "  " + coins).setScore(line--);

        // Saltos dobles
        objective.getScore(ChatColor.AQUA + "Saltos dobles:").setScore(line--);
        objective.getScore(ChatColor.WHITE + "  " + doubleJumpsRemaining).setScore(line--);

        // Jugadores vivos
        objective.getScore(ChatColor.RED + "Vivos:").setScore(line--);
        objective.getScore(ChatColor.WHITE + "  " + playersAlive).setScore(line--);

        // Powerups activos
        if (noBreakTime > 0) {
            objective.getScore(ChatColor.GREEN + "No romper: " + ChatColor.WHITE + noBreakTime + "s").setScore(line--);
        }
        if (speedTime > 0) {
            objective.getScore(ChatColor.AQUA + "Velocidad: " + ChatColor.WHITE + speedTime + "s").setScore(line--);
        }

        objective.getScore("  ").setScore(line--);
        objective.getScore("§8§m-----------------").setScore(line--);

        player.setScoreboard(board);
    }

    public void removeSidebar(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}
