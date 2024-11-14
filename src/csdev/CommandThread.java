package csdev;

import java.util.HashMap;

public class CommandThread extends Thread implements CmdProcessor {

    protected HashMap<String, CmdHandler> smap = new HashMap<String, CmdHandler>();
    protected HashMap<String, CmdHandler> map = new HashMap<String, CmdHandler>();

    protected static void insert(
            HashMap<String, CmdHandler> m,
            String name, CmdHandler handler ) {
        if ( name != null ) {
            m.put( name, handler );
        }
    }

    @Override
    public synchronized void putHandler(String shortName, String fullName, CmdHandler handler ) {
        if ( shortName == null && fullName == null ) {
            throw new IllegalArgumentException(
                    "Bad arguments: shortName and fullName can not be null simultaneusly!" );
        }
        CommandThread.insert(smap, shortName, handler);
        CommandThread.insert(map, fullName, handler);
    }

    protected int errorCode = 0;
    @Override
    public synchronized int lastError() {
        return errorCode;
    }

    @Override
    public synchronized boolean command(String cmd) {
        int[] err = {0};
        return command( cmd, err );
    }

    @Override
    public synchronized boolean command(String cmd, int[] err) {
        CmdHandler handler = null;
        if ( smap.containsKey( cmd )) {
            handler = smap.get(cmd);
        } else if ( map.containsKey( cmd )) {
            handler = map.get(cmd);
        }
        if ( handler != null ) {
            boolean res = handler.onCommand(err);
            errorCode = err[0];
            return res;
        }
        return false;
    }
}
