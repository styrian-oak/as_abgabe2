package berlin.tu.eecs;

import berlin.tu.eecs.chat.ChatMessage;
import berlin.tu.eecs.chat.Empty;
import berlin.tu.eecs.chat.MessageList;
import berlin.tu.eecs.chat.SendMessageRequest;
import berlin.tu.eecs.chat.UserChatServiceGrpc;
import io.grpc.stub.StreamObserver;

// Server-seitige Implementierung des User-Services
// Hier wird festgelegt, was auf Server passiert, wenn ein User-Client gRPC-Methoden
// wie SendMessage oder ListMessages aufruft

// Grundidee: Klasse ist Verbindung zwischen:
//
// gRPC-Anfrage vom User-Client
//        |
//        V
// Server-Logik
//        |
//        V
// ChatStore-Datenhaltung
//        |
//        V
// gRPC-Antwort an den Client

// User-Client ruft also nicht direkt ChatStore auf
// Er ruft gRPC-Methode auf, & diese Klasse übersetzt Aufruf in normale Java-Logik

// extends sagt: "Ich implementiere den UserChatService auf dem Server"
public class UserChatServiceImpl extends UserChatServiceGrpc.UserChatServiceImplBase {

    private final ChatStore store; // Der gleiche ChatStore wird auch vom Admin-Service verwendet
                                   // Deshalb können User Nachrichten schreiben & Admins dieselben
                                   // Nachrichten löschen

    // Konstruktor bekommt ChatStore übergeben, damit sich User- & Admin-Service dieselbe
    // Datenhaltung teilen können
    public UserChatServiceImpl(ChatStore store) {
        this.store = store;
    }

    // Methode wird ausgeführt, wenn ein User-Client Nachricht sendet
    @Override
    public void sendMessage(
            SendMessageRequest request,
            StreamObserver<ChatMessage> responseObserver // StreamObserver ist das Objekt,
                                                         // mit dem Server Antwort zurück an
                                                         // Client schickt
    ) {
        ChatMessage message = store.addMessage( // Nachricht speichern (ChatStore erzeugt
                                                // autom. ID & Zeitstempel)
                request.getAuthor(),
                request.getText()
        );

        responseObserver.onNext(message);       // Antwort an Client senden
        responseObserver.onCompleted();         // gRPC-Aufruf beenden
    }

    // Methode wird ausgeführt, wenn ein User-Client alle Nachrichten anzeigen möchte
    @Override
    public void listMessages(
            Empty request,
            StreamObserver<MessageList> responseObserver
    ) {
        MessageList list = MessageList.newBuilder()   // Eine MessageList bauen
                .addAllMessages(store.listMessages()) // listMessages-Methode von ChatStore
                                                      // (dh gespeicherte Nachrichten werden
                                                      // aus ChatStore geholt)
                .build();

        responseObserver.onNext(list);                // Liste an Client senden
        responseObserver.onCompleted();
    }
}
