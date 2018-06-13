import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class AuthGatewayHandler extends ChannelInboundHandlerAdapter {
    private boolean authorized;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("New unauthorized client connected");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }
        if (!authorized) {
            if (msg instanceof AuthMessage) {
                AuthMessage am = (AuthMessage) msg;
                if (am.getLogin().equals("login") && am.getPassword().equals("password")) {
                    authorized = true;
                    CommandMessage amAuthOk = new CommandMessage(CommandMessage.CMD_MSG_AUTH_OK);
                    ChannelFuture future = ctx.writeAndFlush(amAuthOk);
                    future.await();
                    String username = "client";
                    ServerUtilities.sendFileList(ctx.channel(), username);
                    ctx.pipeline().addLast(new ServerHandler(username));
                } else {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
