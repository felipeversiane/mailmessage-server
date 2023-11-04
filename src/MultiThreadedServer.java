import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiThreadedServer {
    static Set<String> usernames = Collections.synchronizedSet(new HashSet<>());
    static Map<String, ClientDetails> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
