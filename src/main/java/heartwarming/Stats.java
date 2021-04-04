package heartwarming;

import heartwarming.game.gameof2048.GameOf2048Save;
import heartwarming.game.tetris.TetrisSave;
import lombok.Data;

@Data
public class Stats {

	private GameOf2048Save gameOf2048;
	private TetrisSave tetrisSave;

}
