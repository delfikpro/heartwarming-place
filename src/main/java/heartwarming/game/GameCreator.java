package heartwarming.game;

import heartwarming.User;
import org.bukkit.Location;

import java.util.List;

@FunctionalInterface
public interface GameCreator {

	Game createGame(List<User> users, Location location);

}
