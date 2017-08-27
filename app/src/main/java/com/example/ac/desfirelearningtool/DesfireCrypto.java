package com.example.ac.desfirelearningtool;

import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Ac on 8/26/2017.
 */

public class DesfireCrypto {

    public final byte MODE_3DES = (byte) 0x0A;
    public final byte MODE_3K3DES = (byte) 0x1A;
    public final byte MODE_AES = (byte) 0xAA;
    public final byte KEYTYPE_DES = 0;
    public final byte KEYTYPE_3DES = 1;
    public final byte KEYTYPE_3K3DES = 2;
    public final byte KEYTYPE_AES = 3;



    protected SecureRandom randomGenerator;
    private byte authType;
    private byte keyType;
    private Cipher cipher;
    private SecretKey keySpec;
    private byte[] rndA, rndB ;
    private byte [] currentIV;
    private int blockLength;
    private byte[] sessionKey;
    private byte[] K1, K2;  // Subkey for CMAC
    public boolean trackCMAC;


    public DesfireCrypto (){
        reset ();
        this.randomGenerator = new SecureRandom();

    }
    public void reset () {
        rndA = null;
        rndB = null;
        cipher = null;
        authType = (byte) 0x00;
        trackCMAC = false;
    }

    public boolean initialize (byte authToSet, byte [] key) throws Exception {
        boolean res;
        authType = authToSet;
        if (authType == MODE_3DES || authType == MODE_3K3DES) {
            getKeySpec(key);
        }else if (authType == MODE_AES) {
             getKeySpecAES(key);
        } else {
            Log.e("initialize", "AuthType not valid");
            return false;
        }
        return true;
    }

    // ENCRYPTION RELATED

    // Mifare Desfire specifications require DESede/ECB without padding
    protected boolean getKeySpec(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        ByteArray tripleDesKey = new ByteArray();
        if (origKey.length == 8) {
            tripleDesKey.append(origKey).append(origKey).append(origKey);
            keyType = KEYTYPE_DES;
        } else if (origKey.length == 16) {
            keyType = KEYTYPE_3DES;
            byte[] last8Byte = new byte[8];
            System.arraycopy(origKey, 0, last8Byte, 0, 8);
            tripleDesKey.append(origKey).append(last8Byte);
        } else if (origKey.length == 24) {
            keyType = KEYTYPE_3K3DES;
            tripleDesKey.append(origKey);
        } else {
            Log.e("getKeySpec", "Wrong Key Length");
            return false;
        }

        keySpec = new SecretKeySpec(tripleDesKey.toArray(), "DESede");;
        return true;
    }

