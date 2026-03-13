package promiseserializationtest;

import currency.classes.*;
import currencyGroupTests.CurrencyGroupTests;
import org.junit.Test;
import org.junit.jupiter.api.*;
import testHelper.AsapCurrencyTestHelper;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PromisSerializationTest {



    @Test
    public void serializeAndDeserializePromise() throws Exception {
        // 1. Arrange: Create the promise
        // Assumption based on the code snippet: parameters are (Identifier/Creditor, Amount)

        SharkCurrency sharkCurrency = new SharkLocalCurrency(false, "USD", "USD");

        byte [] groupID ={1,1,1,1,1,1};
        byte [] atest = {1,1,0,1,0,1};
        byte [] btest = {1,1,0,1,0,1,1,1,1,1,1,1,1};
        SharkPromise originalPromise = new SharkInMemoPromise( "Test" ,4, sharkCurrency, groupID ,false , false, 5000, "Alice", "BOB", atest , btest );

        // 2. Act: Simple object serialization (without cryptography)
        byte[] serializedBytes = SharkPromiseSerializer.sharkPromiseToByteArray(originalPromise, false);

        // 3. Assert: Byte array must not be null or empty
        assertNotNull(serializedBytes, "The serialized byte array must not be null.");
        assertTrue(serializedBytes.length > 0, "The serialized byte array must not be empty.");

        // 4. Act: Deserialization
        SharkPromise deserializedPromise = SharkPromiseSerializer.byteArrayToSharkPromise(serializedBytes);

        // 5. Assert: Verify values after deserialization
        assertNotNull(deserializedPromise, "The deserialized object must not be null.");
        assertEquals(originalPromise.getAmount(), deserializedPromise.getAmount(), "The amount must match.");
        // Optionally assert other getters, e.g.:
        // assertEquals(originalPromise.getCreditorID(), deserializedPromise.getCreditorID());

    }

    @Test
    public void serializeAndDeserializePromiseWithExcludeSignature() throws Exception {
        SharkCurrency sharkCurrency = new SharkLocalCurrency(false, "USD", "USD");

        byte [] groupID ={1,1,1,1,1,1};
        byte [] atest = {1,1,0,1,0,1};
        byte [] btest = {1,1,0,1,0,1,1,1,1,1,1,1,1};
        SharkPromise originalPromise = new SharkInMemoPromise( "Test" ,4, sharkCurrency, groupID ,false , false, 5000, "Alice", "BOB", atest , btest );
        // Set a dummy signature to test the filtering behavior
        byte[] dummySignature = new byte[]{1, 2, 3, 4};
        originalPromise.setCreditorSignature(dummySignature);

        // 2. Act: Serialize with the excludeSignature flag set to true
        byte[] serializedBytes = SharkPromiseSerializer.sharkPromiseToByteArray(originalPromise, true);
        SharkPromise deserializedPromise = SharkPromiseSerializer.byteArrayToSharkPromise(serializedBytes);

        // 3. Assert
        assertNotNull(deserializedPromise);
        assertEquals(originalPromise.getAmount(), deserializedPromise.getAmount());

    }

}
