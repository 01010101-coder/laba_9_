package csdev;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte id;
    public byte getID() {
        return id;
    }
    public void setID(byte x) {
        id = x;
    }

    protected Message() {
        assert( false );
    }

    protected Message( byte id ) {
        assert( Protocol.validID( id ) == true );
        this.id = id;
    }
}
