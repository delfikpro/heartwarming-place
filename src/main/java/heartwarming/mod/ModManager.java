package heartwarming.mod;

import clepto.bukkit.B;
import clepto.bukkit.event.EventContext;
import dev.xdark.feder.NetUtil;
import heartwarming.HeartwarmingPlugin;
import heartwarming.User;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutCustomPayload;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.cristalix.core.display.DisplayChannels;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static java.nio.file.StandardWatchEventKinds.*;

public class ModManager {

    private final long updateWindow = 500;

    private final Map<String, Mod> modMap = new HashMap<>();

    public String getModName(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    public Mod getMod(Path path) {
        return modMap.get(getModName(path));
    }

    @SneakyThrows
    public void init() {

        WatchService watchService = FileSystems.getDefault().newWatchService();


        File modsDirFile = new File("mods");
        modsDirFile.mkdir();

        for (File file : modsDirFile.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                Mod mod = new Mod(file.toPath().toAbsolutePath(), file.getName().toLowerCase(Locale.ROOT));
                modMap.put(getModName(mod.getPath()), mod);
                reloadMod(mod);
            }
        }

        Path modsDir = modsDirFile.toPath();

        modsDir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        new Thread(() -> {
            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;

                    Path path = (Path) event.context();
                    String modName = getModName(path);
                    if (!modName.endsWith(".jar")) continue;

                    Mod mod = getMod(path);

                    if (event.kind() == ENTRY_DELETE) {
                        modMap.remove(modName);
                        continue;
                    }

                    if (mod == null) {
                        mod = new Mod(modsDir.resolve(path), modName);
                        modMap.put(modName, mod);
                    }

                    mod.setDirty(true);
                    mod.setLastUpdate(System.currentTimeMillis());

                }

                key.reset();

            }
        }, "Mod filesystem watcher").start();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(B.plugin, () -> {

            for (Iterator<Mod> iterator = modMap.values().iterator(); iterator.hasNext(); ) {
                Mod mod = iterator.next();
                if (mod.isDirty() && mod.getTimeSinceLastUpdate() > 500) {
                    System.out.println("reloading mod " + mod.getPath());
                    try {
                        reloadMod(mod);
                        mod.setDirty(false);
                    } catch (Exception ex) {

                        for (User user : mod.getUsedBy()) {
                            user.getPlayer().sendMessage("§cМод " + mod.getPath().getFileName() + " повреждён, для его перезагрузки придётся перезайти.");
                        }

                        iterator.remove();
                        ex.printStackTrace();

                    }
                }
            }
        }, 1, 1);

        B.regCommand((sender, args) -> {
            ByteBuf buffer = Unpooled.buffer();
            NetUtil.writeUtf8(args[1], buffer);
            ((CraftPlayer) sender).getHandle().playerConnection.sendPacket(new PacketPlayOutCustomPayload(args[0], new PacketDataSerializer(buffer)));
            return "§aSent §f" + args[1] + " §ainto channel §f" + args[0] + "§a.";
        }, "pm");

        Bukkit.getMessenger().registerIncomingPluginChannel(B.plugin, "sdkconfirm", (s, player, bytes) -> {
            User user = HeartwarmingPlugin.userManager.getUser(player);

            System.out.println("Reload confirm for " + user.getName() + ", " + bytes.length);

            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
            String modClass = NetUtil.readUtf8(byteBuf);
            System.out.println(modClass);
            byteBuf.release();

            System.out.println(String.join(", ", user.getWaitingReloadConfirm().asMap().keySet()) + " - keys");
            Mod mod = user.getWaitingReloadConfirm().getIfPresent(modClass);
            System.out.println(mod == null ? null : mod.getPath());
            if (mod != null) {
                player.sendMessage("§aМод §f" + mod + "§a был обновлён и перезагружен.");
                mod.send(user);
            }

        });


        PluginCommand modCommand = Bukkit.getPluginCommand("mod");
        modCommand.setExecutor((sender, command, s, args) -> {
            try {
                Mod mod = args.length == 0 ? null : modMap.get(args[0].toLowerCase(Locale.ROOT));
                if (mod == null) {
                    sender.sendMessage("§eСписок доступных модов:");
                    for (String modKey : modMap.keySet()) {
                        sender.sendMessage("§e+ §f" + modKey);
                    }
                    return true;
                }

                User user = HeartwarmingPlugin.userManager.getUser(sender);
                if (sender.isOp() && args.length == 3 && args[1].equals("-u")) {
                    Pattern mask = Pattern.compile(args[2].replace("?", ".").replace("*", ".*").toLowerCase(Locale.ROOT));
                    for (User value : HeartwarmingPlugin.userManager.getUserMap().values()) {
                        if (mask.matcher(value.getName().toLowerCase(Locale.ROOT)).matches()) {
                            sender.sendMessage("§aМод §f" + mod + "§a отправлен игроку §f" + value.getName() + ".");
                            mod.send(value);
                            mod.getUsedBy().add(value);
                        }
                    }
                    return true;
                }

                mod.getUsedBy().add(user);

                if (mod.getBuffer() == null) {
                    sender.sendMessage("§cМод §f" + mod + "§c ещё не прогрузился, подождите немного...");
                    return true;
                }

                mod.send(user);

                sender.sendMessage("§aВы подключили мод §f" + mod + "§a.");

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage("§cПри отправке мода произошла ошибка.");
            }
            return true;
        });
        modCommand.setTabCompleter((sender, cmd, label, args) -> {
            return modMap.keySet().stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        });

        new EventContext(anything -> true).on(PlayerQuitEvent.class, EventPriority.LOW, e -> {
            User user = HeartwarmingPlugin.userManager.getUser(e.getPlayer());
            for (Mod mod : modMap.values()) {
                mod.getUsedBy().remove(user);
            }
        });

    }

    private void reloadMod(Mod mod) throws IOException, NoSuchElementException {
        try (JarFile ignored = new JarFile(mod.getPath().toFile())) {

            if (mod.getBuffer() != null) mod.getBuffer().release();

            ZipEntry entry = ignored.getEntry("mod.properties");
            String propertiesStr = new String(heartwarming.mod.IOUtils.read(ignored.getInputStream(entry)));

            Map<String, String> properties = new HashMap<>();
            for (String s : propertiesStr.replace("\r", "").split("\n")) {
                if (s.startsWith("#")) continue;
                String[] ss = s.split("=");
                properties.put(ss[0], ss[1]);
            }


            String mainClass = properties.get("main");
            if (mainClass == null) throw new NoSuchElementException("main in mod.properties");


            for (User user : mod.getUsedBy()) {
                ByteBuf reloadBuf = Unpooled.buffer();
                NetUtil.writeUtf8(mainClass, reloadBuf);
                System.out.println("Sent reload request to " + user.getName() + ": '" + mainClass + "' " + mainClass.length());
                user.sendPayload("sdk4reload", reloadBuf.retainedSlice());
                user.sendPayload("sdkreload", reloadBuf);
                user.getWaitingReloadConfirm().put(mainClass, mod);
                for (String s : user.getWaitingReloadConfirm().asMap().keySet()) {
                    System.out.println(s + " - key");
                }
            }

            InputStream inputStream = new FileInputStream(mod.getPath().toFile());
            byte[] bytes = IOUtils.readFully(inputStream, inputStream.available());
            ByteBuf byteBuf = Unpooled.buffer();
            NetUtil.writeVarInt(bytes.length, byteBuf);
            byteBuf.writeBytes(bytes);

            mod.setBuffer(byteBuf);

            inputStream.close();

        }
    }

}
