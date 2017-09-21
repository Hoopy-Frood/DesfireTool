package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Ac on 8/19/2017.
 */

public class fCommandMenu extends Fragment{
    IMainActivityCallbacks mCallback;

    // Buttons - Informational
    private Button buttonGetVersion;
    private Button buttonGetCardUID;
    private Button buttonGetDFNames;
    private Button buttonGetAppID;
    private Button buttonGetFreeMem;
    // Buttons - Security
    private Button buttonAuthenticate;
    private Button buttonGetKeyVersion;
    private Button buttonGetKeySettings;
    // Buttons - Application
    private Button buttonSelect;
    private Button buttonGetFileSettings;
    private Button buttonGetFileIDs;
    private Button buttonGetISOFileIDs;
    // Buttons - Data Manipulation
    private Button buttonReadData;
    private Button buttonWriteData;
    private Button buttonReadRecord;
    private Button buttonWriteRecord;
    private Button buttonClearRecordFile;
    private Button buttonGetValue;
    private Button buttonCredit;
    private Button buttonDebit;
    private Button buttonLimitedCredit;
    private Button buttonCommitTransaction;
    private Button buttonAbortTransaction;
    // Buttons - Perso
    private Button buttonCreateApplication;
    private Button buttonDeleteApplication;
    private Button buttonFormatPICC;
    // Buttons - Key Management
    private Button buttonSetConfiguration;
    private Button buttonChangeKey;
    private Button buttonChangeKeySettings;
    // Buttons - File Management
    private Button buttonCreateFile;
    private Button buttonDeleteFile;
    private Button buttonChangeFileSettings;
    // TEST
    private Button buttonAuthISO;
    private Button buttonAuthAES;



    public fCommandMenu () {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.f_commandmenu, container, false);

        // Buttons - Informational
        buttonGetVersion = (Button) rootView.findViewById(R.id.button_GetVersion);
        buttonGetCardUID= (Button) rootView.findViewById(R.id.button_GetCardUID);
        buttonGetDFNames= (Button) rootView.findViewById(R.id.button_GetDFNames);
        buttonGetAppID= (Button) rootView.findViewById(R.id.button_GetAppID);
        buttonGetFreeMem= (Button) rootView.findViewById(R.id.button_GetFreeMem);
        // Buttons - Security
        buttonAuthenticate = (Button) rootView.findViewById(R.id.button_Authenticate);
        buttonGetKeyVersion = (Button) rootView.findViewById(R.id.button_GetKeyVersion);
        buttonGetKeySettings = (Button) rootView.findViewById(R.id.button_GetKeySettings);
        // Buttons - Application
        buttonSelect = (Button) rootView.findViewById(R.id.button_Select);
        buttonGetFileSettings = (Button) rootView.findViewById(R.id.button_GetFileSettings);
        buttonGetFileIDs = (Button) rootView.findViewById(R.id.button_GetFileIDs);
        buttonGetISOFileIDs = (Button) rootView.findViewById(R.id.button_GetISOFileIDs);
        // Buttons - Data Manipulation
        buttonReadData = (Button) rootView.findViewById(R.id.button_ReadData);
        buttonWriteData = (Button) rootView.findViewById(R.id.button_WriteData);
        buttonReadRecord = (Button) rootView.findViewById(R.id.button_ReadRecord);
        buttonWriteRecord = (Button) rootView.findViewById(R.id.button_WriteRecord);
        buttonClearRecordFile = (Button) rootView.findViewById(R.id.button_ClearRecordFile);
        buttonGetValue = (Button) rootView.findViewById(R.id.button_GetValue);
        buttonCredit = (Button) rootView.findViewById(R.id.button_Credit);
        buttonDebit = (Button) rootView.findViewById(R.id.button_Debit);
        buttonLimitedCredit = (Button) rootView.findViewById(R.id.button_LimitedCredit);
        buttonCommitTransaction = (Button) rootView.findViewById(R.id.button_CommitTransaction);
        buttonAbortTransaction = (Button) rootView.findViewById(R.id.button_AbortTransaction);
        // Buttons - Perso
        buttonCreateApplication = (Button) rootView.findViewById(R.id.button_CreateApplication);
        buttonDeleteApplication = (Button) rootView.findViewById(R.id.button_DeleteApplication);
        buttonFormatPICC = (Button) rootView.findViewById(R.id.button_FormatPICC);
        // Buttons - Key Management
        buttonSetConfiguration = (Button) rootView.findViewById(R.id.button_SetConfiguration);
        buttonChangeKey = (Button) rootView.findViewById(R.id.button_ChangeKey);
        buttonChangeKeySettings = (Button) rootView.findViewById(R.id.button_ChangeKeySettings);
        // Buttons - File Management
        buttonCreateFile = (Button) rootView.findViewById(R.id.button_CreateFile);
        buttonDeleteFile = (Button) rootView.findViewById(R.id.button_DeleteFile);
        buttonChangeFileSettings = (Button) rootView.findViewById(R.id.button_ChangeFileSettings);

        //Button - Test
        buttonAuthISO = (Button) rootView.findViewById(R.id.button_AuthISOTest);
        buttonAuthAES = (Button) rootView.findViewById(R.id.button_AuthAESTest);

        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("onCreateView", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement commandInterfaceListener");
        }

