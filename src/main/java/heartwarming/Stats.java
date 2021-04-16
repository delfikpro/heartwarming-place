package heartwarming;

import heartwarming.game.gameof2048.GameOf2048Save;
import heartwarming.game.tetris.TetrisSave;
import lombok.Data;

import java.util.UUID;

@Data
public class Stats {

	private final UUID id;
	private final String name;

	private long online;

	private GameOf2048Save gameOf2048;
	private TetrisSave tetrisSave;

}
