package com.example.ac.desfirelearningtool;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.security.NoSuchAlgorithmException;



/**
 * A class to wrap Mifare Desfire commands, using a generic "Communicator"
 *
 * Commands and parameters from libfreefare (https://github.com/nfc-tools)
 */
public class Desfire {

    private final int macSize = 4;
    private final int maxDataSize = 52 - macSize;

    private ICardCommunicator cardCommunicator;

    private byte[] uid;
    private ScrollLog scrollLog;
    DesfireCrypto dfCrypto;

    public enum statusType {
        SUCCESS,
        NO_CHANGES,
        OUT_OF_EEPROM_ERROR,
        ILLEGAL_COMMAND_CODE,
        INTEGRITY_ERROR,
        NO_SUCH_KEY,
        LENGTH_ERROR,
        WRONG_LENGTH,
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
        PCD_PARAMETER_ERROR,
        UNKNOWN_ERROR
    }

    public class DesfireResponse {
        public byte[] data;
        public statusType status;
    }

    public class ISOResponse {
        public byte[] data;
        public statusWord status;
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

    public Desfire(ICardCommunicator cardCommunicator, byte[] uid, ScrollLog tv_scrollLog) {
        this.cardCommunicator = cardCommunicator;
        this.uid = uid;

        this.scrollLog = tv_scrollLog;
        this.dfCrypto = new DesfireCrypto();

    }

    /**
     * Returns manufacturing related data of the PICC. First part returns HW related information as
     * specified in CardVersionList Table.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getVersion() throws IOException {
        return sendBytes((byte) 0x60, commMode.MAC);
    }

    /**
     * Returns the the Application IDentifiers together with a File ID and (optionally) a DF Name of all
     * active applications with ISO/IEC 7816-4 support.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getDFNames() throws IOException {
        return sendBytes((byte) 0x6D, commMode.MAC);
    }

    /**
     * Returns the File IDentifiers of all active files within the currently selected application.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getFileIds() throws IOException {
        return sendBytes((byte) 0x6F, commMode.MAC);
    }

    /**
     * Get back the ISO File IDs.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getIsoFileIds() throws IOException {
        return sendBytes((byte) 0x61, commMode.MAC);
    }

    /**
     * Returns the UID.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getCardUID() throws Exception {
        dfCrypto.encryptedLength = 7;
        return sendBytes((byte) 0x51, commMode.ENCIPHERED);
    }

    /**
     * Returns the free memory available on the card.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getFreeMem() throws IOException {
        return sendBytes((byte) 0x6E, commMode.MAC);
    }

    /**
     * Depending on the currently selected AID, this command retrieves the PICCKeySettings of the
     * PICC or the AppKeySettings of the (primary) application. In
     * addition it returns the number of keys which are configured
     * for the selected application and if applicable the AppKeySetSettings.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getKeySettings() throws IOException {
        return sendBytes((byte) 0x45, commMode.MAC);
    }

    /**
     * Depending on the currently selected AID and given key number parameter, return key version of
     * the key targeted or return all key set versions of the selected application.
     * @param selectedKey Key number of the targeted key
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getKeyVersion(byte selectedKey) throws IOException {
        return sendBytes((byte) 0x64, new byte[]{selectedKey}, null, commMode.MAC);
    }

    /**
     * Depending on the currently selected AID and given key number parameter, return key version of
     *      * the key targeted or return all key set versions of the selected application.
     * @param selectedKey Key number of the targeted key
     * @param keySetNum Key set number
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getKeyVersion(byte selectedKey, byte keySetNum) throws IOException {
        return sendBytes((byte) 0x64, new byte[]{selectedKey, keySetNum}, null, commMode.MAC);
    }

    /**
     * getKeyVersion with selected key input as int
     * @param selectedKey Key number of the targeted key as int
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getKeyVersion(int selectedKey) throws IOException {
        return getKeyVersion(ByteArray.fromInt(selectedKey, 1)[0]);
    }

    /**
     * Get information on the properties of a specific file.
     * @param fid File number of the targeted file.
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getFileSettings(byte fid) throws IOException {
        return sendBytes((byte) 0xF5, new byte[]{fid}, null, commMode.MAC);
    }

    /**
     * Get information on the properties of a specific file.
     * @param fid File number of the targeted file as integer
     * @return DesfireResponse with status and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getFileSettings(int fid) throws IOException {
        return getFileSettings(ByteArray.fromInt(fid, 1)[0]);
    }

    /**
     * Change File Settings of a specific file id with adding number of access right sets
     * @param fid File number of the targeted file
     * @param fileOption File option of having additional ARs to follow [b7] and of CommMode [b1-0]
     * @param accessRights 2 byte access mode
     * @param NrAddARs  if fileOption b7 == 1, number of additional access conditions sets
     * @param AddAccessRights Additional AR sets (2bytes x NrAddARs)
     * @return Status response
     * @throws IOException Thrown if problem with the smart card connection
     */
    public statusType changeFileSettings(byte fid, byte fileOption, byte [] accessRights, byte NrAddARs, byte [] AddAccessRights) throws IOException {
        ByteArray cmdData = new ByteArray();
        cmdData.append(fileOption).append(accessRights);

        if ((fileOption & 0x80) == 0x80) {
            cmdData.append(NrAddARs).append(AddAccessRights);
        }
        Log.d("changeFileSettings", "cmd Data= " + cmdData.toHexString());

        return sendBytes((byte) 0x5F, new byte[]{fid}, cmdData.toArray(), commMode.ENCIPHERED).status;
    }

    /**
     * Change File Settings of a specific file id
     * @param fid File number of the targeted file
     * @param fileOption File option of having additional ARs to follow [b7] and of CommMode [b1-0]
     * @param accessRights 2 byte access mode
     * @return Status response
     * @throws IOException Thrown if problem with the smart card connection
     */
    public statusType changeFileSettings(byte fid, byte fileOption, byte [] accessRights) throws IOException {
        return changeFileSettings(fid,fileOption,accessRights,(byte) 0,null);
    }


