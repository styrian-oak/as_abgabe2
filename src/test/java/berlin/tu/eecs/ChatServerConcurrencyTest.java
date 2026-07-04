package berlin.tu.eecs;

import berlin.tu.eecs.chat.AdminChatServiceGrpc;
import berlin.tu.eecs.chat.DeleteMessageRequest;
import berlin.tu.eecs.chat.Empty;
import berlin.tu.eecs.chat.SendMessageRequest;
import berlin.tu.eecs.chat.UserChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Die Unit-Tests prüfen, ob gRPC-Server bei parallelen Zugriffen korrekt funktioniert
// Test soll zeigen: Mehrere User- & Admin-Clients können gleichzeitig auf Server
// zugreifen, ohne dass interne Datenhaltung inkonsistent wird

class ChatServerConcurrencyTest {

    @Test
    void mehrereClientsKoennenParallelSendenUndLoeschen() throws Exception {

        ChatServer server = new ChatServer(0); // Port 0 heißt: OS wählt autom. freien Port
        server.start();                        // Server starten

        int port = server.getPort();

        try {
            // Erst 100 Nachrichten anlegen
            ManagedChannel setupChannel = ManagedChannelBuilder
                    .forAddress("localhost", port)
                    .usePlaintext()
                    .build();

            UserChatServiceGrpc.UserChatServiceBlockingStub setupUser =
                    UserChatServiceGrpc.newBlockingStub(setupChannel);

            for (int i = 0; i < 100; i++) {
                setupUser.sendMessage(
                        SendMessageRequest.newBuilder()
                                .setAuthor("setup")
                                .setText("Nachricht " + i)
                                .build()
                );
            }

            setupChannel.shutdownNow();

            ExecutorService executor = Executors.newFixedThreadPool(20);

            // Gleichzeitiger Start mit CountDownLatch
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Callable<Void>> tasks = new ArrayList<>();

            // 100 parallele User-Clients senden neue Nachrichten
            for (int i = 0; i < 100; i++) {
                final int index = i;

                tasks.add(() -> {
                    startSignal.await();

                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress("localhost", port)
                            .usePlaintext()
                            .build();

                    try {
                        UserChatServiceGrpc.UserChatServiceBlockingStub user =
                                UserChatServiceGrpc.newBlockingStub(channel);

                        user.sendMessage(
                                SendMessageRequest.newBuilder()
                                        .setAuthor("user-" + index)
                                        .setText("parallel-" + index)
                                        .build()
                        );
                    } finally {
                        channel.shutdownNow();
                    }

                    return null;
                });
            }

            // 50 parallele Admin-Clients löschen Nachrichten 1 bis 50
            for (long id = 1; id <= 50; id++) {
                final long messageId = id;

                tasks.add(() -> {
                    startSignal.await();

                    ManagedChannel channel = ManagedChannelBuilder
                            .forAddress("localhost", port)
                            .usePlaintext()
                            .build();

                    try {
                        AdminChatServiceGrpc.AdminChatServiceBlockingStub admin =
                                AdminChatServiceGrpc.newBlockingStub(channel);

                        admin.deleteMessage(
                                DeleteMessageRequest.newBuilder()
                                        .setId(messageId)
                                        .build()
                        );
                    } finally {
                        channel.shutdownNow();
                    }

                    return null;
                });
            }

            List<Future<Void>> futures = new ArrayList<>();

            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }

            startSignal.countDown();

            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            ManagedChannel checkChannel = ManagedChannelBuilder
                    .forAddress("localhost", port)
                    .usePlaintext()
                    .build();

            try {
                AdminChatServiceGrpc.AdminChatServiceBlockingStub admin =
                        AdminChatServiceGrpc.newBlockingStub(checkChannel);

                int count = admin.countMessages(Empty.newBuilder().build()).getCount();

                // Ergebnis prüfen
                // 100 Startnachrichten + 100 neue Nachrichten - 50 Löschungen = 150
                assertEquals(150, count);
            } finally {
                checkChannel.shutdownNow();
            }
        } finally {
            server.stop();
        }
    }
}
