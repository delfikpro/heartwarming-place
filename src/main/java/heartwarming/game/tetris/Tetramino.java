package heartwarming.game.tetris;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.DyeColor;

@Getter
@RequiredArgsConstructor
public enum
Tetramino {

	I(DyeColor.LIGHT_BLUE, -1, 0, 0, 0, 1, 0, 2, 0),
	J(DyeColor.ORANGE, -1, -1, 0, -1, 0, 0, 0, 1),
	L(DyeColor.BLUE, 0, -1, 1, -1, 0, 0, 0, 1),
	O(DyeColor.YELLOW, -1, -1, 0, -1, -1, 0, 0, 0),
	T(DyeColor.PURPLE, 0, 0, 0, 1, 1, 0, -1, 0),
	Z(DyeColor.LIME, 0, -1, -1, 0, 0, 0, 1, -1),
	S(DyeColor.RED, -1, -1, 0, -1, 0, 0, 1, 0);
	private final int[][] pairs;

	private final DyeColor color;

	Tetramino(DyeColor color, int... coords) {
		this.color = color;
		this.pairs = new int[coords.length / 2][2];
		for (int i = 0; i < coords.length; i++) {
			pairs[i / 2][i % 2] = coords[i];
		}
	}

	public int[][] getVariant(Rotation rotation) {

		int[][] pairs = new int[this.pairs.length][];

		for (int i = 0; i < pairs.length; i++) {

			int[] pair = {this.pairs[i][0], this.pairs[i][1]};

			for (int j = 0; j < 3 - rotation.ordinal(); j++) {
				int temp = pair[0];
				pair[0] = pair[1];
				pair[1] = -temp;
			}

			pairs[i] = pair;

		}

		return pairs;

	}

}
