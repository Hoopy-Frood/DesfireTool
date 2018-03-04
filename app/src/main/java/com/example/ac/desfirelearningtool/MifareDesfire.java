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





    public MifareDesfire(ICardCommunicator cardCommunicator, byte[] uid, ScrollLog tv_scrollLog) throws NoSuchAlgorithmException {
        this.cardCommunicator = cardCommunicator;
        this.uid = uid;

        this.scrollLog = tv_scrollLog;
        this.dfCrypto = new DesfireCrypto();

    }

    /**
     * Returns a byte array that represents the card version
     *
     * @throws IOException
     */
    public DesfireResponse getVersion() throws IOException {
        DesfireResponse result = sendBytes(new byte[]{(byte)0x60});

        if (result.status != statusType.ADDITONAL_FRAME)
            scrollLog.appendError("Error in card response: " + DesFireErrorMsg(result.status));
        return result;
    }

    public DesfireResponse getMoreData() throws IOException {
        return sendBytes(new byte[]{(byte)0xAF});
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

    /**
     * Returns a byte array of the card's Unique ID
     *
     * @throws IOException
     */
    public DesfireResponse getCardUID() throws Exception {
        DesfireResponse res = sendBytes(new byte[]{(byte)0x51});
        
        if (res.status != statusType.SUCCESS ) {
            return res;
        }

        res.data = dfCrypto.decrypt(res.data);
        return res;
    }

    /**
     * Returns Free Memory
     *
     * @throws IOException
     */
    public DesfireResponse getFreeMem() throws IOException {
        return sendBytes(new byte[]{(byte)0x6E});
    }

    /**
     * Returns Key Settings
     *
     * @throws IOException
     */
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
        byte[] params = ByteArray.from((byte) 0x5a).append(applicationId).toArray();
        DesfireResponse res = sendBytes(params);


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

    protected statusType deleteApplication(byte[] applicationId) throws IOException {
        byte[] params = ByteArray.from((byte) 0xDA).append(applicationId).toArray();
        DesfireResponse res = sendBytes(params);

        return res.status;
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



    public statusType createValueFile(byte bFileType, byte bFileID, byte bCommSetting, byte [] baAccessRights, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) throws IOException {
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


    public byte[] readRecordFile(byte fid, int start, int count, commMode curCommMode) throws IOException {
        byte[] cmd = new ByteArray().append((byte)0xBB).append(fid).append(start, 3).append(count, 3).toArray();
        DesfireResponse result = sendBytes(cmd);
        return result.data;
    }

    public DesfireResponse  readData(byte fid, int start, int count) throws IOException {
        return readData(fid,start,count,commMode.PLAIN);
    }


    public DesfireResponse getMoreData(commMode curCommMode) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmd = new byte[] {(byte)0xAF};


        byte[] response = cardCommunicator.transceive(cmd);

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);

        if (result.status == statusType.SUCCESS) {
            if (curCommMode == commMode.ENCIPHERED) {

                dfCrypto.storeAFEncrypted(response);
                try {
                    result.data = dfCrypto.decryptReadData();
                } catch (GeneralSecurityException e) {
                    scrollLog.appendError(e.getMessage());
                }

            } else if (dfCrypto.trackCMAC) {
                Log.d("getMoreData", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verified");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
                if (dfCrypto.verifyD40MAC(response)) {
                    scrollLog.appendStatus("MAC Verified");
                } else {
                    scrollLog.appendError("MAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,4);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (curCommMode == commMode.ENCIPHERED) {
                Log.d ("getMoreData", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFEncrypted(response);
            } else if (dfCrypto.trackCMAC) {
                Log.d ("getMoreData", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);
            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }

        return result;

    }

    public DesfireResponse  readData(byte fid, int start, int count, commMode curCommMode) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmd = array.append((byte) 0xBD).append(fid).append(start, 3).append(count, 3).toArray();

        if ((dfCrypto.trackCMAC)) {
            Log.d("readData", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmd));
            dfCrypto.calcCMAC(cmd);
        }

        byte[] response = cardCommunicator.transceive(cmd);

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);

        if (result.status == statusType.SUCCESS) {
            if (curCommMode == commMode.ENCIPHERED) {
                dfCrypto.storeAFEncryptedSetLength(response, count);
                try {
                    result.data = dfCrypto.decryptReadData();
                } catch (GeneralSecurityException e) {
                    scrollLog.appendError(e.getMessage());
                }

            } else if (dfCrypto.trackCMAC) {
                Log.d("readData", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verified");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
                if (dfCrypto.verifyD40MAC(response)) {
                    scrollLog.appendStatus("MAC Verified");
                } else {
                    scrollLog.appendError("MAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,4);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (curCommMode == commMode.ENCIPHERED) {
                Log.d ("readData", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFEncryptedSetLength(response,count);
            } else if ((dfCrypto.trackCMAC) || (curCommMode == commMode.MAC)) {
                Log.d ("readData", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);
            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }
        return result;
    }

    public DesfireResponse  readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode) throws IOException {
        ByteArray array = new ByteArray();
        byte[] cmd = array.append((byte) 0xBB).append(fid).append(offsetRecord, 3).append(numOfRecords, 3).toArray();


        if ((dfCrypto.trackCMAC)) {
            Log.d ("readData", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmd) );
            dfCrypto.calcCMAC(cmd);
        }

        byte[] response = cardCommunicator.transceive(cmd);

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);

        if (result.status == statusType.SUCCESS) {
            if (curCommMode == commMode.ENCIPHERED) {
                dfCrypto.storeAFEncrypted(response);
                try {
                    result.data = dfCrypto.decryptReadData();   // Decrypt all data together
                } catch (GeneralSecurityException e) {
                    scrollLog.appendError(e.getMessage());
                }

            } else if (dfCrypto.trackCMAC) {
                Log.d("readRecord", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verified");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
                if (dfCrypto.verifyD40MAC(response)) {
                    scrollLog.appendStatus("MAC Verified");
                } else {
                    scrollLog.appendError("MAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,4);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (curCommMode == commMode.ENCIPHERED) {
                Log.d ("readRecord", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFEncrypted(response);
            } else if ((dfCrypto.trackCMAC) || (curCommMode == commMode.MAC)) {
                Log.d ("readRecord", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);
            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }
        return result;

    }


    public DesfireResponse writeData(byte fid, int start, int count, byte [] dataToWrite, commMode curCommMode) throws IOException {

        byte [] macToSend;
        ByteArray baCmdToSend = new ByteArray();
        baCmdToSend.append((byte) 0x3D).append(fid).append(start, 3).append(count, 3);

        if (curCommMode == commMode.ENCIPHERED) {
            try {
                byte [] encipheredData = dfCrypto.encryptWriteDataBlock(baCmdToSend.toArray(), dataToWrite);

                baCmdToSend.append(encipheredData);
            } catch (GeneralSecurityException e) {
                scrollLog.appendError(e.getMessage());
                DesfireResponse badResult = new DesfireResponse();

                badResult.status = statusType.PCD_ENCRYPTION_ERROR;
                badResult.data = null;
                return badResult;
            }

        } else if (dfCrypto.trackCMAC) {
            ByteArray arrayMAC = new ByteArray();
            byte[] cmdToCMAC = arrayMAC.append((byte) 0x3D).append(fid).append(start, 3).append(count, 3).append(dataToWrite).toArray();

            Log.d ("writeData", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmdToCMAC) );
            macToSend = dfCrypto.calcCMAC(cmdToCMAC);
            if (curCommMode == commMode.MAC) {
                baCmdToSend.append(dataToWrite).append(macToSend);
            }
        } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)){
            ByteArray arrayMAC = new ByteArray();
            byte[] cmdToMAC = arrayMAC.append(dataToWrite).toArray();

            Log.d ("writeData", "Command to MAC = " + ByteArray.byteArrayToHexString(cmdToMAC) );
            macToSend = dfCrypto.calcD40MAC(cmdToMAC);
            baCmdToSend.append(dataToWrite).append(macToSend);

        } else { // if (curCommMode == commMode.PLAIN){
            baCmdToSend.append(dataToWrite);
        }

        Log.d("writeData","Command to send: " + ByteArray.byteArrayToHexString(baCmdToSend.toArray()));

        byte[] response = cardCommunicator.transceive(baCmdToSend.toArray());


        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);


        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.trackCMAC) {
                Log.d("writeData", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verified");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (curCommMode == commMode.ENCIPHERED) { //TODO: This is wrong AF not handled in writing situations yet
                Log.d ("writeData", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFEncryptedSetLength(response,count);
            } else if ((dfCrypto.trackCMAC) || (curCommMode == commMode.MAC)) {
                Log.d ("writeData", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);
            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }
        return result;
    }


    public DesfireResponse writeRecord(byte fid, int startRecord, int sizeToWrite, byte [] dataToWrite, commMode curCommMode) throws IOException {

        byte [] macToSend;
        ByteArray baCmdToSend = new ByteArray();
        baCmdToSend.append((byte) 0x3B).append(fid).append(startRecord, 3).append(sizeToWrite, 3);

        if (curCommMode == commMode.ENCIPHERED) {
            try {
                byte [] encipheredData = dfCrypto.encryptWriteDataBlock(baCmdToSend.toArray(), dataToWrite);

                baCmdToSend.append(encipheredData);
            } catch (GeneralSecurityException e) {
                scrollLog.appendError(e.getMessage());
                DesfireResponse badResult = new DesfireResponse();

                badResult.status = statusType.PCD_ENCRYPTION_ERROR;
                badResult.data = null;
                return badResult;
            }

        } else if (dfCrypto.trackCMAC) {
            ByteArray arrayMAC = new ByteArray();
            byte[] cmdToCMAC = arrayMAC.append((byte) 0x3B).append(fid).append(startRecord, 3).append(sizeToWrite, 3).append(dataToWrite).toArray();

            Log.d ("writeData", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmdToCMAC) );
            macToSend = dfCrypto.calcCMAC(cmdToCMAC);
            if (curCommMode == commMode.MAC) {
                baCmdToSend.append(dataToWrite).append(macToSend);
            }
        } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)){
            ByteArray arrayMAC = new ByteArray();
            byte[] cmdToMAC = arrayMAC.append(dataToWrite).toArray();

            Log.d ("writeData", "Command to MAC = " + ByteArray.byteArrayToHexString(cmdToMAC) );
            macToSend = dfCrypto.calcD40MAC(cmdToMAC);
            baCmdToSend.append(dataToWrite).append(macToSend);

        } else { // if (curCommMode == commMode.PLAIN){
            baCmdToSend.append(dataToWrite);
        }

        Log.d("writeData","Command to send: " + ByteArray.byteArrayToHexString(baCmdToSend.toArray()));

        byte[] response = cardCommunicator.transceive(baCmdToSend.toArray());


        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);


        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.trackCMAC) {
                Log.d("writeData", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verified");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
                if (dfCrypto.verifyD40MAC(response)) {
                    scrollLog.appendStatus("MAC Verified");
                } else {
                    scrollLog.appendError("MAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,4);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (curCommMode == commMode.ENCIPHERED) {//TODO: This is wrong AF not handled in writing situations yet
                Log.d ("readData", "Response AF - Store Hex Str for CRC:  " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFEncrypted(response);
            } else if ((dfCrypto.trackCMAC) || (curCommMode == commMode.MAC)) {
                Log.d ("readData", "Response AF - Store Hex Str for CMAC: " + ByteArray.byteArrayToHexString(response) );
                dfCrypto.storeAFCMAC(response);
            }
            result.data = ByteArray.appendCut(null, response);
        } else {
            dfCrypto.trackCMAC = false;
        }
        return result;
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

    public DesfireResponse clearRecordFile(byte fid) throws IOException {
        return sendBytes(new byte[]{(byte)0xEB, fid});
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

        if ((dfCrypto.trackCMAC) && (cmd[0] != (byte) 0xAF)) {
            Log.d ("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmd) );
            dfCrypto.calcCMAC(cmd);
        }


        byte[] response = cardCommunicator.transceive(cmd);

        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);
        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.trackCMAC) {
                Log.d("sendBytes  ", "Response to verify CMAC = " + ByteArray.byteArrayToHexString(response));
                if (dfCrypto.verifyCMAC(response)) {
                    scrollLog.appendStatus("CMAC Verfied");
                } else {
                    scrollLog.appendError("CMAC Incorrect");
                }
                result.data = ByteArray.appendCutMAC(response,8);
            } else {
                result.data = ByteArray.appendCut(null, response);
            }
        } else if (result.status == statusType.ADDITONAL_FRAME) {
            if (dfCrypto.trackCMAC) {
                Log.d ("sendBytes  ", "Response to verify CMAC AF = " + ByteArray.byteArrayToHexString(response) );
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
            default:
                returnString = "Unknown error";
        }
        return returnString;
    }

    DesfireCrypto dfCrypto;

    public int currentAuthenticatedKey () {
        return dfCrypto.currentAuthenticatedKey;
    }


    public statusType authenticate(byte authType, byte keyNumber, byte[] key) throws Exception {


        dfCrypto.initialize(authType, key);

        byte[] cmd = ByteArray.from(authType).append(keyNumber).toArray();
        // Send the command to the key, receive the challenge
        DesfireResponse CardChallenge = sendBytes(cmd);

        if (CardChallenge.status != statusType.ADDITONAL_FRAME){
            Log.d("authenticate", "Exitied after sending get card challenge");
            return CardChallenge.status;
        }



        // Compute next command and required response


        byte[] challengeMessage = ByteArray.from((byte)0xAF).append(dfCrypto.computeResponseAndDataToVerify(CardChallenge.data)).toArray();

        // send AF
        DesfireResponse cardResponse = sendBytes(challengeMessage);
        if (cardResponse.status != statusType.SUCCESS){
            Log.d("authenicate", "Exitied after sending challengMessage");
            return cardResponse.status;
        }

        if (dfCrypto.verifyCardResponse(cardResponse.data)) {
            dfCrypto.currentAuthenticatedKey = keyNumber;
            return statusType.SUCCESS;
        }

        return statusType.PCD_AUTHENTICATION_ERROR;

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
