package berlin.tu.eecs;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// Zentraler gRPC-Server
// Hier wird gRPC-Server gestartet, User- und Admin-Service registriert
// und dafür gesorgt, dass beide Services dieselbe gemeinsame Chat-Datenhaltung verwenden
public class ChatServer {

    private final Server server;

    public ChatServer(int port) {
        ChatStore store = new ChatStore(); // Interne Datenhaltung erstellen

        this.server = ServerBuilder.forPort(port)
                .addService(new UserChatServiceImpl(store))  // gRPC-Services registrieren
                .addService(new AdminChatServiceImpl(store)) // Server bietet zwei Dienste an
                .build();                                    // Einen für User & einen für Admins
                                                             // Beide bekommen denselben ChatStore
    }

    public void start() throws IOException {

        // Server starten; danach wartet er auf Verbindungen von Clients
        server.start();
        System.out.println("ChatServer läuft auf Port " + getPort());
    }

    public int getPort() {
        return server.getPort();
    }

    // Beendet Server sauber
    public void stop() throws InterruptedException {
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    // Hält Programm offen, damit Server weiterläuft
    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer(50051);
        server.start();
        server.blockUntilShutdown();
    }
}
