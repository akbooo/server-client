import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.println("\nPlease Input Command in either of the following forms:");
                System.out.println("GET <key>");
                System.out.println("PUT <key> <val>");
                System.out.println("DELETE <key>");
                System.out.println("KEYS");
                System.out.println("LASTCLIENT");
                System.out.println("QUIT");
                System.out.print("Enter Command: ");

                String commandLine = scanner.nextLine();
                String[] tokens = commandLine.split(" ");
                String cmd = tokens[0];
                String key = tokens.length > 1 ? tokens[1] : null;
                String value = tokens.length > 2 ? tokens[2] : null;

                switch (cmd) {
                    case "GET":
                        handleGetRequest(cmd, key);
                        sendCommand(output, commandLine);
                        System.out.println("["+getCurrentTimestamp()+"]"+"  Response: " + receiveResponse(input));
                        break;
                    case "PUT":
                        handlePutRequest(cmd, key, value);
                        sendCommand(output, commandLine);
                        System.out.println("["+getCurrentTimestamp()+"]"+"  Response: " + receiveResponse(input));
                        break;
                    case "DELETE":
                        handleDelRequest(cmd, key);
                        sendCommand(output, commandLine);
                        System.out.println("["+getCurrentTimestamp()+"]"+"  Response: " + receiveResponse(input));
                        break;
                    case "KEYS":
                        handleKeysRequest(cmd);
                        sendCommand(output, commandLine);
                        System.out.println("["+getCurrentTimestamp()+"]"+"  Response: " + receiveResponse(input));
                        break;
                    case "LASTCLIENT":
                        handleLCRequest(cmd);
                        sendCommand(output, commandLine);
                        System.out.println("["+getCurrentTimestamp()+"]"+"  Response: " + receiveResponse(input));
                        break;
                    case "QUIT":
                        System.out.println("Disconnected from server.");
                        sendCommand(output, commandLine);
                        return;
                    default:
                        System.out.println("Invalid command.");
                        break;
                }
            }
        }
        catch (IOException e) {
            System.err.println("IO Error.");
        }
    }

    private static void sendCommand(DataOutputStream out, String command) throws IOException {
        out.writeUTF(command);
        out.flush();
    }

    private static String receiveResponse(DataInputStream in) throws IOException {
        return in.readUTF();
    }

    private static String getCurrentTimestamp() {

        return java.time.LocalDateTime.now().toString();
    }
    private static void handleGetRequest(String cmd, String key) {
        System.out.println("Handling request: " + cmd + " for " + key);
    }
    private static void handlePutRequest(String cmd, String key, String value) {
        System.out.println("Handling request: " + cmd + " for: " + key + " with value: " + value);
    }
    private static void handleDelRequest(String cmd, String key) {
        System.out.println("Handling request: " + cmd + " for: " + key);
    }
    private static void handleKeysRequest(String cmd) {
        System.out.println("Handling request: "+cmd);
    }
    private static void handleLCRequest(String cmd){
        System.out.println("handling request: "+cmd);
    }
}