        //Log.d("fCommandMenu Create", "4");
        buttonGetVersion.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               mCallback.onGetVersion();
            }
        });
        buttonGetCardUID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetCardUID();
            }
        });
        buttonGetAppID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetApplicationIDs();
            }
        });
        buttonGetDFNames.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onGetDFNames();
            }
        });
        buttonGetFreeMem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onGetFreeMem();
            }
        });
        buttonAuthenticate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onAuthenticate();
            }
        });
        buttonGetKeySettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetKeySettings();
            }
        });
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onSelectApplication();
            }
        });
        buttonGetFileIDs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onGetFileIDs();
            }
        });
        buttonGetISOFileIDs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetISOFileIDs();
            }
        });
        buttonGetFileSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetFileSettings();
            }
        });
        buttonCreateApplication.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onCreateApplication();
            }
        });
        buttonCreateFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onCreateFile();
            }
        });
        buttonDeleteFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onDeleteFile();
            }
        });
        buttonFormatPICC.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onFormatPICC();
            }
        });
        buttonGetKeyVersion.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onGetKeyVersion();
            }
        });
        buttonDeleteApplication.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.onDeleteApplication();
            }
        });
        buttonReadData.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mCallback.onReadDataTest((byte) 0x01);  // Plain Free / Free
                mCallback.onReadDataMACTest((byte) 0x02);  // Mac   Key 0 / Key 0 (cannot access unless auth with key 0
                mCallback.onReadDataTest((byte) 0x03);  // Enc   Free / Free
                mCallback.onReadDataTest((byte) 0x04);  // Enc   Key 1 / Free (result plain + MAC
                // mCallback.onReadDataTest((byte) 0x05);  // Enc   Key 2 / 1 (No access after auth key 0
                mCallback.onReadDataEncryptedTest((byte) 0x06);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0


            }
        });


        buttonAuthISO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onAuthISOTest();
            }
        });
        buttonAuthAES.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {mCallback.onAuthAESTest();
            }
        });
        //Log.d("fCommandMenu Create", "5");
        
        disableAllButtons();

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void disableAllButtons() {
        // Buttons - Informational
        buttonGetVersion.setEnabled(false);
        buttonGetCardUID.setEnabled(false);
        buttonGetDFNames.setEnabled(false);
        buttonGetAppID.setEnabled(false);
        buttonGetFreeMem.setEnabled(false);
        // Buttons - Security
        buttonAuthenticate.setEnabled(false);
        buttonGetKeyVersion.setEnabled(false);
        buttonGetKeySettings.setEnabled(false);
        // Buttons - Application
        buttonSelect.setEnabled(false);
        buttonGetFileSettings.setEnabled(false);
        buttonGetFileIDs.setEnabled(false);
        buttonGetISOFileIDs.setEnabled(false);
        // Buttons - Data Manipulation
        buttonReadData.setEnabled(false);
        buttonWriteData.setEnabled(false);
        buttonReadRecord.setEnabled(false);
        buttonWriteRecord.setEnabled(false);
        buttonClearRecordFile.setEnabled(false);
        buttonGetValue.setEnabled(false);
        buttonCredit.setEnabled(false);
        buttonDebit.setEnabled(false);
        buttonLimitedCredit.setEnabled(false);
        buttonCommitTransaction.setEnabled(false);
        buttonAbortTransaction.setEnabled(false);
        // Buttons - Perso
        buttonCreateApplication.setEnabled(true);
        buttonDeleteApplication.setEnabled(false);
        buttonFormatPICC.setEnabled(false);
        // Buttons - Key Management
        buttonSetConfiguration.setEnabled(false);
        buttonChangeKey.setEnabled(false);
        buttonChangeKeySettings.setEnabled(false);
        // Buttons - File Management
        buttonCreateFile.setEnabled(true);
        buttonDeleteFile.setEnabled(false);
        buttonChangeFileSettings.setEnabled(false);
    }

    protected void enableAllButtons() {
        // Buttons - Informational
        buttonGetVersion.setEnabled(true);
        buttonGetCardUID.setEnabled(true);
        buttonGetDFNames.setEnabled(true);
        buttonGetAppID.setEnabled(true);
        buttonGetFreeMem.setEnabled(true);
        // Buttons - Security
        buttonAuthenticate.setEnabled(true);
        buttonGetKeyVersion.setEnabled(true);
        buttonGetKeySettings.setEnabled(true);
        // Buttons - Application
        buttonSelect.setEnabled(true);
        buttonGetFileSettings.setEnabled(true);
        buttonGetFileIDs.setEnabled(true);
        buttonGetISOFileIDs.setEnabled(true);
        // Buttons - Data Manipulation
        buttonReadData.setEnabled(true);
        buttonWriteData.setEnabled(false);
        buttonReadRecord.setEnabled(false);
        buttonWriteRecord.setEnabled(false);
        buttonClearRecordFile.setEnabled(false);
        buttonGetValue.setEnabled(false);
        buttonCredit.setEnabled(false);
        buttonDebit.setEnabled(false);
        buttonLimitedCredit.setEnabled(false);
        buttonCommitTransaction.setEnabled(false);
        buttonAbortTransaction.setEnabled(false);
        // Buttons - Perso
        buttonCreateApplication.setEnabled(true);
        buttonDeleteApplication.setEnabled(true);
        buttonFormatPICC.setEnabled(true);
        // Buttons - Key Management
        buttonSetConfiguration.setEnabled(false);
        buttonChangeKey.setEnabled(false);
        buttonChangeKeySettings.setEnabled(false);
        // Buttons - File Management
        buttonCreateFile.setEnabled(true);
        buttonDeleteFile.setEnabled(true);
        buttonChangeFileSettings.setEnabled(false);
    }

}