import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Stack;
import java.util.concurrent.*;
import java.util.HashMap;

public class TCPServer {
    private static final int PORT = 1234;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static final HashMap<String, String> keyValueStore = new HashMap<>();
    private static String response;
    private static final Stack<String> clientsStack = new Stack<>();


    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("["+ ClientHandler.getCurrentTimestamp()+"]"+"    TCP Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executorService.submit(new ClientHandler(clientSocket));
            System.out.println("[" + ClientHandler.getCurrentTimestamp() + "]" + "    Client connection successfully created.");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String clientIP;
        private final PUT<String, String> put;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientIP = socket.getRemoteSocketAddress().toString();
            this.put = new PUT<>(keyValueStore);
        }

        @Override
        public void run() {
            clientsStack.push(clientIP);
            try {
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

                while (true) {
                    String command = input.readUTF();
                    String[] tokens = command.split(" ");
                    synchronized (keyValueStore) {switch (tokens[0]) {
                        case "PUT":
                            handlePutRequest(clientIP, PORT, tokens);
                            output.writeUTF(response);
                            break;
                        case "GET":
                            handleGetRequest(clientIP, PORT, tokens);
                            output.writeUTF(response);
                            break;
                        case "DELETE":
                            handleDelRequest(clientIP, PORT, tokens);
                            output.writeUTF(response);
                            break;
                        case "KEYS":
                            handleKeysRequest(clientIP, PORT, tokens);
                            output.writeUTF(response);
                            break;
                        case "LASTCLIENT":
                            handleLastClientRequest(output, clientIP, PORT);
                            output.writeUTF(response);
                            break;
                        case "QUIT":
                            handleQuitRequest(clientIP, PORT, tokens);
                            clientSocket.close();
                            output.writeUTF(response);
                            return;
                        default:
                            System.out.println("[" + getCurrentTimestamp() + "]    Improper command is entered.");
                            response = "ERROR: Wrong command type.\n";
                            output.writeUTF(response);
                    }
                    }
                }
            }
            catch(IOException e){
                System.out.println("[" + getCurrentTimestamp() + "]    IO Error.");
            } finally{
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private static String getCurrentTimestamp() {
            return LocalDateTime.now().toString();
        }
        private void handlePutRequest(String clientIP, int clientPort, String[] tokens){
            if(tokens.length==3) {
                System.out.println("[" + getCurrentTimestamp() + "]    Received PUT request from " + getLogHeader(clientIP, clientPort) + " to put key (" + tokens[1] + ") with value (" + tokens[2] + ").");
                if(tokens[1].isEmpty() || tokens[2].isEmpty()){
                    response = "Key or Value cannot be equal to null.";
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to emptiness of Key.");
                }
                else {
                    put.putTech(tokens[1], tokens[2]);
                }
            }
            else{
                System.out.println("[" + getCurrentTimestamp() + "]    Received PUT request from " + getLogHeader(clientIP, clientPort) + " to put key.");
                response = "Not valid input for PUT command.";
                System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to not valid input.");
            }
        }
        private static void handleGetRequest(String clientIP, int clientPort, String[] tokens){
            if(tokens.length==2) {
                System.out.println("["+getCurrentTimestamp()+"]    Received GET request from "+getLogHeader(clientIP, clientPort)+" to read key ("+tokens[1]+").");
                if (keyValueStore.containsKey(tokens[1])) {
                    response = "Success: (" + keyValueStore.get(tokens[1]) + ") Value found in the store.\n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Value of (" + tokens[1] +
                            ") has successfully received by client.");
                } else {
                    response = "the Key " + tokens[1] + " does not exist in the store\n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to non-existence of such key.");
                }
            } else{
                System.out.println("["+getCurrentTimestamp()+"]    Received GET request from "+getLogHeader(clientIP, clientPort)+" to read key.");
                response = "Not valid input for GET command.";
                System.out.println("["+getCurrentTimestamp()+"]    Request cancelled due to not valid input.");
            }
        }

