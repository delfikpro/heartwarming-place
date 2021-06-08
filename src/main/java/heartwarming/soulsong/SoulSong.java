package heartwarming.soulsong;

import clepto.bukkit.B;
import clepto.bukkit.event.EventContext;
import clepto.bukkit.world.Label;
import clepto.cristalix.WorldMeta;
import heartwarming.HeartwarmingPlugin;
import lombok.Data;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityMountEvent;
import ru.cristalix.npcs.server.Npc;
import ru.cristalix.npcs.server.Npcs;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Это был невероятно красивый эпилог.
 * Будь счастлива.
 */
public class SoulSong {

    public void init() {

        WorldMeta map = HeartwarmingPlugin.worldMeta;

        Npcs.init(B.plugin);

        Label alestaLoc = map.requireLabel("alesta");
        alestaLoc.setYaw(30);
        alestaLoc.setPitch(38);


        Npcs.spawn(Npc.builder()
                .name("§7Алеста")
                .type(EntityType.PLAYER)
                .location(alestaLoc)
                .slimArms(true)
                .skinUrl("https://texture.namemc.com/26/1d/261dfb8b87e4f1ce.png")
                .skinDigest("261dfb8b87e4f1ce").build());


        Label llamaLoc = map.requireLabel("llama");
        Llama llama = map.getWorld().spawn(llamaLoc, Llama.class);
        llama.setColor(Llama.Color.WHITE);
        llama.setCustomName("Лама Алесты");
        llama.setCustomNameVisible(true);
        llama.setInvulnerable(true);
        llama.setAge(0);

        B.repeat(1, () -> {
            if (llama.getLocation().distanceSquared(llamaLoc) > 100) llama.teleport(llamaLoc);
        });

        map.getWorld().setGameRuleValue("doDayLightCycle", "false");
        map.getWorld().setTime(13900);

        EventContext eventContext = new EventContext(e -> true);
        eventContext.on(EntityDamageEvent.class, e -> {
            if (e.getEntity() == llama) e.setCancelled(true);
        });
        eventContext.on(PlayerJoinEvent.class, e -> {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100000000, 1, true, false));
        });
        eventContext.on(PlayerRespawnEvent.class, e -> {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100000000, 1, true, false));
        });

        eventContext.on(EntityMountEvent.class, e -> {
            if (e.getMount().getType() == EntityType.LLAMA) e.setCancelled(true);
        });

        eventContext.on(BlockPlaceEvent.class, e -> {
            if (e.getBlockPlaced().getLocation().distanceSquared(alestaLoc) < 36) e.setCancelled(true);
        });

        @Data
        class A {
            long time;
            boolean out_flag;
        }

        new ArrayList<A>().sort(Comparator.comparingLong(A::getTime).thenComparing(A::isOut_flag));



    }

}
