package heartwarming;

import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class GameItems {

	public CraftItemStack convertItem(ItemStack itemStack) {
		return itemStack instanceof CraftItemStack ? (CraftItemStack) itemStack : CraftItemStack.asCraftCopy(itemStack);
	}

	public GameType getGameType(ItemStack stack) {
		NBTTagCompound tag = convertItem(stack).handle.tag;
		if (tag == null) return null;
		return GameType.valueOf(tag.getString("game"));
	}

}