    protected boolean getKeySpecAES(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {

        ByteArray AESKey = new ByteArray();
       if (origKey.length == 16) {
           keyType = KEYTYPE_AES;
           AESKey.append(origKey);
        } else{
           Log.e("getKeySpec", "Wrong Key Length");
           return false;
       }

        keySpec = new SecretKeySpec(AESKey.toArray(), "AES");
        return true;
    }



    // Mifare Desfire specifications require DESede/ECB without padding
    protected void initCipher()
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,InvalidAlgorithmParameterException {
        switch (authType) {
            case MODE_3DES:
                cipher = Cipher.getInstance("DESede/ECB/NoPadding");
                blockLength = 8;
                break;
            case MODE_3K3DES:
                cipher = Cipher.getInstance("DESede/CBC/NoPadding");
                blockLength = 8;
                currentIV = new byte [blockLength];
                Arrays.fill(currentIV, (byte)0);
                break;
            case MODE_AES:
                cipher = Cipher.getInstance("AES/CBC/NoPadding");
                blockLength = 16;
                currentIV = new byte [blockLength];
                Arrays.fill(currentIV, (byte)0);
                break;
            default:
                throw new InvalidAlgorithmParameterException("No such AuthType");
        }



    }



    public byte [] encrypt(byte [] encInput) throws GeneralSecurityException {
        if (cipher == null) {
            initCipher();
        }

        byte [] encOutput = null;

        switch (authType) {
            case MODE_3DES:
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                encOutput = cipher.doFinal(encInput);
                break;
            case MODE_3K3DES:
            case MODE_AES:
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                encOutput = cipher.doFinal(encInput);   // Decrypt
                // Write the first IV as the result from PICC's encryption
                System.arraycopy(encOutput, encOutput.length-blockLength, currentIV, 0, blockLength);
        }
        return encOutput;
    }

    public byte [] decrypt(byte [] decInput) throws GeneralSecurityException {
        if (cipher == null) {
            initCipher();
        }

        byte [] decOutput = null;

        switch (authType) {
            case MODE_3DES:
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                decOutput = cipher.doFinal(decInput);
                break;
            case MODE_3K3DES:
            case MODE_AES:
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                decOutput = cipher.doFinal(decInput);   // Decrypt
                // Write the first IV as the result from PICC's encryption
                System.arraycopy(decInput, decInput.length-blockLength, currentIV, 0, blockLength);
        }
        return decOutput;
    }







    public byte[] computeResponseAndDataToVerify(byte[] encRndB)
            throws GeneralSecurityException {

        byte[] decRndA, rndBPrime, decRndBPrime;

        byte[] challengeMessage = null;


        if (authType == MODE_3DES){


            // We decrypt the challenge, and rotate one byte to the left
            rndB = decrypt(encRndB);
            rndBPrime = ByteArray.rotateLT(rndB);

            // Then we generate a random number as our challenge for the coupler
            rndA = new byte[8];
            randomGenerator.nextBytes(rndA);


            decRndA = decrypt(rndA);
            // XOR of rndA, rndB  // This is CBC done manually
            decRndBPrime = ByteArray.xor(decRndA, rndBPrime);
            // The result is encrypted again
            decRndBPrime = decrypt(decRndBPrime);

            challengeMessage = new byte[decRndA.length + decRndBPrime.length];
            System.arraycopy(decRndA, 0, challengeMessage, 0, decRndA.length);
            System.arraycopy(decRndBPrime, 0, challengeMessage, decRndA.length, decRndBPrime.length);


        } else if (authType == MODE_3K3DES || authType == MODE_AES) {


            // We decrypt the challenge, and rotate one byte to the left
            rndB = decrypt(encRndB);
            Log.d("computeRAndDataToVerify", "currentIV           = " + ByteArray.byteArrayToHexString(currentIV));

            rndBPrime = ByteArray.rotateLT(rndB);
            Log.d("computeRAndDataToVerify", "rndBPrime           = " + ByteArray.byteArrayToHexString(rndBPrime));

            rndA = new byte[rndB.length];   // Length 8 byte for DES/3DES, 16 byte for 3k3des and AES
            randomGenerator.nextBytes(rndA);
            //Arrays.fill(rndA, (byte)0);


            byte[] encInput = new byte[rndA.length + rndBPrime.length];
            System.arraycopy(rndA, 0, encInput, 0, rndA.length);
            System.arraycopy(rndBPrime, 0, encInput, rndA.length, rndBPrime.length);


            challengeMessage = encrypt(encInput);

        }

        // And sent back to the card
        return challengeMessage;
    }



    public boolean verifyCardResponse(byte[] cardResponse)
            throws Exception {

        if ((cardResponse == null))
            return false;

        Log.d("verifyCardResponse", "cardResponse                       = " + ByteArray.byteArrayToHexString(cardResponse));


        // We decrypt the response and shift the rightmost byte "all around" (to the left)


        cardResponse = decrypt(cardResponse);
        Log.d("verifyCardResponse", "cardResponse decrypted             = " + ByteArray.byteArrayToHexString(cardResponse));
        cardResponse = ByteArray.rotateRT(cardResponse);
        Log.d("verifyCardResponse", "cardResponse decrypted and shifted = " + ByteArray.byteArrayToHexString(cardResponse));
        Log.d("verifyCardResponse", "                          origRndA = " + ByteArray.byteArrayToHexString(rndA));
        if (Arrays.equals(cardResponse, rndA)) {
            genSessionKey ();
            // ComputeSession Key
            return true;
        }

        return false;
    }

    private void genSessionKey()  throws Exception{
        switch (keyType) {
            case KEYTYPE_DES:
                sessionKey = new byte [8];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                Log.d("genSessionKey", "DES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpec(sessionKey);
                blockLength = 8;
                currentIV = new byte [blockLength];
                Arrays.fill(currentIV, (byte)0);
                break;
            case KEYTYPE_3DES:
                sessionKey = new byte [16];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 4, sessionKey, 8, 4);
                System.arraycopy(rndB, 4, sessionKey, 12, 4);
                Log.d("genSessionKey", "3DES sessionKey   = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpec(sessionKey);
                blockLength = 8;
                Arrays.fill(currentIV, (byte)0);
                break;
            case KEYTYPE_3K3DES:
                sessionKey = new byte [24];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 6, sessionKey, 8, 4);
                System.arraycopy(rndB, 6, sessionKey, 12, 4);
                System.arraycopy(rndA, 12, sessionKey, 16, 4);
                System.arraycopy(rndB, 12, sessionKey, 20, 4);
                Log.d("genSessionKey", "3K3DES sessionKey  = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpec(sessionKey);
                blockLength = 8;
                Arrays.fill(currentIV, (byte)0);
                trackCMAC = true;
                break;
            case KEYTYPE_AES:
                sessionKey = new byte [16];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 12, sessionKey, 8, 4);
                System.arraycopy(rndB, 12, sessionKey, 12, 4);
                Log.d("genSessionKey", "AES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));

                getKeySpecAES(sessionKey);
                blockLength = 16;
                Arrays.fill(currentIV, (byte)0);
                trackCMAC = true;
                break;
        }
        genSubKeys();
    }

    private static byte[] shiftLeft(byte[] data, int len) {
        int word_size = (len / 8) + 1;
        int shift = len % 8;
        byte carry_mask = (byte) ((1 << shift) - 1);
        int offset = word_size - 1;

        Log.d("shiftLeft", "        data = " + ByteArray.byteArrayToHexString(data));

        for (int i = 0; i < data.length; i++) {
            int src_index = i+offset;
            if (src_index >= data.length) {
                data[i] = 0;
            } else {
                byte src = data[src_index];
                byte dst = (byte) (src << shift);
                if (src_index+1 < data.length) {
                    dst |= data[src_index+1] >>> (8-shift) & carry_mask;
                }
                data[i] = dst;
            }
        }
        Log.d("shiftLeft", "shifted data = " + ByteArray.byteArrayToHexString(data));
        return data;
    }

    private byte [] shiftLeft1Bit (byte [] data) {
        return shiftLeft(data, 1);
    }

    private byte [] shiftLeft1Bit (byte [] data, byte Rb) {
        byte [] output = new byte[data.length];
        output = shiftLeft(data, 1);
        output[data.length-1] ^= Rb;
        return output;
    }

    public void genSubKeys () throws GeneralSecurityException{
        byte [] encInput = new byte [blockLength];
        Arrays.fill(encInput, (byte)0);

        // CMAC subkey generation according to NIST SP800-38B
        // http://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38b.pdf
        byte Rb;  // According to SP800-38B

        if (blockLength == 8) {
            Rb = (byte) 0x87;
        } else {
            Rb = (byte) 0x1B;
        }

        // 1. Let L = CIPHK(0)).
        byte [] L = encrypt(encInput);

        // 2. If MSB1(L) = 0, then K1 = L << 1;
        // Else K1 = (L << 1) XOR Rb; see Sec. 5.3 for the definition of Rb.
        if ((L[0] & (byte)0x80) == (byte)0) {
            K1 = shiftLeft1Bit (L);
        } else {
            K1 = shiftLeft1Bit (L, Rb);
        }

        // 3. If MSB1(K1) = 0, then K2 = K1 << 1;
        // Else K2 = (K1 << 1) XOR Rb.
        if ((K1[0] & (byte)0x80) == (byte)0) {
            K2 = shiftLeft1Bit (K1);
        } else {
            K2 = shiftLeft1Bit (K1, Rb);
        }

        // 4. Return K1, K2.
    }

    public byte [] calcCMAC (byte [] data)  {
        byte[] output, encInput;
        ByteArray baEncInput = new ByteArray();

        int extraLength = data.length % blockLength;
        if (extraLength != 0) {
            encInput = new byte[data.length + blockLength - extraLength];
            baEncInput.append(data).append(ByteArray.hexStringToByteArray("80000000000000000000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, data.length + blockLength - extraLength);
        } else {
            encInput = new byte[data.length];
            System.arraycopy(data, 0, encInput, 0, data.length);
        }

        int startIndex = encInput.length - blockLength;
        if (extraLength == 0) {
            //Mn = K1 XOR Mn*
            for (int i = startIndex; i < encInput.length; i++){
                encInput[i] ^= K1[i];
            }
        } else {
            //Mn = K2 XOR (Mn* with padding)
            for (int i = startIndex; i < encInput.length; i++){
                encInput[i] ^= K2[i];
            }
        }
        try {
            encrypt(encInput);
        } catch (GeneralSecurityException e) {
            Log.e("calcCMAC", e.getMessage(), e);
        }
        Log.d ("calcCMAC  ", "CMAC computed  = " + ByteArray.byteArrayToHexString(currentIV) );

        return currentIV;
    }

    public byte [] verifyCMAC (byte [] recvData)  {
        if (recvData.length < blockLength)
            return null;

        byte [] dataToVerify = new byte[recvData.length - blockLength];
        byte [] CMACToVerify = new byte[blockLength];

        System.arraycopy(recvData, 0, dataToVerify, 0, recvData.length - blockLength);
        System.arraycopy(recvData, recvData.length - blockLength, dataToVerify, 0,blockLength );
        calcCMAC(dataToVerify);
        Log.d ("verifyCMAC", "CMAC to Verify = " + ByteArray.byteArrayToHexString(CMACToVerify) );
        Log.d ("verifyCMAC", "CMAC computed  = " + ByteArray.byteArrayToHexString(currentIV) );

        return currentIV;
    }
}
