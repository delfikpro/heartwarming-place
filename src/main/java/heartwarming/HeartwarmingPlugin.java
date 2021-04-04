package heartwarming;

import clepto.bukkit.B;
import clepto.bukkit.item.ItemBuilder;
import clepto.cristalix.Cristalix;
import clepto.cristalix.WorldMeta;
import com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent;
import heartwarming.game.Slot;
import heartwarming.game.gameof2048.Direction;
import heartwarming.game.gameof2048.GameOf2048;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutCustomPayload;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.build.models.Point;
import ru.cristalix.core.display.DisplayChannels;
import ru.cristalix.core.display.messages.Mod;
import ru.cristalix.core.map.*;
import ru.cristalix.core.permissions.IGroup;
import ru.cristalix.core.permissions.IPermissionContext;
import ru.cristalix.core.permissions.IPermissionService;
import ru.cristalix.core.realm.IRealmService;
import ru.cristalix.core.realm.RealmId;
import ru.cristalix.core.realm.RealmInfo;
import ru.cristalix.core.realm.RealmStatus;
import ru.cristalix.core.stats.IStatService;
import ru.cristalix.core.stats.PlayerScope;
import ru.cristalix.core.stats.UserManager;
import ru.cristalix.core.stats.impl.StatService;
import ru.cristalix.core.stats.impl.network.StatServiceConnectionData;
import ru.cristalix.core.util.UtilV3;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class HeartwarmingPlugin extends JavaPlugin implements Listener {
	
	@Getter
	public static HeartwarmingPlugin instance;
	public static final PlayerScope<Stats> statScope = new PlayerScope<>("heartwarming", Stats.class);

	private Location spawn;

	public static UserManager<User> userManager;
	public static WorldMeta worldMeta;

	private final Map<Location, Slot> slots = new HashMap<>();

	@SneakyThrows
	@Override
	public void onEnable() {

		B.plugin = instance = this;

		RealmInfo info = IRealmService.get().getCurrentRealmInfo();
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
				(user, context) -> context.store(statScope, user.getStats())

													 );

		MapVersion latest = IMapService.get().getLatestMapByGameTypeAndMapName("MISC", "DelfikPro2").get().getLatest();
		LoadedMap<World> map = IMapService.get().loadMap(latest, BukkitWorldLoader.INSTANCE).get();
		map.getWorld().setGameRuleValue("doTileDrops", "false");
		Point spawn = map.getBuildWorldState().getPoints().get("spawn").get(0);
		this.spawn = UtilV3.toLocation(spawn.getV3(), map.getWorld());
		this.spawn.setYaw((float) Double.parseDouble(spawn.getTag()));

		worldMeta = new WorldMeta(map);

		for (Location slot : worldMeta.getLabels("slot")) {
			slot.getBlock().setType(Material.DIAMOND_BLOCK);
			Location location = slot.getBlock().getLocation();
			slots.put(location, new Slot(location));
		}

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

		Bukkit.getPluginCommand("mod").setExecutor((sender, command, s, args) -> {
			try {
				byte[] serialize = Mod.serialize(new Mod(Files.readAllBytes(new File(args.length > 0 ? args[0].replaceAll("[/\\\\]", "") : "mod.jar").toPath())));
				ByteBuf buffer = Unpooled.buffer();
				buffer.writeBytes(serialize);
				PacketDataSerializer ds = new PacketDataSerializer(buffer);
				PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload(DisplayChannels.MOD_CHANNEL, ds);
				((CraftPlayer) sender).getHandle().playerConnection.sendPacket(packet);

				sender.sendMessage("OK");
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage("Fail");
			}
			return true;
		});

		Bukkit.getPluginCommand("bs").setExecutor((sender, command, s, args) -> {
			if (sender.isOp()) Cristalix.transfer(Collections.singletonList(((Player) sender).getUniqueId()), RealmId.of("BSL-TEST-1"));
			return true;
		});

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

	@EventHandler
	public void handle(PlayerDropItemEvent e) {
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
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
		try {
			Direction direction = Direction.byName(e.getMessage());
			Bukkit.getScheduler().runTask(this, () -> {
				e.getPlayer().sendMessage("§aOK");
			});
			e.setCancelled(true);
		} catch (Exception ignored) {
		}
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
			if (e.getBlockPlaced().getType() != Material.SANDSTONE || e.getBlockPlaced().getLocation().distanceSquared(spawn) < 25) e.setCancelled(true);
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
	//	    	if (((Player) e.getDamager()).getGameMode() == GameMode.CREATIVE &&
	//			((Player) e.getEntity()).getGameMode() != GameMode.CREATIVE) {
	//	    		e.setCancelled(true);
	//	    		e.getDamager().sendMessage("§сНе бей его, ты же в креативе...");
	//			}
	//		}
	//	}

	@EventHandler
	public void handle(EntityDamageEvent e) {
		if (e.getEntityType() == EntityType.PLAYER) e.setCancelled(true);
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

		inventory.setItem(3, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_down").build().asBukkitMirror());
		inventory.setItem(4, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_left").build().asBukkitMirror());
		inventory.setItem(5, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "arrow_right").build().asBukkitMirror());
		inventory.setItem(6, new ItemBuilder().item(Material.CLAY_BALL).nbt("other", "reload").build().asBukkitMirror());
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