    /**
     * Get record length by calling getFileSettings.
     * @param fid File number of the targeted record file.
     * @return record length as integer
     * @throws IOException Thrown if problem with the smart card connection
     */
    public int getRecordLength(byte fid) throws IOException {
        DesfireResponse resp = sendBytes((byte) 0xf5, new byte[]{fid}, null, commMode.MAC);
        int recordLength = -1;

        if (resp.status == statusType.SUCCESS) {
            recordLength = ByteBuffer.wrap(resp.data, 4, 3).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        return recordLength;
    }

    /**
     * Get record length by calling getFileSettings.
     * @param fid File number of the targeted record file as integer.
     * @return record length as integer
     * @throws IOException Thrown if problem with the smart card connection
     */
    public int getRecordLength(int fid) throws IOException {
        return getRecordLength(ByteArray.fromInt(fid, 1)[0]);
    }

    /**
     * Returns the Application IDentifiers of all active applications on a PICC.
     * @return AIDList in 3 byte sets
     * @throws IOException Thrown if problem with the smart card connection
     */
    public DesfireResponse getApplicationIDs() throws IOException {
        ByteArrayOutputStream appIDs = new ByteArrayOutputStream();
        DesfireResponse result = sendBytes((byte) 0x6a, commMode.MAC);

        if (result.status != statusType.SUCCESS) {
            return result;
        }

        appIDs.write(result.data);

        if (result.status == statusType.ADDITONAL_FRAME) {
            result = sendBytes((byte) 0xAF, commMode.MAC);
            appIDs.write(result.data);
        }

        result.data = appIDs.toByteArray();
        return result;
    }

    public DesfireResponse setConfifguration(byte option, byte [] data) throws IOException {
        return sendBytes((byte) 0x5C, new byte[] {option}, data, commMode.ENCIPHERED);
    }

    /**
     * Select 1 or 2 applications or the PICC level specified by their application identifier.
     * @param applicationId 1 or 2 AIDs - 3 or 6 bytes
     * @return statusType
     * @throws IOException Thrown if problem with the smart card connection
     */
    public statusType selectApplication(byte[] applicationId) throws IOException {
        dfCrypto.reset();
        return sendBytes((byte) 0x5a, applicationId, null, commMode.PLAIN).status;
    }

    public ISOResponse selectIsoFileId(byte[] isoFileId) throws IOException {
        dfCrypto.reset();

        return ISOSendBytes((byte)0xA4, (byte)0x00, (byte) 0x00, isoFileId, (byte) 0x00);
    }


    /**
     * Creates new applications on the PICC. The application is initialized according to the given
     * settings. The application keys of the active key set are initialized with the Default Application Key.
     * @param appId Application Identifier of the application to be created
     * @param iChangeKeyKey Key ID to change key
     * @param boolKeySettingChangeable boolean key settings changeable
     * @param boolFreeCreateDelete boolean Free to create and delete
     * @param boolFreeDirInfoAccess boolean Free directory information access
     * @param boolMasterKeyChangeable boolean master key changeable?
     * @param iKeyType integer key type 0, 1  or 2?
     * @param boolUseISOFileId boolean use ISO file ID
     * @param boolKeySett3Exist boolean key setting 3 exist?
     * @param iNumKeys interger number of application key
     * @param baKeySett3 byte array of key setting 3
     * @param IsoFileId ISO file ID 2 bytes
     * @param IsoDFName ISO DF Name 1-16 bytes
     * @return status of the command
     * @throws IOException Thrown if problem with the smart card connection
     */
    public statusType createApplication(byte [] appId,
                                        int iChangeKeyKey, boolean boolKeySettingChangeable, boolean boolFreeCreateDelete, boolean boolFreeDirInfoAccess, boolean boolMasterKeyChangeable, // Key setting 1
                                        int iKeyType, boolean boolUseISOFileId, boolean boolKeySett3Exist, int iNumKeys,// Key setting 2
                                        byte [] baKeySett3, byte [] IsoFileId, byte [] IsoDFName) throws IOException{

        byte bKeySett1, bKeySett2;
        if ((appId == null) || (appId.length != 3))
            return statusType.PCD_PARAMETER_ERROR;

        try {
            bKeySett1 = buildAppKeySetting(iChangeKeyKey, boolKeySettingChangeable, boolFreeCreateDelete, boolFreeDirInfoAccess, boolMasterKeyChangeable);
            bKeySett2 = buildKeySett2(iKeyType, boolUseISOFileId, boolKeySett3Exist, iNumKeys);
        } catch (Exception e) {
            return statusType.PCD_PARAMETER_ERROR;
        }
        if ((baKeySett3 != null) && ((baKeySett3.length <4) || (baKeySett3.length > 5)))
            return statusType.PCD_PARAMETER_ERROR;

        if ((IsoFileId != null) && (IsoFileId.length != 2))
            return statusType.PCD_PARAMETER_ERROR;
        if ((IsoDFName != null) && (IsoDFName.length > 16))
            return statusType.PCD_PARAMETER_ERROR;

        return createApplication(appId,bKeySett1,bKeySett2,baKeySett3,IsoFileId,IsoDFName);
    }

    public statusType createApplication(byte [] appId, byte bKeySett1, byte bKeySett2, byte [] IsoFileId, byte [] bDFName) throws IOException {
        return createApplication(appId,bKeySett1,bKeySett2,null,IsoFileId,bDFName);
    }

    public statusType createApplication(byte [] appId, byte bKeySett1, byte bKeySett2, byte [] baKeySett3, byte [] IsoFileId, byte [] bDFName) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateApplicationArray = new ByteArray();
        baCreateApplicationArray.append(appId).append(bKeySett1).append(bKeySett2).append(baKeySett3);
        if ((IsoFileId != null) && (IsoFileId.length == 2)) {
            baCreateApplicationArray.append(IsoFileId);
        }
        baCreateApplicationArray.append(bDFName);

        Log.v("createApplication", "CmdHeader for Create Application  File  : " + ByteArray.byteArrayToHexString(baCreateApplicationArray.toArray()));

        return sendBytes((byte) 0xCA, baCreateApplicationArray.toArray(), null, commMode.MAC).status;
    }

