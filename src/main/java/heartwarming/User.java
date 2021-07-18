package heartwarming;

import clepto.bukkit.B;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.implario.kensuke.KensukeSession;
import dev.implario.kensuke.impl.bukkit.IBukkitKensukeUser;
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
import org.bukkit.entity.Player;
import ru.cristalix.core.CoreApi;
import ru.cristalix.core.network.ISocketClient;
import ru.cristalix.core.network.packages.PluginMessagePackage;
import ru.cristalix.core.plugin.TextPluginMessage;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class User implements IBukkitKensukeUser {

	@Setter
	private Game game;

	private final long joinOnline;

	@Getter
	@Setter
	private Player player;

	@Getter
	private final KensukeSession session;

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

	public User(KensukeSession session, Stats stats) {
		if (stats == null) stats = new Stats();
		this.session = session;
		this.stats = stats;

		this.joinOnline = this.stats.getOnline();

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
