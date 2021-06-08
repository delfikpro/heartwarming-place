package heartwarming;

import clepto.bukkit.B;
import clepto.bukkit.routine.BukkitDoer;
import clepto.bukkit.routine.Doer;
import clepto.bukkit.world.Label;
import clepto.cristalix.Cristalix;
import clepto.cristalix.WorldMeta;
import com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent;
import dev.implario.bukkit.item.ItemBuilder;
import heartwarming.game.Slot;
import heartwarming.game.gameof2048.Direction;
import heartwarming.mod.ModManager;
import heartwarming.script.ScriptManager;
import heartwarming.soulsong.SoulSong;
import implario.humanize.TimeFormatter;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Items;
import net.minecraft.server.v1_12_R1.NBTBase;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftMetaSkull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import ru.cristalix.boards.bukkitapi.Board;
import ru.cristalix.boards.bukkitapi.Boards;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.account.IAccountService;
import ru.cristalix.core.build.models.Point;
import ru.cristalix.core.chat.ChatTextComponent;
import ru.cristalix.core.chat.IChatView;
import ru.cristalix.core.event.PermissionsLoadEvent;
import ru.cristalix.core.map.*;
import ru.cristalix.core.permissions.IGroup;
import ru.cristalix.core.permissions.IPermissionContext;
import ru.cristalix.core.permissions.IPermissionService;
import ru.cristalix.core.permissions.StaffGroups;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmId;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.realm.RealmStatus;
import ru.cristalix.core.stats.IStatService;
import ru.cristalix.core.stats.PlayerScope;
import ru.cristalix.core.stats.UserManager;
import ru.cristalix.core.stats.impl.StatService;
import ru.cristalix.core.stats.impl.network.StatServiceConnectionData;
import ru.cristalix.core.text.TextFormat;
import ru.cristalix.core.util.UtilV3;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static implario.humanize.TimeFormatter.Interval.TICK;
import static java.lang.Enum.valueOf;

public class HeartwarmingPlugin extends JavaPlugin implements Listener {

    @Getter
    public static HeartwarmingPlugin instance;
    public static final PlayerScope<Stats> statScope = new PlayerScope<>("heartwarming", Stats.class);

    private Location spawn;

    public static UserManager<User> userManager;
    public static WorldMeta worldMeta;

    private final Map<Location, Slot> slots = new HashMap<>();
    private Label arena;

