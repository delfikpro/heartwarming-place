package heartwarming.mod;

import heartwarming.User;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.cristalix.core.display.DisplayChannels;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
public class Mod {

    private final Path path;
    private final String name;

    private ByteBuf buffer;
    private long lastUpdate;
    private boolean dirty;

    private final Set<User> usedBy = new HashSet<>();

    public long getTimeSinceLastUpdate() {
        return System.currentTimeMillis() - lastUpdate;
    }

    public void send(User user) {
        user.sendPayload(DisplayChannels.MOD_CHANNEL, buffer.retainedSlice());
    }

    @Override
    public String toString() {
        return name;
    }


}
