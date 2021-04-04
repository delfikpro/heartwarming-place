package heartwarming;

import heartwarming.game.Game;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import ru.cristalix.core.stats.player.PlayerWrapper;

import java.util.UUID;

@Getter
public class User extends PlayerWrapper {

	@Setter
	private Game game;

	@Delegate
	private final Stats stats;

	public User(UUID uuid, String name, Stats stats) {
		super(uuid, name);
		this.stats = stats == null ? new Stats() : stats;
	}

}
