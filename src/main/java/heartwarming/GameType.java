package heartwarming;

import heartwarming.game.GameCreator;
import heartwarming.game.gameof2048.GameOf2048;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameType {

	GAME_2048(null);

	private final GameCreator creator;


}
