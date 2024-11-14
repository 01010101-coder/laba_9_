package csdev;

import java.io.Serializable;

public class MessageUserResult extends MessageResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public String[] userNics = null;

    public MessageUserResult( String errorMessage ) {
        super( Protocol.CMD_USER, errorMessage );
    }

    public MessageUserResult( String[] userNics ) {
        super( Protocol.CMD_USER );
        this.userNics = userNics;
    }
}
