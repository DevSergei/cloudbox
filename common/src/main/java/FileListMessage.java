import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileListMessage {
    private List<File> files;

    public List<File> getFiles() {
        return files;
    }

    public FileListMessage(Path path) throws IOException {
        files = Files.list(path).map(Path::toFile).collect(Collectors.toList());
    }
}
