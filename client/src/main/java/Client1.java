import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Client1 {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost",8189);
        ObjectEncoderOutputStream outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
        outputStream.writeObject("Hello to Server");
        outputStream.close();
        socket.close();
    }
}
