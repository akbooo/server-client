import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class UDPClient {
    private DatagramSocket socket;
    private InetAddress address;

    public UDPClient() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public void close() {
        socket.close();
    }

    public void sendInfo(String msg) throws IOException {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 12345);
        socket.send(packet);
        buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Server response: " + received);
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            UDPClient client = new UDPClient();
            String input;
            while (true) {
                System.out.println("\nPlease Input Command in either of the following forms:");
                System.out.println("GET <key>");
                System.out.println("PUT <key> <val>");
                System.out.println("DELETE <key>");
                System.out.println("KEYS");
                System.out.println("LASTCLIENT");
                System.out.println("QUIT");
                System.out.print("Enter Command: ");
                input = scanner.nextLine();
                if ("QUIT".equals(input)) {
                    break;
                }
                client.sendInfo(input);
            }

            client.close();
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}