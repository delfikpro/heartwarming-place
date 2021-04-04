package heartwarming.game;

import clepto.bukkit.event.EventContext;
import heartwarming.HeartwarmingPlugin;
import heartwarming.User;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

@RequiredArgsConstructor
public abstract class Game {

	protected final EventContext context = new EventContext(event -> {
		return event instanceof PlayerEvent && HeartwarmingPlugin.userManager.getUser(((PlayerEvent) event).getPlayer()).getGame() == this;
	});

	protected final List<User> players;
	protected final Slot slot;

	public void end() {
		slot.setGame(null);
	}

	public abstract void save();

}
