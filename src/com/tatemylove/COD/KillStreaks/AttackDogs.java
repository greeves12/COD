package com.tatemylove.COD.KillStreaks;

import com.tatemylove.COD.Arenas.TDM;
import com.tatemylove.COD.Main;
import com.tatemylove.COD.ThisPlugin.ThisPlugin;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Random;

public class AttackDogs {
    Main main;
    private static AttackDogs attackDogs = null;
    public final static ItemStack dogs = new ItemStack(Material.BONE);

    public AttackDogs(Main m){
        main = m;
        attackDogs = AttackDogs.this;
    }

    public static void settUp(){
        ItemMeta meta = dogs.getItemMeta();
        meta.setDisplayName("§c§lDogs");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("§6RELEASE THE HOUNDS!");
        meta.setLore(lore);
        dogs.setItemMeta(meta);
    }
    public void onKill(EntityDeathEvent e){
        Entity one = e.getEntity();
        Entity two = e.getEntity().getKiller();

        TDM tdm = new TDM(main);

        if(one instanceof Player) {
            if (two instanceof Player) {
                Player p = (Player) e.getEntity();
                Player pp = e.getEntity().getKiller();

                if ((main.PlayingPlayers.contains(p)) && (main.PlayingPlayers.contains(pp))) {
                    if (Main.killStreak.get(pp.getName()) == 8) {
                        pp.getInventory().addItem(dogs);
                        pp.sendMessage(main.prefix + "§c§lYou got Dogs. Right click to deploy!");

                    }
                }
            }
        }
    }

    public void onInteract(PlayerInteractEvent e) {
        GetPlayersOnOtherTeam getPlayersOnOtherTeam = new GetPlayersOnOtherTeam(main);

        if (e.getAction() == Action.RIGHT_CLICK_AIR && e.getPlayer().getInventory().getItemInMainHand().equals(AttackDogs.dogs)) {
            e.getPlayer().sendMessage(main.prefix + "§5You released the hounds");

            for (Player pp : main.PlayingPlayers) {
                pp.sendMessage(main.prefix + e.getPlayer().getName() + " §6§lreleased the hounds!");
            }

            final Player p = e.getPlayer();


            if (!(main.PlayingPlayers.isEmpty())) {
                if (!(getPlayersOnOtherTeam.get(p).isEmpty())) {
                    p.getInventory().setItemInMainHand(null);
                    for (int i = 0; i < 5; i++) {
                        Player pp = getPlayersOnOtherTeam.get(p).get(new Random().nextInt(getPlayersOnOtherTeam.get(p).size()));

                        Location loc = p.getLocation();
                        final Wolf w = p.getWorld().spawn(loc, Wolf.class);

                        w.setMetadata("codAllowHit", new FixedMetadataValue(ThisPlugin.getPlugin(), w));
                        w.setAngry(true);
                        w.setAdult();
                        w.setOwner(p);
                        w.setCollarColor(DyeColor.BLUE);
                        w.setTarget(pp);

                        BukkitRunnable br = new BukkitRunnable() {
                            public void run() {
                                w.remove();
                            }
                        };

                        br.runTaskLater(ThisPlugin.getPlugin(), 20 * 30);
                    }
                } else {
                    p.sendMessage(main.prefix + "§cThere needs to be 1 more player for this killsteak to work!");
                }
            }
        }
    }
}
