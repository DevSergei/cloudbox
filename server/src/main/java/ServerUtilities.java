import io.netty.channel.Channel;

import java.io.IOException;
import java.nio.file.Paths;

public class ServerUtilities {
    public static void sendFileList(Channel channel, String username) {
        FileListMessage flm = null;
        try {
            flm = new FileListMessage(Paths.get(getUserRootPath(username)));
            channel.writeAndFlush(flm);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getUserRootPath(String username) {
        System.out.println("username is " + username);
        return "server/repository/" + username;
    }
}