    public statusType deleteApplication(byte[] applicationId) throws IOException {
        return sendBytes((byte)0xDA, applicationId, null, commMode.MAC).status;
    }

    public statusType createDelegatedApplicaiotn (byte [] appId, short sDAMSlotNo, byte bDAMSlotVersion, short sQuotaLimit, byte bKeySett1, byte bKeySett2, byte [] baKeySett3, byte [] IsoFileId, byte [] bDFName) throws IOException {
        ByteArray baCreateApplicationArray = new ByteArray();
        baCreateApplicationArray.append(appId).append(bKeySett1).append(bKeySett2).append(baKeySett3);
        if ((IsoFileId != null) && (IsoFileId.length == 2)) {
            baCreateApplicationArray.append(IsoFileId);
        }
        baCreateApplicationArray.append(bDFName);

        Log.v("createApplication", "CmdHeader for Create Application  File  : " + ByteArray.byteArrayToHexString(baCreateApplicationArray.toArray()));

        return sendBytes((byte) 0xCA, baCreateApplicationArray.toArray(), null, commMode.MAC).status;

    }

    public byte [] arBuilder(int readAccess, int writeAccess, int readWriteAccess, int changeAccess) {
        return arBuilder(ByteArray.fromInt(readAccess,1)[0],ByteArray.fromInt(writeAccess,1)[0],ByteArray.fromInt(readWriteAccess,1)[0],ByteArray.fromInt(changeAccess,1)[0]);
    }

    public byte [] arBuilder(byte readAccess, byte writeAccess, byte readWriteAccess, byte changeAccess) {
        byte [] accessRights = new byte[2];
        accessRights[0] = (byte) (readWriteAccess << 4);
        accessRights[0] |= changeAccess;
        accessRights[1] = (byte) (readAccess << 4);
        accessRights[1] |= writeAccess;

        return accessRights;
    }

    public statusType createDataFile(byte bFileType, byte bFileID, byte [] baISOName, byte bCommSetting, byte [] baAccessRights, int iFileSize) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCreateDataFileArray = new ByteArray();

        baCreateDataFileArray.append(bFileID);