    @SneakyThrows
    @Override
    public void onEnable() {

        B.plugin = instance = this;

        ScriptManager.init();
        RealmInfo info = IRealmService.get().getCurrentRealmInfo();
//        info.setInitialPayload(new byte[] {62, 45, 10});
//        System.out.println(new String(info.getInitialPayload()));
//        info.setTrustMyHost(true);

        info.setStatus(RealmStatus.GAME_STARTED_CAN_SPACTATE);
        info.setReadableName("Уютное местечко");
        info.setGroupName("Уютное местечко");

        CoreApi core = CoreApi.get();
        core.registerService(IMapService.class, new MapService());
        IStatService statService = new StatService(core.getPlatformServer(), StatServiceConnectionData.fromEnvironment());
        core.registerService(IStatService.class, statService);

        statService.useScopes(statScope);

        userManager = statService.registerUserManager(
                context -> new User(context.getUuid(), context.getName(), context.getData(statScope)),
                (user, context) -> {
                    user.setOnline(user.getJoinOnline() + (System.currentTimeMillis() - user.getJoinTime()));
                    context.store(statScope, user.getStats());
                }

        );

        TimeFormatter formatter = TimeFormatter.builder().accuracy(0).excludeIntervals(TICK).build();


        B.regCommand((sender, args) -> {
            User user = userManager.getUser(sender);
            sender.sendMessage("§eСтатистика:");
            sender.sendMessage("§eНаиграно: " + formatter.format(Duration.ofMillis(user.getPlayedTime())));
            return null;
        }, "stats");

        MapVersion latest = IMapService.get().getLatestMapByGameTypeAndMapName("MISC", "DelfikPro2")
        .orElseThrow(() -> new NoSuchElementException("MISC/DelfikPro2 not found")).getLatest();

//        MapVersion latest = IMapService.get().getLatestMapByGameTypeAndMapName("HUB", "Spring2021").get().getLatest();
//
//        Optional<MapListDataItem> op = IMapService.get().getAllMapsByGameTypeAndMapName("HUB", "Spring2021");
//
//        if (op.isPresent()) {
//            op.get().getVersions().forEach(v -> {
//                System.out.println(v.getData().getName() + " hello");
//            });
//        } else System.out.println("goodbye");

        LoadedMap<World> map = IMapService.get().loadMap(latest, BukkitWorldLoader.INSTANCE).get();
        map.getWorld().setGameRuleValue("doTileDrops", "false");


        worldMeta = new WorldMeta(map);

        Label spawnLabel = worldMeta.requireLabel("spawn");
        this.spawn = spawnLabel;
        this.spawn.setYaw(Float.parseFloat(spawnLabel.getTag()));

//        this.spawn = new Location(worldMeta.getWorld(), 0, 0, 0);

        Board onlineTop = Boards.newBoard();
        Label topLabel = worldMeta.requireLabel("top");
        topLabel.setYaw(Float.parseFloat(topLabel.getTag()));
        topLabel.add(0, 5, 0);
        onlineTop.setLocation(topLabel);
        onlineTop.setTitle("§d§lАфкашеры");

        onlineTop.addColumn("§d#", 10);
        onlineTop.addColumn("§dИгрок", 90);
        onlineTop.addColumn("§dОнлайн", 90);

        Boards.addBoard(onlineTop);

        Doer doer = new BukkitDoer(this);

        doer.every(10).seconds(() -> {
            statService.getLeaderboard(statScope, "online", 10).thenAccept(r -> {
                onlineTop.clearContent();
                int i = 1;
                for (Stats stats : r) {
                    onlineTop.addContent(stats.getId(), "§d" + i++, stats.getName(),
                            formatter.format(Duration.ofMillis(stats.getOnline())));
                }
                onlineTop.updateContent();
            });
        });

        for (Location slot : worldMeta.getLabels("slot")) {
            slot.getBlock().setType(Material.DIAMOND_BLOCK);
            Location location = slot.getBlock().getLocation();
            slots.put(location, new Slot(location));
        }

        SoulSong soulSong = new SoulSong();
        soulSong.init();

//        for (int x = -50; x < 50; x++) {
//            System.out.println("Loading chunks - " + (x + 51) + "%");
//            for (int z = -50; z < 50; z++) {
//
//                worldMeta.getWorld().loadChunk(x, z, false);
//
//            }
//        }

//        int i = 0;
//        for (Entity entity : worldMeta.getWorld().getEntities()) {
//            if (entity instanceof ArmorStand) {
//                net.minecraft.server.v1_12_R1.ItemStack helmet = ((CraftArmorStand) entity).getHandle().bz.get(3);
//                if (helmet.item == Items.SKULL) {
//                    NBTTagCompound tag = helmet.tag;
////                    System.out.println(tag);
//                    if (tag != null) {
//                        NBTBase skullOwner = tag.get("SkullOwner");
//                        if (skullOwner != null) {
//                            String name = ((NBTTagCompound) skullOwner).getString("Name");
//                            if (name != null && name.length() > 2) {
//                                Location location = entity.getLocation();
//                                System.out.println(name + " head found on " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
//                            }
//                        }
//                    }
//                }
//
//            }
//        }


        //		Box field = worldMeta.getBoxes("field").values().iterator().next();
        //
        //		implario.math.V3 dims = field.getDimensions();
        //
        //		System.out.println(dims);
        //		this.tetris = new Tetris(((int) ((dims.getX() + 1) * (dims.getZ() + 1))), (int) (dims.getY() + 1),
        //				pair -> new Location(map.getWorld(), field.getMin().x + pair[0], field.getMin().y + pair[1], field.getMin().z));
        //
        //		this.tetris.spawnNew();
        //		B.repeat(10, () -> {
        //			if (!this.tetris.update()) {
        //				this.tetris.clear();
        //			}
        //		});

        List<String> sharks = Arrays.asList(
                "a6ab9f04-acb7-11eb-acca-1cb72caa35fd",
                "e7c13d3d-ac38-11e8-8374-1cb72caa35fd"
        );

        B.regCommand((sender, args) -> {
            if (!sharks.contains(sender.getUniqueId().toString()))
                return "§cЭта команда доступна только группе §9Пушистые акулы§c.";
            String realm = args[0];
            Cristalix.transfer(Collections.singleton(sender.getUniqueId()), RealmId.of(realm));
            return "§aВы были перемещены на реалм §f" + realm;
        }, "jump");

        Bukkit.getPluginCommand("bs").setExecutor((sender, command, s, args) -> {
            if (sender.isOp())
                Cristalix.transfer(Collections.singletonList(((Player) sender).getUniqueId()), RealmId.of("BSL-TEST-1"));
            return true;
        });


        new ModManager().init();

        B.regCommand((sender, args) -> {
            sender.setResourcePack(args[0], UUID.randomUUID().toString().substring(0, 16));
            return "Resourcepack changed";
        }, "rp");

        arena = worldMeta.requireLabel("arena");

        ArmorStand arenaStand = worldMeta.getWorld().spawn(arena.toCenterLocation(), ArmorStand.class);
        arenaStand.setVisible(false);
        arenaStand.setBasePlate(false);
        arenaStand.setCustomName("§dPvP-арена");
        arenaStand.setGravity(false);
        arenaStand.setMarker(true);
        arenaStand.setInvulnerable(true);
        arenaStand.setCustomNameVisible(true);

        B.repeat(1, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                User user = userManager.getUser(p);
                if (p.getLocation().distanceSquared(arena) < 81) {
                    if (!user.isPvp()) {
                        p.setAllowFlight(false);
                        user.setCombo(0);
                        user.setPvp(true);
                    }
                } else {
                    if (user.isPvp()) {
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        user.setCombo(0);
                        user.setPvp(false);
                    }
                }
            }
        });

        B.regCommand((sender, args) -> {
            return sender.getLocation().toString();
        }, "info");

        Bukkit.getPluginManager().registerEvents(this, this);

        //		Bukkit.getPluginCommand("gamemode").setExecutor((sender, a, b, args) -> {
        //			Player player = (Player) sender;
        //			if (args.length == 0) {
        //				sender.sendMessage("§eИспользование: §f/gamemode 0-3");
        //				return true;
        //			}
        //			player.setGameMode(GameMode.getByValue(Integer.parseInt(args[0])));
        //			sender.sendMessage("§eИгровой режим изменён.");
        //			return true;
        //		});

    }

    @EventHandler
    public void handle(PlayerDeathEvent e) {
        e.setKeepInventory(true);
        e.setDeathMessage("");
    }

    @EventHandler
    public void handle(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.DIAMOND_BLOCK) return;

        Slot slot = slots.get(block.getLocation());
        if (slot == null) return;
        User user = userManager.getUser(e.getPlayer());
        Player player = user.getPlayer();
        if (user.getGame() != null) {
            player.sendMessage("§cВы уже играете.");
            return;
        }
        if (slot.getGame() != null) {
            player.sendMessage("§cЭтот слот уже занят.");
        }

        e.setCancelled(true);
        B.destroy(block);

