package com.tatemylove.COD2.Arenas;

import com.tatemylove.COD2.Achievement.AchievementAPI;
import com.tatemylove.COD2.Events.CODEndEvent;
import com.tatemylove.COD2.Events.CODLeaveEvent;
import com.tatemylove.COD2.Files.ArenasFile;
import com.tatemylove.COD2.Files.PlayerData;
import com.tatemylove.COD2.Inventories.GameInventory;
import com.tatemylove.COD2.KillStreaks.AttackDogs;
import com.tatemylove.COD2.KillStreaks.Mortar;
import com.tatemylove.COD2.KillStreaks.UAV;
import com.tatemylove.COD2.Leveling.LevelRegistryAPI;
import com.tatemylove.COD2.Listeners.PlayerJoin;
import com.tatemylove.COD2.Locations.GetLocations;
import com.tatemylove.COD2.Main;
import com.tatemylove.COD2.MySQL.RegistryAPI;
import com.tatemylove.COD2.Perks.Scavenger;
import com.tatemylove.COD2.Tasks.CountDown;
import com.tatemylove.COD2.ThisPlugin;
import me.zombie_striker.qg.api.QualityArmory;
import me.zombie_striker.qg.guns.Gun;
import net.minecraft.server.v1_16_R2.PacketPlayInClientCommand;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

public class GunGame implements Listener {

    private   ArrayList<Player> PlayingPlayers = new ArrayList<>();

    public  HashMap<UUID, Integer> kills = new HashMap<>();
    public  HashMap<UUID, Integer> deaths = new HashMap<>();
    public  HashMap<UUID, Integer> killstreak = new HashMap<>();
    private HashMap<UUID, Integer> currentWeapon = new HashMap<>();

    private ArrayList<String> loadedGuns = new ArrayList<>(ThisPlugin.getPlugin().getConfig().getStringList("GunGame"));

    private BossBar bossBar = Bukkit.getServer().createBossBar("<PENDING>: ", BarColor.BLUE, BarStyle.SEGMENTED_6);
    private ArrayList<Integer> spawns = new ArrayList<>();


    private HashMap<UUID, ArrayList<String>> loadedPerks = new HashMap<>();


    private  String arena = "";




    public  void assignTeams(String name){

        Bukkit.getServer().getPluginManager().registerEvents(this, ThisPlugin.getPlugin());
        arena = name;

        for(String i : ArenasFile.getData().getConfigurationSection("Arenas." + name + ".Spawns.").getKeys(false)){
            int k = Integer.parseInt(i);

            spawns.add(k);
        }

        if(Main.WaitingPlayers.size() >ThisPlugin.getPlugin().getConfig().getInt("max-players")) {
            for (int x = 0; x < ThisPlugin.getPlugin().getConfig().getInt("max-players"); x++) {
                PlayingPlayers.add(Main.WaitingPlayers.get(0));
                Main.AllPlayingPlayers.add(Main.WaitingPlayers.get(0));
                Main.WaitingPlayers.remove(0);
            }
        }else{
        PlayingPlayers.addAll(Main.WaitingPlayers);
        Main.AllPlayingPlayers.addAll(Main.WaitingPlayers);
        Main.WaitingPlayers.clear();

        }


        startTDM(name);
        startCountdown(name);

    }

