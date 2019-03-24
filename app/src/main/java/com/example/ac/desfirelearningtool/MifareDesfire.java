package com.example.ac.desfirelearningtool;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;


/**
 * A class to wrap Mifare Desfire commands, using a generic "Communicator"
 * 
 * Commands and parameters from libfreefare (https://github.com/nfc-tools) 
 */
public class MifareDesfire {

    private final int macSize = 4;
    private final int maxDataSize = 52 - macSize;

    protected ICardCommunicator cardCommunicator;

    public byte[] uid;
    private ScrollLog scrollLog;
    DesfireCrypto dfCrypto;

    public MifareDesfire(ICardCommunicator cardCommunicator, byte[] uid, ScrollLog tv_scrollLog) throws NoSuchAlgorithmException {
        this.cardCommunicator = cardCommunicator;
        this.uid = uid;

        this.scrollLog = tv_scrollLog;
        this.dfCrypto = new DesfireCrypto();

    }

    public DesfireResponse getVersion() throws IOException {
        DesfireResponse result = sendBytes(new byte[]{(byte)0x60});

        if (result.status != statusType.ADDITONAL_FRAME)
            scrollLog.appendError("Error in card response: " + DesFireErrorMsg(result.status));
        return result;
    }

    public DesfireResponse getDFNames() throws IOException {
        return sendBytes(new byte[]{(byte)0x6D});
    }

    public DesfireResponse getFileIDs() throws IOException {
        return sendBytes(new byte[]{(byte)0x6F});
    }

    public DesfireResponse getISOFileIDs() throws IOException {
        return sendBytes(new byte[]{(byte)0x61});
    }

    public DesfireResponse getCardUID() throws Exception {
        dfCrypto.encryptedLength = 7;
        return sendBytes((byte)0x51, null, null, commMode.ENCIPHERED);
    }

    public DesfireResponse getFreeMem() throws IOException {
        return sendBytes(new byte[]{(byte)0x6E});
    }

    public DesfireResponse getKeySettings() throws IOException {
        return sendBytes(new byte[]{(byte)0x45});
    }

    public DesfireResponse getKeyVersion(byte selectedKey) throws IOException {
        byte[] params = ByteArray.from((byte) 0x64).append(selectedKey).toArray();
        return sendBytes(params);
    }

