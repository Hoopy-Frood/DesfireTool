package com.example.ac.desfirelearningtool;

import android.os.Bundle;

/**
 * Created by Ac on 8/19/2017.
 */

public interface IMainActivityCallbacks {
    // Informational
    void onGetVersion ();
    void onGetCardUID();
    void onGetApplicationIDs();
    Bundle onFragmentGetApplicationIDs();
    Bundle onFragmentGetFileIDs();
    void onGetDFNames();
    void onGetFreeMem();

    // Security
    void onAuthenticate();
    void onAuthenticateReturn (byte bAuthCmd, byte bKeyNo, byte[] key);
    void onGetKeyVersion();
    void onGoGetKeyVersionReturn(byte iKeyToInquire);
    void onGetKeySettings();
    byte[] onFragmentGetKeySettings ();
    void onChangeKey();
    void onChangeKeyReturn(byte bKeyToChange, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey);


    // Application
    void onSelectApplication();
    void onSelectApplicationReturn(byte [] appId);
    void onGetFileIDs();
    void onGetISOFileIDs();
    void onGetFileSettings();
    void onGetFileSettingsReturn(byte bFileID);
    Bundle onFragmentGetFileSettings (byte bFileID);

    // Card Management
    void onCreateApplication();
    void onCreateApplicationReturn(byte [] appId, byte bKeySetting1, byte bKeySetting2, byte [] baISOName, byte [] DFName);
    void onDeleteApplication();
    void onDeleteApplicationReturn(byte [] appId);
    void onFormatPICC ();

    // File Management
    void onCreateFile ();
    void onCreateFileDataReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iFileSize);
    void onCreateFileRecordReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iRecordSize, int iNumOfRecords);
    void onCreateFileValueReturn (byte bFileId, byte bCommSetting, byte[] baAccessBytes, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte);
    void onDeleteFile();
    void onDeleteFileReturn(byte bFileID);

    // Data Manipulation - DATA File
    void onReadData();
    void onReadDataReturn(byte bFileID, int iOffset, int iLength, MifareDesfire.commMode iCommMode);
    void onWriteData();
    void onWriteDataReturn(byte bFileID, int iOffset, int iLength, byte [] bDataToWrite, MifareDesfire.commMode iCommMode);

    // Data Manipulation - RECORD File
    void onReadRecords();
    void onReadRecordsReturn(byte bFileID, int iOffset, int iLength, MifareDesfire.commMode iCommMode);
    void onWriteRecord();
    void onWriteRecordReturn(byte bFileID, int iOffset, int iLength, byte [] bDataToWrite, MifareDesfire.commMode iCommMode);
    void onClearRecordFile();
    void onClearRecordFileReturn(byte bFileID);

    // Data Manipulation - VALUE File
    void onGetValue();
    void onGetValueReturn(byte bFileID, MifareDesfire.commMode iCommMode);
    void onCredit();
    void onCreditReturn(byte bFileID, int iCreditValue, MifareDesfire.commMode iCommMode);
    void onDebit();
    void onDebitReturn(byte bFileID, int iDebitValue, MifareDesfire.commMode iCommMode);
    void onLimitedCredit();
    void onLimitedCreditReturn(byte bFileID, int iDebitValue, MifareDesfire.commMode iCommMode);

    // Data Manipulation - Commit
    void onCommitTransaction ();
    void onAbortTransaction ();

    // Testing
    void onCreateTestPerso();
    void onAuthISOTest();
    void onAuthAESTest();
    void onTestAll();
    void onSetConfiguration();
    
    ScrollLog getScrollLogObject ();
}
