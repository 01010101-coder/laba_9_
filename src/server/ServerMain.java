package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

import csdev.*;

public class ServerMain {

    private static int MAX_USERS = 100;

    public static void main(String[] args) {

        try (ServerSocket serv = new ServerSocket(Protocol.PORT)) {
            System.err.println("initialized");
            ServerStopThread tester = new ServerStopThread();
            tester.start();
            while (true) {
                Socket sock = accept(serv);
                if (sock != null) {
                    if (ServerMain.getNumUsers() < ServerMain.MAX_USERS) {
                        System.err.println(sock.getInetAddress().getHostName() + " connected");
                        ServerThread server = new ServerThread(sock);
                        server.start();
                    } else {
                        System.err.println(sock.getInetAddress().getHostName() + " connection rejected");
                        sock.close();
                    }
                }
                if (ServerMain.getStopFlag()) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            stopAllUsers();
            System.err.println("stopped");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    public static Socket accept(ServerSocket serv) {
        assert(serv != null);
        try {
            serv.setSoTimeout(1000);
            Socket sock = serv.accept();
            return sock;
        } catch (SocketException e) {
        } catch (IOException e) {
        }
        return null;
    }

    private static void stopAllUsers() {
        String[] nic = getUsers();
        for (String user : nic) {
            ServerThread ut = getUser(user);
            if (ut != null) {
                ut.disconnect();
            }
        }
    }

    private static Object syncFlags = new Object();
    private static boolean stopFlag = false;
    public static boolean getStopFlag() {
        synchronized (ServerMain.syncFlags) {
            return stopFlag;
        }
    }
    public static void setStopFlag(boolean value) {
        synchronized (ServerMain.syncFlags) {
            stopFlag = value;
        }
    }

    private static Object syncUsers = new Object();
    private static TreeMap<String, ServerThread> users =
            new TreeMap<String, ServerThread>();

    public static ServerThread getUser(String userNic) {
        synchronized (ServerMain.syncUsers) {
            return ServerMain.users.get(userNic);
        }
    }

    public static ServerThread registerUser(String userNic, ServerThread user) {
        synchronized (ServerMain.syncUsers) {
            ServerThread old = ServerMain.users.get(userNic);
            if (old == null) {
                ServerMain.users.put(userNic, user);
            }
            return old;
        }
    }

    public static ServerThread setUser(String userNic, ServerThread user) {
        synchronized (ServerMain.syncUsers) {
            ServerThread res = ServerMain.users.put(userNic, user);
            if (user == null) {
                ServerMain.users.remove(userNic);
            }
            return res;
        }
    }

    public static String[] getUsers() {
        synchronized (ServerMain.syncUsers) {
            return ServerMain.users.keySet().toArray(new String[0]);
        }
    }

    public static int getNumUsers() {
        synchronized (ServerMain.syncUsers) {
            return ServerMain.users.keySet().size();
        }
    }
}

class ServerStopThread extends CommandThread {

    static final String cmd  = "q";
    static final String cmdL = "quit";

    Scanner fin;

    public ServerStopThread() {
        fin = new Scanner(System.in);
        ServerMain.setStopFlag(false);
        putHandler(cmd, cmdL, new CmdHandler() {
            @Override
            public boolean onCommand(int[] errorCode) {	return onCmdQuit(); }
        });
        this.setDaemon(true);
        System.err.println("Enter '" + cmd + "' or '" + cmdL + "' to stop server\n");
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
            if (!fin.hasNextLine()) continue;
            String str = fin.nextLine();
            if (command(str)) {
                break;
            }
        }
    }

    public boolean onCmdQuit() {
        System.err.print("stop server...");
        fin.close();
        ServerMain.setStopFlag(true);
        return true;
    }
}

class ServerThread extends Thread {

    private Socket sock;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private InetAddress addr;

    private String userNic = null;
    private String userFullName;

    private Object syncLetters = new Object();
    private Vector<String> letters = null;

    public void addLetter(String letter) {
        synchronized (syncLetters) {
            if (letters == null) {
                letters = new Vector<String>();
            }
            letters.add(letter);
        }
    }

    public synchronized String[] getLetters() {
        if (letters != null && !letters.isEmpty()) {
            String[] lts = letters.toArray(new String[0]);
            //letters.clear();  // Очищаем после чтения
            return lts;
        }
        return new String[0];
    }

    public ServerThread(Socket s) throws IOException {
        sock = s;
        s.setSoTimeout(1000);
        os = new ObjectOutputStream(s.getOutputStream());
        is = new ObjectInputStream(s.getInputStream());
        addr = s.getInetAddress();
        this.setDaemon(true);
    }

    public void run() {
        try {
            while (true) {
                Message msg = null;
                try {
                    msg = (Message) is.readObject();
                } catch (IOException | ClassNotFoundException e) {
                }
                if (msg != null) switch (msg.getID()) {
                    case Protocol.CMD_CONNECT:
                        if (!connect((MessageConnect) msg)) return;
                        break;

                    case Protocol.CMD_DISCONNECT:
                        return;

                    case Protocol.CMD_USER:
                        user((MessageUser) msg);
                        break;

                    case Protocol.CMD_CHECK_MAIL:
                        checkMail((MessageCheckMail) msg);
                        break;

                    case Protocol.CMD_LETTER:
                        letter((MessageLetter) msg);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.print("Disconnect...");
        } finally {
            disconnect();
        }
    }

    boolean connect(MessageConnect msg) throws IOException {
        ServerThread old = register(msg.userNic, msg.userFullName);
        if (old == null) {
            os.writeObject(new MessageConnectResult());
            broadcastMessage("User " + msg.userFullName + " connected.");
            return true;
        } else {
            os.writeObject(new MessageConnectResult("User " + old.userFullName + " already connected as " + userNic));
            return false;
        }
    }

    void letter(MessageLetter msg) throws IOException {
        String message = userNic + ": " + msg.txt;
        broadcastMessage(message);
        os.writeObject(new MessageLetterResult(""));
    }

    void user(MessageUser msg) throws IOException {
        String[] nics = ServerMain.getUsers();
        if (nics != null)
            os.writeObject(new MessageUserResult(nics));
        else
            os.writeObject(new MessageUserResult("Unable to get users list"));
    }

    void checkMail(MessageCheckMail msg) throws IOException {
        String[] lts = getLetters();
        if (lts != null)
            os.writeObject(new MessageCheckMailResult(lts));
        else
            os.writeObject(new MessageCheckMailResult("Unable to get mail"));
    }

    private boolean disconnected = false;
    public void disconnect() {
        if (!disconnected) {
            try {
                System.err.println(addr.getHostName() + " disconnected");
                broadcastMessage("User " + userFullName + " disconnected.");
                unregister();
                os.close();
                is.close();
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                this.interrupt();
                disconnected = true;
            }
        }
    }

    private void unregister() {
        if (userNic != null) {
            ServerMain.setUser(userNic, null);
            userNic = null;
        }
    }

    private ServerThread register(String nic, String name) {
        ServerThread old = ServerMain.registerUser(nic, this);
        if (old == null) {
            if (userNic == null) {
                userNic = nic;
                userFullName = name;
                System.err.println("User '" + name + "' registered as '" + nic + "'");
            }
        }
        return old;
    }

    private void broadcastMessage(String message) {
        String[] users = ServerMain.getUsers();
        for (String user : users) {
            ServerThread userThread = ServerMain.getUser(user);
            if (userThread != null) {
                userThread.addLetter(message);  // Добавляем сообщение в очередь почты

                /*try {
                    // Задержка перед отправкой сообщения следующему пользователю
                    Thread.sleep(100);  // Задержка 100 мс

                    // Отправляем сообщение текущему пользователю
                    userThread.os.writeObject(new MessageLetterResult(message));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }*/
            }
        }
    }
}
