package com.example.ac.desfirelearningtool;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
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
 * A class to wrap Mifare Desfire commands, using a generic "Communicator"
 * 
 * Commands and parameters from libfreefare (https://github.com/nfc-tools) 
 */
public class MifareDesfire {

    private final int macSize = 4;
    private final int maxDataSize = 52 - macSize;

    protected ICardCommunicator cardCommunicator;
    protected SecureRandom randomGenerator;
    public byte[] uid;
    private ScrollLog scrollLog;





    public MifareDesfire(ICardCommunicator cardCommunicator, byte[] uid, ScrollLog tv_scrollLog) throws NoSuchAlgorithmException {
        this.cardCommunicator = cardCommunicator;
        this.uid = uid;
        this.randomGenerator = new SecureRandom();
        this.scrollLog = tv_scrollLog;
    }

    /**
     * Returns a byte array that represents the card version
     *
     * @throws IOException
     */
    public MifareResult getVersion() throws IOException {
        MifareResult result = sendBytes(new byte[]{(byte)0x60});

        if (result.resultType != MifareResultType.ADDITONAL_FRAME)
            scrollLog.appendError("Error in card response: " + DesFireErrorMsg(result.resultType));
        return result;
    }

    public MifareResult getMoreData() throws IOException {
        return sendBytes(new byte[]{(byte)0xAF});
    }

    public MifareResult getDFNames() throws IOException {
        return sendBytes(new byte[]{(byte)0x6D});
    }

    public MifareResult getFileIDs() throws IOException {
        return sendBytes(new byte[]{(byte)0x6F});
    }

    public MifareResult getISOFileIDs() throws IOException {
        return sendBytes(new byte[]{(byte)0x61});
    }

    /**
     * Returns a byte array of the card's Unique ID
     *
     * @throws IOException
     */
    public byte[] getCardUID() throws IOException {
        MifareResult result = sendBytes(new byte[]{(byte)0x51});

        if (result.resultType == MifareResultType.AUTHENTICATION_ERROR)
            scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
        return result.data;
    }

    /**
     * Returns Free Memory
     *
     * @throws IOException
     */
    public MifareResult getFreeMem() throws IOException {
        return sendBytes(new byte[]{(byte)0x6E});
    }

    /**
     * Returns Key Settings
     *
     * @throws IOException
     */
    public MifareResult getKeySettings() throws IOException {
        return sendBytes(new byte[]{(byte)0x45});
    }

    public MifareResult getKeyVersion(byte selectedKey) throws IOException {
        byte[] params = ByteArray.from((byte) 0x64).append(selectedKey).toArray();
        return sendBytes(params);
    }

    public MifareResult getFileSettings(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xf5, fid});
    }

    public MifareResult getApplicationIDs() throws IOException {
        ByteArrayOutputStream appIDs = new ByteArrayOutputStream();
        MifareResult result = sendBytes(new byte[]{(byte)0x6a});

        if (result.resultType != MifareDesfire.MifareResultType.SUCCESS) {
            return result;
        }

        appIDs.write(result.data);

        if (result.resultType == MifareResultType.ADDITONAL_FRAME) {
            result = sendBytes(new byte[]{(byte)0xAF});
            appIDs.write(result.data);
        }

        result.data = appIDs.toByteArray();
        return result;
    }

    public MifareResultType selectApplication(byte[] applicationId) throws IOException {
        byte[] params = ByteArray.from((byte) 0x5a).append(applicationId).toArray();
        MifareResult res = sendBytes(params);

        return res.resultType;
    }

    public MifareResultType createApplication(byte [] appId, byte bKeySetting1, byte bKeySetting2, byte [] baISOName, byte [] baDFName) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append((byte) 0xCA)
                .append(appId)
                .append(bKeySetting1)
                .append(bKeySetting2);

        if (baISOName.length == 2) {
            baCreateDataFileArray.append(baISOName);
        }
        baCreateDataFileArray.append(baDFName);

        Log.v("createDataFile", "Command for Create Data File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));


        // byte[] params = ByteArray.from((byte) 0xCA).append(createAppByteArray).toArray();
        MifareResult res = sendBytes(baCreateDataFileArray.toArray());

        return res.resultType;
    }

    protected MifareResultType deleteApplication(byte[] applicationId) throws IOException {
        byte[] params = ByteArray.from((byte) 0xDA).append(applicationId).toArray();
        MifareResult res = sendBytes(params);

        return res.resultType;
    }

    /**
     * Create data file
     *
     * @param bFileType
     * @param bFileID
     * @param baISOName
     * @param bCommSetting
     * @param baAccessRights
     * @param iFileSize
     * @return
     * @throws IOException
     */
    public MifareResultType createDataFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileType)
                .append(bFileID);

        if (baISOName.length == 2) {
            baCreateDataFileArray.append(baISOName);
        }
        baCreateDataFileArray.append(bCommSetting).append(baAccessRights);

        // File Size changed to 4 byte string then copy it to 3 bytes
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] baFileSize = b.putInt(iFileSize).array();
        byte[] ba3FileSize = new byte[3];
        System.arraycopy(baFileSize, 0, ba3FileSize, 0, 3);

        baCreateDataFileArray.append(ba3FileSize);

        Log.v("createDataFile", "Command for Create Data File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));


        // byte[] params = ByteArray.from((byte) 0xCA).append(createAppByteArray).toArray();
        MifareResult res = sendBytes(baCreateDataFileArray.toArray());

        return res.resultType;
    }

    /**
     * Create Record file
     *
     * @param bFileType
     * @param bFileID
     * @param baISOName
     * @param bCommSetting
     * @param baAccessRights
     * @param iRecordSize
     * @param iNumOfRecords
     * @return
     * @throws IOException
     */
    public MifareResultType createRecordFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iRecordSize, int iNumOfRecords) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileType)
                .append(bFileID);

        if (baISOName.length == 2) {
            baCreateDataFileArray.append(baISOName);
        }
        baCreateDataFileArray.append(bCommSetting).append(baAccessRights);

        // File Size changed to 4 byte string then copy it to 3 bytes
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] baRecordSize = b.putInt(iRecordSize).array();
        byte[] ba3RecordSize = new byte[3];
        System.arraycopy(baRecordSize, 0, ba3RecordSize, 0, 3);
        baCreateDataFileArray.append(ba3RecordSize);

        ByteBuffer NoR = ByteBuffer.allocate(4);
        NoR.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        byte[] baNumOfRecords = NoR.putInt(iNumOfRecords).array();
        byte[] ba3NumOfRecords = new byte[3];
        System.arraycopy(baNumOfRecords, 0, ba3NumOfRecords, 0, 3);
        baCreateDataFileArray.append(ba3NumOfRecords);

        Log.v("createRecordFile", "Command for Create Record File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));

        // byte[] params = ByteArray.from((byte) 0xCA).append(createAppByteArray).toArray();
        MifareResult res = sendBytes(baCreateDataFileArray.toArray());

        return res.resultType;
    }

    public MifareResultType createValueFile(byte bFileType, byte bFileID, byte bCommSetting, byte [] baAccessRights, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileType)
                .append(bFileID);

        baCreateDataFileArray.append(bCommSetting).append(baAccessRights);

        // Int to 4 byte array conversion
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);

        byte[] baLowerLimit = b.putInt(iLowerLimit).array();
        baCreateDataFileArray.append(baLowerLimit);

        // Int to 4 byte array conversion
        ByteBuffer c = ByteBuffer.allocate(4);
        c.order(ByteOrder.LITTLE_ENDIAN);

        byte[] baUpperLimit = c.putInt(iUpperLimit).array();
        baCreateDataFileArray.append(baUpperLimit);

        // Int to 4 byte array conversion
        ByteBuffer d = ByteBuffer.allocate(4);
        d.order(ByteOrder.LITTLE_ENDIAN);

        byte[] baValue = d.putInt(iValue).array();
        baCreateDataFileArray.append(baValue);

        baCreateDataFileArray.append(bOptionByte);


        Log.v("createRecordFile", "Command for Create Value File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));

        MifareResult res = sendBytes(baCreateDataFileArray.toArray());

        return res.resultType;
    }

    public MifareResult deleteFile(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xDF, fid});
    }

    public MifareResultType formatPICC() throws IOException {
        MifareResult result = sendBytes(new byte[]{(byte)0xFC});

        return result.resultType;
    }


    public byte[] readRecordFile(byte fid, int start, int count) throws IOException {
        byte[] cmd = new ByteArray().append((byte)0xBB).append(fid).append(start, 3).append(count, 3).toArray();
        MifareResult result = sendBytes(cmd);
        return result.data;
    }

    public byte[] readFile(byte fid, int start, int count) throws IOException {
        ByteArray ret = new ByteArray();

        boolean done = false;
        int bytesToGo = count;

        while (!done) {

            int upTo;
            if (count == 0)
                upTo = maxDataSize;
            else
                upTo = Math.min(maxDataSize, bytesToGo);

            ByteArray array = new ByteArray();
            byte[] cmd = array.append((byte)0xBD).append(fid).append(start, 3).append(upTo, 3).toArray();

            MifareResult result = sendBytes(cmd);

            if (result.resultType == MifareResultType.BOUNDARY_ERROR) {
                // We reached the end of the file.
                // Ensure we got anything that was left
                array.clear();
                cmd = array.append((byte)0xBD).append(fid).append(start, 3).append(0, 3).toArray();
                result = sendBytes(cmd);
                done = true;
            }

            ret.append(result.data);

            start += upTo;
            if (count > 0) {
                bytesToGo -= upTo;
                if (bytesToGo == 0)
                    done = true;
            }
        }
        return ret.toArray();
    }

    private void writeInternal(byte cmd, byte[] data, int file, int offset, int size) throws IOException {
        int data_size;

        if (size == 0)
            data_size = data.length;
        else
            data_size = size;

        int data_to_go = data_size;
        while (data_to_go > 0) {

            int bytes_to_write;

            if (data_to_go > maxDataSize)
                bytes_to_write = maxDataSize;
            else
                bytes_to_write = data_to_go;


            ByteArray args = new ByteArray();
            args.append(cmd).append((byte)file).append(offset, 3).append(bytes_to_write, 3)
                .append(data, offset, bytes_to_write);

            data_to_go -= maxDataSize;
            offset += maxDataSize;

            byte[] message = args.toArray();
            byte[] result = cardCommunicator.transceive(message);
            if (result == null || result.length == 0)
                throw new IOException("Transceive returned an empty response");

            if (result[0] != 0)
                throw new IOException("Transceive error: " + ByteArray.byteArrayToHexString(result));
        }
    }

    public void writeFile(byte[] data, int file, int offset, int size) throws IOException {
        writeInternal((byte)0x3D, data, file, offset, size);
    }

    public void commit() throws IOException {
        byte[] result = cardCommunicator.transceive(new byte[]{(byte)0xC7});
        if (result == null || result.length == 0)
            throw new IOException("Commit returned an empty response");

        if (!(result[0] == 0x00 || result[0] == 0x0C))
            throw new IOException("Commit error: " + ByteArray.byteArrayToHexString(result));
    }



    public enum MifareResultType {
        SUCCESS,
        NO_CHANGES,
        OUT_OF_EEPROM_ERROR,
        ILLEGAL_COMMAND_CODE,
        INTEGRITY_ERROR,
        NO_SUCH_KEY,
        LENGTH_ERROR,
        PERMISSION_DENIED,
        PARAMETER_ERROR,
        APPLICATION_NOT_FOUND,
        APPL_INTEGRITY_ERROR,
        AUTHENTICATION_ERROR,
        ADDITONAL_FRAME,
        BOUNDARY_ERROR,
        PICC_INTEGRITY_ERROR,
        COMMAND_ABORTED,
        PICC_DISABLED_ERROR,
        COUNT_ERROR,
        DUPLICATE_ERROR,
        EEPROM_ERROR,
        FILE_NOT_FOUND,
        FILE_INTEGRITY_ERROR
    }

    public class MifareResult {
        public byte[] data;
        public MifareResultType resultType;
    }

    public MifareResult sendBytes(byte[] cmd) throws IOException {
        byte[] response = cardCommunicator.transceive(cmd);

        MifareResult result = new MifareResult();
        result.data = ByteArray.appendCut(null, response);

        switch (response[0]) {
            case (byte)0x00:
                result.resultType = MifareResultType.SUCCESS;
                break;
            case (byte)0x0C:
                result.resultType = MifareResultType.NO_CHANGES;
                break;
            case (byte)0x0E:
                result.resultType = MifareResultType.OUT_OF_EEPROM_ERROR;
                break;
            case (byte)0x1C:
                result.resultType = MifareResultType.ILLEGAL_COMMAND_CODE;
                break;
            case (byte)0x1E:
                result.resultType = MifareResultType.INTEGRITY_ERROR;
                break;
            case (byte)0x40:
                result.resultType = MifareResultType.NO_SUCH_KEY;
                break;
            case (byte)0x7E:
                result.resultType = MifareResultType.LENGTH_ERROR;
                break;
            case (byte)0x9D:
                result.resultType = MifareResultType.PERMISSION_DENIED;
                break;
            case (byte)0x9E:
                result.resultType = MifareResultType.PARAMETER_ERROR;
                break;
            case (byte)0xA0:
                result.resultType = MifareResultType.APPLICATION_NOT_FOUND;
                break;
            case (byte)0xA1:
                result.resultType = MifareResultType.APPL_INTEGRITY_ERROR;
                break;
            case (byte)0xAE:
                result.resultType = MifareResultType.AUTHENTICATION_ERROR;
                break;
            case (byte)0xAF:
                result.resultType = MifareResultType.ADDITONAL_FRAME;
                break;
            case (byte)0xBE:
                result.resultType = MifareResultType.BOUNDARY_ERROR;
                break;
            case (byte)0xC1:
                result.resultType = MifareResultType.PICC_INTEGRITY_ERROR;
                break;
            case (byte)0xCA:
                result.resultType = MifareResultType.COMMAND_ABORTED;
                break;
            case (byte)0xCD:
                result.resultType = MifareResultType.PICC_DISABLED_ERROR;
                break;
            case (byte)0xCE:
                result.resultType = MifareResultType.COUNT_ERROR;
                break;
            case (byte)0xDE:
                result.resultType = MifareResultType.DUPLICATE_ERROR;
                break;
            case (byte)0xEE:
                result.resultType = MifareResultType.EEPROM_ERROR;
                break;
            case (byte)0xF0:
                result.resultType = MifareResultType.FILE_NOT_FOUND;
                break;
            case (byte)0xF1:
                result.resultType = MifareResultType.FILE_INTEGRITY_ERROR;
                break;

            default:
                throw new IOException("Error in card response: " + ByteArray.byteArrayToHexString(response));
        }

        return result;
    }

    public String DesFireErrorMsg (MifareResultType resultType) {
        String returnString;

        switch (resultType) {
            case SUCCESS:
                returnString = "Command OK";
                break;
            case NO_CHANGES:
                returnString = "No Change";
                break;
            case OUT_OF_EEPROM_ERROR:
                returnString = "Out of EEPROM";
                break;
            case ILLEGAL_COMMAND_CODE:
                returnString = "Illegal command code";
                break;
            case INTEGRITY_ERROR:
                returnString = "Integrity Error";
                break;
            case NO_SUCH_KEY:
                returnString = "No such key";
                break;
            case LENGTH_ERROR:
                returnString = "Length error";
                break;
            case PERMISSION_DENIED:
                returnString = "Permission Denied";
                break;
            case PARAMETER_ERROR:
                returnString = "Parameter error";
                break;
            case APPLICATION_NOT_FOUND:
                returnString = "Application not found";
                break;
            case APPL_INTEGRITY_ERROR:
                returnString = "Application Integrity Error";
                break;
            case AUTHENTICATION_ERROR:
                returnString = "Authentication error";
                break;
            case ADDITONAL_FRAME:
                returnString = "Additional frame";
                break;
            case BOUNDARY_ERROR:
                returnString = "Boundary error";
                break;
            case PICC_INTEGRITY_ERROR:
                returnString = "PICC integrity error";
                break;
            case COMMAND_ABORTED:
                returnString = "Command aborted";
                break;
            case PICC_DISABLED_ERROR:
                returnString = "PICC disabled error";
                break;
            case COUNT_ERROR:
                returnString = "Count error";
                break;
            case DUPLICATE_ERROR:
                returnString = "Duplicate error";
                break;
            case EEPROM_ERROR:
                returnString = "EEPROM error";
                break;
            case FILE_NOT_FOUND:
                returnString = "File not found";
                break;
            case FILE_INTEGRITY_ERROR:
                returnString = "File integrity error";
                break;
            default:
                returnString = "Unknown error";
        }
        return returnString;
    }

    // ENCRYPTION RELATED

    // Mifare Desfire specifications require DESede/ECB without padding
    protected Cipher getDecipher3DES(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cryptoAlgo = Cipher.getInstance("DESede/ECB/NoPadding");

        ByteArray tripleDesKey = new ByteArray();
        if (origKey.length == 8) {
            tripleDesKey.append(origKey).append(origKey).append(origKey);
        } else if (origKey.length == 16) {
            byte[] firstKey = new byte[8];
            System.arraycopy(origKey, 0, firstKey, 0, 8);
            tripleDesKey.append(origKey).append(firstKey);
        } else if (origKey.length == 24) {
            tripleDesKey.append(origKey);
        } else
            throw new IllegalArgumentException("Wrong key length");

        // And we initialize it with our (diversified) read or write key
        final SecretKey key = new SecretKeySpec(tripleDesKey.toArray(), "DESede");
        cryptoAlgo.init(Cipher.DECRYPT_MODE, key);

        return cryptoAlgo;
    }

    // Mifare Desfire specifications require DESede/ECB without padding
    protected Cipher getEncipher3K3DES(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cryptoAlgo = Cipher.getInstance("DESede/CBC/NoPadding");

        ByteArray tripleDesKey = new ByteArray();
        if (origKey.length == 8) {
            tripleDesKey.append(origKey).append(origKey).append(origKey);
        } else if (origKey.length == 16) {
            byte[] firstKey = new byte[8];
            System.arraycopy(origKey, 0, firstKey, 0, 8);
            tripleDesKey.append(origKey).append(firstKey);
        } else if (origKey.length == 24) {
            tripleDesKey.append(origKey);
        } else
            throw new IllegalArgumentException("Wrong key length");

        // And we initialize it with our (diversified) read or write key
        final SecretKey keySpec = new SecretKeySpec(tripleDesKey.toArray(), "DESede");

        byte[] zeroBytes = new byte[8];
        Arrays.fill(zeroBytes, (byte)0);
        try {
            cryptoAlgo.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(zeroBytes));
        } catch (Exception e) {
            Log.e("getEncipher3K3DES", e.getMessage(), e);
        }

        return cryptoAlgo;
    }

    // Mifare Desfire AuthISO specifications require DESede/cbc without padding
    protected Cipher getDecipher3K3DES(byte[] origKey)
            throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cryptoAlgo = Cipher.getInstance("DESede/CBC/NoPadding");

        ByteArray tripleDesKey = new ByteArray();
        if (origKey.length == 8) {
            tripleDesKey.append(origKey).append(origKey).append(origKey);
        } else if (origKey.length == 16) {
            byte[] firstKey = new byte[8];
            System.arraycopy(origKey, 0, firstKey, 0, 8);
            tripleDesKey.append(origKey).append(firstKey);
        } else if (origKey.length == 24) {
            tripleDesKey.append(origKey);
        } else
            throw new IllegalArgumentException("Wrong key length");

        // And we initialize it with our (diversified) read or write key
        final SecretKey keySpec = new SecretKeySpec(tripleDesKey.toArray(), "DESede");
        byte[] zeroBytes = new byte[8];
        Arrays.fill(zeroBytes, (byte)0);
        try {
            cryptoAlgo.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(zeroBytes));
        } catch (Exception e) {
            Log.e("getDecipher3K3DES", e.getMessage(), e);
        }

        return cryptoAlgo;
    }


    public byte MODE_3DES = (byte) 0x0A;
    public byte MODE_3K3DES = (byte) 0x1A;
    public byte MODE_AES = (byte) 0xAA;


    public Challenge computeResponseAndDataToVerify(byte authType, byte[] encRndB, byte[] key)
            throws GeneralSecurityException {

        byte[] decRndA, rndB, rndBPrime, decRndBPrime;
        byte[] rndA = null;
        byte[] challengeMessage = null;
        Cipher encipher;
        Cipher decipher;

        if (authType == MODE_3DES){

            decipher = this.getDecipher3DES(key);

            if (encRndB == null || encRndB.length < 9) {
                throw new IllegalArgumentException("Not a valid challenge (application not existing?)");
            }

            encRndB = ByteArray.appendCut(null, encRndB);

            // We decrypt the challenge, and rotate one byte to the left
            rndB = decipher.doFinal(encRndB);
            rndBPrime = ByteArray.shiftLT(rndB);


            // Then we generate a random number as our challenge for the coupler
            rndA = new byte[8];
            randomGenerator.nextBytes(rndA);

            decRndA = decipher.doFinal(rndA);
            // XOR of rndA, rndB  // This is CBC done manually
            decRndBPrime = ByteArray.xor(decRndA, rndBPrime);
            // The result is encrypted again
            decRndBPrime = decipher.doFinal(decRndBPrime);

            challengeMessage = ByteArray.from((byte)0xAF).append(decRndA).append(decRndBPrime).toArray();

        } else if (authType == MODE_3K3DES) {
            encipher = this.getEncipher3K3DES(key);
            decipher = this.getDecipher3K3DES(key);

            if (encRndB == null || encRndB.length < 17) {
                throw new IllegalArgumentException("Not a valid challenge (application not existing?)");
            }

            encRndB = ByteArray.appendCut(null, encRndB);

            // We decrypt the challenge, and rotate one byte to the left
            rndB = decipher.doFinal(encRndB);
            rndBPrime = ByteArray.shiftLT(rndB);


            rndA = new byte[rndB.length];   // Length 8 byte for DES/3DES, 16 byte for 3k3des and AES
            randomGenerator.nextBytes(rndA);

            byte[] encInput = new byte[rndA.length + rndBPrime.length];
            System.arraycopy(rndA, 0, encInput, 0, rndA.length);
            System.arraycopy(rndBPrime, 0, encInput, rndA.length, rndBPrime.length);

            challengeMessage = ByteArray.from((byte)0xAF).append(encipher.doFinal(encInput));
        }

        // And sent back to the card
        return new Challenge(challengeMessage, rndA);
    }

    public boolean verifyCardResponse(byte authType, byte[] cardResponse, byte[] origRndA, byte[] key)
            throws GeneralSecurityException {
        Cipher decipher;
        if (authType == MODE_3DES)
            decipher = this.getDecipher3DES(key);
        else
            decipher = this.getDecipher3K3DES(key);

        if ((cardResponse == null) || (cardResponse.length == 1))
            return false;

        if ((cardResponse.length == 9) || (cardResponse.length == 17))
            cardResponse = ByteArray.appendCut(null, cardResponse);

        // We decrypt the response and shift the rightmost byte "all around" (to the left)
        cardResponse = ByteArray.shiftRT(decipher.doFinal(cardResponse));
        if (Arrays.equals(cardResponse, origRndA)) {
            return true;
        }

        return false;
    }

    public byte[] getCardChallenge(byte authType, byte keyNumber) throws Exception {
        byte[] cmd = ByteArray.from(authType).append(keyNumber).toArray();
        // Send the command to the key, receive the challenge
        byte[] response = cardCommunicator.transceive(cmd);

        return response;
    }


    public boolean authenticate(byte authType, byte keyNumber, byte[] key) throws Exception {

        // Send 0A
        byte[] encRndB = getCardChallenge(authType, keyNumber);

        // Compute next command and required response
        Challenge challenge = computeResponseAndDataToVerify(authType, encRndB, key);

        byte[] challengeMessage = challenge.getChallenge();
        byte[] plainCouplerChallenge = challenge.getChallengeResponse();

        // send AF
        byte[] cardResponse = cardCommunicator.transceive(challengeMessage);

        return verifyCardResponse(authType, cardResponse, plainCouplerChallenge, key);
    }

    /**
     * Opens communication to the card
     *
     * @throws IOException
     */
    public boolean connect() throws IOException {
        cardCommunicator.connect();
        return cardCommunicator.isConnected();
    }

    public void close() throws IOException {
        cardCommunicator.close();
    }
}
