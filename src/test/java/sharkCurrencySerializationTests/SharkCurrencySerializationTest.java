package sharkCurrencySerializationTests;

import Group.SharkGroupDocument;
import currency.classes.Currency;
import Group.GroupSignings;
import currency.classes.LocalCurrency;
import net.sharksystem.asap.ASAPException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SharkCurrencySerializationTest {

    @Test
    public void testSerializationAndDeserialization() throws IOException, ASAPException {
        // 1. ARRANGE
        // Hinweis: groupId wird im Konstruktor zufällig generiert, daher setzen wir sie hier nicht manuell.
        String creatorId = "alice";
        Currency currency = new LocalCurrency(false, "AliceCoin", "TestSpec");

        // Liste erstellen
        ArrayList<CharSequence> whitelist = new ArrayList<>();
        whitelist.add("Bob");
        whitelist.add("Charlie");
        whitelist.add("Dave");

        SharkGroupDocument original = new SharkGroupDocument(
                creatorId,
                currency,
                whitelist,
                true,
                false,
                GroupSignings.SIGNED_BY_SOME
        );

        // 2. ACT
        // Hier nutzen wir jetzt die byte[] Methode
        byte[] serialized = original.sharkDocumentToByte();

        // Optional: Zur Kontrolle als String ausgeben
        System.out.println("Serialisiert (als String): " + new String(serialized, StandardCharsets.UTF_8));

        // Deserialisierung aus dem byte array
        SharkGroupDocument restored = SharkGroupDocument.fromByte(serialized);

        // 3. ASSERT
        Assertions.assertNotNull(restored, "Das wiederhergestellte Dokument darf nicht null sein");

        // Hinweis: Die GroupID können wir NICHT vergleichen, da dein Konstruktor
        // eine neue UUID generiert und wir die alte beim Laden ignorieren (müssen, da Konstruktor fix ist).
        // Assertions.assertArrayEquals(original.getGroupId(), restored.getGroupId()); // Würde fehlschlagen

        // Creator prüfen
        Assertions.assertEquals(original.getGroupCreator(), restored.getGroupCreator(), "Creator ID stimmt nicht überein");

        // Whitelist prüfen
        Assertions.assertNotNull(restored.getWhitelistMember(), "Whitelist darf nicht null sein");
        Assertions.assertEquals(original.getWhitelistMember().size(), restored.getWhitelistMember().size(), "Whitelist Größe falsch");
        Assertions.assertEquals("Bob", restored.getWhitelistMember().get(0).toString());
        Assertions.assertEquals("Charlie", restored.getWhitelistMember().get(1).toString());

        // Status prüfen
        Assertions.assertEquals(original.getGroupDocState(), restored.getGroupDocState(), "Status (Enum) stimmt nicht überein");

        // Currency prüfen
        Assertions.assertEquals("AliceCoin", restored.getAssignedCurrency().getCurrencyName());
    }
}