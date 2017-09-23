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
    private int blockLength;
    private byte[] sessionKey;
    private byte[] K1, K2;  // Subkey for CMAC
    public boolean trackCMAC;
    public int CRCLength;    // Length of CRC 2 or 4 bytes
    public int encryptedLength;  // specified dataLength at the first AF for Read Data


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
    }

    //region Key Related
    //---------------------------------------------------------------------------------------
    public boolean initialize (byte authToSet, byte [] key) throws Exception {
        boolean res;
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

        byte [] encOutput = null;

        try {
            switch (authMode) {
                case MODE_AUTHD40:
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));  // IV is always 00..00
                    encOutput = cipher.doFinal(encInput);   // Decrypt
                    break;
                case MODE_AUTHISO:
                case MODE_AUTHAES:
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(currentIV));
                    encOutput = cipher.doFinal(encInput);   // Decrypt
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
        //Log.d ("verifyCMAC", "MAC to Verify = " + ByteArray.byteArrayToHexString(MACToVerify) );
        //Log.d ("verifyCMAC", "MAC computed  = " + ByteArray.byteArrayToHexString(computedMACToVerify) );

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

        Log.d ("calcCMAC  ", "Encryt Input Data = " + ByteArray.byteArrayToHexString(encInput));
        output = encrypt(encInput);
        Log.d ("calcCMAC  ", "Encrytped Data    = " + ByteArray.byteArrayToHexString(output));

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

    //region CRC Related
    /********** CRC RELATED **********/
    public static byte[] longToBytesInvertCRC(long l) {
        byte[] result = new byte[4];
        for (int i = 0; i< 4; i++) {
            result[i] = (byte)~(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    //https://www.lammertbies.nl/forum/viewtopic.php?t=1907
    static byte[] ComputeCRC(byte[] val) {
        int crc;
        int q;
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
        int byte1 = (crcend & 0xff);
        int byte2 = ((crcend >> 8) & 0xff);
        int result = ((byte1 << 8) | (byte2));
//Swap

        Log.d ("ComputeCRC", "Calc CRC" + result);
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] returnCRC = new byte[2];
        System.arraycopy(b.putInt(result).array(), 0 , returnCRC, 0 , 2);

        return returnCRC;
    }
    //region bad CRC16s
    static byte [] crc16(final byte[] buffer) {
        int crc = 0xFFFF;

        for (int j = 0; j < buffer.length ; j++) {
            crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
            crc ^= (buffer[j] & 0xff);//byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] returnCRC = new byte[4];
        System.arraycopy(b.putInt(crc).array(), 0 , returnCRC, 0 , 4);

        return returnCRC;

    }

    public static byte []  CRC16CCITT(byte[] bytes) {
        int crc = 0x6363;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        //System.out.println("CRC16-CCITT = " + Integer.toHexString(crc));
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] returnCRC = new byte[4];
        System.arraycopy(b.putInt(crc).array(), 0 , returnCRC, 0 , 4);

        return returnCRC;
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
            returnCRC = ComputeCRC(data);
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
            throw new IOException("Returned no data");
        }
        Log.d("decryptReadData", "Encrypted Data = " + ByteArray.byteArrayToHexString(storedAFData.toArray()));

        decryptedData = decrypt(storedAFData.toArray());

        if (decryptedData == null)
            throw new IOException("Decryption Error");

        Log.d("decryptReadData", "Decrypted Data = " + ByteArray.byteArrayToHexString(decryptedData));

        ByteArray baDecryptedPlainData = new ByteArray();
        ByteArray baCRC = new ByteArray();
        if (encryptedLength != 0) {  // if count is specified 00 .. 00 padding is used
            baDecryptedPlainData.append(decryptedData, 0, encryptedLength);
            baCRC.append(decryptedData,encryptedLength,CRCLength);

        } else {  // Count == 0, remove 80..00 padding
            int padCount = ByteArray.ISO9797m2PadCount(decryptedData);
            if (padCount == -1) throw new IOException("Decryption padding error");

            baDecryptedPlainData.append(decryptedData, 0, decryptedData.length - CRCLength - padCount);
            baCRC.append(decryptedData, decryptedData.length - CRCLength - padCount, CRCLength);
        }

        Log.d("decryptReadData", "CRC  Data      = " + ByteArray.byteArrayToHexString(baCRC.toArray()));
        byte[] returnData = baDecryptedPlainData.toArray();
        if (CRCLength == 4) {
            baDecryptedPlainData.append((byte) 0x00);  // status must be 0x00
        }

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

}