//		user.setGame(new GameOf2048(Collections.singletonList(user), block.getLocation()));

    }

//    @EventHandler
//    public void handle(ChunkUnloadEvent e) {
//        e.setCancelled(true);
//    }


    @EventHandler
    public void handle(PlayerDropItemEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntityType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER) {
            User user = userManager.getUser(e.getDamager().getUniqueId());
            long time = System.currentTimeMillis();
            if (user.getLastHitTime() + 1000 < time) {
                user.setCombo(0);
            }
            user.setLastHitTime(time);
            user.setCombo(user.getCombo() + 1);
            if (user.getCombo() >= 3)
                user.getPlayer().sendActionBar("Комбо: §ex" + user.getCombo());
        }
    }

    //	@EventHandler
    //	public void handle(PlayerInteractEvent e) {
    //
    //		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
    //			int slot = e.getPlayer().getInventory().getHeldItemSlot();
    //			if (slot == 4) tetris.shift(false);
    //			else if (slot == 5) tetris.shift(true);
    //			else if (slot == 6) tetris.rotate();
    //			else if (slot == 3) tetris.drop();
    //		}
    //
    //		Block clickedBlock = e.getClickedBlock();
    //		if (clickedBlock == null || lock) return;
    //		V3 v3 = new V3(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
    //		for (Point control : controls) {
    //			if (control.getV3().distanceSquared(v3) < 9) {
    //				gameOf2048.shift(The2048.Direction.byName(control.getTag()));
    //				Bukkit.getScheduler().runTaskLater(this, () -> {
    //					if (!gameOf2048.addRandom()) {
    //						The2048.Cell[][] cells = gameOf2048.getCells();
    //						for (int i = 0; i < cells.length; i++) {
    //							for (int j = 0; j < cells[i].length; j++) {
    //								cells[i][j].getBlock().remove();
    //								cells[i][j] = null;
    //							}
    //						}
    //						gameOf2048.addRandom();
    //					}
    //					lock = false;
    //				}, 6);
    //				lock = true;
    //				return;
    //			}
    //		}
    //	}

    @EventHandler
    public void handle(PlayerArmorStandManipulateEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler
    public void handle(EntityAirChangeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(EntityBlockFormEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(EntityChangeBlockEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        B.bc(Cristalix.getDisplayName(e.getPlayer()) + " » §f" + e.getMessage());
    }

    public static String getDisplayName(Player player) {
        IPermissionContext context = IPermissionService.get().getPermissionContextDirect(player.getUniqueId());
        IGroup group = context.getBestGroup();
        String color = context.getColor() == null ? "" : context.getColor();
        String prefix = group.getPrefix();
        return group.getPrefixColor() + (prefix != null && !prefix.isEmpty() ? prefix + " " : "") + group.getNameColor() + color + player.getName();
    }

    @EventHandler
    public void handle(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockPlaceEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            Location location = e.getBlockPlaced().getLocation();
            if (e.getBlockPlaced().getType() != Material.SANDSTONE ||
                    location.distanceSquared(spawn) < 25 ||
                    location.distanceSquared(arena) < 81)
                e.setCancelled(true);
            else {
                e.getPlayer().getInventory().setItem(1, new ItemStack(Material.SANDSTONE, 64, (short) 2));
                Block block = e.getBlock();
                Bukkit.getScheduler().runTaskLater(this, () ->
                {
                    BlockPosition blockposition = new BlockPosition(block.getX(), block.getY(), block.getZ());
                    ((CraftWorld) block.getLocation().getWorld()).getHandle().setAir(blockposition, true);
                }, 100);
            }
        }
    }

    @EventHandler
    public void handle(ProjectileLaunchEvent e) {
        if (e.getEntity().getType() == EntityType.ENDER_PEARL) {
            ProjectileSource shooter = e.getEntity().getShooter();
            if (shooter instanceof Player) {
                Bukkit.getScheduler().runTask(this, () -> ((Player) shooter).getInventory().setItem(0, new ItemStack(Material.ENDER_PEARL, 16)));
            }
        }
    }

    @EventHandler
    public void handle(BlockExplodeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(EntityExplodeEvent e) {
        e.setCancelled(true);
    }

    //	@EventHandler
    //	public void handle(EntityDamageByEntityEvent e) {
    //	    if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
    //	    	if (((Player) e.getDamager()).getGameMode()
    //	    	== GameMode.CREATIVE &&
    //			((Player) e.getEntity()).getGameMode() != GameMode.CREATIVE) {
    //	    		e.setCancelled(true);
    //	    		e.getDamager().sendMessage("§сНе бей его, ты же в креативе...");
    //			}
    //		}
    //	}

    @EventHandler
    public void handle(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER)
            return;
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                userManager.getUser(((Player) e.getEntity())).isPvp())
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(PlayerMoveEvent e) {
        if (e.getTo().getY() < 0) e.getPlayer().teleport(spawn);
    }

    @EventHandler
    public void handle(PlayerInitialSpawnEvent e) {
        e.setSpawnLocation(spawn);
    }

    @EventHandler
    public void handle(PlayerSpawnLocationEvent e) {
        e.setSpawnLocation(spawn);
    }

    @EventHandler
    public void handle(BlockSpreadEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockFromToEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockBurnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockFadeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockFormEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockPhysicsEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(BlockIgniteEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void handle(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        inventory.setItem(0, new ItemStack(Material.ENDER_PEARL, 16));
        inventory.setItem(1, new ItemStack(Material.SANDSTONE, 64, (short) 2));

        player.sendMessage("§eДобро пожаловать на Уютное местечко.");
        player.sendMessage("§eЭто технический сервер, на котором разработчики тестируют разную магию.");
        player.sendMessage("§eКарта, которую пострили игроки: https://implario.dev/hw.schematic");


//        inventory.setItem(3, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_down").build().asBukkitMirror());
//        inventory.setItem(4, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_left").build().asBukkitMirror());
//        inventory.setItem(5, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_right").build().asBukkitMirror());
//        inventory.setItem(6, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "reload").build().asBukkitMirror());
        //		e.getPlayer().sendMessage("§fУютный совет: §eУ вас есть доступ к команде §f/gamemode§e.");
    }

    @EventHandler
    public void handle(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
    }

    @EventHandler
    public void handle(InventoryClickEvent e) {
        if (e.getWhoClicked().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

}
