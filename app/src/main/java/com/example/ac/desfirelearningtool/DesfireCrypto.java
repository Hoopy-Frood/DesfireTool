package com.example.ac.desfirelearningtool;

import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Ac on 8/26/2017.
 */

public class DesfireCrypto {

    public final byte MODE_AUTHD40 = (byte) 0x0A;
    public final byte MODE_AUTHISO = (byte) 0x1A;
    public final byte MODE_AUTHAES = (byte) 0xAA;
    public final byte MODE_AUTHEV2 = (byte) 0x71;
    public final byte KEYTYPE_DES = 0;
    public final byte KEYTYPE_3DES = 1;
    public final byte KEYTYPE_3K3DES = 2;
    public final byte KEYTYPE_AES = 3;


    protected SecureRandom randomGenerator;
    private byte authMode;
    private byte keyType;
    private Cipher cipher;
    private SecretKey keySpec;
    private byte[] rndA, rndB;
    private byte[] currentIV;
    private byte[] encryptionIV;
    private int blockLength;
    private byte[] K1, K2;  // Subkey for CMAC
    private byte[] EV2_KSesAuthENC, EV2_KSesAuthMAC; // EV2 Session keys for Enc and Mac
    private SecretKey  EV2_KeySpecSesAuthENC, EV2_KeySpecSesAuthMAC; // EV2 Session key SPEC for Enc and Mac
    private byte [] EV2_K1, EV2_K2;
    private byte[] EV2_TI;
    private int EV2_CmdCtr;
    private byte [] nullBytes8, nullBytes16;


    public boolean trackCMAC;
    public boolean EV2_Authenticated;
    public int CRCLength;    // Length of CRC 2 or 4 bytes
    public int encryptedLength;  // specified dataLength at the first AF for Read Data
    public int currentAuthenticatedKey;


    public DesfireCrypto() {
        this.randomGenerator = new SecureRandom();

        storedAFData = new ByteArray();
        EV2_TI = new byte[4];
        nullBytes8 = new byte[8];
        nullBytes16 = new byte[16];
        reset();


    }

    public void reset() {
        rndA = null;
        rndB = null;
        cipher = null;
        authMode = (byte) 0x00;
        trackCMAC = false;
        EV2_Authenticated = false;
        CRCLength = 0;
        encryptedLength = 0;
        storedAFData.clear();
        currentAuthenticatedKey = -1;
        Arrays.fill(EV2_TI, (byte) 0);
        EV2_CmdCtr = 0;

        Arrays.fill(nullBytes8, (byte) 0);
        Arrays.fill(nullBytes16, (byte) 0);
    }

    //region Key Related
    //---------------------------------------------------------------------------------------
    public boolean initialize(byte authToSet, byte[] key) throws Exception {
        boolean res;

        reset();
        authMode = authToSet;

        genKeySpec(key);

        return true;
    }

    // ENCRYPTION RELATED

    // Mifare Desfire specifications require DESede/ECB without padding
    protected boolean genKeySpec(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        ByteArray fullLengthKey = new ByteArray();
        if (origKey.length == 8) {
            //tripleDesKey.append(origKey).append(origKey).append(origKey);
            fullLengthKey.append(origKey).append(origKey).append(origKey);
            keyType = KEYTYPE_DES;
            keySpec = new SecretKeySpec(fullLengthKey.toArray(), "DESede");
        } else if (origKey.length == 16) {
            if (authMode == MODE_AUTHAES || authMode == MODE_AUTHEV2 ) {
                keyType = KEYTYPE_AES;
                fullLengthKey.append(origKey);
                keySpec = new SecretKeySpec(fullLengthKey.toArray(), "AES");
            } else {
                keyType = KEYTYPE_3DES;
                byte[] last8Byte = new byte[8];
                System.arraycopy(origKey, 0, last8Byte, 0, 8);
                fullLengthKey.append(origKey).append(last8Byte);
                keySpec = new SecretKeySpec(fullLengthKey.toArray(), "DESede");
            }

        } else if (origKey.length == 24) {
            keyType = KEYTYPE_3K3DES;
            fullLengthKey.append(origKey);
            keySpec = new SecretKeySpec(fullLengthKey.toArray(), "DESede");
        } else {
            Log.e("genKeySpec", "Wrong Key Length");
            return false;
        }



        return true;
    }