        if ((baISOName != null) && (baISOName.length == 2)) {
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
        if ((baISOName != null) && (baISOName.length == 2)) {
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

    public statusType createTransactionMACFile(byte bFileID, byte bFileOption, byte [] baAccessRights, byte bTMKeyOption, byte [] baTMKey, byte bTMKeyVer) throws IOException {
        // TODO: Sanity Checks

        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(bFileID).append(bFileOption);
        if ((baAccessRights == null) ||  (baAccessRights.length != 2)) {
            return statusType.PCD_PARAMETER_ERROR;
        }
        baCmdHeader.append(baAccessRights).append(bTMKeyOption);

        ByteArray baCmdData = new ByteArray();
        baCmdData.append(baTMKey).append(bTMKeyVer);

        return sendBytes((byte) 0xCE, baCmdHeader.toArray(), baCmdData.toArray(), commMode.ENCIPHERED).status;
    }

    public DesfireResponse deleteFile(byte fid) throws IOException {
        return sendBytes((byte)0xDF, new byte[] {fid}, null, commMode.MAC);
    }

    public statusType formatPICC() throws IOException {
        return sendBytes((byte) 0xFC, commMode.MAC).status;
    }

    /**
     * changeKeyEV2 handles changeKey and changeKeyEV2.
     * @param bKeyToChange
     * @param baKeySetNo array of 1 byte if null, it will be same as changekey command
     * @param bKeyVersion
     * @param baNewKey
     * @param baOldKey
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public statusType changeKeyEV2 (byte bKeyToChange, byte [] baKeySetNo, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey) throws GeneralSecurityException, IOException {
        ByteArray cmdHeaderBuilder = new ByteArray();
        ByteArray cmdDataBuilder = new ByteArray();

        byte [] cmdData;
        byte [] cmdHeader;


        // Case 1
        // If [oldKey != null, AND bKeyToChange != currAuthKey] or keySetNo != 0
        if ((bKeyToChange != dfCrypto.currentAuthenticatedKey) || ((baKeySetNo != null) && (baKeySetNo[0] != (byte) 0x00) )) {
            Log.v("changeKeyEV2","Change Key Case 1 - Different KeyNo");

            if (dfCrypto.keyType == dfCrypto.KEYTYPE_AES) {
                cmdData = cmdDataBuilder.append(ByteArray.xor(baNewKey, baOldKey)).append(bKeyVersion).append(dfCrypto.calcCRC(baNewKey)).toArray();
            } else {
                cmdData = cmdDataBuilder.append(ByteArray.xor(baNewKey, baOldKey)).append(dfCrypto.calcCRC(baNewKey)).toArray();
            }
            cmdHeader = cmdHeaderBuilder.append(baKeySetNo).append(bKeyToChange).toArray();
        } else {
            Log.v("changeKeyEV2","Change Key Case 2 - Same KeyNo");

            if (dfCrypto.keyType == dfCrypto.KEYTYPE_AES) {
                cmdData = cmdDataBuilder.append(baNewKey).append(bKeyVersion).toArray();
            } else {
                cmdData = cmdDataBuilder.append(baNewKey).toArray();
            }
            cmdHeader = cmdHeaderBuilder.append(baKeySetNo).append(bKeyVersion).toArray();
        }

        Log.v ("changeKeyEV2","cmdHeader: " + ByteArray.byteArrayToHexString(cmdHeader));
        Log.v ("changeKeyEV2","cmdData  : " + ByteArray.byteArrayToHexString(cmdData));

        DesfireResponse resp;
        if (baKeySetNo == null) // changeKey
            resp = sendBytes((byte) 0xC4, cmdHeader, cmdData, commMode.ENCIPHERED);
        else
            resp = sendBytes((byte) 0xC6, cmdHeader, cmdData, commMode.ENCIPHERED);

        return resp.status;
    }

    public statusType changeKey (byte bKeyToChange, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey) throws GeneralSecurityException, IOException {
        ByteArray keyBlockBuilder = new ByteArray();
        ByteArray commandBuilder = new ByteArray();
        // Ensure it is currently authenticated

        if (dfCrypto.currentAuthenticatedKey == -1)
            return statusType.AUTHENTICATION_ERROR;


        //EV2 change Key method
        if (dfCrypto.EV2_Authenticated) {
            return changeKeyEV2(bKeyToChange,null,bKeyVersion,baNewKey,baOldKey);
        }

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
                Log.d("changeKey", "CRC32 Input of XOR = " + ByteArray.byteArrayToHexString(baDataToCRC.toArray()));
                computedCrcXorKey = dfCrypto.calcCRC(baDataToCRC.toArray());
            } else {
                Log.d("changeKey", "CRC16 Input of XOR= " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));
                computedCrcXorKey = dfCrypto.calcCRC(keyBlockBuilder.toArray());
            }

            keyBlockBuilder.append(computedCrcXorKey);
            keyBlockBuilder.append(dfCrypto.calcCRC(baNewKey));
            Log.d("changeKey", "Case 1 encryption Input = " + ByteArray.byteArrayToHexString(keyBlockBuilder.toArray()));

            byte [] encryptedKey = dfCrypto.encryptDataAddPadding (keyBlockBuilder.toArray());

            Log.d("changeKey", "encrypted key" + ByteArray.byteArrayToHexString(encryptedKey));
            commandBuilder.append(encryptedKey);
        } else {
            Log.d("changeKey","Change Key Case 2 - Same KeyNo");
            keyBlockBuilder.append(baNewKey);

            // Append / Modify to include key version;
            if (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHAES) {
                keyBlockBuilder.append(bKeyVersion);
            } else {  // TODO: Set key version into last bit of each byte

            }

            commandBuilder.append((byte) 0xC4).append(bKeyToChange);
            commandBuilder.append(dfCrypto.encryptWriteDataAddCRC(commandBuilder.toArray(), keyBlockBuilder.toArray()));
        }


        Log.d("changeKey","Command to send: " + ByteArray.byteArrayToHexString(commandBuilder.toArray()));

        byte[] response = cardCommunicator.transceive(commandBuilder.toArray());
        DesfireResponse result = new DesfireResponse();

        result.status = findStatus(response[0]);
        if (result.status == statusType.SUCCESS) {
            if ((dfCrypto.trackCMAC) && (response.length > 5)) {
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

    private byte buildAppKeySetting(int iChangeKeyKey, boolean boolKeySettingChangeable, boolean boolFreeCreateDelete, boolean boolFreeDirInfoAccess, boolean boolMasterKeyChangable) throws Exception {
        byte bKeySetting;
        if ((iChangeKeyKey < 0) || (iChangeKeyKey > 0x0F) || (iChangeKeyKey == 0x0D)) {
            Log.e("buildAppKeySetting", "Change Key Key out of range");
            throw new Exception("PCD_Parameter_Error");
        }

        bKeySetting = ByteArray.fromInt(iChangeKeyKey << 4,1)[0];

        if (boolKeySettingChangeable)
            bKeySetting |= (byte)0x08;
        if (boolFreeCreateDelete)
            bKeySetting |= (byte)0x04;
        if (boolFreeDirInfoAccess)
            bKeySetting |= (byte)0x02;
        if (boolMasterKeyChangable)
            bKeySetting |= (byte)0x01;

        Log.d("buildAppKeySetting", "Key Setting set: 0x" + ByteArray.byteToHexString(bKeySetting));

        return bKeySetting;
    }

    private byte buildKeySett2(int iKeyType, boolean boolUseISOFileId, boolean boolKeySett3Exist, int iNumKeys) throws Exception {
        byte bKeySett2;
        if ((iKeyType < 0) || (iKeyType > 2)) {
            Log.e("buildKeySett2", "Change Key Key out of range");
            throw new Exception("PCD_Parameter_Error");
        }

        bKeySett2 = ByteArray.fromInt(iKeyType << 6,1)[0];

        if (boolUseISOFileId)
            bKeySett2 |= (byte)0x08;
        if (boolKeySett3Exist)
            bKeySett2 |= (byte)0x04;

        if ((iNumKeys < 0) || (iNumKeys > 13)) {
            Log.e("buildKeySett2", "Number of application key is out of range");
            throw new Exception("PCD_Parameter_Error");
        }
        bKeySett2 = ByteArray.fromInt(iKeyType,1)[0];

        Log.d("buildKeySett2", "Key Setting 2 set: 0x" + ByteArray.byteToHexString(bKeySett2));

        return bKeySett2;
    }

    public statusType changeKeySettings(int iChangeKeyKey, boolean boolKeySettingChangeable, boolean boolFreeCreateDelete, boolean boolFreeDirInfoAccess, boolean boolMasterKeyChangable) throws IOException {
        byte bKeySetting;
        try {
            bKeySetting = buildAppKeySetting(iChangeKeyKey, boolKeySettingChangeable, boolFreeCreateDelete, boolFreeDirInfoAccess, boolMasterKeyChangable);
        } catch (Exception e) {
            return statusType.PCD_PARAMETER_ERROR;
        }

        return sendBytes((byte)0x54, null, new byte[]{bKeySetting},commMode.ENCIPHERED).status;
    }

    public statusType initializeKeySet (byte bKeySetNo, byte bKeySetType) throws IOException {
        return sendBytes((byte)0x56, new byte[] {bKeySetNo, bKeySetType}, null, commMode.MAC).status;
    }

    public statusType finalizeKeySet (byte bKeySetNo, byte bKeySetVersion) throws IOException {
        ByteArray cmdHeader = new ByteArray();
        cmdHeader.append(bKeySetNo).append(bKeySetVersion);
        return sendBytes((byte)0x57, cmdHeader.toArray(), null, commMode.MAC).status;
    }

    public statusType rollKeySet  (byte bKeySetNo) throws IOException {
        statusType status = sendBytes((byte)0x55, new byte [] {bKeySetNo}, null, commMode.MAC).status;
        dfCrypto.reset();
        return status;
    }

    public DesfireResponse getMoreData() throws IOException {
        return sendBytes((byte)0xAF, null, null, commMode.MAC);
    }

    public DesfireResponse getMoreData(commMode curCommMode) throws IOException {
        return sendBytes((byte)0xAF, null, null, curCommMode);
    }

    /**
     * Reads data from FileType.StandardData, FileType.BackupData or FileType.TransactionMAC files.
     * Uses 0xBD command code that supports native chaining
     * @param fid File ID
     * @param start Start byte to read
     * @param count Number of bytes to read.
     * @param curCommMode Communication mode of the file
     * @return DesfireResponse with status and data read
     * @throws IOException Thrown if Smart Card Communication Error
     */
    public DesfireResponse readData(byte fid, int start, int count, commMode curCommMode) throws IOException {
        return readDataCore((byte) 0xBD, fid, start, count, curCommMode);
    }
    public DesfireResponse readData2(byte fid, int start, int count, commMode curCommMode) throws IOException {
        return readDataCore((byte) 0xAD, fid, start, count, curCommMode);
    }
    private DesfireResponse readDataCore (byte cmd, byte fid, int start, int count, commMode curCommMode) throws IOException {
        ByteArray cmdHeader = new ByteArray();
        cmdHeader.append(fid).append(start, 3).append(count, 3);

        dfCrypto.setAFLength(count);  //if count is zero, whole data file is read
        DesfireResponse resp = sendBytes(cmd, cmdHeader.toArray(), null, curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            if (count == 0) {
                cmdHeader.clear();
                cmdHeader.append(fid).append(start, 3).append(resp.data.length, 3);
            }
            dfCrypto.updateTMI (cmd, cmdHeader.toArray(), null, resp.data, true);
        }

        return resp;
    }


    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode) throws IOException {
        return readRecordsCore((byte) 0xBB, fid, offsetRecord, numOfRecords,curCommMode,0,0);
    }
    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode, int recordLength) throws IOException {
        return readRecordsCore((byte) 0xBB, fid, offsetRecord, numOfRecords,curCommMode,0,recordLength);
    }
    public DesfireResponse readRecords(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode, int encryptedLength, int recordLength) throws IOException {
        return readRecordsCore((byte) 0xBB, fid, offsetRecord, numOfRecords,curCommMode,0,0);
    }
    public DesfireResponse readRecords2(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode) throws IOException {
        return readRecordsCore((byte) 0xAB, fid, offsetRecord, numOfRecords,curCommMode,0,0);
    }
    public DesfireResponse readRecords2(byte fid, int offsetRecord, int numOfRecords, commMode curCommMode, int encryptedLength, int recordLength) throws IOException {
        return readRecordsCore((byte) 0xAB, fid, offsetRecord, numOfRecords,curCommMode,0,0);
    }
    private DesfireResponse readRecordsCore(byte cmd, byte fid, int offsetRecord, int numOfRecords, commMode curCommMode, int encryptedLength, int recordLength) throws IOException {
        ByteArray cmdHeader = new ByteArray();
        cmdHeader.append(fid).append(offsetRecord, 3).append(numOfRecords, 3);
        dfCrypto.encryptedLength = encryptedLength;
        DesfireResponse resp = sendBytes(cmd, cmdHeader.toArray(), null, curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.EV2_Authenticated)) {
            if (numOfRecords == 0) {
                int determinedNumOfRecords = numOfRecords;
                if (recordLength == 0) {
                    int determinedRecordLength = getRecordLength(fid);
                    if (determinedRecordLength == -1 ) {
                        Log.w("readRecords", "Unable to determine record size TMI calculation");
                        determinedRecordLength = 1;
                    }
                    determinedNumOfRecords =  resp.data.length / determinedRecordLength;
                }
                cmdHeader.clear();
                cmdHeader.append(fid).append(offsetRecord, 3).append(determinedNumOfRecords, 3);
            }
            dfCrypto.updateTMI (cmd, cmdHeader.toArray(), null, resp.data, true);
        }

