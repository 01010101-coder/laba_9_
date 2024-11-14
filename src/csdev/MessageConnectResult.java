package csdev;

import java.io.Serializable;

public class MessageConnectResult extends MessageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public MessageConnectResult( String errorMessage ) {
        super( Protocol.CMD_CONNECT, errorMessage );
    }

    public MessageConnectResult() {
        super( Protocol.CMD_CONNECT );
    }
}
