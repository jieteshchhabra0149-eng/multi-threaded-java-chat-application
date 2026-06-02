import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Download {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Download.java <url> <dest>");
            System.exit(1);
        }
        String url = args[0];
        String dest = args[1];
        System.out.println("Downloading " + url + " to " + dest);
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Download complete.");
    }
}