        return resp;
    }

    public DesfireResponse writeData(byte fid, int start, int count, byte [] dataToWrite, commMode curCommMode) throws IOException {
        return writeDataCore ((byte) 0x3D, fid, start, count, dataToWrite, curCommMode);
    }
    public DesfireResponse writeData2(byte fid, int start, int count, byte [] dataToWrite, commMode curCommMode) throws IOException {
        return writeDataCore ((byte) 0x8D, fid, start, count, dataToWrite, curCommMode);
    }
    private DesfireResponse writeDataCore(byte cmd, byte fid, int start, int count, byte [] dataToWrite, commMode curCommMode) throws IOException {
        ByteArray baCmdHeaderToSend = new ByteArray();
        baCmdHeaderToSend.append(fid).append(start, 3).append(count, 3);
        DesfireResponse resp = sendBytes(cmd,baCmdHeaderToSend.toArray(),dataToWrite, curCommMode);
        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI (cmd, baCmdHeaderToSend.toArray(), dataToWrite, null, true);
        }
        return resp;
    }

    public DesfireResponse writeRecord(byte fid, int startRecord, int sizeToWrite, byte [] dataToWrite, commMode curCommMode) throws IOException {
        return writeRecordCore((byte) 0x3B,fid,startRecord,sizeToWrite,dataToWrite,curCommMode);
    }
    public DesfireResponse writeRecord2(byte fid, int startRecord, int sizeToWrite, byte [] dataToWrite, commMode curCommMode) throws IOException {
        return writeRecordCore((byte) 0x8B,fid,startRecord,sizeToWrite,dataToWrite,curCommMode);
    }
    private  DesfireResponse writeRecordCore(byte cmd, byte fid, int startRecord, int sizeToWrite, byte [] dataToWrite, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid).append(startRecord, 3).append(sizeToWrite, 3);
        DesfireResponse resp = sendBytes(cmd, baCmdHeader.toArray(),dataToWrite,curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI (cmd, baCmdHeader.toArray(), dataToWrite, null, true);
        }
        return resp;
    }

    public DesfireResponse clearRecordFile(byte fid) throws IOException {
        DesfireResponse resp = sendBytes((byte) 0xEB, new byte[] {fid}, null, commMode.MAC);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0xEB, new byte[] {fid}, null, null, false);
        }
        return resp;
    }

    public DesfireResponse getValue(byte bFileID,commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(bFileID);

        dfCrypto.encryptedLength = 4;
        DesfireResponse resp = sendBytes((byte) 0x6C, baCmdHeader.toArray(), null, curCommMode);
        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0x6C, baCmdHeader.toArray(), null, resp.data, false);
        }
        return resp;
    }

    public DesfireResponse credit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        DesfireResponse resp = sendBytes((byte) 0x0C, baCmdHeader.toArray(), baValue.toArray(), curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0x0C, baCmdHeader.toArray(), baValue.toArray(), null, false);
        }
        return resp;
    }

    public DesfireResponse debit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        DesfireResponse resp = sendBytes((byte) 0xDC, baCmdHeader.toArray(), baValue.toArray(), curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0xDC, baCmdHeader.toArray(), baValue.toArray(), null, false);
        }
        return resp;
    }

    public DesfireResponse limitedCredit(byte fid, int value, commMode curCommMode) throws IOException {
        ByteArray baCmdHeader = new ByteArray();
        baCmdHeader.append(fid);
        ByteArray baValue = new ByteArray();
        baValue.append(value, 4);
        DesfireResponse resp = sendBytes((byte) 0x1C, baCmdHeader.toArray(), baValue.toArray(), curCommMode);

        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0x1C, baCmdHeader.toArray(), baValue.toArray(), null, false);
        }
        return resp;
    }

    public DesfireResponse commitTransaction() throws IOException {
        DesfireResponse resp = sendBytes((byte) 0xC7, commMode.MAC);
        return resp;
    }

    public DesfireResponse commitTransaction(byte option) throws IOException {
        DesfireResponse resp = sendBytes((byte) 0xC7, new byte[] {option},null, commMode.MAC);
        return resp;
    }

    public DesfireResponse abortTransaction() throws IOException {
        DesfireResponse resp =sendBytes((byte) 0xA7, commMode.MAC);
        dfCrypto.clearTMI();
        return resp;
    }

    public DesfireResponse commitReaderID(byte [] TMRI) throws IOException {
        DesfireResponse resp;
        if ((TMRI == null) || (TMRI.length != 16)) {
            resp = new DesfireResponse();
            resp.status = statusType.PCD_PARAMETER_ERROR;
            resp.data = null;
            return resp;
        }
        resp =sendBytes((byte) 0xC8, null, TMRI, commMode.MAC);
        if ((resp.status == statusType.SUCCESS) && (dfCrypto.trackTMI)) {
            dfCrypto.updateTMI ((byte) 0xC8, null, TMRI, resp.data, false);
        }
        return resp;
    }

    public statusType txnMacVerification (byte [] TMKey, byte [] UID, byte [] commitResponseData) throws Exception{
        byte [] commitTMCounter = new byte[4];
        byte [] commitTxnMAC = new byte [8];

        System.arraycopy(commitResponseData,0,commitTMCounter,0,4);
        System.arraycopy(commitResponseData,4,commitTxnMAC,0,8);

        byte [] calcTxnMAC = dfCrypto.finalizeTxnMAC(TMKey, commitTMCounter, UID);

        Log.d ("txnMacVerification", "commitTxnMAC = " + ByteArray.byteArrayToHexString(commitTxnMAC));
        Log.d ("txnMacVerification", "calcTxnMAC   = " + ByteArray.byteArrayToHexString(calcTxnMAC));

        if (!Arrays.equals(commitTxnMAC, calcTxnMAC)) {
            scrollLog.appendError("Error: Transaction MAC Incorrect\nCalculated TxnMAC = " + ByteArray.byteArrayToHexString(calcTxnMAC));
            return statusType.PCD_ENCRYPTION_ERROR;
        } else {
            int iTMC = ByteBuffer.wrap(commitTMCounter).order(ByteOrder.LITTLE_ENDIAN).getInt();
            scrollLog.appendStatus("Transaction MAC Verified.\nTMAC Counter = " + iTMC +"\nCalculated TMAC = " + ByteArray.byteArrayToHexString(calcTxnMAC));
        }
        dfCrypto.clearTMI();

        return statusType.SUCCESS;
    }

    /**
     * This command is implemented in compliance with ISO/IEC 7816-4. It selects either the
     * PICC level, an application or a file within the application.
     * @param P1 00-04h
     * @param P2 00 - return FCI Template, 0Ch No response data
     * @param ISO_IDName 2 byte file identifier (3F00 MF, or DF/EF ID) or DF Naame
     * @return ISOResponse
     * @throws IOException Thrown if problem with the smart card connection
     */
    public ISOResponse ISOSelectFile(byte P1, byte P2, byte[] ISO_IDName) throws IOException {

        return ISOSendBytes((byte) 0xA4, P1, P2,  ISO_IDName, (byte) 0, true);
    }

    /**
     * The ISOReadBinary can be used to read data from the Standard Data File. This command does
     * not support any secure messaging, it is always in plain.
     * @param P1 ShortFile ID/Offset
     * @param P2 Offset
     * @param Le number of byte to be read
     * @return ISOResponse
     * @throws IOException Thrown if problem with the smart card connection
     */
    public ISOResponse ISOReadBinary(byte P1, byte P2, byte Le) throws IOException {

        return ISOSendBytes((byte) 0xB0, P1, P2, null, Le);
    }

    /**
     * The ISOUpdateBinary command is implemented in compliance with ISO/IEC 7816-4, the
     * command is only possible with CommMode.Plain for a Standard Data File.
     * @param P1 ShortFile ID/Offset
     * @param P2 Offset
     * @param data Data to be written
     * @return ISOResponse with 2 byte status word and data
     * @throws IOException Thrown if problem with the smart card connection
     */
    public ISOResponse ISOUpdateBinary(byte P1, byte P2, byte [] data) throws IOException {

        return ISOSendBytes((byte) 0xD6, P1, P2, data, (byte)0, false);
    }

    //region authentication

    public int currentAuthenticatedKey () {
        return dfCrypto.currentAuthenticatedKey;
    }

    public byte currentAuthenticationMode () {
        return dfCrypto.getAuthMode();
    }

    public statusType authenticate(byte authType, byte keyNumber, byte[] key) throws Exception {
        return authenticate(authType, keyNumber, (byte) 0x00, null, key);
    }

    public statusType authenticate(byte authType, byte keyNumber, byte lenCap, byte [] PCDcap2, byte[] key) throws Exception {

        byte [] retainedTI = new byte[4];
        ByteArray retainedTMI = new ByteArray();
        int retainedCmdCtr = 0;
        if (authType == 0x77) {
            System.arraycopy(dfCrypto.EV2_TI, 0, retainedTI, 0, 4);
            retainedCmdCtr = dfCrypto.EV2_CmdCtr;
            retainedTMI.append(dfCrypto.baEV2_TMI.toArray());
            Log.d("authenticate", "Retained baEV2_TMI = "+ retainedTMI.toHexString());
        }

        dfCrypto.initialize(authType, key);

        ByteArray baCmd = ByteArray.from(keyNumber);

        if (authType==0x71) {
            if (lenCap == 0x00) {
                baCmd.append((byte) 0x00);
            } else {
                if ((PCDcap2 != null) && (PCDcap2.length == lenCap)) {
                    baCmd.append(lenCap).append(PCDcap2);
                } else {
                    Log.d("authenticate", "PCDcap2 error");
                    return statusType.PCD_PARAMETER_ERROR;
                }

            }
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
        if (authType == 0x77) {
            System.arraycopy(retainedTI, 0, dfCrypto.EV2_TI, 0, 4);
            dfCrypto.EV2_CmdCtr = retainedCmdCtr;
            dfCrypto.baEV2_TMI.clear();
            dfCrypto.baEV2_TMI.append(retainedTMI.toArray());
            Log.d("authenticate", "Restored baEV2_TMI = " + dfCrypto.baEV2_TMI.toHexString());
        }


        return statusType.SUCCESS;

    }
    //endregion authenticate

    //region send bytes

    private DesfireResponse sendBytes (byte cmd, commMode expectedCommMode) throws IOException {
        return sendBytes(cmd, null, null, expectedCommMode);
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
        byte[] cmdToSend;
        ByteArray baCmdBuilder = new ByteArray();

        baCmdBuilder.append(cmd).append(cmdHeader);
        if ((dfCrypto.EV2_Authenticated)) {

            if ((curCommMode == commMode.ENCIPHERED) && (cmdData != null)) {

                byte[] encryptData = dfCrypto.EV2_EncryptData(cmdData);
                if (encryptData == null) {
                    Log.d("sendBytes", "Command Encrypt error ");
                    scrollLog.appendError("Encrypt CmdData Error");
                    return encryptionError();
                }

                baCmdBuilder.append(encryptData);
            } else {
                baCmdBuilder.append(cmdData);
            }

            if ((cmd != (byte) 0xAF) && (curCommMode != commMode.PLAIN)) {
                Log.d("sendBytes  ", "Command to Track EV2 CMAC   = " + ByteArray.byteArrayToHexString(baCmdBuilder.toArray()));
                cmdToSend = dfCrypto.EV2_GenerateMacCmd(baCmdBuilder.toArray());

                baCmdBuilder.clear();
                baCmdBuilder.append(cmdToSend);
            }

        } else if ((curCommMode == commMode.ENCIPHERED) && (cmdData != null) && (dfCrypto.getAuthMode() != (byte)0x00)) {
            try {
                byte[] encryptData = dfCrypto.encryptWriteDataAddCRC(baCmdBuilder.toArray(), cmdData);

                baCmdBuilder.append(encryptData);
            } catch (GeneralSecurityException e) {
                Log.d("sendBytes", "Command Encrypt error ");
                scrollLog.appendError("Encrypt CmdData Error");

                scrollLog.appendError(e.getMessage());
                return encryptionError();
            }


        } else if ((dfCrypto.trackCMAC) && (cmd != (byte) 0xAF)) {

            baCmdBuilder.append(cmdData);
            byte[] cmdToMac = baCmdBuilder.toArray();


            Log.d("sendBytes  ", "Command to Track CMAC   = " + ByteArray.byteArrayToHexString(cmdToMac));
            byte[] macComputed = dfCrypto.calcCMAC(cmdToMac);

            if ((curCommMode == commMode.MAC) && (cmdData != null)) {
                baCmdBuilder.append(macComputed);
            }

        } else if ((curCommMode == commMode.MAC) && (dfCrypto.getAuthMode() == dfCrypto.MODE_AUTHD40)) {
            if (cmdData != null) {
                ByteArray arrayMAC = new ByteArray();
                byte[] cmdToMAC = arrayMAC.append(cmdData).toArray();

                Log.d("sendBytes", "Command to MAC = " + ByteArray.byteArrayToHexString(cmdToMAC));
                byte[] macToSend = dfCrypto.calcD40MAC(cmdToMAC);
                baCmdBuilder.append(cmdData).append(macToSend);
            }
        } else {
            baCmdBuilder.append(cmdData);
        }

        response = null;
        try {
            response = cardCommunicator.transceive(baCmdBuilder.toArray());
        } catch (IOException e) {
            Log.e("sendBytes", "Card communication problem");
        }

        DesfireResponse result = new DesfireResponse();
        result.status = findStatus(response[0]);

        if (result.status == statusType.SUCCESS) {
            if (dfCrypto.EV2_Authenticated) {

                if (curCommMode == commMode.PLAIN) {
                    dfCrypto.EV2_CmdCtr ++;
                } else {
                    if (response.length > 8) {
                        if (!dfCrypto.EV2_verifyMacResponse(response)) {
                            scrollLog.appendError("Error: CMAC Incorrect");
                            return encryptionError();
                        } else {
                            scrollLog.appendStatus("OK: CMAC Verified");
                        }
                    }
                }

                if ((curCommMode == commMode.ENCIPHERED) && (response.length > 9)) {

                    result.data = dfCrypto.EV2_DecryptData(ByteArray.appendCutCMAC(response, 8));
                } else if ((curCommMode == commMode.MAC) || (response.length > 8)){
                    result.data = ByteArray.appendCutCMAC(response, 8);
                } else {
                    result.data = ByteArray.appendCut(null,response);
                }

            } else if (dfCrypto.trackCMAC) {    // D41 authenticated
                if ((curCommMode == commMode.ENCIPHERED) && (cmdData == null)){  // Only when cmdData is null would there be return data
                    dfCrypto.storeAFEncrypted(response);
                    try {
                        result.data = dfCrypto.decryptReadDataRemPadVerCRC();
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
                        result.data = dfCrypto.decryptReadDataRemPadVerCRC();
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
            short sStatus = (short) (response[response.length-1] + (response[response.length-2] << 8));
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


    private DesfireResponse encryptionError () {
        DesfireResponse badResult = new DesfireResponse();

        dfCrypto.reset();
        badResult.status = statusType.PCD_ENCRYPTION_ERROR;
        badResult.data = null;
        return badResult;
    }

    //endregion send bytes

    //region Status Codes

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
            case (byte)0x67:
                retStatusType = statusType.WRONG_LENGTH;
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

    public static String DesfireErrorMsg (statusType status) {
        String returnString;

        switch (status) {
            case SUCCESS:
                returnString = "Command Successful";
                break;
            case NO_CHANGES:
                returnString = "No Change";
                break;
            case OUT_OF_EEPROM_ERROR:
                returnString = "Out of EEPROM";
                break;
            case ILLEGAL_COMMAND_CODE:
                returnString = "Command Not Supported";
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
            case WRONG_LENGTH: //6700
                returnString = "Wrong Length - Use ISO Wrap Mode";
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
            case PCD_PARAMETER_ERROR:
                returnString = "PCD parameter error";
                break;
            default:
                returnString = "Unknown error";
        }
        return returnString;
    }

    //endregion

    //region ISO Status Word
    public enum statusWord {
        SUCCESS,                    // 9000
        TMC_LIMIT_REACHED,          // 6283
        WRONG_ADPU_LENGTH,          // 6700
        WRAPPED_COMMAND__ONGOING,   // 6985
        FILE_NOT_FOUND,             // 6A82
        WRONG_P1P2,                 // 6A86
        WRONG_LC,                   // 6A87
        WRONG_LE,                   // 6CXX
        WRONG_CLA,                  // 6E00
        WRONG_INS,                  // 6D00

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
                retStatusWord = statusWord.WRAPPED_COMMAND__ONGOING;
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
            case (short) 0x6D00:
                retStatusWord = statusWord.WRONG_INS;
                break;
            case (short) 0x6F00:
                retStatusWord = statusWord.READER_ISSUE;
                break;
            default:
                if (((sStatus >> 8) & 0xFF) == 0x6C) {
                    retStatusWord = statusWord.WRONG_LE;
                } else {
                    retStatusWord = statusWord.UNKNOWN_ERROR;
                }
                break;
        }
        return retStatusWord;
    }


    public static String DesfireErrorMsg (statusWord sw) {
        String returnString = "";

        switch (sw) {
            case SUCCESS:
                returnString = "Command Successful";
                break;
            case TMC_LIMIT_REACHED:
                returnString = "Selected file or application deactivated: selected with limited functionality"; //6283
                break;
            case WRONG_ADPU_LENGTH:
                returnString = "Wrong or inconsistent APDU length"; //6700
                break;
            case WRAPPED_COMMAND__ONGOING:
                returnString = "Condition of use not satisfied";
                break;
            case FILE_NOT_FOUND:
                returnString = "Application or file not found, currently selected application remains selected";
                break;
            case WRONG_P1P2:
                returnString = "Wrong parameter P1 and/or P2";
                break;
            case WRONG_LC:
                returnString = "Wrong parameter Lc inconstantent with P1-P2";
                break;
            case WRONG_LE:
                returnString = "Wrong Le field";
                break;
            case WRONG_CLA:
                returnString = "Class not supported"; //6E00
                break;
            case MEMORY_FAILURE:
                returnString = "Memory failure"; //6581
                break;
            case SECURITY_STATUS_NOT_SATISIFED:
                returnString = "Security status not satisfied";
                break;
            case READER_ISSUE:
                break;
            default:
                returnString = "Unknown Error";
        }
        return returnString;
    }





    //endregion



    public boolean connect() throws IOException {

        cardCommunicator.connect();
        return cardCommunicator.isConnected();
    }

    public void close() throws IOException {
        cardCommunicator.close();
    }
}

