package ru.kos.neb_viewer;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.*;

public class DesEncrypter
{
    Cipher ecipher;
    Cipher dcipher;
    
    // 8-byte Salt
    byte[] salt = {
        (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
        (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
    };
    
    // Iteration count
    int iterationCount = 19;
    
    @SuppressWarnings("unused")
    DesEncrypter(String passPhrase)
    {
        try
        {
            // Create the key
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
            SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
            ecipher = Cipher.getInstance(key.getAlgorithm());
            dcipher = Cipher.getInstance(key.getAlgorithm());
            
            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);

            // Create the ciphers
            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        }
        catch (InvalidAlgorithmParameterException | InvalidKeySpecException | NoSuchPaddingException |
               NoSuchAlgorithmException | InvalidKeyException _)
        {
        }
    }

}

