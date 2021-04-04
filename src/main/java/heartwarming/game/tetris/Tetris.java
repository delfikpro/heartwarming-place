package heartwarming.game.tetris;

import clepto.bukkit.B;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Tetris {

	private final byte[][] field;
	private final int rows;
	private final Function<int[], Location> locFunction;

	private Tetramino currentMino;
	private Rotation currentRotation;
	private int spawnedMinos;
	private int x;
	private int y;

	public Tetris(int columns, int rows, Function<int[], Location> locFunction) {
		this.rows = rows;
		this.locFunction = locFunction;
		this.field = new byte[columns][rows];
	}

	public boolean update() {

		int[][] pairs = apply(currentMino.getVariant(currentRotation), x, y - 1);

		removeCurrent();
		if (!isValid(pairs)) {
			placeCurrent();
			clearLines();
			return spawnNew();
		} else {
			y -= 1;
			placeCurrent();
		}

		return true;

	}

	private void clearLines() {

		List<Integer> clear = new ArrayList<>();

		a:
		for (int i = 0; i < rows; i++) {
			for (byte[] column : field) {
				if (column[i] == 0) continue a;
			}
			clear.add(i);
		}

		if (clear.isEmpty()) return;

		B.bc("Clearing lines " + clear);

		int pos = 0;

		for (int row = 0; row < rows; row++) {
			if (clear.contains(row)) continue;
			for (int column = 0; column < field.length; column++) {
				setFilled(column, pos, field[column][row]);
				setFilled(column, row, (byte) 0);
			}
			pos++;
		}

	}

	public void removeCurrent() {
		for (int[] ints : apply(currentMino.getVariant(currentRotation), x, y)) {
			setFilled(ints[0], ints[1], (byte) 0);
		}
	}

	public void placeCurrent() {
		for (int[] pair : apply(currentMino.getVariant(currentRotation), x, y)) {
			setFilled(pair[0],
					pair[1],
					currentMino.getColor().woolData);
		}
	}

	public void drop() {

		int spawned = spawnedMinos;
		while (spawned == spawnedMinos) {
			if (!update()) return;
		}

	}

	private void setFilled(int x, int y, byte color) {
		if (x >= field.length) return;
		byte[] f = field[x];
		if (y >= f.length) return;
		f[y] = color;
		Location apply = locFunction.apply(new int[] {x, y});
		Block block = apply.getBlock();
		block.setType(color > 0 ? Material.CONCRETE : Material.AIR);
		if (color > 0) block.setData(color);
	}

	public boolean spawnNew() {
		spawnedMinos++;
		currentMino = Tetramino.values()[(int) (Tetramino.values().length * Math.random())];
//		currentMino = Tetramino.O;
		currentRotation = Rotation.ROT_0;

		x = 4;
		y = 20;
		boolean valid = isValid(apply(currentMino.getVariant(currentRotation), x, y));
//		if (valid) placeCurrent();

		return valid;

	}

	public int[][] apply(int[][] pairs, int x, int y) {
		for (int i = 0; i < pairs.length; i++) {
			pairs[i][0] += x;
			pairs[i][1] += y;
		}
		return pairs;
	}

	public boolean isValid(int[][] pairs) {
		for (int[] pair : pairs) {
			int x = pair[0];
			int y = pair[1];
			if (x < 0 || x >= field.length || y < 0 || y > 25) return false;
			if (y < field[x].length && field[x][y] > 0) return false;
		}
		return true;
	}

	public void shift(boolean left) {

		int[][] pairs = apply(currentMino.getVariant(currentRotation), x + (left ? -1 : 1), y);

		removeCurrent();
		if (!isValid(pairs)) {
			placeCurrent();
			return;
		}

		x += left ? -1 : 1;
		placeCurrent();

	}

	public int[][] current() {
		return apply(currentMino.getVariant(currentRotation), x, y);
	}

	public void rotate() {
		if (currentMino == Tetramino.O) return;

		removeCurrent();
		currentRotation = Rotation.values()[currentRotation.ordinal() >= Rotation.values().length - 1 ? 0 : currentRotation.ordinal() + 1];

		a: while (true) {
			for (int[] pair : current()) {
				if (pair[0] < 0) {
					this.x++;
					continue a;
				}
				else if (pair[0] >= field.length) {
					this.x--;
					continue a;
				}
			}
			break;
		}

		int y = this.y;
		while (!isValid(apply(currentMino.getVariant(currentRotation), x, y)) && y < 25) {
			y++;
		}

		this.y = y;

		placeCurrent();
	}

	public void clear() {
		B.bc("§eТетрис очищен.");
		for (int i = 0; i < field.length; i++) {
			for (int j = 0; j < field[i].length; j++) {
				setFilled(i, j, (byte) 0);
			}
		}
	}

	//	public static void main(String[] args) {
//
//		Scanner scanner = new Scanner(System.in);
//
//		Tetris tetris = new Tetris(10, 20);
//		tetris.spawnNew();
//
//		while (true) {
//
//			for (int i = 0; i < tetris.field.length; i++) {
//				for (int j = 0; j < tetris.field[i].length; j++) {
//					boolean aBoolean = tetris.field[i][j];
//					System.out.print(aBoolean ? "#" : ".");
//				}
//				System.out.println();
//			}
//			System.out.print("> ");
//
//			String next = scanner.next();
//			if ("l".equals(next)) {
//				tetris.shift(true);
//			} else if ("r".equals(next)) {
//				tetris.shift(false);
//			} else if ("o".equals(next)) {
//				tetris.rotate();
//			} else if ("d".equals(next)) {
//				tetris.drop();
//			} else {
//				tetris.update();
//			}
//
//		}
//
//	}

}
