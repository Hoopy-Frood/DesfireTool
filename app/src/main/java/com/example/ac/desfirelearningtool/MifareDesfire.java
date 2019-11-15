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

    private ICardCommunicator cardCommunicator;

    private byte[] uid;
    private ScrollLog scrollLog;
    DesfireCrypto dfCrypto;

    public class ISOResponse {
        public byte[] data;
        public statusWord status;
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

    public MifareDesfire(ICardCommunicator cardCommunicator, byte[] uid, ScrollLog tv_scrollLog) throws NoSuchAlgorithmException {
        this.cardCommunicator = cardCommunicator;
        this.uid = uid;

        this.scrollLog = tv_scrollLog;
        this.dfCrypto = new DesfireCrypto();

    }

    public DesfireResponse getVersion() throws IOException {
        DesfireResponse result = sendBytes((byte)0x60, commMode.MAC);

        if (result.status != statusType.ADDITONAL_FRAME)
            scrollLog.appendError("Error in card response: " + DesFireErrorMsg(result.status));
        return result;
    }

    public  DesfireResponse getDFNames() throws IOException {
        return sendBytes((byte)0x6D, commMode.MAC);
    }

    public DesfireResponse getFileIds() throws IOException {
        return sendBytes((byte)0x6F, commMode.MAC);
    }

    public DesfireResponse getIsoFileIds() throws IOException {
        return sendBytes((byte)0x61, commMode.MAC);
    }

    public DesfireResponse getCardUID() throws Exception {
        dfCrypto.encryptedLength = 7;
        return sendBytes((byte)0x51, commMode.ENCIPHERED);
    }

    public DesfireResponse getFreeMem() throws IOException {
        return sendBytes((byte)0x6E, commMode.MAC);
    }

    public DesfireResponse getKeySettings() throws IOException {
        return sendBytes((byte)0x45, commMode.MAC);
    }

    public DesfireResponse getKeyVersion(byte selectedKey) throws IOException {
        return sendBytes((byte)0x64, new byte[] {selectedKey}, null, commMode.MAC);
    }

    public DesfireResponse getFileSettings(byte fid) throws IOException {
        return sendBytes((byte)0xf5, new byte[] {fid}, null, commMode.MAC);
    }

    public int getRecordLength(byte fid) throws IOException {
        DesfireResponse resp = sendBytes((byte)0xf5, new byte[] {fid}, null, commMode.MAC);
        int recordLength = -1;

        if (resp.status == statusType.SUCCESS) {
            recordLength = ByteBuffer.wrap(resp.data, 4,3).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        return recordLength;
    }

    public DesfireResponse getApplicationIDs() throws IOException {
        ByteArrayOutputStream appIDs = new ByteArrayOutputStream();
        DesfireResponse result = sendBytes((byte)0x6a, commMode.MAC);

        if (result.status != MifareDesfire.statusType.SUCCESS) {
            return result;
        }

        appIDs.write(result.data);

        if (result.status == statusType.ADDITONAL_FRAME) {
            result = sendBytes((byte)0xAF, commMode.MAC);
            appIDs.write(result.data);
        }

        result.data = appIDs.toByteArray();
        return result;
    }

    public statusType selectApplication(byte[] applicationId) throws IOException {
        dfCrypto.reset();

        return sendBytes((byte)0x5a, applicationId, null, commMode.MAC).status;
    }

    public ISOResponse selectIsoFileId(byte[] isoFileId) throws IOException {
        dfCrypto.reset();

        return ISOSendBytes((byte)0xA4, (byte)0x00, (byte) 0x00, isoFileId, (byte) 0x00);
    }


    public statusType createApplication(byte [] appId, byte bKeySetting1, byte bKeySetting2, byte [] bISOName, byte [] bDFName) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateApplicationArray = new ByteArray();
        baCreateApplicationArray.append(appId).append(bKeySetting1).append(bKeySetting2);
        if (bISOName.length == 2) {
            baCreateApplicationArray.append(bISOName);
        }
        baCreateApplicationArray.append(bDFName);

        Log.v("createApplication", "Command for Create Application  File  : " + ByteArray.byteArrayToHexString(baCreateApplicationArray.toArray()));

        return sendBytes((byte) 0xCA, baCreateApplicationArray.toArray(), null, commMode.MAC).status;
    }

    public statusType deleteApplication(byte[] applicationId) throws IOException {
        return sendBytes((byte)0xDA, applicationId, null, commMode.MAC).status;
    }

    public statusType createDataFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileID);

        if (baISOName.length == 2) {
            baCreateDataFileArray.append(baISOName);
        }
        baCreateDataFileArray.append(bCommSetting).append(baAccessRights).append(iFileSize, 3);

        Log.v("createDataFile", "Command for Create Data File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));

        return sendBytes(bFileType, baCreateDataFileArray.toArray(), null, commMode.MAC).status;
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
        baCreateDataFileArray.append(bFileID);
        if (baISOName.length == 2) {
            baCreateDataFileArray.append(baISOName);
        }
        baCreateDataFileArray.append(bCommSetting).append(baAccessRights).append(iRecordSize,3).append(iNumOfRecords,3);

        Log.v("createRecordFile", "Command Header for Create Record File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));
        return sendBytes(bFileType, baCreateDataFileArray.toArray(), null, commMode.MAC).status;
    }

    public statusType createValueFile(byte bFileID, byte bCommSetting, byte [] baAccessRights, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileID);

        baCreateDataFileArray.append(bCommSetting).append(baAccessRights).append(iLowerLimit,4).append(iUpperLimit,4).append(iValue,4).append(bOptionByte);

        Log.v("createRecordFile", "Command for Create Value File  : " + ByteArray.byteArrayToHexString(baCreateDataFileArray.toArray()));
        DesfireResponse res = sendBytes((byte) 0xCC, baCreateDataFileArray.toArray(), null, commMode.MAC);

        return res.status;
    }

    public DesfireResponse deleteFile(byte fid) throws IOException {
        return sendBytes((byte)0xDF, new byte[] {fid}, null, commMode.MAC);
    }

    public statusType formatPICC() throws IOException {
        return sendBytes((byte) 0xFC, commMode.MAC).status;
    }

    public statusType changeKey (byte bKeyToChange, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey) throws GeneralSecurityException, IOException{
        ByteArray keyBlockBuilder = new ByteArray();
        ByteArray commandBuilder = new ByteArray();
        // Ensure it is currently authenticated

        // Case 1
        // If oldKey != null, AND bKeyToChange != currAuthKey
        if (bKeyToChange != dfCrypto.currentAuthenticatedKey) {
            Log.d("changeKey","Change Key Case 1 - Different KeyNo");
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

            byte[] computedCrcXorKey;

            if (dfCrypto.CRCLength == 4) {
                ByteArray baDataToCRC = new ByteArray();
                baDataToCRC.append(commandBuilder.toArray()).append(keyBlockBuilder.toArray()); //
                Log.d("changeKey", "CRC32 Input of XOR = " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));
                computedCrcXorKey = dfCrypto.calcCRC(keyBlockBuilder.toArray());
            } else {
                Log.d("changeKey", "CRC16 Input of XOR= " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));
                computedCrcXorKey = dfCrypto.calcCRC(keyBlockBuilder.toArray());
            }

            keyBlockBuilder.append(computedCrcXorKey);
            keyBlockBuilder.append(dfCrypto.calcCRC(baNewKey));
            Log.d("changeKey", "Case 1 encryption Input = " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));

            commandBuilder.append(dfCrypto.encryptDataBlock (keyBlockBuilder.toArray()));

        } else {
            Log.d("changeKey","Change Key Case 2 - Same KeyNo");
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

        if (result.status == statusType.SUCCESS) {
            if ((dfCrypto.trackCMAC) && (response.length > 1)) {
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

        if (bKeyToChange == dfCrypto.currentAuthenticatedKey)
            dfCrypto.reset();

        return result.status;

    }

    public DesfireResponse getMoreData() throws IOException {
        return sendBytes((byte)0xAF, null, null, commMode.PLAIN);
    }

    public DesfireResponse getMoreData(commMode curCommMode) throws IOException {
        return sendBytes((byte)0xAF, null, null, curCommMode);
    }

    public DesfireResponse readData(byte fid, int start, int count, commMode curCommMode) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmdHeader = array.append(fid).append(start, 3).append(count, 3).toArray();

        dfCrypto.setAFLength(count);  //if count is zero, whole data file is read
        return sendBytes((byte) 0xBD, cmdHeader, null, curCommMode);
    }

    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode) throws IOException {
        return readRecords(fid, offsetRecord, numOfRecords,curCommMode,0);
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
        return sendBytes((byte) 0xEB, new byte[] {fid}, null, commMode.MAC);
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
        return sendBytes((byte) 0xC7, commMode.MAC);
    }

    public DesfireResponse abortTransaction() throws IOException {
        return sendBytes((byte) 0xA7, commMode.MAC);
    }



    public DesfireResponse sendBytes (byte cmd, commMode expectedCommMode) throws IOException {
        return sendBytes(cmd, null, null, expectedCommMode);
    }

    private DesfireResponse encryptionError () {
        DesfireResponse badResult = new DesfireResponse();

        dfCrypto.reset();
        badResult.status = statusType.PCD_ENCRYPTION_ERROR;
        badResult.data = null;
        return badResult;
    }
    /**
     * sendBytes handles all DESFire D40, EV1, EV2 secure messaging and sending/receiving of data
     * @param cmd cmd byte as specified in data sheet
     * @param cmdHeader command Header
     * @param cmdData data to send
     * @param curCommMode communicaiton mode - PLAIN, MAC, ENCIPHERED
     * @return DESFireResponse status byte and data if any
     * @throws IOException Lost connection of smart card reader
     */
    private DesfireResponse sendBytes(byte cmd, byte[] cmdHeader, byte [] cmdData, commMode curCommMode) throws IOException {
        byte[] response;
        byte [] cmdToSend;
        ByteArray baCmdBuilder = new ByteArray();


        baCmdBuilder.append(cmd).append(cmdHeader);
        if ((dfCrypto.EV2_Authenticated)) {

            if ((curCommMode == commMode.ENCIPHERED) && (cmdData != null)) {


                byte [] encryptData = dfCrypto.EV2_EncryptData(cmdData);
                if (encryptData == null) {
                    Log.d("sendBytes", "Command Encrypt error ");
                    scrollLog.appendError("Encrypt CmdData Error");
                    return encryptionError();
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
                Log.d("sendBytes", "Command Encrypt error ");
                scrollLog.appendError("Encrypt CmdData Error");

                scrollLog.appendError(e.getMessage());
                return encryptionError();
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
                        return encryptionError();
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
                if ((response.length != 1) && ((cmd == (byte) 0xBD) || (cmd == (byte) 0x6C) || (cmd == (byte) 0xBB))) {  // D40 MAC only applied to data manipulation response data
                    if (!dfCrypto.verifyD40MAC(response)) {
                        scrollLog.appendError("MAC Incorrect");
                        result.status = statusType.PCD_ENCRYPTION_ERROR;
                    } else {
                        scrollLog.appendStatus("MAC Verified");
                        result.data = ByteArray.appendCutMAC(response, 4);
                    }
                } else {
                    result.data = ByteArray.appendCut(null, response);
                }
            }else if (curCommMode == commMode.ENCIPHERED){  // D40 encrypted
                if (response.length != 1) {
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
            dfCrypto.reset();
        }

        return result;
    }

    //region isoSendBytes
    private ISOResponse ISOSendBytes(byte INS, byte P1, byte P2, byte[] cmdData, byte Le) throws IOException {
        return ISOSendBytes(INS,P1,P2,cmdData,Le,false);
    }

    private ISOResponse ISOSendBytes(byte INS, byte P1, byte P2, byte[] cmdData, byte Le, boolean sendLe) throws IOException {
        ByteArray baCmdBuilder = new ByteArray();
        baCmdBuilder.append((byte) 0x00).append(INS).append(P1).append(P2);
        ISOResponse resp = new ISOResponse();

        if (cmdData == null) {
            //baCmdBuilder.append((byte) 0x00);
        } else if (cmdData.length < 255) {
            baCmdBuilder.append(cmdData.length, 1).append(cmdData);
        } else {
            return errorISOResponse ();
        }
        if ((cmdData == null) || (sendLe)){
            baCmdBuilder.append(Le);
        }

        byte [] response = null;
        try {
            response = cardCommunicator.transceiveISO(baCmdBuilder.toArray());
        } catch (IOException e) {
            Log.e("sendBytes", "Card communication problem");
            return errorISOResponse ();
        }

        if (response.length >= 2) {
            short sStatus = (short) (response[response.length-2] + (response[response.length-1] << 8));
            resp.status = findStatus(sStatus);
            resp.data = new byte[response.length-2];
            System.arraycopy(response, 0, resp.data, 0, response.length - 2);
        } else {
            return errorISOResponse ();
        }
        return resp;

    }

    private ISOResponse errorISOResponse () {
        ISOResponse resp = new ISOResponse();
        resp.status = findStatus( (short) 0x6F00);
        resp.data = null;
        return resp;
    }

    //endregion

    //region Mifare Status
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
    //endregion


    //region Mifare Status Word
    public enum statusWord {
        SUCCESS,                    // 9000
        TMC_LIMIT_REACHED,          // 6283
        WRONG_ADPU_LENGTH,          // 6700
        WRAPPED_COMMNAD__ONGOING,   // 6985
        FILE_NOT_FOUND,             // 6A82
        WRONG_P1P2,                 // 6A86
        WRONG_LC,                   // 6A87
        WRONG_CLA,                  // 6E00

        MEMORY_FAILURE,             // 6581
        SECURITY_STATUS_NOT_SATISIFED, // 6982

        READER_ISSUE,               // 0x6F00
        UNKNOWN_ERROR               //
    }

    private statusWord findStatus(short sStatus) {
        statusWord retStatusWord;
        switch (sStatus) {
            case (short) 0x9000:
                retStatusWord = statusWord.SUCCESS;
                break;
            case (short) 0x6283:
                retStatusWord = statusWord.TMC_LIMIT_REACHED;
                break;
            case (short) 0x6700:
                retStatusWord = statusWord.WRONG_ADPU_LENGTH;
                break;
            case (short) 0x6985:
                retStatusWord = statusWord.WRAPPED_COMMNAD__ONGOING;
                break;
            case (short) 0x6A82:
                retStatusWord = statusWord.FILE_NOT_FOUND;
                break;
            case (short) 0x6A86:
                retStatusWord = statusWord.WRONG_P1P2;
                break;
            case (short) 0x6A87:
                retStatusWord = statusWord.WRONG_LC;
                break;
            case (short) 0x6E00:
                retStatusWord = statusWord.WRONG_CLA;
                break;
            case (short) 0x6581:
                retStatusWord = statusWord.MEMORY_FAILURE;
                break;
            case (short) 0x6982:
                retStatusWord = statusWord.SECURITY_STATUS_NOT_SATISIFED;
                break;
            case (short) 0x6F00:
                retStatusWord = statusWord.READER_ISSUE;
                break;
            default:
                retStatusWord = statusWord.UNKNOWN_ERROR;
                break;
        }
        return retStatusWord;
    }

    public String DesFireErrorMsg (statusWord swStatus) {
        String returnString;

        switch (swStatus) {
            case SUCCESS:
                returnString = "Command OK";
                break;
            case TMC_LIMIT_REACHED:
                returnString = "TMCLimit / sesTMC maximum limit reached";
                break;
            case WRONG_ADPU_LENGTH:
                returnString = "Wrong or inconsistent APDU Length";
                break;
            case WRAPPED_COMMNAD__ONGOING:
                returnString = "Wrapped chained command or multiple pass command ongoing";
                break;
            case FILE_NOT_FOUND:
                returnString = "Application or file not found, currently selected application remains selected";
                break;
            case WRONG_P1P2:
                returnString = "Wrong parameter P1 and/or P2";
                break;
            case WRONG_LC:
                returnString = "Wrong parameter Lc inconsistent with P1-P2";
                break;
            case WRONG_CLA:
                returnString = "Wong CLA";
                break;
            case MEMORY_FAILURE:
                returnString = "Memory failure";
                break;
            case SECURITY_STATUS_NOT_SATISIFED:
                returnString = "Seucrity status not satisfied";
                break;
            case READER_ISSUE:
                returnString = "Reader or connection issues";
                break;
            default:
                returnString = "Unknown error";
        }
        return returnString;
    }
    //endregion


    public int currentAuthenticatedKey () {
        return dfCrypto.currentAuthenticatedKey;
    }

    public byte currentAuthenticationMode () {
        return dfCrypto.getAuthMode();
    }

    public statusType authenticate(byte authType, byte keyNumber, byte[] key) throws Exception {


        dfCrypto.initialize(authType, key);

        ByteArray baCmd = ByteArray.from(keyNumber);

        if (authType==0x71) {
            baCmd.append((byte)0x00);
        }
        // Send the command to the key, receive the challenge
        DesfireResponse CardChallenge = sendBytes(authType,baCmd.toArray(),null,commMode.PLAIN);

        if (CardChallenge.status != statusType.ADDITONAL_FRAME){
            Log.d("authenticate", "Exited after sending get card challenge");
            return CardChallenge.status;
        }

        // Compute next command and required response
        byte[] challengeMessage = dfCrypto.computeResponseAndDataToVerify(CardChallenge.data);



        // send AF
        DesfireResponse cardResponse = sendBytes((byte)0xAF, challengeMessage, null, commMode.PLAIN);

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
