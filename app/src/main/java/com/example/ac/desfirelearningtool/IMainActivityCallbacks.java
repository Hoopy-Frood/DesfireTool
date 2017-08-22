package com.example.ac.desfirelearningtool;

/**
 * Created by Ac on 8/19/2017.
 */

public interface IMainActivityCallbacks {
    public void onGetVersion ();
    public void onGetCardUID();
    public void onGetApplicationIDs();
    public void onGetDFNames();
    public void onGetFreeMem();
    public void onAuthenticate();
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




}
