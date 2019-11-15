package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2018/03/21.
 */

public class fGetValue extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGo;
    private Button buttonGetFileSettings;
    private Spinner spinnerFileID;
    private EditText etOffset;
    private EditText etLength;
    private RadioGroup commModeGroup;
    private Button buttonGetFileID;
    private MifareDesfire.commMode selCommMode;
    private int iOffset, iLength;

    private byte [] fileList;
    private boolean fileListPopulated;
    private ScrollLog scrollLog;



    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_getvalue, container, false);
        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("onCreateView", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }



        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        spinnerFileID = (Spinner) rootView.findViewById(R.id.spinner_FileID);
        etOffset = (EditText) rootView.findViewById(R.id.et_Offset);
        etLength = (EditText) rootView.findViewById(R.id.et_Length);

        commModeGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_CommMode);
        commModeGroup.check(R.id.radio_PlainCommunication);
        buttonGetFileID = (Button) rootView.findViewById(R.id.button_GetFiles);
        buttonGetFileSettings = (Button) rootView.findViewById(R.id.button_GetFileSettings);


        fileList = getArguments().getByteArray("baFileIDList");
        fileListPopulated = getArguments().getBoolean("bFileIDListPopulated");

        if (fileListPopulated) {
            populateFileIDs(fileList);
        } else {
            populateFileIDs (new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C, (byte) 0x0D, (byte) 0x0E, (byte) 0x0F, (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x1b, (byte) 0x1c, (byte) 0x1d, (byte) 0x1e, (byte) 0x1f});

        }


        selCommMode = MifareDesfire.commMode.PLAIN;

        scrollLog = mCallback.getScrollLogObject ();

        commModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("commModeGroup:", "Radio button clicked");
                // checkedId is the RadioButton selected
                onRadioButtonClicked(group, checkedId);
            }
        });

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoGetValue();
            }
        });
        buttonGetFileID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGetFileIDs();
            }
        });
        buttonGetFileSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onFileSettings();
            }
        });
        return rootView;

    }



    private void populateFileIDs (byte[] fileIDs) {
        List <String> list = new ArrayList<>();

        list.add("--");

        for (int i = 0; i < fileIDs.length; i++) {
            list.add(ByteArray.byteToHexString(fileIDs[i]));
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFileID.setAdapter(dataAdapter);
        spinnerFileID.setSelection(0);

    }



    public void onRadioButtonClicked(RadioGroup group, int checkedId) {
        // Is the button now checked?
        Log.d("fGetValue", "onRadioButtonClicked");

        // Check which radio button was clicked
        switch(checkedId) {
            case R.id.radio_PlainCommunication:
                selCommMode = MifareDesfire.commMode.PLAIN;
                break;
            case R.id.radio_MACCommunication:
                selCommMode = MifareDesfire.commMode.MAC;
                break;
            case R.id.radio_EncryptedCommunication:
                selCommMode = MifareDesfire.commMode.ENCIPHERED;
                break;
        }
    }



    public void onGetFileIDs () {
        Log.d("fGetValue", "onGetFileIDs");

        Bundle fileListInfo = mCallback.onFragmentGetFileIds();
        fileList = fileListInfo.getByteArray("baFileIDList");
        fileListPopulated = fileListInfo.getBoolean("bFileIDListPopulated");


        if (fileList.length > 0) {
            Log.d("fileList", "File list: " + ByteArray.byteArrayToHexString(fileList));
            populateFileIDs(fileList);
        }
    }

    public void onFileSettings() {

        Log.d("fGetValue", "onFileSettings with item: " + spinnerFileID.getSelectedItem());

        if (spinnerFileID.getSelectedItemPosition() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }


        Bundle fileSettings = mCallback.onFragmentGetFileSettings(ByteArray.hexStringToByte( (String) spinnerFileID.getSelectedItem()));
        if (!fileSettings.getBoolean("boolCommandSuccess")){
            return;
        }

        byte fileType = fileSettings.getByte("fileType");
        if (fileType != (byte) 0x02) {
            scrollLog.appendWarning("Warning: Selected file ID is not Value type");
            return;
        }
        byte fileCommSetting = fileSettings.getByte("commSetting");
        int currentAuthenticatedKey = fileSettings.getInt("currentAuthenticatedKey");


        byte GVDAccess = fileSettings.getByte("GVD");
        byte GVDLCAccess = fileSettings.getByte("GVDLC");
        byte GVDLCCAccess = fileSettings.getByte("GVDLCC");
        byte LC_FreeGV_Flag = fileSettings.getByte("LC_FreeGV_Flag");

        if ((LC_FreeGV_Flag & (byte) 0x02) == (byte) 0x02) {
            Log.d("fGetValue", "Free GetValue Flag Enabled - setting to plain text ");
            scrollLog.appendData("Free Get Value access");
            commModeGroup.check(R.id.radio_PlainCommunication);
            return;
        }
        // If Free Access
        if ((GVDAccess == (byte)0x0E) || (GVDLCAccess == (byte)0x0E) || (GVDLCCAccess == (byte)0x0E)) {
            Log.d("fGetValue", "Free Access enabled, Setting to plain text");
            scrollLog.appendData("Free Get Value access");
            commModeGroup.check(R.id.radio_PlainCommunication);
            return;
        }

        // If No Access
        if ((GVDAccess == (byte)0x0F) && (GVDLCAccess == (byte)0x0F) && (GVDLCCAccess == (byte)0x0F)) {
            Log.d("fGetValue", "All settings to no access");
            scrollLog.appendWarning("Warning: No Get Value access");
            return;
        }

        if (currentAuthenticatedKey != -1) {
            if ((currentAuthenticatedKey == (int) GVDAccess) || (currentAuthenticatedKey == (int) GVDLCAccess) || (currentAuthenticatedKey == (int) GVDLCCAccess)) {
                switch (fileCommSetting) {
                    case (byte) 0x00:
                        scrollLog.appendData("Key required authenticated; Plaintext communication");
                        commModeGroup.check(R.id.radio_PlainCommunication);
                        break;
                    case (byte) 0x01:
                        scrollLog.appendData("Key required authenticated; MAC communication");
                        commModeGroup.check(R.id.radio_MACCommunication);
                        break;
                    case (byte) 0x03:
                        scrollLog.appendData("Key required authenticated; Encrypted communication");
                        commModeGroup.check(R.id.radio_EncryptedCommunication);
                        break;
                    default:
                        //toast a warning
                        scrollLog.appendWarning("Key required authenticated; Communication unknown");
                }
                return;
            }
            scrollLog.appendWarning("Warning: Correct key is not authenticated.  GetValue/Debit Access: " + GVDAccess + " and GV/D/Limited Credit Access: " + GVDLCAccess + " and GV/D/LC/Credit Access: " + GVDLCCAccess +  " but current authenticated key is " + currentAuthenticatedKey);
        } else {
            scrollLog.appendWarning("Warning: No key is authenticated.  GetValue/Debit Access: " + GVDAccess + " and GV/D/Limited Credit Access: " + GVDLCAccess + " and GV/D/LC/Credit Access: " + GVDLCCAccess);
        }

    }



    private void onGoGetValue(){
        boolean isIncompleteForm = false;

        if (spinnerFileID.getSelectedItemPosition() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a file", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (commModeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a communication method", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (isIncompleteForm)
            return;

        Log.d("GetValue", "Input OK");
        byte fileSelected = ByteArray.hexStringToByte( (String) spinnerFileID.getSelectedItem());



        mCallback.onGetValueReturn(fileSelected,
                selCommMode
        );
    }
}