package berlin.tu.eecs;

import berlin.tu.eecs.chat.DeleteMessageReply;
import berlin.tu.eecs.chat.DeleteMessageRequest;
import berlin.tu.eecs.chat.Empty;
import berlin.tu.eecs.chat.AdminChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Verbindet sich mit gRPC-Server & benutzt AdminChatService
// Löscht Nachricht per ID & fragt aktuelle Nachrichtenanzahl ab

public class AdminClient {

    public static void main(String[] args) {
        int port = 50051;
        long messageIdToDelete = 1;

        // Verbindung zum Server auf Port 50051 aufbauen
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        try {

            // Admin-Stub erstellen (über diesen werden Admin-Methoden aufgerufen)
            AdminChatServiceGrpc.AdminChatServiceBlockingStub stub =
                    AdminChatServiceGrpc.newBlockingStub(channel);

            // Nachricht löschen (Client schickt ID der Nachricht, die gelöscht werden soll)
            DeleteMessageReply reply = stub.deleteMessage(
                    DeleteMessageRequest.newBuilder()
                            .setId(messageIdToDelete)
                            .build()
            );

            // Server antwortet mit:
            // deleted = true/false
            // remaining = Anzahl verbleibender Nachrichten
            System.out.println("Gelöscht: " + reply.getDeleted());
            System.out.println("Verbleibende Nachrichten: " + reply.getRemaining());

            // Nachrichten zählen
            int count = stub.countMessages(Empty.newBuilder().build()).getCount();
            System.out.println("Aktuelle Anzahl: " + count);

        } finally {
            channel.shutdownNow(); // Verbindung schließen
        }
    }
}
