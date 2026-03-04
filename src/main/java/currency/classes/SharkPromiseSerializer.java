package currency.classes;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.crypto.ASAPKeyStore;
import net.sharksystem.asap.utils.ASAPSerialization;


import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class SharkPromiseSerializer {

    static byte [] serializePromise(SharkPromise promise, CharSequence sender,
                                       Set<CharSequence> receiver,
                                       boolean sign, boolean encrypt,
                                       ASAPKeyStore asapKeyStore, boolean excludeSignature,
                                       int usedFor) throws ASAPSecurityException, IOException {

        if( (receiver != null && receiver.size() > 1) && encrypt) {
            throw new ASAPSecurityException("cannot (yet) encrypt one message for more than one recipient - split it into more messages");
        }

        if(receiver == null || receiver.isEmpty()) {
            if(encrypt) throw new ASAPSecurityException("impossible to encrypt a message without a receiver");
            receiver = new HashSet<>();
            receiver.add(promise.getDebtorID());
        }

        if(sender == null) {
            sender = promise.getCreditorID();
        }

        // Convert promise to byteArray
        // use excludeSignature to exclude or include previous signatures (that can be helpful for a late verification of the the bond signatures)
        byte[] content = sharkPromiseToByteArray(promise, excludeSignature);

        // merge content, sender and recipient
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ///// content
        ASAPSerialization.writeByteArray(content, baos);
        ///// sender is the creditor
        ASAPSerialization.writeCharSequenceParameter(sender, baos);
        ///// recipients
        ASAPSerialization.writeCharSequenceSetParameter(receiver, baos);

        content = baos.toByteArray();

        byte flags = 0;
        // Sign Promise
        if(sign) {
            byte[] signature = ASAPCryptoAlgorithms.sign(content, asapKeyStore);
            // usedFor == 1 function is used for signature purpose
            if (usedFor == 1) {
                return signature;
            }

            baos = new ByteArrayOutputStream();
            ASAPSerialization.writeByteArray(content, baos);

            // usedFor == 2 function is used for verification purpose
            if (usedFor == 2) {
                return content;
            }

            // append signature
            ASAPSerialization.writeByteArray(signature, baos);
            // attach signature to message
            content = baos.toByteArray();
            flags += SharkPromise.SIGNED_MASK;
        }

        if(encrypt) {
            // Encrypt Message
            content = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(
                    content,
                    receiver.iterator().next(),
                    asapKeyStore);
            flags += SharkPromise.ENCRYPTED_MASK;
        }

        baos = new ByteArrayOutputStream();
        ASAPSerialization.writeByteParameter(flags, baos);
        ASAPSerialization.writeByteArray(content, baos);

        return baos.toByteArray();
    }

    public static SharkPromise deserialzePromise(byte [] asapMessage, ASAPKeyStore asapKeyStore) throws IOException, ASAPException {
        ByteArrayInputStream bais = new ByteArrayInputStream(asapMessage);
        byte flags = ASAPSerialization.readByte(bais);
        byte[] tmpMessage = ASAPSerialization.readByteArray(bais);

        boolean signed = (flags & SharkPromise.SIGNED_MASK) != 0;
        boolean encrypted = (flags & SharkPromise.ENCRYPTED_MASK) != 0;

        if (encrypted) {
            // decrypt
            bais = new ByteArrayInputStream(tmpMessage);
            ASAPCryptoAlgorithms.EncryptedMessagePackage
                    encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(bais);

            // for me?
            if (!asapKeyStore.isOwner(encryptedMessagePackage.getReceiver())) {
                throw new ASAPException("SharkPromise Message: message not for me. Current user: "
                        + asapKeyStore.getOwner()
                        + ", recipient: "
                        + encryptedMessagePackage.getReceiver());
            }
            // replace message with decrypted message
            tmpMessage = ASAPCryptoAlgorithms.decryptPackage(
                    encryptedMessagePackage, asapKeyStore);
        }

        byte[] signature = null;
        byte[] signedMessage = null;
        if (signed) {
            // split message from signature
            bais = new ByteArrayInputStream(tmpMessage);
            tmpMessage = ASAPSerialization.readByteArray(bais);
            signedMessage = tmpMessage;
            signature = ASAPSerialization.readByteArray(bais);
        }

        ///////////////// produce object form serialized bytes
        bais = new ByteArrayInputStream(tmpMessage);

        ////// content
        byte[] snMessage = ASAPSerialization.readByteArray(bais);
        ////// sender
        String snSender = ASAPSerialization.readCharSequenceParameter(bais);
        ////// recipients
        Set<CharSequence> snReceivers = ASAPSerialization.readCharSequenceSetParameter(bais);
        ///// timestamp
        // String timestampString = ASAPSerialization.readCharSequenceParameter(bais);
        // Timestamp creationTime = Timestamp.valueOf(timestampString);

        boolean verified = false; // initialize
        if (signature != null) {
            try {
                verified = ASAPCryptoAlgorithms.verify(
                        signedMessage, signature, snSender, asapKeyStore);
            } catch (ASAPSecurityException e) {
                // verified definitely false
                verified = false;
            }
        }

        // replace special sn symbols
        return byteArrayToSharkPromise(snMessage);
    }

    static byte[] sharkPromiseToByteArray(SharkPromise promise, boolean excludeSignature) {
            byte[] byteArray = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(promise);
                out.flush();
                byteArray = bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bos.close();
                } catch (IOException ex) {
                }
            }

            return byteArray;
    }

    static SharkPromise byteArrayToSharkPromise(byte [] byteArray) {
        SharkPromise bond = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            bond = (SharkPromise) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return bond;
    }



}
