import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(ServerHandler.class.getName());
    private String clientName;

    public ServerHandler(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            logger.debug("New message " + msg.getClass().getSimpleName() + " received from " + clientName);
//            System.out.println(msg.getClass());
            if (msg instanceof CommandMessage) {
                CommandMessage cm = (CommandMessage) msg;
                if (cm.getType() == CommandMessage.CMD_MSG_REQEST_FILES_LIST) {
                    ServerUtilities.sendFileList(ctx.channel(), clientName);
                }
                if (cm.getType() == CommandMessage.CMD_MSG_REQEST_SERVER_DELETE_FILE) {
                    Path path = Paths.get("server/repository/" + clientName + "/" + (String) cm.getAttachment()[0]);
                    Files.delete(path);
                    ServerUtilities.sendFileList(ctx.channel(), clientName);
                }
                if (cm.getType() == CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD) {
                    try {
                        Path path = Paths.get(((File) cm.getAttachment()[0]).getAbsolutePath());
                        FilePartitionWorker.sendFileFromServer(path, ctx.channel());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                try {
                    Path path = Paths.get("server/repository/client" + fm.getFilename());
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }

                    RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
                    FileChannel outChannel = raf.getChannel();
                    outChannel.position(fm.getPartNumber() * FilePartitionWorker.PART_SIZE);
                    ByteBuffer buf = ByteBuffer.allocate(fm.getData().length);
                    buf.put(fm.getData());
                    buf.flip();
                    outChannel.write(buf);
                    buf.clear();
                    outChannel.close();
                    raf.close();
                    if (fm.getPartNumber() == fm.getPartsCount()) {
                        ServerUtilities.sendFileList(ctx.channel(), clientName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //ctx.flush();
        ctx.close();
    }
}
