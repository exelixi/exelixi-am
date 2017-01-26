package xyz.exelixi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by endrix on 1/17/17.
 */
public class Utils {

    /**
     * Create a Directory
     *
     * @param parent
     * @param name
     * @return
     */
    public static Path createDirectory(Path parent, String name) {
        try {
            Path path = parent.resolve(name);
            if (!path.toFile().exists()) {
                return Files.createDirectory(parent.resolve(name));
            } else {
                return path;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
