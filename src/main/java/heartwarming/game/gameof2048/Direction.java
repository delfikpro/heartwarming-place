package heartwarming.game.gameof2048;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public enum Direction {
	PX(1, 0),
	PY(0, 1),
	NX(-1, 0),
	NY(0, -1);
	final int dx, dy;

	public boolean isNegative() {
		return dx < 0 || dy < 0;
	}

	public static Direction byName(String name) {
		switch (name.toLowerCase(Locale.ROOT)) {
			case "left":
				return NX;
			case "right":
				return PX;
			case "up":
				return NY;
			case "down":
				return PY;
			default:
				throw new IllegalArgumentException();
		}
	}
}
