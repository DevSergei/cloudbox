import java.io.File;

public class CommandMessage extends AbstractMessage {
    public static final int CMD_MSG_AUTH_OK = 1;
    public static final int CMD_MSG_REQUEST_FILE_DOWNLOAD = 2;
    public static final int CMD_MSG_REQEST_FILES_LIST = 3;
    public static final int CMD_MSG_REQEST_SERVER_DELETE_FILE = 4;

    private int type;
    private Object[] attachment;

    public CommandMessage(int type) {
        this.type = type;
    }

//    public CommandMessage(int type, File file) {
//        this.type = type;
//        this.attachment = new Object[1];
//        this.attachment[0] = file;
//    }

    public int getType() {
        return type;
    }

    public Object[] getAttachment() {
        return attachment;
    }

    public CommandMessage(int type, Object... attachment) {
        this.type = type;
        this.attachment = attachment;
    }
}
