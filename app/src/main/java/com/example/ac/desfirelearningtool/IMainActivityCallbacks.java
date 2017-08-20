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
    //public void onGetKeyVersion();
    public void onGetKeySettings();
    public void onSelectApplication();
    public void onSelectReturn(byte [] appId);
    //public void onCreateApplication();
    //public void onDeleteApplication();
    //public void onFormatPICC ();


}
