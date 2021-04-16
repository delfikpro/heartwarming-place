package heartwarming.game.gameof2048;

import clepto.bukkit.B;
import clepto.bukkit.world.Box;
import heartwarming.HeartwarmingPlugin;
import heartwarming.User;
import heartwarming.game.Game;
import heartwarming.game.Slot;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GameOf2048 extends Game {

	private final Location location;
	private final int sizeX, sizeY;

	@Getter
	private final Cell[][] cells;

	private final Box[] buttons;

	private boolean lock;

	public GameOf2048(List<User> players, Slot slot) {
		this(players, slot, 4, 4);
	}

	protected GameOf2048(List<User> players, Slot slot, int sizeX, int sizeY) {
		super(players, slot);

		Location location = slot.getLocation();

		this.location = location.clone().add(1 - 0.1, -1, 1 - 0.1);
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.cells = new Cell[sizeX][sizeY];

		buttons = new Box[] {
				new Box(HeartwarmingPlugin.worldMeta, location.clone().add(-4, 0, -1), location.clone().add(-2, 0, 1), "", ""),
				new Box(HeartwarmingPlugin.worldMeta, location.clone().add(-1, 0, -4), location.clone().add(1, 0, -2), "", ""),
				new Box(HeartwarmingPlugin.worldMeta, location.clone().add(2, 0, -1), location.clone().add(4, 0, 1), "", ""),
				new Box(HeartwarmingPlugin.worldMeta, location.clone().add(-1, 0, 2), location.clone().add(1, 0, 4), "", ""),
		};

		for (Box button : buttons) {
			button.forEachBukkit(loc -> {
				Block block = loc.getBlock();
				block.setType(Material.STAINED_CLAY);
				block.setData((byte) 5);
			});
		}

		addRandom();
		addRandom();

		context.on(PlayerInteractEvent.class, event -> {
			if (lock) return;
			if (event.getHand() != EquipmentSlot.HAND) return;
			if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

			Player player = event.getPlayer();

			Block block = player.getTargetBlock(null, 15);

			if (block == null) return;

			Direction direction = null;
			for (int i = 0; i < buttons.length; i++) {
				Box button = buttons[i];
				if (button.contains(block.getLocation())) {
					direction = Direction.values()[i];
					break;
				}
			}

			if (direction == null) return;

			shift(direction);
			lock = true;
			B.postpone(6, () -> {
				if (!addRandom()) {

				}
				lock = false;
			});

			buttons[direction.ordinal()].forEachBukkit(loc -> {
				Block b = loc.getBlock();
				B.destroy(b);
				b.setType(Material.STAINED_CLAY);
				b.setData((byte) 5);
			});



		});

	}

	@Override
	public void save() {
		User user = players.get(0);

		IntList list = new IntArrayList(sizeX * sizeY);
		for (Cell[] row : cells) {
			for (Cell cell : row) {
				list.add(cell.value);
			}
		}
		user.getStats().setGameOf2048(new GameOf2048Save(list.toIntArray()));
	}

	public void shift(Direction direction) {

		int lines = Math.abs(direction.dx * sizeX + direction.dy * sizeY);
		int length = Math.abs(direction.dx * sizeY + direction.dy * sizeX);

		for (int line = 0; line < lines; line++) {

			int index = 0;

			for (int i = 0; i < length; i++) {

				int x = x(line, i, direction);
				int y = y(line, i, direction);

				Cell value = cells[x][y];

				if (value == null) continue;

				cells[x][y] = null;

				if (index > 0) {
					Cell previous = getValue(line, index - 1, direction);
					if (previous != null && previous.value == value.value) {
						B.postpone(3, previous.block::remove);
						B.postpone(3, value.block::remove);
						setValue(line, index - 1, direction, create(x(line, index - 1, direction), y(line, index - 1, direction), value.value * 2));
						continue;
					}
				}

				value.block.teleport(location.clone().add(
						3.0 / 4.0 * x(line, index, direction) - 1.5,
						0,
						3.0 / 4.0 * y(line, index, direction) - 1.5
														 ));

				setValue(line, index, direction, value);

				index++;

			}
		}
	}

	public boolean addRandom() {
		List<Integer> empty = new ArrayList<>();
		int index = 0;
		for (Cell[] number : cells) {
			for (Cell i : number) {
				if (i == null) empty.add(index);
				index++;
			}
		}
		if (empty.isEmpty()) return false;
		Collections.shuffle(empty);
		int i = empty.get(0);
		cells[i / sizeX][i % sizeX] = create(i / sizeX, i % sizeX, Math.random() > 0.5 ? 2 : 4);
		return true;
	}

	public Cell create(int x, int z, int value) {

		double dx = 3.0 / 4.0 * x - 1.5;
		double dz = 3.0 / 4.0 * z - 1.5;

		ArmorStand fallingBlock = location.getWorld().spawn(location.clone().add(dx, 0, dz), ArmorStand.class);
		fallingBlock.setHelmet(new ItemStack(Material.CONCRETE_POWDER, 1, (short) (Math.log(value) / Math.log(2))));
		//				location.getWorld().spawnFallingBlock(location.clone().add(dx, 0, dz), new MaterialData(Material.CONCRETE_POWDER, (byte) (int) (Math.log(value) / Math.log(2))));
		//		fallingBlock.setTicksLived(1);
		fallingBlock.setGravity(false);
		fallingBlock.setInvulnerable(true);
		fallingBlock.setVisible(false);
		fallingBlock.setCustomName(value + "");
		fallingBlock.setCustomNameVisible(true);

		return new Cell(fallingBlock, value);
	}

	private int x(int lineId, int cellId, Direction direction) {
		int x = lineId * direction.dy + cellId * direction.dx;
		if (direction.isNegative()) x += cells.length - 1;
		return x;
	}

	private int y(int lineId, int cellId, Direction direction) {
		int y = lineId * direction.dx + cellId * direction.dy;
		if (direction.isNegative()) y += cells.length - 1;
		return y;
	}

	private Cell getValue(int lineId, int cellId, Direction direction) {
		int x = lineId * direction.dy + cellId * direction.dx;
		int y = lineId * direction.dx + cellId * direction.dy;
		if (direction.isNegative()) {
			x += cells.length - 1;
			y += cells.length - 1;
		}

		return cells[x][y];
	}

	private void setValue(int lineId, int cellId, Direction direction, Cell value) {
		int x = lineId * direction.dy + cellId * direction.dx;
		int y = lineId * direction.dx + cellId * direction.dy;
		if (direction.isNegative()) {
			x += sizeX - 1;
			y += sizeY - 1;
		}

		this.cells[x][y] = value;
	}

}
