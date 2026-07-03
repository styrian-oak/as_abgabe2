package berlin.tu.eecs;

import berlin.tu.eecs.chat.ChatMessage;

import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

// Interne Datenhaltung des Servers
// Hier werden Nachrichten gespeichert, gelesen, gezählt & gelöscht

// ChatStore wird sowohl vom User- als auch vom Admin-Service benutzt (keine gRPC-Klasse)
public class ChatStore {

    // ID-Erzeugung
    private final AtomicLong nextId = new AtomicLong(1); // nextId erzeugt autom. neue Nachrichten-IDs
    // AtomicLong wichtig, weil mehrere Clients gleichzeitig Nachrichten senden können; Trotzdem darf
    // keine ID doppelt vergeben werden

    // Hier werden die Nachrichten gespeichert
    private final ConcurrentNavigableMap<Long, ChatMessage> messages = new ConcurrentSkipListMap<>();
    // Die Struktur ist ungefähr:
    // 1 -> ChatMessage #1
    // 2 -> ChatMessage #2
    // 3 -> ChatMessage #3
    // Schlüssel: Nachrichten-ID, Wert: eigentliche ChatMessage

    // ConcurrentSkipListMap ist thread-safe; dh mehrere Clients dürfen gleichzeitig lesen, schreiben
    // oder löschen, ohne dass die Map kaputtgeht


    // Neue Nachricht speichern
    // Methode wird aufgerufen, wenn ein User-Client Nachricht sendet
    public ChatMessage addMessage(String author, String text) {

                                                       // Ablauf:
        long id = nextId.getAndIncrement();            // 1. Neue ID erzeugen

        ChatMessage message = ChatMessage.newBuilder() // 2. Neues ChatMessage-Objekt bauen
                .setId(id)                             // 3. Autor, Text, ID, Zeitstempel setzen
                .setAuthor(author)
                .setText(text)
                .setTimestamp(System.currentTimeMillis())
                .build();

        messages.put(id, message);                     // 4. Nachricht in Map speichern
        return message;                                // 5. Fertige Nachricht zurückgeben
    }

    // Diese Methode gibt alle gespeicherten Nachrichten zurück
    public List<ChatMessage> listMessages() {
        return List.copyOf(messages.values()); // Aufrufer bekommt nicht direkt Zugriff auf
                                               // interne Datenstruktur, nur Kopie
                                               // Sauberer, weil niemand von außen interne
                                               // Map ändern kann
    }

    // Nachricht löschen (Methode wird vom Admin-Service benutzt)
    public boolean deleteMessage(long id) {
        return messages.remove(id) != null; // Versucht, Nachricht mit angegebener ID zu löschen
                                            // false, wenn keine Nachricht mit dieser ID existiert
                                            // true, wenn Nachricht gelöscht wurde
    }

    // Gibt zurück, wie viele Nachrichten aktuell gespeichert sind
    // Admin-Client benutzt das zB nach Löschvorgang
    public int countMessages() {
        return messages.size();
    }
}
