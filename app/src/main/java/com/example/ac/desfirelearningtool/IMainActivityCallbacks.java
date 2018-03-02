package com.example.ac.desfirelearningtool;


import android.os.Bundle;

/**
 * Created by Ac on 8/19/2017.
 */

public interface IMainActivityCallbacks {
    public void onGetVersion ();
    public void onGetCardUID();
    public void onGetApplicationIDs();
    public Bundle onFragmentGetApplicationIDs();
    public void onGetDFNames();
    public void onGetFreeMem();
    public void onGetKeyVersion();
    public void onGoGetKeyVersionReturn(byte iKeyToInquire);
    public void onGetKeySettings();
    public void onSelectApplication();
    public void onSelectApplicationReturn(byte [] appId);
    public void onCreateApplication();
    public void onCreateApplicationReturn(byte [] appId, byte bKeySetting1, byte bKeySetting2, byte [] baISOName, byte [] DFName);
    public void onDeleteApplication();
    public void onDeleteApplicationReturn(byte [] appId);
    public void onFormatPICC ();
    public void onCreateFile ();
    public void onCreateFileDataReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iFileSize);
    public void onCreateFileRecordReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iRecordSize, int iNumOfRecords);
    public void onCreateFileValueReturn (byte bFileType, byte bFileId, byte bCommSetting, byte[] baAccessBytes, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte);
    public void onAuthenticate();
    public void onAuthenticateReturn (byte bAuthCmd, byte bKeyNo, byte[] key);
    public void onDeleteFile();
    public void onDeleteFileReturn(byte bFileID);
    public void onGetFileIDs();
    public Bundle onFragmentGetFileIDs();
    public void onGetISOFileIDs();
    public void onGetFileSettings();
    public void onGetFileSettingsReturn(byte bFileID);
    public Bundle onFragmentGetFileSettings (byte bFileID);

    public void onReadData();
    public void onReadDataReturn(byte bFileID, int iOffset, int iLength, MifareDesfire.commMode iCommMode);
    public void onCreateTestPerso();

    public void onWriteData();
    public void onWriteDataReturn(byte bFileID, int iOffset, int iLength, byte [] bDataToWrite, MifareDesfire.commMode iCommMode);

    public void onReadRecords();
    public void onReadRecordsReturn(byte bFileID, int iOffset, int iLength, MifareDesfire.commMode iCommMode);

    public void onWriteRecord();
    public void onWriteRecordReturn(byte bFileID, int iOffset, int iLength, byte [] bDataToWrite, MifareDesfire.commMode iCommMode);
    public void onClearRecordFile();
    public void onClearRecordFileReturn(byte bFileID);

    public void onCommitTransaction ();
    public void onAbortTransaction ();


    public void onReadDataTest(byte fileID);
    public void onReadDataMACTest(byte fileID);
    public void onReadDataEncryptedTest(byte fileID, int bytesToRead);
    public void onAuthISOTest();
    public void onAuthAESTest();
    public void onTestAll();

     public ScrollLog getScrollLogObject ();
}