    public  void startTDM(String name){
        for(int ID =0; ID < PlayingPlayers.size(); ID++){
            final Player p = PlayingPlayers.get(ID);

            kills.put(p.getUniqueId(), 0);
            deaths.put(p.getUniqueId(), 0);
            killstreak.put(p.getUniqueId(), 0);

            currentWeapon.put(p.getUniqueId(), 0);

            setBoard(p);

            p.getInventory().clear();




            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(20);
            p.setHealth(20);
            p.sendMessage(Main.prefix + "§aGame starting. Arena: §e" + name);

            p.teleport(GetArena.getNumericSpawn(p, name, ID));
            Random rand = new Random();

            p.setCustomName(ChatColor.getByChar(Integer.toHexString(rand.nextInt(16))) + p.getName());
            p.setCustomNameVisible(true);

            p.setPlayerListName(ChatColor.getByChar(Integer.toHexString(rand.nextInt(16))) + p.getName());

            Color c = Color.fromBGR(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255));
            p.getInventory().setHelmet(getColorArmor(Material.LEATHER_HELMET, c));
            p.getInventory().setChestplate(getColorArmor(Material.LEATHER_CHESTPLATE, c));
            p.getInventory().setLeggings(getColorArmor(Material.LEATHER_LEGGINGS, c));
            getNewLoadout(p);


        }
    }

    public void endTDM(String name){
        Bukkit.getServer().getPluginManager().callEvent(new CODEndEvent(PlayingPlayers, name, ArenasFile.getData().getString("Arenas." + name + ".Type")));

        for(Player p : PlayingPlayers){

            if(p.getUniqueId().equals(getTopPlayer())) {

                RegistryAPI.registerWin(p);
                LevelRegistryAPI.addExp(p, ThisPlugin.getPlugin().getConfig().getInt("exp-win"));
                AchievementAPI.grantAchievement(p, "Victory");
            }else{
                LevelRegistryAPI.addExp(p, ThisPlugin.getPlugin().getConfig().getInt("exp-loss"));
            }
            p.teleport(GetLocations.getLobby());
            p.getInventory().clear();
            GameInventory.lobbyInv(p);
            p.sendMessage(Main.prefix + "§6Winner: §a" + Bukkit.getPlayer(getTopPlayer()).getName());
            if(deaths.get(p.getUniqueId()) != 0){
                double kd = (double) kills.get(p.getUniqueId()) / deaths.get(p.getUniqueId());
                p.sendMessage(Main.prefix + "§eYour KD is §a" +kd);
            }else{
                p.sendMessage(Main.prefix + "§eYour KD is §a" + kills.get(p.getUniqueId()));
            }
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setPlayerListName(p.getName());
            p.setCustomNameVisible(true);
            p.setPlayerListName(p.getName());
            p.removePotionEffect(PotionEffectType.SPEED);

            Main.AllPlayingPlayers.remove(p);
            bossBar.removePlayer(p);
            p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);

            if(ThisPlugin.getPlugin().getConfig().getBoolean("BungeeCord.enabled")){
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bout);

                try{
                    out.writeUTF("Connect");
                    out.writeUTF(ThisPlugin.getPlugin().getConfig().getString("BungeeCord.fall-back"));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        Main.arenas.add(name);
        Main.onGoingArenas.remove(name);

        if(Main.arenas.size() == 1) {
            new CountDown().runTaskTimer(ThisPlugin.getPlugin(), 0, 20);
        }
        Main.WaitingPlayers.addAll(PlayingPlayers);
        PlayingPlayers.clear();




    }



    private  ItemStack getMaterial(Material m, String name, ArrayList<String> lore){
        ItemStack s = new ItemStack(m);
        ItemMeta meta = s.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        s.setItemMeta(meta);
        return s;
    }
    private  ItemStack getColorArmor(Material m, Color c) {
        ItemStack i = new ItemStack(m, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta) i.getItemMeta();
        meta.setColor(c);
        i.setItemMeta(meta);
        return i;
    }


    private  void startCountdown(String name){

        for(Player p : PlayingPlayers){
            bossBar.addPlayer(p);
            bossBar.setVisible(true);

        }

        new BukkitRunnable(){
            int time = ThisPlugin.getPlugin().getConfig().getInt("game-time");

            @Override
            public void run() {



                if(Bukkit.getPlayer(getTopPlayer()) != null) {
                    bossBar.setTitle("§9§lLeader: §e" + Bukkit.getPlayer(getTopPlayer()).getName() + "§7 «§f" + formatThis(time) + "§7»");
                }
                if(PlayingPlayers.size() < ThisPlugin.getPlugin().getConfig().getInt("min-players")) {
                    endTDM(arena);
                    cancel();
                }


                if(currentWeapon.get(getTopPlayer()) >= loadedGuns.size()){
                    endTDM(name);
                    cancel();
                }


                if(time == 0){
                    endTDM(name);


                    cancel();
                }
                time-=1;
            }
        }.runTaskTimer(ThisPlugin.getPlugin(), 0, 10);
    }

    private String formatThis(int time){
        long minutes = time /60;
        int secs = time %60;

        return (minutes+":"+secs);


    }

    private UUID getTopPlayer(){
        return Collections.max(kills.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Integer getTopValue(){
        return Collections.max(kills.entrySet(), Map.Entry.comparingByValue()).getValue();
    }

    @EventHandler
    public void filterChat(AsyncPlayerChatEvent e){
        Player p = e.getPlayer();

        if(ThisPlugin.getPlugin().getConfig().getBoolean("cod-chat")) {
            if (PlayingPlayers.contains(p)) {
                for (Player pp : PlayingPlayers) {

                    if (PlayerData.getData().getInt("Players." + p.getUniqueId().toString() + ".Prestige") == 0) {
                        pp.sendMessage("§8[§bLevel " + PlayerData.getData().getInt("Players." + p.getUniqueId().toString() + ".Level") + "§8] §a" + p.getName() + ": §7" + e.getMessage());

                    } else {
                        pp.sendMessage("§8[§3Prestige " + PlayerData.getData().getInt("Players." + p.getUniqueId().toString() + ".Prestige") + "§8] [§bLevel " + PlayerData.getData().getInt("Players." + p.getUniqueId().toString() + ".Level") + "§8] §a" + p.getName() + ": §7" + e.getMessage());
                    }
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e){

        if(e.getEntity() instanceof Player ) {
            Player p = (Player) e.getEntity();
            Player pp = p.getKiller();

            if (pp != null) {
                kills.put(pp.getUniqueId(), kills.get(pp.getUniqueId()) + 1);
                deaths.put(p.getUniqueId(), deaths.get(p.getUniqueId()) +1);
                killstreak.put(pp.getUniqueId(), killstreak.get(pp.getUniqueId()) +1);

                RegistryAPI.registerDeath(p);
                RegistryAPI.registerKill(pp);

                if(RegistryAPI.getKills(pp) == 1){
                    AchievementAPI.grantAchievement(pp, "FirstBlood");
                }else if(RegistryAPI.getKills(pp) == 10){
                    AchievementAPI.grantAchievement(pp, "10Kill");
                }else if(RegistryAPI.getKills(pp) == 50){
                    AchievementAPI.grantAchievement(pp, "50Kills");
                }else if(RegistryAPI.getKills(pp) == 200){
                    AchievementAPI.grantAchievement(pp, "200Kills");
                }

                LevelRegistryAPI.addExp(pp, ThisPlugin.getPlugin().getConfig().getInt("exp-kill"));

                for(Player ppp : PlayingPlayers){
                    ppp.sendMessage(Main.prefix + "§dPlayer: §a" + pp.getName() + " §dkilled §a " + p.getName());
                }

            }else{
                deaths.put(p.getUniqueId(), deaths.get(p.getUniqueId()) +1);
                RegistryAPI.registerDeath(p);
                for(Player ppp : PlayingPlayers){
                    ppp.sendMessage(Main.prefix + "§dPlayer: §a " + p.getName() + " §ddied");
                }

            }
            //  new UAV().onKill(e, killstreak, PlayingPlayers);
            // new AttackDogs().onKill(e, killstreak, PlayingPlayers);
            // new Mortar().onEntityKill(e, PlayingPlayers, killstreak);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e){
        //  new UAV().onUse(e, RedTeam, BlueTeam, PlayingPlayers);
        //  new AttackDogs().onInteract(e, PlayingPlayers, RedTeam, BlueTeam);
        //   new Mortar().onInteract(e, PlayingPlayers, RedTeam, BlueTeam);

    }

    @EventHandler
    public void onDEath(PlayerDeathEvent e){
        if(PlayingPlayers.contains(e.getEntity())){
            e.setDeathMessage(null);
            e.getDrops().clear();

            killstreak.put(e.getEntity().getUniqueId(), 0);

            Main.cooldowns.add(e.getEntity());
            new BukkitRunnable(){

                @Override
                public void run() {
                    Main.cooldowns.remove(e.getEntity());
                }
            }.runTaskLater(ThisPlugin.getPlugin(), 60);


        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Player){
            if(e.getDamager() instanceof Player){
                Player p = (Player) e.getEntity();
                Player pp = (Player) e.getDamager();


                if(PlayingPlayers.contains(p) && PlayingPlayers.contains(pp)){
                    if(Main.cooldowns.contains(p)){
                        e.setCancelled(true);
                    }


                    if(pp.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD){
                        e.setDamage(100);
                        if(currentWeapon.get(p.getUniqueId()) > 0) {
                            currentWeapon.put(p.getUniqueId(), currentWeapon.get(p.getUniqueId()) - 1);
                            p.sendMessage(Main.prefix + "§bOh my! You've been demoted one gun.");
                        }
                    }else if(pp.getInventory().getItemInMainHand().getType() != Material.AIR && pp.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) {
                       // if(!disallowHit(p, pp)) {
                            currentWeapon.put(pp.getUniqueId(), currentWeapon.get(pp.getUniqueId()) + 1);

                            pp.getInventory().clear();
                            new BukkitRunnable() {

                                @Override
                                public void run() {
                                    getNewLoadout(pp);
                                }
                            }.runTaskLater(ThisPlugin.getPlugin(), 10);

                       // }
                    }

                }
            }
        }
    }
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e){
        if(PlayingPlayers.contains(e.getPlayer())){
            Random rand = new Random();
            e.setRespawnLocation(GetArena.getNumericSpawn(e.getPlayer(), arena,spawns.get(rand.nextInt(spawns.size()))));

            getNewLoadout(e.getPlayer());
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e){
        Player p = e.getPlayer();

        if(PlayingPlayers.contains(p)) {
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            PlayingPlayers.remove(e.getPlayer());

            Main.AllPlayingPlayers.remove(p);

            p.removePotionEffect(PotionEffectType.SPEED);

            Bukkit.getServer().getPluginManager().callEvent(new CODLeaveEvent(e.getPlayer()));
        }

    }

    private boolean disallowHit(Player one, Player two){
        double x1 = one.getLocation().getX();
        double z1 = one.getLocation().getZ();

        double x2 = two.getLocation().getX();
        double z2 = two.getLocation().getZ();


        if(x1 > x2 && z1 > z2){
            double fin = x1-x2;
            double fins = z1-z2;

            if(fin <3.5){
                return true;
            }
        }else if(x1 < x2 && z1 > z2){
            double fin = x2-x1;
            double fins = z1-z2;
            if(fin <3.5){
                return true;
            }
        }  else      if(x1 > x2 && z1 < z2){
            double fin = x1-x2;
            double fins = z2-z1;
            if(fin <3.5){
                return true;
            }
        }else if(x1 < x2 && z1 < z2){
            double fin = x2-x1;
            double fins = z2-z1;
            if(fin <3.5){
                return true;
            }
        }
        return false;
    }

    private  void getNewLoadout(Player p){
       /* ArrayList<String> ss = new ArrayList<>();
        ss.add(PlayerData.getData().getString("Players." + p.getUniqueId().toString() + ".Classes." + PlayerJoin.clazz.get(p.getUniqueId()) + ".Perk1"));
        ss.add(PlayerData.getData().getString("Players." + p.getUniqueId().toString() + ".Classes." + PlayerJoin.clazz.get(p.getUniqueId()) + ".Perk2"));
        ss.add(PlayerData.getData().getString("Players." + p.getUniqueId().toString() + ".Classes." + PlayerJoin.clazz.get(p.getUniqueId()) + ".Perk3"));*/

        //loadedPerks.put(p.getUniqueId(), ss);


        if(currentWeapon.get(p.getUniqueId()) < loadedGuns.size()) {
            Gun g = QualityArmory.getGunByName(loadedGuns.get(currentWeapon.get(p.getUniqueId())));

            p.getInventory().setItem(0, g.getItemStack());

            Random random = new Random();

            Color c = Color.fromBGR(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            p.getInventory().setHelmet(getColorArmor(Material.LEATHER_HELMET, c));
            p.getInventory().setChestplate(getColorArmor(Material.LEATHER_CHESTPLATE, c));
            p.getInventory().setLeggings(getColorArmor(Material.LEATHER_LEGGINGS, c));
            p.getInventory().setBoots(getColorArmor(Material.LEATHER_BOOTS, c));
           // p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true));

            p.getInventory().setItem(8, getMaterial(Material.IRON_SWORD, "§bKnife", null));
        }
    }

    private   ItemStack getColorArmor2(Material m, Color c) {
        ItemStack i = new ItemStack(m, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta) i.getItemMeta();
        meta.setColor(c);
        meta.addEnchant(Enchantment.PROTECTION_FALL, 1, true);
        i.setItemMeta(meta);
        return i;
    }


    private HashMap<String, Scoreboard> gameboard = new HashMap<>();

    private void setBoard(Player p){
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective objective = board.registerNewObjective("Gameboard", "dummy");

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName("§b§lYour Scores");

      /*  Score kills = objective.getScore("");
        kills.setScore(12);*/

        Score blank2 = objective.getScore("  ");
        blank2.setScore(10);

       /* Score deaths = objective.getScore("");
        deaths.setScore(9);*/

        Score blank3 = objective.getScore("   ");
        blank3.setScore(7);
        Score blank4 = objective.getScore("    ");
        blank4.setScore(13);

       /* Score killstreak = objective.getScore("");
        killstreak.setScore(6);*/


        Team kill = board.registerNewTeam("kill");
        kill.addEntry(ChatColor.AQUA.toString());
        kill.setPrefix(ChatColor.GREEN.toString() + "§a");
        kill.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.AQUA.toString()).setScore(12);

        Team death = board.registerNewTeam("death");
        death.addEntry(ChatColor.RED.toString());
        death.setPrefix(ChatColor.GREEN.toString() + "§a");
        death.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.RED.toString()).setScore(9);

        Team killstreaks = board.registerNewTeam("killstreak");
        killstreaks.addEntry(ChatColor.DARK_GREEN.toString());
        killstreaks.setPrefix(ChatColor.GREEN.toString() + "§a");
        killstreaks.setSuffix(ChatColor.GREEN.toString() + "0");
        objective.getScore(ChatColor.DARK_GREEN.toString()).setScore(6);

        gameboard.put(p.getName(), board);
        createBoard(p);

        new BukkitRunnable(){

            @Override
            public void run() {

                int kill = kills.get(p.getUniqueId());
                int deathh = deaths.get(p.getUniqueId());
                int killstreakz = killstreak.get(p.getUniqueId());

                board.getTeam("kill").setSuffix("§aKills: §6" + kill);
                board.getTeam("death").setSuffix("§aDeaths: §6" + deathh);
                board.getTeam("killstreak").setSuffix("§aKill Streak: §6"  + killstreakz);
            }
        }.runTaskTimer(ThisPlugin.getPlugin(), 0, 20);
    }

    private void createBoard(Player p){
        p.setScoreboard(gameboard.get(p.getName()));
    }
}