    public DesfireResponse getFileSettings(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xf5, fid});
    }

    public int getRecordLength(byte fid) throws IOException {
        DesfireResponse resp = sendBytes(new byte[]{(byte)0xf5, fid});
        int recordLength = -1;

        if (resp.status == statusType.SUCCESS) {
            recordLength = ByteBuffer.wrap(resp.data, 4,3).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        return recordLength;
    }

    public DesfireResponse getApplicationIDs() throws IOException {
        ByteArrayOutputStream appIDs = new ByteArrayOutputStream();
        DesfireResponse result = sendBytes(new byte[]{(byte)0x6a});

        if (result.status != MifareDesfire.statusType.SUCCESS) {
            return result;
        }

        appIDs.write(result.data);

        if (result.status == statusType.ADDITONAL_FRAME) {
            result = sendBytes(new byte[]{(byte)0xAF});
            appIDs.write(result.data);
        }

        result.data = appIDs.toByteArray();
        return result;
    }

    public statusType selectApplication(byte[] applicationId) throws IOException {
        dfCrypto.reset();
        byte[] cmd = ByteArray.from((byte) 0x5a).append(applicationId).toArray();
        DesfireResponse res = sendBytes(cmd);


        return res.status;
    }

    public statusType createApplication(byte [] appId, byte bKeySetting1, byte bKeySetting2, byte [] baISOName, byte [] baDFName) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateApplicationArray = new ByteArray();

        baCreateApplicationArray.append((byte) 0xCA)
                .append(appId)
                .append(bKeySetting1)
                .append(bKeySetting2);

        if (baISOName.length == 2) {
            baCreateApplicationArray.append(baISOName);
        }
        baCreateApplicationArray.append(baDFName);

        Log.v("createApplication", "Command for Create Application  File  : " + ByteArray.byteArrayToHexString(baCreateApplicationArray.toArray()));


        // byte[] params = ByteArray.from((byte) 0xCA).append(createAppByteArray).toArray();
        DesfireResponse res = sendBytes(baCreateApplicationArray.toArray());

        return res.status;
    }

    public statusType deleteApplication(byte[] applicationId) throws IOException {
        byte[] params = ByteArray.from((byte) 0xDA).append(applicationId).toArray();
        DesfireResponse res = sendBytes(params);

        return res.status;
    }

    public statusType createDataFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
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
        DesfireResponse res = sendBytes(baCreateDataFileArray.toArray());

        return res.status;
    }

    public statusType createStdDataFile(byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
        return createDataFile((byte)0xCD, bFileID, baISOName, bCommSetting, baAccessRights, iFileSize);
    }

    public statusType createBackupDataFile(byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
        return createDataFile((byte)0xCB, bFileID, baISOName, bCommSetting, baAccessRights, iFileSize);
    }

    public statusType createLinearRecordFile(byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iRecordSize, int iNumOfRecords) throws IOException {
        return createRecordFile((byte) 0xC1, bFileID, baISOName, bCommSetting, baAccessRights, iRecordSize, iNumOfRecords);
    }

    public statusType createCyclicRecordFile(byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iRecordSize, int iNumOfRecords) throws IOException {
        return createRecordFile((byte) 0xC0, bFileID, baISOName, bCommSetting, baAccessRights, iRecordSize, iNumOfRecords);
    }

    public statusType createRecordFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iRecordSize, int iNumOfRecords) throws IOException {
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
        DesfireResponse res = sendBytes(baCreateDataFileArray.toArray());

        return res.status;
    }

    public statusType createValueFile(byte bFileID, byte bCommSetting, byte [] baAccessRights, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append((byte)0xcc)
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

        DesfireResponse res = sendBytes(baCreateDataFileArray.toArray());

        return res.status;
    }

    public DesfireResponse deleteFile(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xDF, fid});
    }

    public statusType formatPICC() throws IOException {
        DesfireResponse result = sendBytes(new byte[]{(byte)0xFC});

        return result.status;
    }

    public statusType changeKey (byte bKeyToChange, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey) throws GeneralSecurityException, IOException{
        ByteArray keyBlockBuilder = new ByteArray();
        ByteArray commandBuilder = new ByteArray();
        // Ensure it is currently authenticated

        // Case 1
        // If oldKey != null, AND bKeyToChange != currAuthKey
        if (bKeyToChange != dfCrypto.currentAuthenticatedKey) {
            Log.d("changeKey","Change Key Case 1");
            if (baOldKey == null) {
                throw new GeneralSecurityException("Previous Key not specified");
            }
            if (baOldKey.length != baNewKey.length) {
                throw new GeneralSecurityException("Previous and New Keys are not the same length");
            }
            keyBlockBuilder.append(ByteArray.xor(baOldKey, baNewKey));

            // Append / Modify to include key version;
            if (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHAES) {
                keyBlockBuilder.append(bKeyVersion);
            } else {  // TODO: Set key version into last bit of each byte

            }
            commandBuilder.append((byte) 0xC4).append(bKeyToChange);


            // CALC CRC

            byte[] computedCRC;

            if (dfCrypto.CRCLength == 4) {
                ByteArray baDataToCRC = new ByteArray();
                baDataToCRC.append(commandBuilder.toArray()).append(keyBlockBuilder.toArray());
                Log.d("changeKey", "CRC Input = " + ByteArray.byteArrayToHexString(baDataToCRC.toArray()));
                computedCRC = dfCrypto.calcCRC(baDataToCRC.toArray());
            } else {
                Log.d("changeKey", "CRC Input = " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));
                computedCRC = dfCrypto.calcCRC(keyBlockBuilder.toArray());
            }

            keyBlockBuilder.append(computedCRC);
            keyBlockBuilder.append(dfCrypto.calcCRC(baNewKey));
            Log.d("changeKey", "Case 1 Input = " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));

            commandBuilder.append(dfCrypto.encryptDataBlock (keyBlockBuilder.toArray()));

        } else {
            Log.d("changeKey","Change Key Case 2");
            // Case 2
            // Append NewKey
            keyBlockBuilder.append(baNewKey);

            // Append / Modify to include key version;
            if (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHAES) {
                keyBlockBuilder.append(bKeyVersion);
            } else {  // TODO: Set key version into last bit of each byte

            }

            commandBuilder.append((byte) 0xC4).append(bKeyToChange);

            commandBuilder.append(dfCrypto.encryptWriteDataBlock(commandBuilder.toArray(), keyBlockBuilder.toArray()));
        }


        Log.d("changeKey","Command to send: " + ByteArray.byteArrayToHexString(commandBuilder.toArray()));


        byte[] response = cardCommunicator.transceive(commandBuilder.toArray());

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);

        if (bKeyToChange == dfCrypto.currentAuthenticatedKey)
            dfCrypto.reset();

        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.trackCMAC) {
                Log.d("changeKey", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("OK: CMAC Verified");
                } else {
                    scrollLog.appendError("Failed: CMAC Incorrect");
                    result.status=statusType.PCD_ENCRYPTION_ERROR;
                }
            }

        } else {
            dfCrypto.reset();
        }

        return result.status;

    }

    public DesfireResponse getMoreData() throws IOException {
        return sendBytes((byte)0xAF, null, null, commMode.PLAIN);
        //return sendBytes(new byte[]{(byte)0xAF});
    }

    public DesfireResponse getMoreData(commMode curCommMode) throws IOException {
        return sendBytes((byte)0xAF, null, null, curCommMode);
    }

    public DesfireResponse readData(byte fid, int start, int count, commMode curCommMode) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmdHeader = array.append(fid).append(start, 3).append(count, 3).toArray();

        dfCrypto.setAFLength(count);
        return sendBytes((byte) 0xBD, cmdHeader, null, curCommMode);
    }

    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode) throws IOException {
        dfCrypto.encryptedLength = 0;
        return readRecords(fid, offsetRecord, numOfRecords,curCommMode);
    }

    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode, int encryptedLength) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmdHeader = array.append(fid).append(offsetRecord, 3).append(numOfRecords, 3).toArray();
        dfCrypto.encryptedLength = encryptedLength;
        return sendBytes((byte) 0xBB, cmdHeader, null, curCommMode);
    }

    public DesfireResponse writeData(byte fid, int start, int count, byte [] dataToWrite, commMode curCommMode) throws IOException {

        ByteArray baCmdHeaderToSend = new ByteArray();
        baCmdHeaderToSend.append(fid).append(start, 3).append(count, 3);
        return sendBytes((byte) 0x3D,baCmdHeaderToSend.toArray(),dataToWrite, curCommMode);
    }

    public DesfireResponse writeRecord(byte fid, int startRecord, int sizeToWrite, byte [] dataToWrite, commMode curCommMode) throws IOException {

        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid).append(startRecord, 3).append(sizeToWrite, 3);

        return sendBytes((byte) 0x3B, baCmdHeader.toArray(),dataToWrite,curCommMode);
    }

    public DesfireResponse clearRecordFile(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xEB, fid});
    }

    public DesfireResponse getValue(byte bFileID,commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(bFileID);

        dfCrypto.encryptedLength = 4;
        return sendBytes((byte) 0x6C, baCmdHeader.toArray(), null, curCommMode);
    }

    public DesfireResponse credit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        return sendBytes((byte) 0x0C, baCmdHeader.toArray(), baValue.toArray(), curCommMode);
    }

    public DesfireResponse debit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        return sendBytes((byte) 0xDC, baCmdHeader.toArray(), baValue.toArray(), curCommMode);
    }

    public DesfireResponse limitedCredit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        return sendBytes((byte) 0x1C, baCmdHeader.toArray(), baValue.toArray(), curCommMode);
    }

    public DesfireResponse commitTransaction() throws IOException {
        return sendBytes(new byte[]{(byte)0xC7});
    }

    public DesfireResponse abortTransaction() throws IOException {
        return sendBytes(new byte[]{(byte)0xA7});
    }

    public enum statusType {
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
        FILE_INTEGRITY_ERROR,
        PCD_AUTHENTICATION_ERROR,
        PCD_ENCRYPTION_ERROR,
        UNKNOWN_ERROR
    }

    public class DesfireResponse {
        public byte[] data;
        public statusType status;
    }

    public enum commMode {
        PLAIN,
        MAC,
        ENCIPHERED;

        public static byte getSetting(commMode cm){
            switch (cm) {
                case PLAIN:
                    return 0x00;
                case MAC:
                    return 0x01;
                case ENCIPHERED:
                    return 0x03;
                default:
                    return 0x00;
            }
        }
    }

    public DesfireResponse sendBytes(byte[] cmd) throws IOException {
        byte[] response;

        if ((dfCrypto.EV2_Authenticated) && (cmd[0] != (byte) 0xAF)) {

            Log.d ("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmd) );
            cmd = dfCrypto.EV2_GenerateMacCmd(cmd);



        } else if ((dfCrypto.trackCMAC) && (cmd[0] != (byte) 0xAF)) {
            Log.d ("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmd) );
            dfCrypto.calcCMAC(cmd);

        }

        response = cardCommunicator.transceive(cmd);

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);
        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.EV2_Authenticated) {
                if (!dfCrypto.EV2_verifyMacResponse(response)) {
                    scrollLog.appendError("Error: CMAC Incorrect");

                } else {
                    scrollLog.appendStatus("OK: CMAC Verified");

                    result.data = ByteArray.appendCutCMAC(response,8);
                }
            } else if (dfCrypto.trackCMAC) {
                Log.d("sendBytes  ", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (!dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendError("Error: CMAC Incorrect");
                }
                scrollLog.appendStatus("OK: CMAC Verified");

                result.data = ByteArray.appendCutCMAC(response,8);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (dfCrypto.trackCMAC || dfCrypto.EV2_Authenticated) {
                Log.d ("sendBytes  ", "Response to verify CMAC AF = " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);

            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }

        return result;
    }

    public DesfireResponse sendBytes (byte cmd) throws IOException {
        return sendBytes(cmd, null, null, commMode.PLAIN);
    }

    /**
     * sendBytes handles all DESFire D40, EV1, EV2 secure messaging and sending/receiving of data
     * @param cmd cmd byte as specified in data sheet
     * @param cmdHeader command Header
     * @param cmdData data to send
     * @param curCommMode communicaiton mode - PLAIN, MAC, ENCIPHERED
     * @return DESFireResponse status byte and data if any
     * @throws IOException
     */
    public DesfireResponse sendBytes(byte cmd, byte[] cmdHeader, byte [] cmdData, commMode curCommMode) throws IOException {
        byte[] response;
        byte [] cmdToSend;
        ByteArray baCmdBuilder = new ByteArray();


        baCmdBuilder.append(cmd).append(cmdHeader);
        if ((dfCrypto.EV2_Authenticated)) {

            if ((curCommMode == commMode.ENCIPHERED) && (cmdData != null)) {


                byte [] encryptData = dfCrypto.EV2_EncryptData(cmdData);
                if (encryptData == null) {
                    Log.d("sendBytes", "Command Encrypt error ");
                    scrollLog.appendError("Encrypt Command Data Error");
                    DesfireResponse badResult = new DesfireResponse();

                    badResult.status = statusType.PCD_ENCRYPTION_ERROR;
                    badResult.data = null;
                    return badResult;
                }

                baCmdBuilder.append(encryptData);
            } else {
                baCmdBuilder.append(cmdData);
            }

            if ((cmd != (byte) 0xAF) && (curCommMode != commMode.PLAIN) ){
                Log.d("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(baCmdBuilder.toArray()));
                cmdToSend = dfCrypto.EV2_GenerateMacCmd(baCmdBuilder.toArray());

                baCmdBuilder.clear();
                baCmdBuilder.append(cmdToSend);
            }

        } else if ((curCommMode == commMode.ENCIPHERED) && (cmdData != null)) {
            try {
                byte[] encryptData = dfCrypto.encryptWriteDataBlock(baCmdBuilder.toArray(), cmdData);

                baCmdBuilder.append(encryptData);
            } catch (GeneralSecurityException e) {
                scrollLog.appendError(e.getMessage());
                DesfireResponse badResult = new DesfireResponse();

                badResult.status = statusType.PCD_ENCRYPTION_ERROR;
                badResult.data = null;
                return badResult;
            }


        } else if ((dfCrypto.trackCMAC) && (cmd != (byte) 0xAF)) {

            baCmdBuilder.append(cmdData);
            byte [] cmdToMac = baCmdBuilder.toArray();


            Log.d ("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmdToMac) );
            byte [] macComputed = dfCrypto.calcCMAC(cmdToMac);

            if ((curCommMode == commMode.MAC) && (cmdData!=null)) {
                baCmdBuilder.append(macComputed);
            }

        }  else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)){
            if (cmdData != null ) {
                ByteArray arrayMAC = new ByteArray();
                byte[] cmdToMAC = arrayMAC.append(cmdData).toArray();

                Log.d("sendBytes", "Command to MAC = " + ByteArray.byteArrayToHexString(cmdToMAC));
                byte[] macToSend = dfCrypto.calcD40MAC(cmdToMAC);
                baCmdBuilder.append(cmdData).append(macToSend);
            }
        } else {
            baCmdBuilder.append(cmdData);
        }

        response = cardCommunicator.transceive(baCmdBuilder.toArray());

        DesfireResponse result = new DesfireResponse();
        result.status = findStatus(response[0]);

        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.EV2_Authenticated) {

                if (curCommMode == commMode.PLAIN) {
                    dfCrypto.EV2_CmdCtr ++;
                } else {
                    if (!dfCrypto.EV2_verifyMacResponse(response)) {
                        scrollLog.appendError("Error: CMAC Incorrect");
                        result.status = statusType.PCD_ENCRYPTION_ERROR;
                        return result;
                    } else {
                        scrollLog.appendStatus("OK: CMAC Verified");
                    }
                }

                if ((curCommMode == commMode.ENCIPHERED) && (response.length > 9)) {

                    result.data = dfCrypto.EV2_DecryptData(ByteArray.appendCutCMAC(response, 8));
                } else if (curCommMode == commMode.MAC){
                    result.data = ByteArray.appendCutCMAC(response, 8);
                } else {
                    result.data = ByteArray.appendCut(null,response);
                }

            } else if (dfCrypto.trackCMAC) {    // D41 authenticated
                if ((curCommMode == commMode.ENCIPHERED) && (cmdData == null)){  // Only when cmdData is null would there be return data
                    dfCrypto.storeAFEncrypted(response);
                    try {
                        result.data = dfCrypto.decryptReadData();
                        scrollLog.appendStatus("Response decryption/CRC check successful");
                    } catch (GeneralSecurityException e) {
                        result.status = statusType.PCD_ENCRYPTION_ERROR;
                        scrollLog.appendError(e.getMessage());
                    } finally {
                        dfCrypto.storedAFData.clear();
                        dfCrypto.encryptedLength = 0;
                    }
                } else {
                    Log.d("sendBytes  ", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                    if (!dfCrypto.verifyCMAC(response)) {
                        scrollLog.appendError("Error: CMAC Incorrect");
                    } else {
                        scrollLog.appendStatus("OK: CMAC Verified");

                        result.data = ByteArray.appendCutCMAC(response, 8);
                    }
                }
            } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
                if (response.length != 1) {
                    if (!dfCrypto.verifyD40MAC(response)) {
                        scrollLog.appendError("MAC Incorrect");
                        result.status = statusType.PCD_ENCRYPTION_ERROR;
                    } else {
                        scrollLog.appendStatus("MAC Verified");
                        result.data = ByteArray.appendCutMAC(response, 4);
                    }
                }
            }else if (curCommMode == commMode.ENCIPHERED){
                if (response.length != 1) {
                    dfCrypto.storeAFEncrypted(response);
                    try {
                        result.data = dfCrypto.decryptReadData();
                    } catch (GeneralSecurityException e) {
                        result.status = statusType.PCD_ENCRYPTION_ERROR;
                        scrollLog.appendError(e.getMessage());
                    } finally {
                        dfCrypto.storedAFData.clear();
                        dfCrypto.encryptedLength = 0;
                    }
                }
            } else {
                result.data = ByteArray.appendCut(null, response);
            }

        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (dfCrypto.trackCMAC || dfCrypto.EV2_Authenticated) {
                if (curCommMode == commMode.ENCIPHERED) {
                    Log.d ("getMoreData", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                    dfCrypto.storeAFEncrypted(response);
                }

                Log.d ("getMoreData", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);

            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }

        return result;
    }

    private statusType findStatus(byte statusCode) {
        statusType retStatusType;
        switch (statusCode) {
            case (byte)0x00:
                retStatusType = statusType.SUCCESS;
                break;
            case (byte)0x0C:
                retStatusType = statusType.NO_CHANGES;
                break;
            case (byte)0x0E:
                retStatusType = statusType.OUT_OF_EEPROM_ERROR;
                break;
            case (byte)0x1C:
                retStatusType = statusType.ILLEGAL_COMMAND_CODE;
                break;
            case (byte)0x1E:
                retStatusType = statusType.INTEGRITY_ERROR;
                break;
            case (byte)0x40:
                retStatusType = statusType.NO_SUCH_KEY;
                break;
            case (byte)0x7E:
                retStatusType = statusType.LENGTH_ERROR;
                break;
            case (byte)0x9D:
                retStatusType = statusType.PERMISSION_DENIED;
                break;
            case (byte)0x9E:
                retStatusType = statusType.PARAMETER_ERROR;
                break;
            case (byte)0xA0:
                retStatusType = statusType.APPLICATION_NOT_FOUND;
                break;
            case (byte)0xA1:
                retStatusType = statusType.APPL_INTEGRITY_ERROR;
                break;
            case (byte)0xAE:
                retStatusType = statusType.AUTHENTICATION_ERROR;
                break;
            case (byte)0xAF:
                retStatusType = statusType.ADDITONAL_FRAME;
                break;
            case (byte)0xBE:
                retStatusType = statusType.BOUNDARY_ERROR;
                break;
            case (byte)0xC1:
                retStatusType = statusType.PICC_INTEGRITY_ERROR;
                break;
            case (byte)0xCA:
                retStatusType = statusType.COMMAND_ABORTED;
                break;
            case (byte)0xCD:
                retStatusType = statusType.PICC_DISABLED_ERROR;
                break;
            case (byte)0xCE:
                retStatusType = statusType.COUNT_ERROR;
                break;
            case (byte)0xDE:
                retStatusType = statusType.DUPLICATE_ERROR;
                break;
            case (byte)0xEE:
                retStatusType = statusType.EEPROM_ERROR;
                break;
            case (byte)0xF0:
                retStatusType = statusType.FILE_NOT_FOUND;
                break;
            case (byte)0xF1:
                retStatusType = statusType.FILE_INTEGRITY_ERROR;
                break;
            default:
                retStatusType = statusType.UNKNOWN_ERROR;
                break;
        }
        return retStatusType;
    }

    public String DesFireErrorMsg (statusType status) {
        String returnString;

        switch (status) {
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
            case PCD_AUTHENTICATION_ERROR:
                returnString = "PCD authentication verification failed";
                break;
            case PCD_ENCRYPTION_ERROR:
                returnString = "PCD encryption error";
                break;
            default:
                returnString = "Unknown error";
        }
        return returnString;
    }

    public int currentAuthenticatedKey () {
        return dfCrypto.currentAuthenticatedKey;
    }

    public byte currentAuthenticationMode () {
        return dfCrypto.getAuthMode();
    }

    public statusType authenticate(byte authType, byte keyNumber, byte[] key) throws Exception {


        dfCrypto.initialize(authType, key);

        ByteArray baCmd = ByteArray.from(authType).append(keyNumber);

        if (authType==0x71) {
            baCmd.clear();
            baCmd = ByteArray.from(authType).append((byte)0x00).append((byte) 0x00);
        }
        // Send the command to the key, receive the challenge
        DesfireResponse CardChallenge = sendBytes(baCmd.toArray());

        if (CardChallenge.status != statusType.ADDITONAL_FRAME){
            Log.d("authenticate", "Exited after sending get card challenge");
            return CardChallenge.status;
        }

        // TESTEV2
        //System.arraycopy(ByteArray.hexStringToByteArray("C56F576D2444171CF64B196346A81662"), 0, CardChallenge.data, 0, 16);


        // Compute next command and required response


        byte[] challengeMessage = ByteArray.from((byte)0xAF).append(dfCrypto.computeResponseAndDataToVerify(CardChallenge.data)).toArray();



        // send AF
        DesfireResponse cardResponse = sendBytes(challengeMessage);

        // TESTEV2
        //cardResponse.status = statusType.SUCCESS;
        //cardResponse.data = ByteArray.hexStringToByteArray("EE93375DE2190A24F97D4AE363CAEC8DE2ED76DF4C3EE23C9D3499E3EC8D2259");


        if (cardResponse.status != statusType.SUCCESS){
            Log.d("authenticate", "Exited after sending challenge Message");
            return cardResponse.status;
        }

        if (!dfCrypto.verifyCardResponse(cardResponse.data)) {

            return statusType.PCD_AUTHENTICATION_ERROR;
        }
        dfCrypto.currentAuthenticatedKey = keyNumber;
        return statusType.SUCCESS;

    }

    public boolean connect() throws IOException {
        cardCommunicator.connect();
        return cardCommunicator.isConnected();
    }

    public void close() throws IOException {
        cardCommunicator.close();
    }
}
