package csdev;

import java.io.Serializable;

public class MessageCheckMail extends Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public MessageCheckMail() {
        super( Protocol.CMD_CHECK_MAIL );
    }
}
