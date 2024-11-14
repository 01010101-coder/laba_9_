package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.TreeMap;

import csdev.*;

/**
 * <p>Main class of client application
 * <p>Realized in console
 * <br>Use arguments: userNic userFullName [host]
 *
 * @version 1.0
 */
public class ClientMain {
    // arguments: userNic userFullName [host]
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Invalid number of arguments\nUse: nic name [host]");
            waitKeyToStop();
            return;
        }
        try (Socket sock = (args.length == 2 ?
                new Socket(InetAddress.getLocalHost(), Protocol.PORT) :
                new Socket(args[2], Protocol.PORT))) {
            System.err.println("initialized");
            session(sock, args[0], args[1]);
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            System.err.println("bye...");
        }
    }

    static void waitKeyToStop() {
        System.err.println("Press a key to stop...");
        try {
            System.in.read();
        } catch (IOException e) {
        }
    }

    static class Session {
        boolean connected = false;
        String userNic = null;
        String userName = null;
        Session(String nic, String name) {
            userNic = nic;
            userName = name;
        }
    }

    static void session(Socket s, String nic, String name) {
        try (Scanner in = new Scanner(System.in);
             ObjectInputStream is = new ObjectInputStream(s.getInputStream());
             ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream())) {
            Session ses = new Session(nic, name);
            if (openSession(ses, is, os, in)) {
                try {
                    while (true) {
                        Message msg = getCommand(ses, in);
                        if (!processCommand(ses, msg, is, os)) {
                            break;
                        }
                    }
                } finally {
                    closeSession(ses, os);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    static boolean openSession(Session ses, ObjectInputStream is, ObjectOutputStream os, Scanner in)
            throws IOException, ClassNotFoundException {
        os.writeObject(new MessageConnect(ses.userNic, ses.userName));
        MessageConnectResult msg = (MessageConnectResult) is.readObject();
        if (!msg.Error()) {
            System.err.println("connected");
            ses.connected = true;
            return true;
        }
        System.err.println("Unable to connect: " + msg.getErrorMessage());
        System.err.println("Press <Enter> to continue...");
        if (in.hasNextLine()) in.nextLine();
        return false;
    }

    static void closeSession(Session ses, ObjectOutputStream os) throws IOException {
        if (ses.connected) {
            ses.connected = false;
            os.writeObject(new MessageDisconnect());
        }
    }

    static Message getCommand(Session ses, Scanner in) {
        while (true) {
            printPrompt();
            if (!in.hasNextLine()) break;
            String str = in.nextLine();
            byte cmd = translateCmd(str);
            switch (cmd) {
                case -1:
                    return null;
                case Protocol.CMD_CHECK_MAIL:
                    return new MessageCheckMail();
                case Protocol.CMD_USER:
                    return new MessageUser();
                case Protocol.CMD_LETTER:
                    return inputLetter(in);
                case 0:
                    continue;
                default:
                    System.err.println("Unknown command!");
                    continue;
            }
        }
        return null;
    }

    static MessageLetter inputLetter(Scanner in) {
        System.out.print("Enter message: ");
        String letter = in.nextLine();
        return new MessageLetter("all", letter);  // Сообщение отправляется всем
    }

    static TreeMap<String, Byte> commands = new TreeMap<>();
    static {
        commands.put("q", (byte) -1);
        commands.put("quit", (byte) -1);
        commands.put("m", (byte) Protocol.CMD_CHECK_MAIL);
        commands.put("mail", (byte) Protocol.CMD_CHECK_MAIL);
        commands.put("u", (byte) Protocol.CMD_USER);
        commands.put("users", (byte) Protocol.CMD_USER);
        commands.put("l", (byte) Protocol.CMD_LETTER);
        commands.put("letter", (byte) Protocol.CMD_LETTER);
    }

    static byte translateCmd(String str) {
        str = str.trim();
        Byte r = commands.get(str);
        return (r == null ? 0 : r);
    }

    static void printPrompt() {
        System.out.println();
        System.out.print("(q)uit/(m)ail/(u)sers/(l)etter >");
        System.out.flush();
    }

    static boolean processCommand(Session ses, Message msg,
                                  ObjectInputStream is, ObjectOutputStream os)
            throws IOException, ClassNotFoundException {
        if (msg != null) {
            os.writeObject(msg);
            MessageResult res = (MessageResult) is.readObject();

            if (res.Error()) {
                System.err.println(res.getErrorMessage());
            } else {
                // Проверка и вывод результата для каждой команды
                switch (res.getID()) {
                    case Protocol.CMD_CHECK_MAIL:  // Команда mail
                        printMail((MessageCheckMailResult) res);
                        break;
                    case Protocol.CMD_USER:        // Команда users
                        printUsers((MessageUserResult) res);
                        break;
                    case Protocol.CMD_LETTER:      // Команда letter
                        System.out.println("Message sent.");  // Подтверждение отправки
                        break;
                    default:
                        assert false : "Unknown command received!";
                        break;
                }
            }
            return true;
        }
        return false;
    }


    static void printMail(MessageCheckMailResult m) {
        if (m.letters != null && m.letters.length > 0) {
            System.out.println("Your mail {");
            for (String str : m.letters) {
                System.out.println(str);
            }
            System.out.println("}");
        } else {
            System.out.println("No mail...");
        }
    }

    static void printUsers(MessageUserResult m) {
        if (m.userNics != null) {
            System.out.println("Users {");
            for (String str : m.userNics) {
                System.out.println("\t" + str);
            }
            System.out.println("}");
        }
    }
}
