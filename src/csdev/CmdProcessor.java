package csdev;

public interface CmdProcessor {
    void putHandler( String shortName, String fullName, CmdHandler handler );

    int lastError();
    boolean command( String cmd );
    boolean command( String cmd, int[] err );
}
