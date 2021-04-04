package heartwarming.game;

import lombok.Data;
import org.bukkit.Location;

@Data
public class Slot {

	private final Location location;

	private Game game;

}
