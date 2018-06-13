import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FilePartitionWorker {
    public static final int PART_SIZE = 256;

    public static void sendFileFromClient(Path path, ObjectEncoderOutputStream out, ProgressBar progressBar) {
        try {
            byte[] fileData = Files.readAllBytes(path);
            int partsCount = fileData.length / PART_SIZE;
            if (fileData.length % PART_SIZE != 0) {
                partsCount++;
            }
            if (progressBar != null) {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    progressBar.setManaged(true);
                });
            }
            for (int i = 0; i < partsCount; i++) {
                int startPosition = i * PART_SIZE;
                int endPosition = (i + 1) * PART_SIZE;
                if (endPosition > fileData.length) {
                    endPosition = fileData.length;
                }
                if (progressBar != null) {
                    final double prog = (double) i / PART_SIZE;
                    Platform.runLater(() -> progressBar.setProgress(prog));
                }
                FileMessage fm = new FileMessage(path.getFileName().toString(),
                        Arrays.copyOfRange(fileData, startPosition, endPosition), partsCount, i);
                out.writeObject(fm);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (progressBar != null) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                });
            }
        }
    }

    public static void sendFileFromServer(Path path, Channel out) {
        byte[] fileData = new byte[0];
        try {
            fileData = Files.readAllBytes(path);
            int partsCount = fileData.length / PART_SIZE;
            if (fileData.length % PART_SIZE != 0) {
                partsCount++;
            }
            for (int i = 0; i < partsCount; i++) {
                int startPosition = i * PART_SIZE;
                int endPosition = (i + 1) * PART_SIZE;
                if (endPosition > fileData.length) {
                    endPosition = fileData.length;
                }
                FileMessage fm = new FileMessage(path.getFileName().toString(),
                        Arrays.copyOfRange(fileData, startPosition, endPosition), partsCount, i);
                ChannelFuture future = out.writeAndFlush(fm);
                future.await();//
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
