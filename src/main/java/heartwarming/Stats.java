package heartwarming;

import heartwarming.game.gameof2048.GameOf2048Save;
import heartwarming.game.tetris.TetrisSave;
import lombok.Data;

import java.util.UUID;

@Data
public class Stats {

	private String lastSeenName;

	private long online;
	private long joinCounter;

	private GameOf2048Save gameOf2048;
	private TetrisSave tetrisSave;

}