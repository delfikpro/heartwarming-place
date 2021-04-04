package heartwarming.game.gameof2048;

import lombok.Data;
import org.bukkit.entity.ArmorStand;

@Data
class Cell {

	final ArmorStand block;
	final int value;

}
