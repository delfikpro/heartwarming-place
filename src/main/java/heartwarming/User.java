package heartwarming;

import clepto.bukkit.B;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import heartwarming.game.Game;
import heartwarming.mod.Mod;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.val;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutCustomPayload;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import ru.cristalix.core.stats.player.PlayerWrapper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class User extends PlayerWrapper {

	@Setter
	private Game game;

	private final long joinOnline;

	@Setter
	private int combo;

	@Setter
	private long lastHitTime;

	@Setter
	private boolean pvp;

	private final long joinTime = System.currentTimeMillis();

	private final Cache<String, Mod> waitingReloadConfirm = CacheBuilder.newBuilder()
			.expireAfterWrite(15, TimeUnit.SECONDS).build();

	@Delegate
	private final Stats stats;

	public User(UUID uuid, String name, Stats stats) {
		super(uuid, name);
		this.stats = stats == null ? new Stats(uuid, name) : stats;

		this.joinOnline = this.stats.getOnline();

	}

	public void save() {

		stats.setOnline(getPlayedTime());

	}

	public long getPlayedTime() {
		return joinOnline + System.currentTimeMillis() - joinTime;
	}

	public void sendPayload(String channel, ByteBuf data) {
		if (!MinecraftServer.SERVER.isMainThread()) B.run(() -> sendPayload(channel, data));
		else {
			val packet = new PacketPlayOutCustomPayload(channel, new PacketDataSerializer(data));
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		}

	}

}
