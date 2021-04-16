package heartwarming.mod;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class IOUtils {

    public static byte[] read(InputStream inputStream) {
        try {
            byte[] bytes = org.apache.commons.io.IOUtils.readFully(inputStream, inputStream.available());
            return bytes;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {}
        }
    }

}
