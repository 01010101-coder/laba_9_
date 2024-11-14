package csdev;

import java.io.Serializable;

public class MessageDisconnect extends Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public MessageDisconnect() {
        super( Protocol.CMD_DISCONNECT );
    }
}
