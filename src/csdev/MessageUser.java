package csdev;

import java.io.Serializable;

public class MessageUser extends Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public MessageUser() {
        super( Protocol.CMD_USER );
    }
}