        private static void handleDelRequest(String clientIP, int clientPort, String[] tokens){
            if(tokens.length==2) {
                System.out.println("["+getCurrentTimestamp()+"]    Received DELETE request from "+getLogHeader(clientIP, clientPort)+" to delete key ("+tokens[1]+ ") from the store.");
                if (keyValueStore.containsKey(tokens[1])) {
                    keyValueStore.remove(tokens[1]);
                    response = "Key - Value Pair removed from the server. \n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Key (" + tokens[1] + ") had successfully deleted from the store.");
                } else {
                    response = "[Err] the Key (" + tokens[1] + ") does not exist. \n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to non-existence of such key.");
                }
            } else{
                System.out.println("["+getCurrentTimestamp()+"]    Received DELETE request from "+getLogHeader(clientIP, clientPort)+" to delete key from the store.");
                response = "Not valid input for DELETE command.";
                System.out.println("["+getCurrentTimestamp()+"]    Request cancelled due to not valid input.");
            }
        }

        private static void handleKeysRequest(String clientIP, int clientPort, String[] tokens){
            System.out.println("[" + getCurrentTimestamp() + "]    Received KEYS request from "+getLogHeader(clientIP, clientPort)+" to show list of keys.");
            if(tokens.length ==1) {
                if (keyValueStore.isEmpty()) {
                    response = "The Key list is empty.\n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to emptiness of Key-list.");
                } else {
                    response = " Key list: " + String.join(", ", keyValueStore.keySet()) + "\n";
                    System.out.println("[" + getCurrentTimestamp() + "]    Keys are successfully shown to client.");
                }
            } else{
                response = "Not valid input for KEYS command.";
                System.out.println("["+getCurrentTimestamp()+"]    Request cancelled due to not valid input.");
            }
        }

        private static void handleQuitRequest(String clientIP, int clientPort, String[] tokens){
            System.out.println("["+getCurrentTimestamp()+"]    Received QUIT from "+getLogHeader(clientIP, clientPort)+" to exit.");
            System.out.println("[" + getCurrentTimestamp() + "]    Successfully exited.");
            cleanUp();
        }
        private static void cleanUp(){
            keyValueStore.clear();
            System.out.println("[" + getCurrentTimestamp() + "]    Clean up performed.");
        }
        private static String getLogHeader(String clientIP, int clientPort){
            return "[port: "+clientPort+", ip: "+clientIP+"]";
        }
        private static class PUT<K, V> {
            private HashMap<K,V> map;
            public PUT(HashMap<K, V> hash){
                this.map = hash;
            }
            public void putTech(K key, V value){
                if (map.containsKey(key) && map.containsValue(value)) {
                    response = "You have such Key - Value Pair in store yet.";
                    System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to existence of such Key - Value Pair.");
                } else {
                    if (key.toString().length() <= 10 && value.toString().length() <= 10) {
                        map.put(key, value);
                        response = "Success: Key - Value Pair now in the store.\n";
                        System.out.println("[" + getCurrentTimestamp() + "]    Key - " +
                                "Value Pair successfully stored in the store.");
                    } else {
                        response = "[Err] Key or Value too long(max 10 characters).";
                        System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to limit of chars.");
                    }
                }
            }
        }
        private static void handleLastClientRequest(DataOutputStream output, String clientIP, int clientPort) throws IOException {
            System.out.println("["+getCurrentTimestamp()+"]    Received LASTCLIENT from "+getLogHeader(clientIP, clientPort)+" to show the last user.");
            if(clientsStack.isEmpty()){
                response = "No clients are connected.";
                System.out.println("[" + getCurrentTimestamp() + "]    Request cancelled due to non-connection with clients.");
            }
            else{
                String lastClient = clientsStack.peek();
                response = "Last client: "+lastClient;
                System.out.println("[" + getCurrentTimestamp() + "]    Last client is successfully shown to the current one.");
            }
        }
    }
}
