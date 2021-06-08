package heartwarming.script;

import clepto.bukkit.B;
import clepto.cristalix.Cristalix;
import dev.xdark.feder.NetUtil;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import heartwarming.HeartwarmingPlugin;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

@SuppressWarnings({"Convert2Diamond", "ToArrayCallWithZeroLengthArrayArgument"})
public class ScriptManager {

    private static GroovyShell groovyShell;

    private static final List<UUID> allowed = Arrays.asList(
            UUID.fromString("e7c13d3d-ac38-11e8-8374-1cb72caa35fd"),
            UUID.fromString("95c8f748-a7cd-11ea-acca-1cb72caa35fd")
    );

    @SneakyThrows
    public static void init() {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass("heartwarming.script.HeartwarmingScript");

        Set<String> types = new Reflections("org.bukkit", "java.lang", "clepto", "implario",
                "net.minecraft", "java.util", new SubTypesScanner(false)).getAllTypes();

        ByteBuf tabBuf = Unpooled.buffer();
        NetUtil.writeVarInt(types.size(), tabBuf);
        for (String type : types) {
            NetUtil.writeUtf8(type, tabBuf);
        }

        groovyShell = new GroovyShell(ScriptManager.class.getClassLoader(), config);
        Bukkit.getMessenger().registerIncomingPluginChannel(B.plugin, "hw:tab", (s1, player, bytes) -> {
            if (!allowed.contains(player.getUniqueId())) {
                return;
            }
            HeartwarmingPlugin.userManager.getUser(player).sendPayload("hw:tab", tabBuf.retainedSlice());
        });

        Bukkit.getMessenger().registerIncomingPluginChannel(B.plugin, "hw:code", (s1, player, bytes) -> {
            if (!allowed.contains(player.getUniqueId())) {
                return;
            }
            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
            for (Player p : Bukkit.getOnlinePlayers()) {
                HeartwarmingPlugin.userManager.getUser(p).sendPayload("hw:code", byteBuf.retainedSlice());
            }
        });
        Bukkit.getMessenger().registerIncomingPluginChannel(B.plugin, "hw:script", (s1, player, bytes) -> {
            if (!allowed.contains(player.getUniqueId())) {
                player.sendMessage("§cYou are not authorized to do this.");
                return;
            }
            try {
                groovyShell.parse(new String(bytes)).run();
            } catch (Exception ex) {
                StringWriter stringWriter = new StringWriter();
                ex.printStackTrace(new PrintWriter(stringWriter));
                TextComponent tc = new TextComponent(stringWriter.toString());
                tc.setColor(ChatColor.RED);
                player.sendMessage(tc);
            }
        });


    }

}