    /**
     * genKeySpecEV2 - to generate session key spec for EV2 encryption and MAC
     * @param origKeyEnc
     * @param origKeyMac
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    protected boolean genKeySpecEV2(byte[] origKeyEnc, byte [] origKeyMac)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {

        ByteArray AESKey = new ByteArray();
        keyType = KEYTYPE_AES;
        AESKey.append(origKeyEnc);
        EV2_KeySpecSesAuthENC = new SecretKeySpec(AESKey.toArray(), "AES");

        AESKey.clear();
        AESKey.append(origKeyMac);


        EV2_KeySpecSesAuthMAC = new SecretKeySpec(AESKey.toArray(), "AES");
        return true;
    }


    // Mifare Desfire specifications require DESede/ECB without padding
    protected void initCipher() {

        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher = Cipher.getInstance("DESede/CBC/NoPadding");  // ECB or CBC
                    blockLength = 8;
                    break;
                case MODE_AUTHISO:
                    cipher = Cipher.getInstance("DESede/CBC/NoPadding");
                    blockLength = 8;
                    break;
                case MODE_AUTHAES:
                case MODE_AUTHEV2:
                    cipher = Cipher.getInstance("AES/CBC/NoPadding");
                    blockLength = 16;
                    break;
                default:
                    throw new InvalidAlgorithmParameterException("No such authMode");
            }
            currentIV = new byte[blockLength];
            Arrays.fill(currentIV, (byte) 0);
        } catch (GeneralSecurityException e) {
            return;  //throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,InvalidAlgorithmParameterException
        }


    }
    //endregion

    //region Encryption Related

    /********** Encryption Related **********/
    /**
     * encryptData D40 encrypt data doesn't use Init Vector
     * @param encInput
     * @return
     */
    public byte[] encryptData(byte[] encInput) {

        if (cipher == null) {
            initCipher();
        }

        byte[] encOutput;

        try {
            switch (authMode) {
                case MODE_AUTHD40:

                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(nullBytes8));  // IV is always 00..00
                    encOutput = cipher.doFinal(encInput);
                    System.arraycopy(encInput, encInput.length - blockLength, currentIV, 0, blockLength);
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
                case MODE_AUTHEV2:
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    encOutput = cipher.doFinal(encInput);
                    // Write the first IV as the result from PICC's encryption
                    System.arraycopy(encOutput, encOutput.length - blockLength, currentIV, 0, blockLength);
                    break;
                default:
                    encOutput = null;
                    break;
            }
        } catch (GeneralSecurityException e) {
            Log.d("encryptData", "General Security Exception Error: " + e);
            encOutput = null;
        }
        return encOutput;
    }

    public byte[] encryptMAC(byte[] encInput) {

        if (cipher == null) {
            initCipher();
        }

        byte[] encOutput;

        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(nullBytes8));  // IV is always 00..00
                    encOutput = cipher.doFinal(encInput);
                    System.arraycopy(encOutput, encOutput.length - blockLength, currentIV, 0, blockLength);
                    break;
                case MODE_AUTHISO:  // Not used
                case MODE_AUTHAES:  // Not used
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    encOutput = cipher.doFinal(encInput);
                    // Write the first IV as the result from PICC's encryption
                    System.arraycopy(encOutput, encOutput.length - blockLength, currentIV, 0, blockLength);
                    break;
                case MODE_AUTHEV2:
                    cipher.init(Cipher.ENCRYPT_MODE, EV2_KeySpecSesAuthMAC, new IvParameterSpec(nullBytes16));
                    encOutput = cipher.doFinal(encInput);

                    break;
                default:
                    encOutput = null;
                    break;
            }
        } catch (GeneralSecurityException e) {
            Log.d("encryptData", "General Security Exception Error: " + e);
            encOutput = null;
        }
        return encOutput;
    }

    /**
     * decryptData for D40 mode does not use init vector
     * @param decInput
     * @return
     */
    public byte[] decryptData(byte[] decInput) {

        if (cipher == null) {
            initCipher();
        }

        byte[] decOutput = null;
        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(nullBytes8));
                    decOutput = cipher.doFinal(decInput);
                    System.arraycopy(decInput, decInput.length - blockLength, currentIV, 0, blockLength);
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
                case MODE_AUTHEV2:
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    decOutput = cipher.doFinal(decInput);   // Decrypt
                    // Write the first IV as the result from PICC's encryption
                    System.arraycopy(decInput, decInput.length - blockLength, currentIV, 0, blockLength);
            }
        } catch (GeneralSecurityException e) {
            Log.d("decrypt", "General Security Exception Error: " + e);
            decOutput = null;
        }
        return decOutput;
    }

    /********** Encryption Related **********/
    /**
     * encryptData D40 encrypt (for commands like GetCardUID) uses init vector
     * @param encInput
     * @param ks
     * @return
     */
    public byte[] encrypt(byte[] encInput, SecretKey ks) {

        if (cipher == null) {
            initCipher();
        }

        byte[] encOutput;

        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    Log.d("encrypt", "Current IV = " + ByteArray.byteArrayToHexString(currentIV));
                    cipher.init(Cipher.DECRYPT_MODE, ks, new IvParameterSpec(currentIV));
                    encOutput = cipher.doFinal(encInput);
                    System.arraycopy(encInput, encInput.length - blockLength, currentIV, 0, blockLength);
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
                case MODE_AUTHEV2:
                    Log.d("encrypt", "Current IV = " + ByteArray.byteArrayToHexString(currentIV));
                    cipher.init(Cipher.ENCRYPT_MODE, ks, new IvParameterSpec(currentIV));
                    encOutput = cipher.doFinal(encInput);
                    // Write the first IV as the result from PICC's encryption
                    System.arraycopy(encOutput, encOutput.length - blockLength, currentIV, 0, blockLength);
                    break;
                default:
                    encOutput = null;
                    break;
            }
        } catch (GeneralSecurityException e) {
            Log.d("encrypt", "General Security Exception Error: " + e);
            encOutput = null;
        }
        return encOutput;
    }

    /********** Encryption Related **********/
    public byte[] encrypt(byte[] encInput) {

        return encrypt(encInput, keySpec);
    }

    public byte[] decrypt(byte[] decInput) {
        if (cipher == null) {
            initCipher();
        }
        byte[] decOutput = null;
        try {
            Log.d("decrypt", "Current IV = " + ByteArray.byteArrayToHexString(currentIV));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
            decOutput = cipher.doFinal(decInput);
            System.arraycopy(decInput, decInput.length - blockLength, currentIV, 0, blockLength);

        } catch (GeneralSecurityException e) {
            Log.d("decrypt", "General Security Exception Error: " + e);
            decOutput = null;
        }
        return decOutput;
    }


    public byte[] decryptD40Authenticate(byte[] decInput) {

        byte[] decOutput = null;
        try {
            Cipher ECBCipher = Cipher.getInstance("DESede/ECB/NoPadding");
            ECBCipher.init(Cipher.DECRYPT_MODE, keySpec);
            decOutput = ECBCipher.doFinal(decInput);

        } catch (GeneralSecurityException e) {
            Log.d("decryptforD40", "General Security Exception Error: " + e);
            decOutput = null;
        }
        return decOutput;
    }


    //endregion

    //region Authentication

    /********** Authentication Related **********/
    public byte[] computeResponseAndDataToVerify(byte[] encRndB)
            throws GeneralSecurityException {

        byte[] decRndA, rndBPrime, decRndBPrime;

        byte[] challengeMessage = null;


        if (authMode == MODE_AUTHD40) {


            // We decrypt the challenge, and rotate one byte to the left
            rndB = decryptD40Authenticate(encRndB);
            rndBPrime = ByteArray.rotateLT(rndB);

            // Then we generate a random number as our challenge for the coupler
            rndA = new byte[8];
            randomGenerator.nextBytes(rndA);


            decRndA = decryptD40Authenticate(rndA);
            // XOR of rndA, rndB  // This is CBC done manually
            decRndBPrime = ByteArray.xor(decRndA, rndBPrime);
            // The result is encrypted again
            decRndBPrime = decryptD40Authenticate(decRndBPrime);

            challengeMessage = new byte[decRndA.length + decRndBPrime.length];
            System.arraycopy(decRndA, 0, challengeMessage, 0, decRndA.length);
            System.arraycopy(decRndBPrime, 0, challengeMessage, decRndA.length, decRndBPrime.length);


        } else if (authMode == MODE_AUTHISO || authMode == MODE_AUTHAES || authMode == MODE_AUTHEV2 ) {


            // We decrypt the challenge, and rotate one byte to the left
            rndB = decrypt(encRndB);

            Log.d("computeRAndDataToVerify", "currentIV           = " + ByteArray.byteArrayToHexString(currentIV));

            rndBPrime = ByteArray.rotateLT(rndB);
            rndA = new byte[rndB.length];   // Length 8 byte for DES/3DES, 16 byte for 3k3des and AES
            randomGenerator.nextBytes(rndA);


            // TESTEV2
            //System.arraycopy(ByteArray.hexStringToByteArray("876D85B7FC717073AFBF564834F98F1E"), 0, rndA, 0, 16);

            Log.d("computeRAndDataToVerify", "rndB decrypted      = " + ByteArray.byteArrayToHexString(rndB));
            Log.d("computeRAndDataToVerify", "rndBPrime           = " + ByteArray.byteArrayToHexString(rndBPrime));
            Log.d("computeRAndDataToVerify", "rndA generated      = " + ByteArray.byteArrayToHexString(rndA));
            if (authMode == MODE_AUTHEV2)
                Arrays.fill(currentIV,(byte)0);

            byte[] encInput = new byte[rndA.length + rndBPrime.length];
            System.arraycopy(rndA, 0, encInput, 0, rndA.length);
            System.arraycopy(rndBPrime, 0, encInput, rndA.length, rndBPrime.length);


            challengeMessage = encrypt(encInput);

            Log.d("computeRAndDataToVerify", "challengeMessage   = " + ByteArray.byteArrayToHexString(challengeMessage));

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



        if (authMode == MODE_AUTHD40) {
            cardResponse = decryptD40Authenticate(cardResponse);
        } else {
            if (authMode == MODE_AUTHEV2)
                Arrays.fill(currentIV,(byte)0);
            cardResponse = decrypt(cardResponse);
        }

        Log.d("verifyCardResponse", "cardResponse decrypted             = " + ByteArray.byteArrayToHexString(cardResponse));

        if (authMode == MODE_AUTHEV2) {
            System.arraycopy(cardResponse, 0, EV2_TI, 0, 4);
            byte [] RndAPrime = new byte [16];
            System.arraycopy(cardResponse, 4, RndAPrime, 0, 16);
            cardResponse = ByteArray.rotateRT(RndAPrime);


        } else{
            cardResponse = ByteArray.rotateRT(cardResponse);
        }

        Log.d("verifyCardResponse", "                            EV2_TI = " + ByteArray.byteArrayToHexString(EV2_TI));
        Log.d("verifyCardResponse", "cardResponse decrypted and shifted = " + ByteArray.byteArrayToHexString(cardResponse));
        Log.d("verifyCardResponse", "                          origRndA = " + ByteArray.byteArrayToHexString(rndA));
        if (Arrays.equals(cardResponse, rndA)) {
            genSessionKey();
            // ComputeSession Key
            return true;
        }

        return false;
    }
    //endregion

    //region Session Key Generation

    /********** Session Key Generation related After successful authentication **********/
    private void genSessionKey() throws Exception {

        byte [] sessionKey;
        switch (keyType) {
            case KEYTYPE_DES:
                sessionKey = new byte[8];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                Log.d("genSessionKey", "DES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                genKeySpec(sessionKey);
                blockLength = 8;
                break;
            case KEYTYPE_3DES:
                sessionKey = new byte[16];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 4, sessionKey, 8, 4);
                System.arraycopy(rndB, 4, sessionKey, 12, 4);
                Log.d("genSessionKey", "3DES sessionKey   = " + ByteArray.byteArrayToHexString(sessionKey));
                genKeySpec(sessionKey);
                blockLength = 8;
                break;
            case KEYTYPE_3K3DES:
                sessionKey = new byte[24];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 6, sessionKey, 8, 4);
                System.arraycopy(rndB, 6, sessionKey, 12, 4);
                System.arraycopy(rndA, 12, sessionKey, 16, 4);
                System.arraycopy(rndB, 12, sessionKey, 20, 4);
                Log.d("genSessionKey", "3K3DES sessionKey  = " + ByteArray.byteArrayToHexString(sessionKey));
                genKeySpec(sessionKey);
                blockLength = 8;
                break;
            case KEYTYPE_AES:
                sessionKey = new byte[16];
                if (authMode == MODE_AUTHEV2) {

                    // SV1 = A5 5A 00 01 00 80 || RndA[15-14] || (RndA[13-8] XOR (RnB[15-10]) || RndB[9-0] || RndA[7-0]
                    byte [] SV1 = new byte[32];
                    System.arraycopy(ByteArray.hexStringToByteArray("A55A00010080"), 0, SV1, 0,6);
                    System.arraycopy(rndA, 0, SV1, 6, 2);
                    System.arraycopy(ByteArray.xor(rndA, 2, rndB, 0, 6), 0, SV1, 8, 6);//
                    System.arraycopy(rndB, 6, SV1, 14, 10);
                    System.arraycopy(rndA, 8, SV1, 24, 8);

                    // SV2 = 5A A5 00 00 01 00 80 || RndA[15-14] || (RndA[13-8] XOR (RnB[15-10]) || RndB[9-0] || RndA[7-0]
                    byte [] SV2 = new byte[32];
                    System.arraycopy(SV1, 0, SV2, 0, 32);
                    SV2[0] = (byte) 0x5A;
                    SV2[1] = (byte) 0xA5;


                    genSubKeys();  // using Kx of Authentication


                    Arrays.fill(currentIV, (byte) 0);
                    // KSesAuthEnc = PRF(Kx, SV1)
                    EV2_KSesAuthENC = calcCMAC_full(SV1);

                    Arrays.fill(currentIV, (byte) 0);
                    // KSesAuthMAC = PRF(Kx, SV2)
                    EV2_KSesAuthMAC = calcCMAC_full(SV2);

                    Log.d("genSessionKey", "EV2 KSesAuthEnc          = " + ByteArray.byteArrayToHexString(EV2_KSesAuthENC));
                    Log.d("genSessionKey", "EV2 KSesAuthMac          = " + ByteArray.byteArrayToHexString(EV2_KSesAuthMAC));


                    genKeySpecEV2(EV2_KSesAuthENC, EV2_KSesAuthMAC);

                    genSubKeysEV2();
                    EV2_Authenticated = true;


                } else {
                    System.arraycopy(rndA, 0, sessionKey, 0, 4);
                    System.arraycopy(rndB, 0, sessionKey, 4, 4);
                    System.arraycopy(rndA, 12, sessionKey, 8, 4);
                    System.arraycopy(rndB, 12, sessionKey, 12, 4);
                    Log.d("genSessionKey", "AES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                    genKeySpec(sessionKey);
                }
                blockLength = 16;
                break;
        }

        //encryptionIV = Arrays.copyOf(currentIV, currentIV.length);

        switch (authMode) {
            case MODE_AUTHD40:
                trackCMAC = false;
                CRCLength = 2;
                break;
            case MODE_AUTHISO:
            case MODE_AUTHAES:
                trackCMAC = true;
                CRCLength = 4;
                genSubKeys();
                break;
        }
        initCipher();

    }

    public byte getAuthMode() {
        return authMode;
    }

    private static byte[] shiftLeft(byte[] input, int len) {
        int word_size = (len / 8) + 1;
        int shift = len % 8;
        byte carry_mask = (byte) ((1 << shift) - 1);
        int offset = word_size - 1;
        int src_index;
        byte[] data = new byte[input.length];
        System.arraycopy(input, 0, data, 0, data.length);


        for (int i = 0; i < data.length; i++) {
            src_index = i + offset;
            if (src_index >= data.length) {
                data[i] = 0;
            } else {
                byte src = data[src_index];
                byte dst = (byte) (src << shift);
                if (src_index + 1 < data.length) {
                    dst |= data[src_index + 1] >>> (8 - shift) & carry_mask;
                }
                data[i] = dst;
            }
        }
        return data;
    }

    private byte[] shiftLeft1Bit(byte[] data) {
        return shiftLeft(data, 1);
    }

    public void genSubKeys() throws GeneralSecurityException {
        byte[] encInput = new byte[blockLength];


        // CMAC subkey generation according to NIST SP800-38B
        // http://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38b.pdf
        byte Rb;  // According to SP800-38B

        if (blockLength == 8) {
            Rb = (byte) 0x1B;
        } else {
            Rb = (byte) 0x87;
        }

        // 1. Let L = CIPHK(0)).
        Arrays.fill(encInput, (byte) 0x00);
        Arrays.fill(currentIV, (byte) 0x00);
        byte[] L = encrypt(encInput);

        Log.d("genSubKeys", "CIPHk(0^128) = " + ByteArray.byteArrayToHexString(L));

        // 2. If MSB1(L) = 0, then K1 = L << 1;
        // Else K1 = (L << 1) XOR Rb; see Sec. 5.3 for the definition of Rb.
        K1 = shiftLeft1Bit(L);
        if ((L[0] & (byte) 0x80) == (byte) 0x80) {
            K1[blockLength - 1] ^= Rb;
        }

        // 3. If MSB1(K1) = 0, then K2 = K1 << 1;
        // Else K2 = (K1 << 1) XOR Rb.
        K2 = shiftLeft1Bit(K1);
        if ((K1[0] & (byte) 0x80) == (byte) 0x80) {
            K2[blockLength - 1] ^= Rb;
        }
        Log.d("genSubKeys", "K1           = " + ByteArray.byteArrayToHexString(K1));
        Log.d("genSubKeys", "K2           = " + ByteArray.byteArrayToHexString(K2));
        // 4. Return K1, K2.

    }

    public void genSubKeysEV2() throws GeneralSecurityException {
        byte[] encInput = new byte[blockLength];
        Arrays.fill(encInput, (byte) 0);

        // CMAC subkey generation according to NIST SP800-38B
        // http://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38b.pdf
        byte Rb;  // According to SP800-38B

        Rb = (byte) 0x87;

        // 1. Let L = CIPHK(0)).
        Arrays.fill(currentIV, (byte) 0x00);
        byte[] L = encrypt(encInput,EV2_KeySpecSesAuthMAC);

        Log.d("genSubKeysEV2", "CIPHk(0^128) = " + ByteArray.byteArrayToHexString(L));

        // 2. If MSB1(L) = 0, then K1 = L << 1;
        // Else K1 = (L << 1) XOR Rb; see Sec. 5.3 for the definition of Rb.
        EV2_K1 = shiftLeft1Bit(L);
        if ((L[0] & (byte) 0x80) == (byte) 0x80) {
            EV2_K1[EV2_K1.length - 1] ^= Rb;
        }

        // 3. If MSB1(K1) = 0, then K2 = K1 << 1;
        // Else K2 = (K1 << 1) XOR Rb.
        EV2_K2 = shiftLeft1Bit(EV2_K1);
        if ((EV2_K1[0] & (byte) 0x80) == (byte) 0x80) {
            EV2_K2[EV2_K2.length - 1] ^= Rb;
        }
        Log.d("genSubKeysEV2", "EV2_K1           = " + ByteArray.byteArrayToHexString(EV2_K1));
        Log.d("genSubKeysEV2", "EV2_K2           = " + ByteArray.byteArrayToHexString(EV2_K2));
        // 4. Return K1, K2.

    }

    //endregion

    //region D40 MAC Related
    /**
     * D40 MAC only applies to Data field
     *
     * @param data
     * @return
     */
    public byte[] calcD40MAC(byte[] data) {
        byte[] output, encInput;
        ByteArray baEncInput = new ByteArray();
        byte[] outMAC = new byte[4];

        int extraLength = data.length % blockLength;
        if ((extraLength == 0) && (data.length != 0)) {
            encInput = new byte[data.length];
            System.arraycopy(data, 0, encInput, 0, data.length);
        } else {
            encInput = new byte[data.length + blockLength - extraLength];
            baEncInput.append(data).append(ByteArray.hexStringToByteArray("0000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, data.length + blockLength - extraLength);
        }

        output = encryptMAC(encInput);

        System.arraycopy(output, output.length - 8, outMAC, 0, 4);

        return outMAC;
    }

    /**
     * verifyD40DAC Verify Mac
     * @param recvData
     * @return
     */
    public boolean verifyD40MAC(byte[] recvData) {
        byte[] MACToVerify = new byte[4];
        byte[] computedMACToVerify;
        if (recvData.length < 5)  // No MAC
            return false;


        storedAFData.append(recvData, 1, recvData.length - 4 - 1);

        System.arraycopy(recvData, recvData.length - 4, MACToVerify, 0, 4);

        Log.d("verifyD40MAC", "Data to Verify = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        computedMACToVerify = calcD40MAC(storedAFData.toArray());
        Log.d("verifyD40MAC", "MAC to Verify = " + ByteArray.byteArrayToHexString(MACToVerify));
        Log.d("verifyD40MAC", "MAC computed  = " + ByteArray.byteArrayToHexString(computedMACToVerify));

        storedAFData.clear();

        return Arrays.equals(MACToVerify, computedMACToVerify);
    }
    //endregion

    //region D41 CMAC Related
    /********** CMAC RELATED **********/
    public byte[] calcCMAC(byte[] data) {
        byte[] outputCMAC;
        outputCMAC = calcCMAC_full(data);
        byte [] returnCMAC = new byte[8];
        if (authMode == MODE_AUTHEV2) {
            for (int i=0; i < 8; i ++) {
                returnCMAC[i] = outputCMAC[2*i+1];
            }
        } else {

            // Truncate CMAC by taking the most significant bits
            System.arraycopy(outputCMAC, 0, returnCMAC, 0, 8);
        }

        return returnCMAC;
    }

    /**
     * Calculate CMAC without truncation according to SP800-38A
     * @param data data to calc CMAC
     * @return Full CMAC
     */
    public byte[] calcCMAC_full(byte[] data) {
        byte[] output, encInput;

        ByteArray baEncInput = new ByteArray();

        Log.d("calcCMAC_full", "Starting Init Vector  = " + ByteArray.byteArrayToHexString(currentIV));
        int extraLength = data.length % blockLength;

        // CMAC if Extralength = 0, use K1 XOR
        if ((extraLength == 0) && (data.length != 0)) {
            encInput = new byte[data.length];
            System.arraycopy(data, 0, encInput, 0, data.length);

            int startIndex = encInput.length - blockLength;
            //Log.d("calcCMAC_full", "startIndex  = " + startIndex + " Using K1 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            //Mn = K1 XOR Mn*
            for (int i = 0; i < blockLength; i++) {
                encInput[startIndex + i] ^= K1[i];
            }

        } else { // use 80 padding and K2 XOR
            encInput = new byte[data.length + blockLength - extraLength];
            baEncInput.append(data).append(ByteArray.hexStringToByteArray("80000000000000000000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, data.length + blockLength - extraLength);

            int startIndex = encInput.length - blockLength;
            //Mn = K2 XOR (Mn* with padding)
            //Log.d("calcCMAC_full", "startIndex  = " + startIndex + " Using K2 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            for (int i = 0; i < blockLength; i++) {
                encInput[startIndex + i] ^= K2[i];
            }
        }
        Log.d("calcCMAC_full", "extraLength  = " + extraLength + " encInput.length = " + encInput.length);
        Log.d("calcCMAC_full", "Encrypt Input after Padding = " + ByteArray.byteArrayToHexString(encInput));
        output = encryptData(encInput);
        Log.d("calcCMAC_full", "Encrypted Data              = " + ByteArray.byteArrayToHexString(output));

        byte[] lastBlockOutput = new byte [blockLength];
        System.arraycopy(output, output.length - blockLength, lastBlockOutput, 0, blockLength);
        Log.d("calcCMAC_full", "Output full CMAC before truc= " + ByteArray.byteArrayToHexString(lastBlockOutput));
        return lastBlockOutput;
    }

    public boolean verifyCMAC(byte[] recvData) {
        byte[] CMACToVerify = new byte[8];
        byte[] computedCMACToVerify = new byte[8];
        if (recvData.length < 9)  // No CMAC
            return false;


        storedAFData.append(recvData, 1, recvData.length - 8 - 1);
        storedAFData.append(recvData[0]);

        System.arraycopy(recvData, recvData.length - 8, CMACToVerify, 0, 8);

        Log.d("verifyCMAC", "Data to Verify = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        computedCMACToVerify = calcCMAC(storedAFData.toArray());
        Log.d("verifyCMAC", "CMAC to Verify = " + ByteArray.byteArrayToHexString(CMACToVerify));
        Log.d("verifyCMAC", "CMAC computed  = " + ByteArray.byteArrayToHexString(computedCMACToVerify));

        storedAFData.clear();

        return Arrays.equals(CMACToVerify, computedCMACToVerify);
    }

    ByteArray storedAFData;

    public void storeAFCMAC(byte[] recvData) {
        storedAFData.append(recvData, 1, recvData.length - 1);
        return;
    }
    //endregion

    //region EV2 CMAC related
    /**
     * Calculate CMAC without truncation according to SP800-38A
     * @param cmd data to calc CMAC
     * @return Full CMAC
     */
    public byte[] EV2_CalcCMAC(byte[] cmd) {
        byte[] outputCMAC, encInput;

        // cmd || CmdHeader || CmdData || MAC
        //MAC(KSesAuthMAC , Cmd || CmdCtr || TI [|| CmdHeader] [|| CmdData]
        int extraLength = (cmd.length + 6 )% blockLength;

        if (extraLength == 0) {
            encInput = new byte[cmd.length + 6];
        } else {
            encInput = new byte[cmd.length +6 + blockLength - extraLength];
        }

        EV2_CmdCtr ++;

        System.arraycopy(cmd, 0 , encInput, 0, 1);
        System.arraycopy(ByteArray.fromInt(EV2_CmdCtr,2), 0 , encInput, 1, 2);
        System.arraycopy(EV2_TI, 0 , encInput, 3, 4);
        System.arraycopy(cmd, 1 , encInput, 7, cmd.length -1);

        Log.d("EV2CalcCMAC", "Encrypt Input before Padding = " + ByteArray.byteArrayToHexString(encInput));

        // CMAC if Extralength = 0, use K1 XOR
        if ((extraLength == 0) && (cmd.length != 0)) {

            int startIndex = encInput.length - blockLength;
            //Log.d("calcCMAC_full", "startIndex  = " + startIndex + " Using K1 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            //Mn = K1 XOR Mn*
            for (int i = 0; i < blockLength; i++) {
                encInput[startIndex + i] ^= EV2_K1[i];
            }

        } else { // use 80 padding and K2 XOR
            ByteArray baEncInput = new ByteArray();
            baEncInput.append(encInput).append(ByteArray.hexStringToByteArray("80000000000000000000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, encInput.length);

            int startIndex = encInput.length - blockLength;
            //Mn = K2 XOR (Mn* with padding)
            //Log.d("calcCMAC_full", "startIndex  = " + startIndex + " Using K2 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            for (int i = 0; i < blockLength; i++) {
                encInput[startIndex + i] ^= EV2_K2[i];
            }
        }


        Log.d("EV2CalcCMAC", "Encrypt Input after Padding  = " + ByteArray.byteArrayToHexString(encInput));
        outputCMAC = encryptMAC(encInput);
        Log.d("EV2CalcCMAC", "Encrypted Data               = " + ByteArray.byteArrayToHexString(outputCMAC));

        byte [] returnCMAC = new byte[8];
        int lastBlock = outputCMAC.length - blockLength;
        for (int i=0; i < 8; i ++) {
            returnCMAC[i] = outputCMAC[lastBlock+2*i];
            returnCMAC[i] = outputCMAC[lastBlock+2*i];
        }

        ByteArray baOutput = new ByteArray();
        baOutput.append(cmd).append(returnCMAC);

        return baOutput.toArray();
    }

    //endregion

    //region CRC Related

    //region CRC32 Related
    /********** CRC32 RELATED **********/
    public static byte[] longToBytesInvertCRC(long l) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) ~(l & 0xFF);
            l >>= 8;
        }
        return result;
    }
    //endregion

    //region CRC16 Related
    private byte[] iso14443a_crc(byte[] Data)   // DESFireSAM crc16 do not invert the result
    {
        int bt;
        int wCrc = 0x6363;
        int j = 0;
        int t8 = 0;
        int t9 = 0;
        int tA = 0;
        int Len = Data.length;
        final int maskB = 0x0000000000000000FF;
        final int maskW = 0x00000000000000FFFF;


        do {
            bt = Data[j++] & maskB;
            bt = (bt ^ (wCrc & 0x00FF)) & maskB;
            bt = (bt ^ (bt << 4)) & maskB;


            t8 = (bt << 8) & maskW;
            t9 = (bt << 3) & maskW;
            tA = (bt >> 4) & maskW;
            wCrc = (wCrc >> 8) ^ (t8 ^ t9 ^ tA) & maskW;
        }
        while (j < Len);


        byte[] bb = new byte[2];
        bb[0] = (byte) (wCrc & maskB);
        bb[1] = (byte) ((wCrc >> 8) & maskB);
        return bb;
    }
//endregion

    public byte[] calcCRC(byte[] data) {
        long lcrc = 0;
        byte[] returnCRC;
        Log.d("calcCRC", "Data CRC Input      = " + ByteArray.byteArrayToHexString(data));
        if (CRCLength == 4) {
            CRC32 crc = new CRC32();
            crc.update(data);
            lcrc = crc.getValue();
            returnCRC = longToBytesInvertCRC(lcrc);

        } else {
            returnCRC = iso14443a_crc(data);
        }
        Log.d("calcCRC", "Calculated CRC      = " + ByteArray.byteArrayToHexString(returnCRC));
        return returnCRC;
    }

    public boolean verifyCRC(byte[] crcToVerify, byte[] data) {

        return Arrays.equals(crcToVerify, calcCRC(data));
    }
    //endregion

    public void storeAFEncryptedSetLength(byte[] recvData, int len) {
        encryptedLength = len;
        storedAFData.append(recvData, 1, recvData.length - 1);
    }

    public void storeAFEncrypted(byte[] recvData) {
        storedAFData.append(recvData, 1, recvData.length - 1);
    }


    public byte[] decryptReadData() throws IOException, GeneralSecurityException {
        byte[] decryptedData;


        if (storedAFData.length() < 8) {
            throw new GeneralSecurityException("Length error: Data returned too short. Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        }
        Log.d("decryptReadData", "Encrypted Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        decryptedData = decryptData(storedAFData.toArray());

        if (decryptedData == null)
            throw new GeneralSecurityException("Decryption error: Encryption Input = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        Log.d("decryptReadData", "Decrypted Data = " + ByteArray.byteArrayToHexString(decryptedData));

        // Remove padding, separate CRC
        ByteArray baDecryptedPlainData = new ByteArray();
        ByteArray baCRC = new ByteArray();
        if (encryptedLength != 0) {  // if count is specified 00 .. 00 padding is used
            baDecryptedPlainData.append(decryptedData, 0, encryptedLength);
            baCRC.append(decryptedData, encryptedLength, CRCLength);

        } else {  // Count == 0 (wildcard) , remove 80..00 padding
            int padCount = ByteArray.ISO9797m2PadCount(decryptedData);
            if (padCount == -1) {
                throw new GeneralSecurityException("Decryption padding error: Decrypted data = " + ByteArray.byteArrayToHexString(decryptedData));
            }

            baDecryptedPlainData.append(decryptedData, 0, decryptedData.length - CRCLength - padCount);
            baCRC.append(decryptedData, decryptedData.length - CRCLength - padCount, CRCLength);
        }

        // CRC verification
        Log.d("decryptReadData", "CRC  Data      = " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        byte[] returnData = baDecryptedPlainData.toArray();
        if (CRCLength == 4) {
            baDecryptedPlainData.append((byte) 0x00);  // status must be 0x00
        }

        Log.d("decryptReadData", "CRC Input    : " + ByteArray.byteArrayToHexString(baDecryptedPlainData.toArray()));
        byte[] computedCRC = calcCRC(baDecryptedPlainData.toArray());
        Log.d("decryptReadData", "Computed  CRC: " + ByteArray.byteArrayToHexString(computedCRC));
        Log.d("decryptReadData", "CRC to verify: " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        if (!Arrays.equals(baCRC.toArray(), computedCRC)) {
            Log.d("decryptReadData", "CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
            encryptedLength = 0;
            storedAFData.clear();
            throw new GeneralSecurityException("CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
            //return null;
        }
        // Reset
        encryptedLength = 0;
        storedAFData.clear();

        return returnData;
    }

/*    public byte[] decryptWithIV() throws IOException, GeneralSecurityException {
        byte[] decryptedData;


        if (storedAFData.length() < 8) {
            throw new GeneralSecurityException("Length error: Data returned too short. Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        }
        Log.d("decryptWithIV", "Encrypted Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        decryptedData = decrypt(storedAFData.toArray());

        if (decryptedData == null) {
            throw new GeneralSecurityException("Decryption error: Encryption Input = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        }

        Log.d("decryptWithIV", "Decrypted Data = " + ByteArray.byteArrayToHexString(decryptedData));

        // Remove Padding
        ByteArray baDecryptedPlainData = new ByteArray();
        ByteArray baCRC = new ByteArray();
        if (encryptedLength != 0) {  // if count is specified 00 .. 00 padding is used
            baDecryptedPlainData.append(decryptedData, 0, encryptedLength);
            baCRC.append(decryptedData, encryptedLength, CRCLength);

        } else {  // Count == 0 (wildcard) , remove 80..00 padding
            int padCount = ByteArray.ISO9797m2PadCount(decryptedData);
            if (padCount == -1) {
                throw new GeneralSecurityException("Decryption padding error: Decrypted data = " + ByteArray.byteArrayToHexString(decryptedData));
            }

            baDecryptedPlainData.append(decryptedData, 0, decryptedData.length - CRCLength - padCount);
            baCRC.append(decryptedData, decryptedData.length - CRCLength - padCount, CRCLength);
        }

        Log.d("decryptWithIV", "CRC  Data      = " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        byte[] returnData = baDecryptedPlainData.toArray();
        if (CRCLength == 4) {
            baDecryptedPlainData.append((byte) 0x00);  // status must be 0x00
        }

        Log.d("decryptWithIV", "CRC Input    : " + ByteArray.byteArrayToHexString(baDecryptedPlainData.toArray()));
        byte[] computedCRC = calcCRC(baDecryptedPlainData.toArray());
        Log.d("decryptWithIV", "Computed  CRC: " + ByteArray.byteArrayToHexString(computedCRC));
        Log.d("decryptWithIV", "CRC to verify: " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        if (!Arrays.equals(baCRC.toArray(), computedCRC)) {
            Log.d("decryptWithIV", "CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
            throw new GeneralSecurityException("CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
            //return null;
        }
        // Reset
        encryptedLength = 0;
        storedAFData.clear();

        return returnData;
    }*/

    public byte[] encryptWriteDataBlock(byte[] bCmdHeader, byte[] bDataToEncrypt) throws IOException, GeneralSecurityException {


        // CALC CRC
        ByteArray baDataToCRC = new ByteArray();
        byte[] computedCRC;

        if (CRCLength == 4) {
            baDataToCRC.append(bCmdHeader).append(bDataToEncrypt).toArray();
            Log.d("encryptWriteDataBlock", "CRC32 Input = " + ByteArray.byteArrayToHexString(baDataToCRC.toArray()));
            computedCRC = calcCRC(baDataToCRC.toArray());
        } else {
            Log.d("encryptWriteDataBlock", "CRC16 Input = " + ByteArray.byteArrayToHexString(bDataToEncrypt));
            computedCRC = calcCRC(bDataToEncrypt);
        }

        ByteArray baDataToEncrypt = new ByteArray();

        return encryptDataBlock (baDataToEncrypt.append(bDataToEncrypt).append(computedCRC).toArray());
    }

    // encrypt Data Block - encrypts bDataToEncrypt + padding only.  CRC not included
    public byte[] encryptDataBlock(byte[] bDataToEncrypt) throws IOException, GeneralSecurityException {
        // DO PADDING
        int iPaddingLen = (blockLength - (bDataToEncrypt.length % blockLength)) % blockLength;

        //int iDataToEncryptLen = bDataToEncrypt.length + CRCLength + iPaddingLen;
        byte[] bPadding = new byte[iPaddingLen];
        Arrays.fill(bPadding, (byte) 0);

        // Build block for encryption
        ByteArray baDataToEncrypt = new ByteArray();
        baDataToEncrypt.append(bDataToEncrypt).append(bPadding);

        Log.d("encryptDataBlock", "Input Data     = " + ByteArray.byteArrayToHexString(baDataToEncrypt.toArray()));

        byte[] bEncryptedData = encryptData(baDataToEncrypt.toArray());

        if (bEncryptedData == null)
            throw new GeneralSecurityException("Encryption error");

        Log.d("encryptDataBlock", "Encrypted Data = " + ByteArray.byteArrayToHexString(bEncryptedData));

        return bEncryptedData;

    }

    // encrypt Data Block - encrypts bDataToEncrypt + padding only.  CRC not included
    public byte[] encryptDataWithIVBlock(byte[] bDataToEncrypt) throws IOException, GeneralSecurityException {
        // DO PADDING
        int iPaddingLen = (blockLength - (bDataToEncrypt.length % blockLength)) % blockLength;

        //int iDataToEncryptLen = bDataToEncrypt.length + CRCLength + iPaddingLen;
        byte[] bPadding = new byte[iPaddingLen];
        Arrays.fill(bPadding, (byte) 0);

        // Build block for encryption
        ByteArray baDataToEncrypt = new ByteArray();
        baDataToEncrypt.append(bDataToEncrypt).append(bPadding);

        Log.d("encryptDataWithIVBlock", "Input Data     = " + ByteArray.byteArrayToHexString(baDataToEncrypt.toArray()));
        byte[] bEncryptedData = encrypt(baDataToEncrypt.toArray());

        if (bEncryptedData == null)
            throw new GeneralSecurityException("Encryption error");

        Log.d("encryptDataWithIVBlock", "Encrypted Data = " + ByteArray.byteArrayToHexString(bEncryptedData));

        return bEncryptedData;

    }

    public byte[] encryptDataWithCRC(byte[] bCmdHeader, byte[] bDataToEncrypt) throws IOException, GeneralSecurityException {


        // CALC CRC
        ByteArray baDataToCRC = new ByteArray();
        byte[] computedCRC;

        if (CRCLength == 4) {
            baDataToCRC.append(bCmdHeader).append(bDataToEncrypt).toArray();
            Log.d("encryptDataWithIV", "CRC32 Input = " + ByteArray.byteArrayToHexString(baDataToCRC.toArray()));
            computedCRC = calcCRC(baDataToCRC.toArray());
        } else {  // CRC16 does not CRC the header
            Log.d("encryptDataWithIV", "CRC16 Input = " + ByteArray.byteArrayToHexString(bDataToEncrypt));
            computedCRC = calcCRC(bDataToEncrypt);
        }

        ByteArray baDataToEncrypt = new ByteArray();

        return encryptDataWithIVBlock (baDataToEncrypt.append(bDataToEncrypt).append(computedCRC).toArray());
    }


}
