package com.example.ac.desfirelearningtool;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    public final byte KEYTYPE_DES = 0;
    public final byte KEYTYPE_3DES = 1;
    public final byte KEYTYPE_3K3DES = 2;
    public final byte KEYTYPE_AES = 3;



    protected SecureRandom randomGenerator;
    private byte authMode;
    private byte keyType;
    private Cipher cipher;
    private SecretKey keySpec;
    private byte[] rndA, rndB ;
    private byte [] currentIV;
    private byte [] encryptionIV;
    private int blockLength;
    private byte[] sessionKey;
    private byte[] K1, K2;  // Subkey for CMAC
    public boolean trackCMAC;
    public int CRCLength;    // Length of CRC 2 or 4 bytes
    public int encryptedLength;  // specified dataLength at the first AF for Read Data
    public int currentAuthenticatedKey;


    public DesfireCrypto (){
        reset ();
        this.randomGenerator = new SecureRandom();

    }
    public void reset () {
        rndA = null;
        rndB = null;
        cipher = null;
        authMode = (byte) 0x00;
        trackCMAC = false;
        CRCLength = 0;
        encryptedLength = 0;
        storedAFData = new ByteArray();
        currentAuthenticatedKey = -1;
    }

    //region Key Related
    //---------------------------------------------------------------------------------------
    public boolean initialize (byte authToSet, byte [] key) throws Exception {
        boolean res;

        reset();
        authMode = authToSet;
        if (authMode == MODE_AUTHD40 || authMode == MODE_AUTHISO) {
            getKeySpec(key);
        }else if (authMode == MODE_AUTHAES) {
             getKeySpecAES(key);
        } else {
            Log.e("initialize", "authMode not valid");
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
                    cipher = Cipher.getInstance("AES/CBC/NoPadding");
                    blockLength = 16;
                    break;
                default:
                    throw new InvalidAlgorithmParameterException("No such authMode");
            }
            currentIV = new byte[blockLength];
            Arrays.fill(currentIV, (byte) 0);
        } catch (GeneralSecurityException e){
            return;  //throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,InvalidAlgorithmParameterException
        }



    }
    //endregion

    //region Encryption Related
    /********** Encryption Related **********/
    public byte [] encrypt(byte [] encInput) {

        if (cipher == null) {
            initCipher();
        }

        byte [] encOutput;

        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));  // IV is always 00..00
                    encOutput = cipher.doFinal(encInput);
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
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
            Log.d("encrypt", "General Security Exception Error: " + e);
            encOutput = null;
        }
        return encOutput;
    }

    public byte [] decrypt(byte [] decInput)  {
        if (cipher == null) {
            initCipher();
        }
        byte [] decOutput = null;
        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    decOutput = cipher.doFinal(decInput);
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    decOutput = cipher.doFinal(decInput);   // Decrypt
                    // Write the first IV as the result from PICC's encryption
                    System.arraycopy(decInput, decInput.length-blockLength, currentIV, 0, blockLength);
            }
        } catch (GeneralSecurityException e) {
            Log.d("decrypt", "General Security Exception Error: " + e);
            decOutput = null;
        }
        return decOutput;
    }


    public byte [] decryptD40Authenticate(byte [] decInput)  {

        byte [] decOutput = null;
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

    //region Authentication Related
    /********** Authentication Related **********/
    public byte[] computeResponseAndDataToVerify(byte[] encRndB)
            throws GeneralSecurityException {

        byte[] decRndA, rndBPrime, decRndBPrime;

        byte[] challengeMessage = null;


        if (authMode == MODE_AUTHD40){


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


        } else if (authMode == MODE_AUTHISO || authMode == MODE_AUTHAES) {


            // We decrypt the challenge, and rotate one byte to the left
            rndB = decrypt(encRndB);

            Log.d("computeRAndDataToVerify", "currentIV           = " + ByteArray.byteArrayToHexString(currentIV));

            rndBPrime = ByteArray.rotateLT(rndB);
            Log.d("computeRAndDataToVerify", "rndB                = " + ByteArray.byteArrayToHexString(rndB));
            Log.d("computeRAndDataToVerify", "rndBPrime           = " + ByteArray.byteArrayToHexString(rndBPrime));
            rndA = new byte[rndB.length];   // Length 8 byte for DES/3DES, 16 byte for 3k3des and AES
            randomGenerator.nextBytes(rndA);
            Log.d("computeRAndDataToVerify", "rndA                = " + ByteArray.byteArrayToHexString(rndA));
            Log.d("computeRAndDataToVerify", "rndB                = " + ByteArray.byteArrayToHexString(rndB));
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

        // Log.d("verifyCardResponse", "cardResponse                       = " + ByteArray.byteArrayToHexString(cardResponse));
      // We decrypt the response and shift the rightmost byte "all around" (to the left)


        if (authMode == MODE_AUTHD40) {
            cardResponse = decryptD40Authenticate(cardResponse);
        } else {
            cardResponse = decrypt(cardResponse);
        }
        // Log.d("verifyCardResponse", "cardResponse decrypted             = " + ByteArray.byteArrayToHexString(cardResponse));
        cardResponse = ByteArray.rotateRT(cardResponse);
        // Log.d("verifyCardResponse", "cardResponse decrypted and shifted = " + ByteArray.byteArrayToHexString(cardResponse));
        // Log.d("verifyCardResponse", "                          origRndA = " + ByteArray.byteArrayToHexString(rndA));
        if (Arrays.equals(cardResponse, rndA)) {
            genSessionKey ();
            // ComputeSession Key
            return true;
        }

        return false;
    }
    //endregion

    //region Session Key Generation Related
    /********** Session Key Generation related After successful authentication **********/
    private void genSessionKey()  throws Exception{
        switch (keyType) {
            case KEYTYPE_DES:
                sessionKey = new byte [8];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                Log.d("genSessionKey", "DES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpec(sessionKey);
                blockLength = 8;
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
                break;
            case KEYTYPE_AES:
                // TEST
                sessionKey = new byte [] { (byte)0x2b,(byte)0x7e,(byte)0x15,(byte)0x16,(byte)0x28,(byte)0xae,(byte)0xd2,(byte)0xa6,(byte)0xab,(byte)0xf7,(byte)0x15,(byte)0x88,(byte)0x09,(byte)0xcf,(byte)0x4f,(byte)0x3c};
                Log.d("genSessionKey", "AES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpecAES(sessionKey);
                blockLength = 16;
                genSubKeys();
                Arrays.fill(currentIV, (byte)0x00);
                Log.d("800-38B", "CalcMac Ex1  = " + ByteArray.byteArrayToHexString(calcCMAC(new byte [] {})));
                Arrays.fill(currentIV, (byte)0x00);
                Log.d("800-38B", "CalcMac Ex2  = " + ByteArray.byteArrayToHexString(calcCMAC(new byte [] {(byte)0x6b,(byte)0xc1,(byte)0xbe,(byte)0xe2,(byte)0x2e,(byte)0x40,(byte)0x9f,(byte)0x96,(byte)0xe9,(byte)0x3d,(byte)0x7e,(byte)0x11,(byte)0x73,(byte)0x93,(byte)0x17,(byte)0x2a})));
                Arrays.fill(currentIV, (byte)0x00);
                Log.d("800-38B", "CalcMac Ex3  = " + ByteArray.byteArrayToHexString(calcCMAC(new byte [] {(byte)0x6b,(byte)0xc1,(byte)0xbe,(byte)0xe2,(byte)0x2e,(byte)0x40,(byte)0x9f,(byte)0x96,(byte)0xe9,(byte)0x3d,(byte)0x7e,(byte)0x11,(byte)0x73,(byte)0x93,(byte)0x17,(byte)0x2a,(byte)0xae,(byte)0x2d,(byte)0x8a,(byte)0x57,(byte)0x1e,(byte)0x03,(byte)0xac,(byte)0x9c,(byte)0x9e,(byte)0xb7,(byte)0x6f,(byte)0xac,(byte)0x45,(byte)0xaf,(byte)0x8e,(byte)0x51,(byte)0x30,(byte)0xc8,(byte)0x1c,(byte)0x46,(byte)0xa3,(byte)0x5c,(byte)0xe4,(byte)0x11  })));
                Arrays.fill(currentIV, (byte)0x00);
                Log.d("800-38B", "CalcMac Ex4  = " + ByteArray.byteArrayToHexString(calcCMAC(new byte [] {(byte)0x6b,(byte)0xc1,(byte)0xbe,(byte)0xe2,(byte)0x2e,(byte)0x40,(byte)0x9f,(byte)0x96,(byte)0xe9,(byte)0x3d,(byte)0x7e,(byte)0x11,(byte)0x73,(byte)0x93,(byte)0x17,(byte)0x2a,(byte)0xae,(byte)0x2d,(byte)0x8a,(byte)0x57,(byte)0x1e,(byte)0x03,(byte)0xac,(byte)0x9c,(byte)0x9e,(byte)0xb7,(byte)0x6f,(byte)0xac,(byte)0x45,(byte)0xaf,(byte)0x8e,(byte)0x51,(byte)0x30,(byte)0xc8,(byte)0x1c,(byte)0x46,(byte)0xa3,(byte)0x5c,(byte)0xe4,(byte)0x11,(byte)0xe5,(byte)0xfb,(byte)0xc1,(byte)0x19,(byte)0x1a,(byte)0x0a,(byte)0x52,(byte)0xef,(byte)0xf6,(byte)0x9f,(byte)0x24,(byte)0x45,(byte)0xdf,(byte)0x4f,(byte)0x9b,(byte)0x17,(byte)0xad,(byte)0x2b,(byte)0x41,(byte)0x7b,(byte)0xe6,(byte)0x6c,(byte)0x37,(byte)0x10  })));


                // Actual
                sessionKey = new byte [16];
                System.arraycopy(rndA, 0, sessionKey, 0, 4);
                System.arraycopy(rndB, 0, sessionKey, 4, 4);
                System.arraycopy(rndA, 12, sessionKey, 8, 4);
                System.arraycopy(rndB, 12, sessionKey, 12, 4);
                Log.d("genSessionKey", "AES sessionKey    = " + ByteArray.byteArrayToHexString(sessionKey));
                getKeySpecAES(sessionKey);
                blockLength = 16;
                break;
        }

        //encryptionIV = Arrays.copyOf(currentIV, currentIV.length);

        currentIV = new byte[blockLength];
        Arrays.fill(currentIV, (byte) 0);

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
    
    public byte getAuthMode () {
        return authMode;
    }

    private static byte[] shiftLeft(byte[] input, int len) {
        int word_size = (len / 8) + 1;
        int shift = len % 8;
        byte carry_mask = (byte) ((1 << shift) - 1);
        int offset = word_size - 1;
        byte [] data = new byte[input.length];
        System.arraycopy(input, 0, data, 0, data.length);


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
        return data;
    }

    private byte [] shiftLeft1Bit (byte [] data) {
        return shiftLeft(data, 1);
    }

    private byte [] shiftLeft1Bit (byte [] data, byte Rb) {
        byte [] output;
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
            Rb = (byte) 0x1B;
        } else {
            Rb = (byte) 0x87;
        }

        // 1. Let L = CIPHK(0)).
        Arrays.fill(currentIV, (byte)0x00);
        byte [] L = encrypt(encInput);

        Log.d("800-38B", "CIPHk(0^128) = " + ByteArray.byteArrayToHexString(L));

        // 2. If MSB1(L) = 0, then K1 = L << 1;
        // Else K1 = (L << 1) XOR Rb; see Sec. 5.3 for the definition of Rb.
        if ((L[0] & (byte)0x80) == (byte)0x00) {
            K1 = shiftLeft1Bit (L);
        } else {
            K1 = shiftLeft1Bit (L, Rb);
        }

        // 3. If MSB1(K1) = 0, then K2 = K1 << 1;
        // Else K2 = (K1 << 1) XOR Rb.
        if ((K1[0] & (byte)0x80) == (byte)0x00) {
            K2 = shiftLeft1Bit (K1);
        } else {
            K2 = shiftLeft1Bit (K1, Rb);
        }
        Log.d("800-38B", "K1           = " + ByteArray.byteArrayToHexString(K1));
        Log.d("800-38B", "K2           = " + ByteArray.byteArrayToHexString(K2));
        // 4. Return K1, K2.

    }
    //endregion

    //region D40 MAC Related
    /********** MAC RELATED **********/
    /**
     * D40 MAC only applies to Data field
     * @param data
     * @return
     */
    public byte [] calcD40MAC (byte [] data)  {
        byte[] output, encInput;
        ByteArray baEncInput = new ByteArray();
        byte [] outMAC = new byte[4];

        int extraLength = data.length % blockLength;
        if ((extraLength == 0) && (data.length != 0)) {
            encInput = new byte[data.length];
            System.arraycopy(data, 0, encInput, 0, data.length);

        } else {
            encInput = new byte[data.length + blockLength - extraLength];
            baEncInput.append(data).append(ByteArray.hexStringToByteArray("0000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, data.length + blockLength - extraLength);
        }
        //Log.d("calcCMAC", "extraLength  = " + extraLength + " encInput.length = " + encInput.length );
        //Log.d("calcCMAC", "encInput = " + ByteArray.byteArrayToHexString(encInput));

        //Log.d ("calcD40MAC  ", "Encryt Input Data = " + ByteArray.byteArrayToHexString(encInput));
        output = encrypt(encInput);
        //Log.d ("calcD40MAC  ", "Encrytped Data    = " + ByteArray.byteArrayToHexString(output));

        System.arraycopy(output,output.length-8,outMAC, 0,4);

        //Log.d ("calcD40MAC  ", "MAC computed      = " + ByteArray.byteArrayToHexString(outMAC) );

        return outMAC;
    }


    public boolean verifyD40MAC (byte [] recvData)  {
        byte [] MACToVerify = new byte[4];
        byte [] computedMACToVerify = new byte[4];
        if (recvData.length < 9)  // No CMAC
            return false;


        storedAFData.append(recvData,1,recvData.length-4-1);

        System.arraycopy(recvData, recvData.length-4, MACToVerify, 0,4 );

        //Log.d ("verifyCMAC", "Data to Verify = " + ByteArray.byteArrayToHexString(storedAFData.toArray()) );
        computedMACToVerify = calcD40MAC(storedAFData.toArray());
        Log.d ("verifyCMAC", "MAC to Verify = " + ByteArray.byteArrayToHexString(MACToVerify) );
        Log.d ("verifyCMAC", "MAC computed  = " + ByteArray.byteArrayToHexString(computedMACToVerify) );

        storedAFData.clear();

        return Arrays.equals(MACToVerify, computedMACToVerify);
    }
    //endregion

    //region D41 CMAC Related
    /********** CMAC RELATED **********/
    public byte [] calcCMAC (byte [] data)  {
        byte[] output, encInput;
        ByteArray baEncInput = new ByteArray();
        byte [] outMAC = new byte[8];

        Log.d("calcCMAC", "Starting Init Vector  = " + ByteArray.byteArrayToHexString(currentIV));
        int extraLength = data.length % blockLength;
        if ((extraLength == 0) && (data.length != 0)) {
            encInput = new byte[data.length];
            System.arraycopy(data, 0, encInput, 0, data.length);

            int startIndex = encInput.length - blockLength;
            //Log.d("calcCMAC", "startIndex  = " + startIndex + " Using K1 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            //Mn = K1 XOR Mn*
            for (int i = 0; i < blockLength; i++){
                encInput[startIndex + i] ^= K1[i];
            }

        } else {
            encInput = new byte[data.length + blockLength - extraLength];
            baEncInput.append(data).append(ByteArray.hexStringToByteArray("80000000000000000000000000000000"));
            System.arraycopy(baEncInput.toArray(), 0, encInput, 0, data.length + blockLength - extraLength);

            int startIndex = encInput.length - blockLength;
            //Mn = K2 XOR (Mn* with padding)
            //Log.d("calcCMAC", "startIndex  = " + startIndex + " Using K2 To Calc = " + ByteArray.byteArrayToHexString(encInput));
            for (int i = 0; i < blockLength; i++){
                encInput[startIndex + i] ^= K2[i];
            }
        }
        //Log.d("calcCMAC", "extraLength  = " + extraLength + " encInput.length = " + encInput.length );
        Log.d("calcCMAC", "encInput = " + ByteArray.byteArrayToHexString(encInput));

        Log.d ("calcCMAC  ", "Encrypt Input Data = " + ByteArray.byteArrayToHexString(encInput));
        output = encrypt(encInput);
        Log.d ("calcCMAC  ", "Encrypted Data    = " + ByteArray.byteArrayToHexString(output));

        System.arraycopy(output,output.length-blockLength,outMAC, 0,8);

        Log.d ("calcCMAC  ", "CMAC computed     = " + ByteArray.byteArrayToHexString(outMAC) );





        return outMAC;
    }

    public boolean verifyCMAC (byte [] recvData)  {
        byte [] CMACToVerify = new byte[8];
        byte [] computedCMACToVerify = new byte[8];
        if (recvData.length < 9)  // No CMAC
            return false;


        storedAFData.append(recvData,1,recvData.length-8-1);
        storedAFData.append(recvData[0]);

        System.arraycopy(recvData, recvData.length-8, CMACToVerify, 0,8 );

        Log.d ("verifyCMAC", "Data to Verify = " + ByteArray.byteArrayToHexString(storedAFData.toArray()) );
        computedCMACToVerify = calcCMAC(storedAFData.toArray());
        Log.d ("verifyCMAC", "CMAC to Verify = " + ByteArray.byteArrayToHexString(CMACToVerify) );
        Log.d ("verifyCMAC", "CMAC computed  = " + ByteArray.byteArrayToHexString(computedCMACToVerify) );

        storedAFData.clear();

        return Arrays.equals(CMACToVerify, computedCMACToVerify);
    }
    ByteArray storedAFData;

    public void  storeAFCMAC (byte [] recvData)  {
        storedAFData.append(recvData,1,recvData.length-1);
        return;
    }
    //endregion

    //region CRC32 Related
    /********** CRC RELATED **********/
    public static byte[] longToBytesInvertCRC(long l) {
        byte[] result = new byte[4];
        for (int i = 0; i< 4; i++) {
            result[i] = (byte)~(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    //endregion

    //region CRC16 Related
    private byte[] iso14443a_crc(byte[] Data)   // DESFireSAM crc16 do not invert the result
    {
        int  bt;
        int wCrc = 0x6363;
        int j = 0;
        int t8 = 0;
        int t9 = 0;
        int tA = 0;
        int Len = Data.length;
        final int maskB = 0x0000000000000000FF;
        final int maskW = 0x00000000000000FFFF;


        do
        {
            bt = Data[j++]              & maskB;
            bt =  (bt^(wCrc & 0x00FF))  & maskB;
            bt =  (bt^(bt<<4))          & maskB;


            t8 = (bt << 8)          & maskW;
            t9 = (bt<<3)            & maskW;
            tA = (bt>>4)            & maskW;
            wCrc = (wCrc >> 8)^(t8^t9^tA)  & maskW;
        }
        while (j < Len);


        byte[] bb = new byte[2];
        bb[0] = (byte) (wCrc          & maskB);
        bb[1] = (byte) ((wCrc >>8)    & maskB);
        return bb;
    }


    // https://github.com/jekkos/android-hce-desfire/blob/master/hceappletdesfire/src/main/java/net/jpeelaer/hce/desfire/Util.java
    public static byte[] crc16(byte[]data){
        short crc = (short) 0x0000;
        short[] table = {
                (short) 0x0000, (short) 0xC0C1, (short) 0xC181, (short) 0x0140, (short) 0xC301, (short) 0x03C0, (short) 0x0280, (short) 0xC241,
                (short) 0xC601, (short) 0x06C0, (short) 0x0780, (short) 0xC741, (short) 0x0500, (short) 0xC5C1, (short) 0xC481, (short) 0x0440,
                (short) 0xCC01, (short) 0x0CC0, (short) 0x0D80, (short) 0xCD41, (short) 0x0F00, (short) 0xCFC1, (short) 0xCE81, (short) 0x0E40,
                (short) 0x0A00, (short) 0xCAC1, (short) 0xCB81, (short) 0x0B40, (short) 0xC901, (short) 0x09C0, (short) 0x0880, (short) 0xC841,
                (short) 0xD801, (short) 0x18C0, (short) 0x1980, (short) 0xD941, (short) 0x1B00, (short) 0xDBC1, (short) 0xDA81, (short) 0x1A40,
                (short) 0x1E00, (short) 0xDEC1, (short) 0xDF81, (short) 0x1F40, (short) 0xDD01, (short) 0x1DC0, (short) 0x1C80, (short) 0xDC41,
                (short) 0x1400, (short) 0xD4C1, (short) 0xD581, (short) 0x1540, (short) 0xD701, (short) 0x17C0, (short) 0x1680, (short) 0xD641,
                (short) 0xD201, (short) 0x12C0, (short) 0x1380, (short) 0xD341, (short) 0x1100, (short) 0xD1C1, (short) 0xD081, (short) 0x1040,
                (short) 0xF001, (short) 0x30C0, (short) 0x3180, (short) 0xF141, (short) 0x3300, (short) 0xF3C1, (short) 0xF281, (short) 0x3240,
                (short) 0x3600, (short) 0xF6C1, (short) 0xF781, (short) 0x3740, (short) 0xF501, (short) 0x35C0, (short) 0x3480, (short) 0xF441,
                (short) 0x3C00, (short) 0xFCC1, (short) 0xFD81, (short) 0x3D40, (short) 0xFF01, (short) 0x3FC0, (short) 0x3E80, (short) 0xFE41,
                (short) 0xFA01, (short) 0x3AC0, (short) 0x3B80, (short) 0xFB41, (short) 0x3900, (short) 0xF9C1, (short) 0xF881, (short) 0x3840,
                (short) 0x2800, (short) 0xE8C1, (short) 0xE981, (short) 0x2940, (short) 0xEB01, (short) 0x2BC0, (short) 0x2A80, (short) 0xEA41,
                (short) 0xEE01, (short) 0x2EC0, (short) 0x2F80, (short) 0xEF41, (short) 0x2D00, (short) 0xEDC1, (short) 0xEC81, (short) 0x2C40,
                (short) 0xE401, (short) 0x24C0, (short) 0x2580, (short) 0xE541, (short) 0x2700, (short) 0xE7C1, (short) 0xE681, (short) 0x2640,
                (short) 0x2200, (short) 0xE2C1, (short) 0xE381, (short) 0x2340, (short) 0xE101, (short) 0x21C0, (short) 0x2080, (short) 0xE041,
                (short) 0xA001, (short) 0x60C0, (short) 0x6180, (short) 0xA141, (short) 0x6300, (short) 0xA3C1, (short) 0xA281, (short) 0x6240,
                (short) 0x6600, (short) 0xA6C1, (short) 0xA781, (short) 0x6740, (short) 0xA501, (short) 0x65C0, (short) 0x6480, (short) 0xA441,
                (short) 0x6C00, (short) 0xACC1, (short) 0xAD81, (short) 0x6D40, (short) 0xAF01, (short) 0x6FC0, (short) 0x6E80, (short) 0xAE41,
                (short) 0xAA01, (short) 0x6AC0, (short) 0x6B80, (short) 0xAB41, (short) 0x6900, (short) 0xA9C1, (short) 0xA881, (short) 0x6840,
                (short) 0x7800, (short) 0xB8C1, (short) 0xB981, (short) 0x7940, (short) 0xBB01, (short) 0x7BC0, (short) 0x7A80, (short) 0xBA41,
                (short) 0xBE01, (short) 0x7EC0, (short) 0x7F80, (short) 0xBF41, (short) 0x7D00, (short) 0xBDC1, (short) 0xBC81, (short) 0x7C40,
                (short) 0xB401, (short) 0x74C0, (short) 0x7580, (short) 0xB541, (short) 0x7700, (short) 0xB7C1, (short) 0xB681, (short) 0x7640,
                (short) 0x7200, (short) 0xB2C1, (short) 0xB381, (short) 0x7340, (short) 0xB101, (short) 0x71C0, (short) 0x7080, (short) 0xB041,
                (short) 0x5000, (short) 0x90C1, (short) 0x9181, (short) 0x5140, (short) 0x9301, (short) 0x53C0, (short) 0x5280, (short) 0x9241,
                (short) 0x9601, (short) 0x56C0, (short) 0x5780, (short) 0x9741, (short) 0x5500, (short) 0x95C1, (short) 0x9481, (short) 0x5440,
                (short) 0x9C01, (short) 0x5CC0, (short) 0x5D80, (short) 0x9D41, (short) 0x5F00, (short) 0x9FC1, (short) 0x9E81, (short) 0x5E40,
                (short) 0x5A00, (short) 0x9AC1, (short) 0x9B81, (short) 0x5B40, (short) 0x9901, (short) 0x59C0, (short) 0x5880, (short) 0x9841,
                (short) 0x8801, (short) 0x48C0, (short) 0x4980, (short) 0x8941, (short) 0x4B00, (short) 0x8BC1, (short) 0x8A81, (short) 0x4A40,
                (short) 0x4E00, (short) 0x8EC1, (short) 0x8F81, (short) 0x4F40, (short) 0x8D01, (short) 0x4DC0, (short) 0x4C80, (short) 0x8C41,
                (short) 0x4400, (short) 0x84C1, (short) 0x8581, (short) 0x4540, (short) 0x8701, (short) 0x47C0, (short) 0x4680, (short) 0x8641,
                (short) 0x8201, (short) 0x42C0, (short) 0x4380, (short) 0x8341, (short) 0x4100, (short) 0x81C1, (short) 0x8081, (short) 0x4040,
        };
        for (short i = 0; i < data.length; i++) {
            crc = (short) ((crc >>> 8) ^ crc_table[(crc ^ data[i]) & (short) 0xff]);
        }

        return  new byte[] { (byte) (crc >>> 8), (byte) (crc) };
    }


    // http://automationwiki.com/index.php?title=CRC-16-CCITT
    private static int  crc_table [] = {

        0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5,
                0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a, 0xb16b,
                0xc18c, 0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210,
                0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
                0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c,
                0xf3ff, 0xe3de, 0x2462, 0x3443, 0x0420, 0x1401,
                0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b,
                0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
                0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6,
                0x5695, 0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738,
                0xf7df, 0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5,
                0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
                0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969,
                0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96,
                0x1a71, 0x0a50, 0x3a33, 0x2a12, 0xdbfd, 0xcbdc,
                0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
                0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03,
                0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd,
                0xad2a, 0xbd0b, 0x8d68, 0x9d49, 0x7e97, 0x6eb6,
                0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
                0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a,
                0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb,
                0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1,
                0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
                0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c,
                0xe37f, 0xf35e, 0x02b1, 0x1290, 0x22f3, 0x32d2,
                0x4235, 0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb,
                0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
                0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447,
                0x5424, 0x4405, 0xa7db, 0xb7fa, 0x8799, 0x97b8,
                0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2,
                0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
                0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9,
                0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827,
                0x18c0, 0x08e1, 0x3882, 0x28a3, 0xcb7d, 0xdb5c,
                0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
                0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0,
                0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d,
                0xbdaa, 0xad8b, 0x9de8, 0x8dc9, 0x7c26, 0x6c07,
                0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
                0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba,
                0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74,
                0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
    };

    byte [] CRCCCITT(byte [] data, int  seed)
    {

        int count;
        long crc = seed;
        long temp;
        int maxlength = data.length;
        Log.d("CRCCITT", "ENTER data.lenth = " + data.length);
        for (count = 0; count < maxlength; count++)
        {
            Log.d("CRCCITT", "Loop: " + count);
            temp = (data[count] ^ (crc >>> 8)) & 0xff;
            Log.d("CRCCITT", "temp = " + temp);
            if (temp > 255) {
                Log.d("CRCCITT", " temp greater than 255 ERROR");
            }
            crc = crc_table[(int)temp] ^ (crc << 8);
            Log.d("CRCCITT", "crc = " + crc);
        }
        int iCRC = (int) crc & 0xffff;

        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        Log.d("CRCCITT", "finished crc = " + iCRC);
        byte[] returnCRC = new byte[4];
        System.arraycopy(b.putInt(iCRC).array(), 0 , returnCRC, 0 , 4);
        Log.d("CRCCITT", "byte crc = " + ByteArray.byteArrayToHexString(returnCRC));
        return returnCRC;

    }

    //https://www.lammertbies.nl/forum/viewtopic.php?t=1907
    static byte[] ComputeCRC(byte[] val) {
        long crc;
        long q;
        byte c;
        crc = 0x6363;
        for (int i = 0; i < val.length; i++) {
            c = val[i];
            q = (crc ^ c) & 0x0f;
            crc = (crc >> 4) ^ (q * 0x1081);
            q = (crc ^ (c >> 4)) & 0x0f;
            crc = (crc >> 4) ^ (q * 0x1081);
        }
        int crcend = ((byte) crc << 8 | (byte) (crc >> 8)) & 0xffff;
//Swap bytes
        byte [] result = new byte[2];
        result[0] = (byte)  ((crcend >> 8) & 0xff);
        result[1] = (byte) (crcend & 0xff);

        /*int byte1 = (crcend & 0xff);
        int byte2 = ((crcend >> 8) & 0xff);
        int result = ((byte1 << 8) | (byte2));
//Swap


        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] returnCRC = new byte[2];
        System.arraycopy(b.putInt(result).array(), 0 , returnCRC, 0 , 2);
         */
        Log.d ("ComputeCRC", "Calc CRC: " + ByteArray.byteArrayToHexString(result));
        return result;
    }

//endregion

    public byte [] calcCRC (byte [] data) {
        long lcrc = 0;
        byte [] returnCRC;
        if (CRCLength == 4) {
            CRC32 crc = new CRC32();
            crc.update(data);
            lcrc = crc.getValue();
            returnCRC = longToBytesInvertCRC(lcrc);

        } else {
            returnCRC = iso14443a_crc(data);
            //returnCRC = crc16(data);
            //returnCRC = CRCCCITT(data,0xFFFF);
            //returnCRC = CRCCCITT(data,0x6363);
            //returnCRC = crc16(data);
            //returnCRC = ComputeCRC(data);
            //returnCRC = CRC16CCITT(data);
        }
        Log.d("calcCRC", "Calculated CRC      = " +  ByteArray.byteArrayToHexString(returnCRC));
        return returnCRC;
    }

    public boolean verifyCRC (byte [] crcToVerify, byte [] data) {

        return Arrays.equals(crcToVerify, calcCRC(data));
    }
    //endregion

    public void  storeAFEncryptedSetLength (byte [] recvData, int len)  {
        encryptedLength = len;
        storedAFData.append(recvData,1,recvData.length-1);
    }
    public void  storeAFEncrypted (byte [] recvData)  {
        storedAFData.append(recvData,1,recvData.length-1);
    }


    public byte [] decryptReadData () throws IOException, GeneralSecurityException{
        byte [] decryptedData;


        if (storedAFData.length() < 8) {
            throw new  GeneralSecurityException("Length error: Data returned too short. Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));
        }
        Log.d("decryptReadData", "Encrypted Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        decryptedData = decrypt(storedAFData.toArray());

        if (decryptedData == null)
            throw new GeneralSecurityException("Decryption error: Encryption Input = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        Log.d("decryptReadData", "Decrypted Data = " + ByteArray.byteArrayToHexString(decryptedData));

        ByteArray baDecryptedPlainData = new ByteArray();
        ByteArray baCRC = new ByteArray();
        if (encryptedLength != 0) {  // if count is specified 00 .. 00 padding is used
            baDecryptedPlainData.append(decryptedData, 0, encryptedLength);
            baCRC.append(decryptedData,encryptedLength,CRCLength);

        } else {  // Count == 0, remove 80..00 padding
            int padCount = ByteArray.ISO9797m2PadCount(decryptedData);
            if (padCount == -1) {
                throw new GeneralSecurityException("Decryption padding error: Decrypted data = " + ByteArray.byteArrayToHexString(decryptedData));
            }

            baDecryptedPlainData.append(decryptedData, 0, decryptedData.length - CRCLength - padCount);
            baCRC.append(decryptedData, decryptedData.length - CRCLength - padCount, CRCLength);
        }

        Log.d("decryptReadData", "CRC  Data      = " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        byte[] returnData = baDecryptedPlainData.toArray();
        if (CRCLength == 4) {
            baDecryptedPlainData.append((byte) 0x00);  // status must be 0x00
        }

        Log.d("decryptReadData","CRC Input = " + ByteArray.byteArrayToHexString(baDecryptedPlainData.toArray()));
        byte [] computedCRC = calcCRC(baDecryptedPlainData.toArray());
        if (!Arrays.equals(baCRC.toArray(), computedCRC)) {
            Log.d("decryptReadData", "CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
            throw new GeneralSecurityException("CRC Error: Card Returned: " + ByteArray.byteArrayToHexString(baCRC.toArray()) + " Calculated: " + ByteArray.byteArrayToHexString(computedCRC));
        }
        // Reset
        encryptedLength = 0;
        storedAFData.clear();

        return returnData;
    }

    public byte [] encryptWriteDataBlock (byte [] bCmdHeader, byte [] bDataToEncrypt) throws IOException, GeneralSecurityException{


        // CALC CRC
        ByteArray baDataToCRC = new ByteArray();

        Log.d("encryptWriteDataBlock","CRC Input = " + ByteArray.byteArrayToHexString(baDataToCRC.append(bCmdHeader).append(bDataToEncrypt).toArray()));
        byte [] computedCRC = calcCRC(baDataToCRC.append(bCmdHeader).append(bDataToEncrypt).toArray());

        // DO PADDING
        int iPaddingLen = blockLength - ((bDataToEncrypt.length + CRCLength) % blockLength);
        //int iDataToEncryptLen = bDataToEncrypt.length + CRCLength + iPaddingLen;
        byte [] bPadding = new byte[iPaddingLen];
        Arrays.fill(bPadding, (byte) 0);

        // ENCRYPT ALL BLOCKS
        ByteArray baDataToEncrypt = new ByteArray();

        baDataToEncrypt.append(bDataToEncrypt).append(computedCRC).append(bPadding);

      //  currentIV = Arrays.copyOf(encryptionIV, encryptionIV.length);
        //Arrays.fill(currentIV, (byte)0);
        Log.d("encryptWriteDataBlock", "Input Data     = " + ByteArray.byteArrayToHexString(baDataToEncrypt.toArray()));

        byte [] bEncryptedData = encrypt(baDataToEncrypt.toArray());

        if (bEncryptedData == null)
            throw new GeneralSecurityException("Encryption error: Encryption Input = " + ByteArray.byteArrayToHexString(bEncryptedData));

        Log.d("encryptWriteDataBlock", "Encrypted Data = " + ByteArray.byteArrayToHexString(bEncryptedData));

        return bEncryptedData;
    }


}
