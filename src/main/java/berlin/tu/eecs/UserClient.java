package berlin.tu.eecs;

import berlin.tu.eecs.chat.ChatMessage;
import berlin.tu.eecs.chat.Empty;
import berlin.tu.eecs.chat.MessageList;
import berlin.tu.eecs.chat.SendMessageRequest;
import berlin.tu.eecs.chat.UserChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Client für normale Chat-Benutzer
// Verbindet sich mit gRPC-Server, sendet Nachricht & zeigt gespeicherte Nachrichten an
// Dafür benutzt er UserChatService

public class UserClient {

    public static void main(String[] args) {
        int port = 50051;
        String author = "Max Mustermann";
        String text = "Hallo Chat!";

        // Verbindung zum Server auf Port 50051 aufbauen
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        try {

            // Stub erstellen (Objekt, über das Client gRPC-Methoden aufruft)
            UserChatServiceGrpc.UserChatServiceBlockingStub stub =
                    UserChatServiceGrpc.newBlockingStub(channel);

            // Nachricht (Autor + Text) an Server senden
            ChatMessage message = stub.sendMessage(
                    SendMessageRequest.newBuilder()
                            .setAuthor(author)
                            .setText(text)
                            .build()
            );

            // Server erstellt vollständige ChatMessage (mit ID & Zeitstempel)
            System.out.println("Gesendet: #" + message.getId());

            // Client fragt alle gespeicherten Nachrichten ab
            MessageList messages = stub.listMessages(Empty.newBuilder().build());

            // Danach werden sie ausgegeben
            for (ChatMessage m : messages.getMessagesList()) {
                System.out.println(
                        "#" + m.getId() + " " + m.getAuthor() + ": " + m.getText()
                );
            }
        } finally {

            // Verbindung zum Server schließen
            channel.shutdownNow();
        }
    }
}
