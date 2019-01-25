package com.tatemylove.COD.ScoreBoard;

import com.tatemylove.COD.Arenas.TDM;
import com.tatemylove.COD.Main;
import com.tatemylove.COD.Runnables.GameTime;
import com.tatemylove.COD.ThisPlugin.ThisPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.HashMap;

public class GameBoard {
    Main main;
    private GameBoard gameBoard = null;
    private HashMap<String, Scoreboard> gameboard = new HashMap<>();

    public GameBoard(Main m){
        main = m;
        gameBoard = GameBoard.this;
    }

    public void setGameBoard(Player p){
        TDM tdm = new TDM(main);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("GameBoard", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', main.getConfig().getString("gameboard-name")));

        Score blank = objective.getScore("     ");
        blank.setScore(16);
        Score username1 = objective.getScore("§eUsername:");
        username1.setScore(15);
        Score username2 = objective.getScore("    §a" + p.getName());
        username2.setScore(14);

        Score blank1 = objective.getScore(" ");
        blank1.setScore(13);

        Score kills = objective.getScore("§eKills:");
        kills.setScore(12);

        Score blank2 = objective.getScore("  ");
        blank2.setScore(10);

        Score deaths = objective.getScore("§eDeaths:");
        deaths.setScore(9);

        Score blank3 = objective.getScore("   ");
        blank3.setScore(7);

        Score killstreak = objective.getScore("§eKillstreak:");
        killstreak.setScore(6);

        Score blank4 = objective.getScore("    ");
        blank4.setScore(4);

        Score time = objective.getScore("§eTime:");
        time.setScore(3);

        Team kill = board.registerNewTeam("kill");
        kill.addEntry(ChatColor.AQUA.toString());
        kill.setPrefix(ChatColor.GREEN.toString() + "§a");
        kill.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.AQUA.toString()).setScore(11);

        Team death = board.registerNewTeam("death");
        death.addEntry(ChatColor.RED.toString());
        death.setPrefix(ChatColor.GREEN.toString() + "§a");
        death.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.RED.toString()).setScore(8);

        Team killstreaks = board.registerNewTeam("killstreak");
        killstreaks.addEntry(ChatColor.DARK_GREEN.toString());
        killstreaks.setPrefix(ChatColor.GREEN.toString() + "§a");
        killstreaks.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.DARK_GREEN.toString()).setScore(5);

        Team timer = board.registerNewTeam("time");
        timer.addEntry(ChatColor.DARK_AQUA.toString());
        timer.setPrefix(ChatColor.GREEN.toString() + "§a");
        timer.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.DARK_AQUA.toString()).setScore(2);


      gameboard.put(p.getName(), board);

        createBoard(p);

        new BukkitRunnable(){

            @Override
            public void run() {
                int kill = Main.kills.get(p.getName());
                int death = Main.deaths.get(p.getName());
                int killstreak = Main.killStreak.get(p.getName());
                int times = GameTime.timeleft;

                board.getTeam("kill").setSuffix("     §a" + kill);
                board.getTeam("death").setSuffix("     §a" + death);
                board.getTeam("killstreak").setSuffix("     §a"  + killstreak);
                if(times >60) {
                    board.getTeam("time").setSuffix("     §a" + times/60 + " §amins");
                }else if(times < 60){
                    board.getTeam("time").setSuffix("     §a" + times + " §asecs");
                }
            }
        }.runTaskTimer(ThisPlugin.getPlugin(), 0L, 20L);
    }
    private void createBoard(Player p){
        p.setScoreboard(gameboard.get(p.getName()));
    }
}
