import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringJoiner;

public class UDPServer {
    private DatagramSocket socket;
    private static final Map<String, String> store = new HashMap<>();
    private static final Stack<String> historyOfCommands = new Stack<>();
    private Stack<String> clientIPs = new Stack<>();
    private String clientIP;


    public UDPServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    public void listen() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        System.out.println("UDP Server is listening on port " + socket.getLocalPort());

        try {
            while (true) {
                socket.receive(packet);
                String command = new String(packet.getData(), 0, packet.getLength());
                clientIP = packet.getSocketAddress().toString();
                clientIPs.push(clientIP);

                String[] commands = command.split("\n");
                for (String cmd : commands) {
                    String response = processCommand(cmd);
                    byte[] responseData = response.getBytes();

                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String processCommand(String command) {
        historyOfCommands.push(command);
        String[] tokens = command.split(" ");
        switch (tokens[0]) {
            case "PUT":
                if (tokens.length == 3) {
                    PUT<String, String> putCommand = new PUT<>(tokens[1], tokens[2]);
                    System.out.println("[" + getCurrentTimestamp() + "]    Received PUT request from "+getLogHeader(clientIP, socket.getLocalPort())+" to put key: "
                            + putCommand.getKey() + ", value: " + putCommand.getValue() + ".");
                    if (store.containsKey(putCommand.getKey()) && store.containsValue(putCommand.getValue())) {
                        System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to existence of such Key - Value Pair.");
                        return "You have such Key - Value Pair in store yet.";
                    } else {
                        if (putCommand.getKey().isEmpty() || putCommand.getValue().isEmpty()) {
                            System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to emptiness of Key or Value.");
                            return "Key or Value cannot be equal to null.";
                        } else if (putCommand.getKey().length() <= 10 && putCommand.getValue().length() <= 10) {
                            store.put(putCommand.getKey(), putCommand.getValue());
                            System.out.println("[" + getCurrentTimestamp() + "]    Key - " + "Value Pair successfully stored in the store.");
                            return "Success: Key - Value Pair now in the store.";
                        } else {
                            System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to limit of chars.");
                            return "[Err] Key or Value too long(max 10 characters).";
                        }
                    }
                } else {
                    System.out.println("[" + getCurrentTimestamp() + "]    Received PUT request from "+getLogHeader(clientIP, socket.getLocalPort())+" to put key.");
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to not valid input.");
                    return "Not valid input for PUT command.";
                }
            case "GET":
                if (tokens.length == 2 ) {
                    System.out.println("["+getCurrentTimestamp()+"]    Received GET request from "+getLogHeader(clientIP, socket.getLocalPort())+" to read key ("+tokens[1]+").");
                    if(store.containsKey(tokens[1])) {
                        String value = store.getOrDefault(tokens[1], "ERROR: Key not found");
                        System.out.println("[" + getCurrentTimestamp() + "]    Value of (" + tokens[1] +
                                ") has successfully received by client.");
                        return tokens[1] + " = " + value;
                    }else{
                        System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to non-existence of such key.");
                        return "the Key " + tokens[1] + " does not exist in the store";
                    }
                } else {
                    System.out.println("["+getCurrentTimestamp()+"]    Received GET request from "+getLogHeader(clientIP, socket.getLocalPort())+" to read key.");
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to not valid input.");
                    return "Not valid input for PUT command.";
                }
            case "DELETE":
                if (tokens.length == 2) {
                    System.out.println("["+getCurrentTimestamp()+"]    Received DELETE request from "+getLogHeader(clientIP, socket.getLocalPort())+" to delete key ("+tokens[1]+ ") from the store.");
                    if (store.containsKey(tokens[1])) {
                        System.out.println("[" + getCurrentTimestamp() + "]    Key (" + tokens[1] + ") had successfully deleted from the store.");
                        store.remove(tokens[1]);
                        return "Key - Value Pair removed from the server.";
                    } else {
                        System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to non-existence of such key.");
                        return "[Err] the Key (" + tokens[1] + ") does not exist.";
                    }
                } else {
                    System.out.println("["+getCurrentTimestamp()+"]    Received DELETE request from "+getLogHeader(clientIP, socket.getLocalPort())+" to delete key from the store.");
                    System.out.println("["+getCurrentTimestamp()+"]    Request cancelled due to not valid input.");
                    return "Not valid input for DELETE command.";
                }
            case "KEYS":
                System.out.println("[" + getCurrentTimestamp() + "]    Received KEYS from "+getLogHeader(clientIP, socket.getLocalPort())+" to show list of keys.");
                if(tokens.length==1) {
                    if (store.isEmpty()) {
                        System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to emptiness of Key-list.");
                        return "The Key list is empty.";
                    } else {
                        StringJoiner joiner = new StringJoiner(" ");
                        for (String key : store.keySet()) {
                            joiner.add(key);
                        }
                        System.out.println("[" + getCurrentTimestamp() + "]    Keys are successfully shown to client.");
                        return "Keys: " + String.join(", ",store.keySet());
                    }
                }else{
                    System.out.println("["+getCurrentTimestamp()+"]    Request cancelled due to not valid input.");
                    return "Not valid input for KEYS command.";
                }
            case "LASTCLIENT":
                System.out.println("["+getCurrentTimestamp()+"]    Received LASTCLIENT command from "+getLogHeader(clientIP, socket.getLocalPort())+" to show the last client.");
                System.out.println("["+getCurrentTimestamp()+"]    Last client is successfully shown to the current one.");
                return "Last client IP: " + getLastClientIP();
            default:
                return "ERROR: Unknown command";
        }
    }

    public static void main(String[] args) {
        try {
            UDPServer server = new UDPServer(12345);
            server.listen();
            System.out.println("Client connection successfully created.");
        } catch (SocketException e) {
            System.out.println("Failed to initialize server: " + e.getMessage());
        }
    }
    private static String getCurrentTimestamp() {
        return java.time.LocalDateTime.now().toString();
    }
    private String getLastClientIP() {
        if (!clientIPs.isEmpty()) {
            return clientIPs.peek();
        } else {
            return "No clients connected";
        }
    }
    private class PUT<K, V> {
        private K key;
        private V value;

        public PUT(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
    private static String getLogHeader(String clientIP, int clientPort){
        return "[port: "+clientPort+", ip: "+clientIP+"]";
    }
}