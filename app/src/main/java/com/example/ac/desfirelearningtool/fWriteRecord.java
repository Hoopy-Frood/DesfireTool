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

import static java.lang.Integer.parseInt;

/**
 * Created by Ac on 2/20/2018.
 */

public class fWriteRecord extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGo;
    private Button buttonGetFileSettings;
    private Spinner spinnerFileID;
    private EditText etRecordNum;
    private EditText etLength;
    private EditText etDataToWrite;
    private RadioGroup commModeGroup;
    private Button buttonGetFileID;
    private MifareDesfire.commMode selCommMode;


    private byte [] fileList;
    private boolean fileListPopulated;
    private ScrollLog scrollLog;



    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_writerecord, container, false);
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
        etRecordNum = (EditText) rootView.findViewById(R.id.et_RecordNumber);
        etLength = (EditText) rootView.findViewById(R.id.et_Length);
        etDataToWrite = (EditText) rootView.findViewById(R.id.et_DataToWrite);

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
                onGoWriteRecord();
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
        List<String> list = new ArrayList<>();

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
        Log.d("fWriteData", "onRadioButtonClicked");

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
        Log.d("fWriteRecord", "onGetFileIDs");

        Bundle fileListInfo = mCallback.onFragmentGetFileIDs();
        Log.d("onGetFileIDs", "going to fill list");
        fileList = fileListInfo.getByteArray("baFileIDList");
        fileListPopulated = fileListInfo.getBoolean("bFileIDListPopulated");
        Log.d("onGetFileIDs", "after filling list to fill list");

        if (fileList != null)

            if (fileList.length > 0) {
                Log.d("fileList", "File list: " + ByteArray.byteArrayToHexString(fileList));
                populateFileIDs(fileList);
            }
    }

    public void onFileSettings() {
        Log.d("fWriteData", "onGetFileIDs");

        if (spinnerFileID.getSelectedItemPosition() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle fileSettings = mCallback.onFragmentGetFileSettings(ByteArray.hexStringToByte( (String) spinnerFileID.getSelectedItem()));
        if (!fileSettings.getBoolean("boolCommandSuccess")){
            return;
        }

        byte fileType = fileSettings.getByte("fileType");
        if ((fileType != (byte) 0x03) && (fileType != (byte) 0x04)) {
            scrollLog.appendWarning("Warning: Selected file ID is not Data File type");
            return;
        }
        byte fileCommSetting = fileSettings.getByte("commSetting");
        int currentAuthenticatedKey = fileSettings.getInt("currentAuthenticatedKey");

        byte readAccess = fileSettings.getByte("readAccess");
        byte readWriteAccess = fileSettings.getByte("readWriteAccess");

        int ifileSize = fileSettings.getByte("fileSize");
        scrollLog.appendData("File size = " + ifileSize);

        // If Free Access
        if ((readAccess == (byte)0x0E) || (readWriteAccess == (byte)0x0E)) {
            Log.d("onFileSettings", "Setting to plain text");
            scrollLog.appendData("Free read access");
            commModeGroup.check(R.id.radio_PlainCommunication);
            return;
        }

        // If No Access
        if ((readAccess == (byte)0x0F) && (readWriteAccess == (byte)0x0F)) {
            Log.d("onFileSettings", "Setting to plain text");
            scrollLog.appendWarning("Warning: No read access");
            return;
        }

        if (currentAuthenticatedKey != -1) {
            if ((currentAuthenticatedKey == (int) readAccess) || (currentAuthenticatedKey == (int) readWriteAccess)) {
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
            scrollLog.appendWarning("Warning: Read Access: " + readAccess +" and Read/Write Access: " + readWriteAccess + " but current authenticated key is " + currentAuthenticatedKey);
        } else {
            scrollLog.appendWarning("Warning: Read Access: " + readAccess +" and Read/Write Access: " + readWriteAccess + " but no key currently authenticated");
        }

    }



    private void onGoWriteRecord(){
        int iRecordNum =0;
        int iLength = 0;
        boolean isIncompleteForm = false;
        int iDataToWriteLength;

        if (spinnerFileID.getSelectedItemPosition() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a file", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (commModeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a communication method", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        iDataToWriteLength = etDataToWrite.getText().toString().length();
        if (iDataToWriteLength %2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please enter valid hex string to write", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        iDataToWriteLength = iDataToWriteLength / 2;

        if (etRecordNum.getText().toString().length() != 0) {
            try {
                iRecordNum = (parseInt(etRecordNum.getText().toString()));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure a number is entered in file size", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
        }
        if (etLength.getText().toString().length() != 0) {
            try {
                iLength = (parseInt(etLength.getText().toString()));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure a number is entered in file size", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
        }

        if (isIncompleteForm)
            return;


        if (etRecordNum.getText().toString().length() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Using Default offset of 0 ", Toast.LENGTH_SHORT).show();
            etRecordNum.setText(R.string.default_readOffset);
            iRecordNum= (parseInt(etRecordNum.getText().toString()));
        }

        if (etLength.getText().toString().length() == 0) {
            etLength.setText(R.string.default_readLength);
            iLength = (parseInt(etLength.getText().toString()));
        }



        if (iLength != iDataToWriteLength) {
            Toast.makeText(getActivity().getApplicationContext(), "Using input data length of " + iDataToWriteLength + " in length field", Toast.LENGTH_SHORT).show();
            iLength= iDataToWriteLength;
        }

        byte [] bDataToWrite = ByteArray.hexStringToByteArray(etDataToWrite.getText().toString());

        Log.d("WriteData", "Input OK");
        byte fileSelected = ByteArray.hexStringToByte( (String) spinnerFileID.getSelectedItem());



        mCallback.onWriteRecordReturn(fileSelected,
                iRecordNum,
                iLength,
                bDataToWrite,
                selCommMode
        );
    }
}