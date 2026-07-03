package berlin.tu.eecs;

import berlin.tu.eecs.chat.CountReply;
import berlin.tu.eecs.chat.DeleteMessageReply;
import berlin.tu.eecs.chat.DeleteMessageRequest;
import berlin.tu.eecs.chat.Empty;
import berlin.tu.eecs.chat.AdminChatServiceGrpc;
import io.grpc.stub.StreamObserver;

// Server-Implementierung für Admin-Funktionen (gehört zur gRPC-Schnittstelle aus
// admin_chat.proto)

public class AdminChatServiceImpl extends AdminChatServiceGrpc.AdminChatServiceImplBase {

    private final ChatStore store;

    // ChatStore wird im Konstruktor übergeben, weil sich User & Admin denselben store teilen
    public AdminChatServiceImpl(ChatStore store) {
        this.store = store;
    }

    // Wird aufgerufen, wenn Admin-Client eine Nachricht löschen will (löscht Nachricht per ID)
    @Override
    public void deleteMessage(
            DeleteMessageRequest request,
            StreamObserver<DeleteMessageReply> responseObserver
    ) {
        boolean deleted = store.deleteMessage( // Nachricht aus ChatStore löschen
                request.getId()                // Vorher ID aus der Anfrage lesen
                );

        DeleteMessageReply reply = DeleteMessageReply.newBuilder() // Antwort bauen
                .setDeleted(deleted)                 // deleted = true/false
                .setRemaining(store.countMessages()) // remaining = Anzahl übriger Nachrichten
                .build();

        responseObserver.onNext(reply); // Antwort an Client senden
        responseObserver.onCompleted();
    }

    @Override
    public void countMessages(
            Empty request,
            StreamObserver<CountReply> responseObserver
    ) {
        CountReply reply = CountReply.newBuilder()
                .setCount(store.countMessages())
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
