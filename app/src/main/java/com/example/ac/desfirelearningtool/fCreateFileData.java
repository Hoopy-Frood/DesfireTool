package com.example.ac.desfirelearningtool;

import android.app.Activity;
import android.content.Intent;
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
 * Created by Ac on 8/10/2017.
 */

public class fCreateFileData extends Fragment {
    IMainActivityCallbacks mCallback;
    Button buttonGo;
    EditText FileID, ISOName, FileSize;
    Spinner spReadAccess, spWriteAccess, spReadWriteAccess, spChangeAccessRights;
    RadioGroup cryptoModeGroup, dataFileType;
    byte bFileType, bCommSetting;
    int iFileSize;

    public fCreateFileData() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.f_createfiledata, container, false);

        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fCreateFileData", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }

        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        spReadAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAccess);
        spWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_WriteAccess);
        spReadWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAndWriteAccess);
        spChangeAccessRights = (Spinner) rootView.findViewById(R.id.spinner_ChangeAccessRights);
        FileID = (EditText) rootView.findViewById(R.id.EditText_FileID);
        ISOName = (EditText) rootView.findViewById(R.id.EditText_ISOName);
        FileSize = (EditText) rootView.findViewById(R.id.EditText_FileSize);

        cryptoModeGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_CryptoMode);
        dataFileType =(RadioGroup) rootView.findViewById(R.id.radioGroup_DataFileType);
        populateSpinners ();



        bFileType = (byte) 0x00;
        bCommSetting = (byte) 0xff;
        iFileSize = 0;

        dataFileType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("dataFileType:", "Radio button clicked");
                // checkedId is the RadioButton selected
                cdfOnRadioButtonClicked(group, checkedId);
            }
        });

        cryptoModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("cryptoModeGroup:", "Radio button clicked");
                // checkedId is the RadioButton selected
                cdfOnRadioButtonClicked(group, checkedId);
            }
        });

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoCreateFile();
            }
        });

        return rootView;
    }

    private void populateSpinners() {
        //List<Number> https://www.mkyong.com/android/android-spinner-drop-down-list-example/

        List<String> list = new ArrayList<String>();  // There must be at least 1 key
        list.add("0");
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        list.add("6");
        list.add("7");
        list.add("8");
        list.add("9");
        list.add("10");
        list.add("11");
        list.add("12");
        list.add("13");
        list.add("Free");
        list.add("Never");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReadAccess.setAdapter(dataAdapter);
        spReadAccess.setSelection(14);
        spWriteAccess.setAdapter(dataAdapter);
        spWriteAccess.setSelection(14);
        spReadWriteAccess.setAdapter(dataAdapter);
        spReadWriteAccess.setSelection(14);
        spChangeAccessRights.setAdapter(dataAdapter);
        spChangeAccessRights.setSelection(14);
    }

    public void cdfOnRadioButtonClicked(RadioGroup group, int checkedId) {
        Log.d("cdfOnRadioButtonClicked", "initiated");
        // Is the button now checked?
        //boolean checked = ((RadioButton) view).isChecked();
        //Log.d("cdfOnRadioButtonClicked", "got ischecked");
        // Check which radio button was clicked
        switch(checkedId) {
            case R.id.radio_StandardFile:
                bFileType = (byte) 0xCD;
                Toast.makeText(getActivity().getApplicationContext(), "Standard File checked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_BackupFile:
                bFileType = (byte) 0xCB;
                Toast.makeText(getActivity().getApplicationContext(), "Backup File checked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_PlainCommunication:
                bCommSetting = (byte) 0x00;
                break;
            case R.id.radio_MACCommunication:
                bCommSetting = (byte) 0x01;

                break;
            case R.id.radio_EncryptedCommunication:
                bCommSetting = (byte) 0x03;

                break;
        }
    }



    private void onGoCreateFile(){
        boolean isIncompleteForm = false;

        if (dataFileType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a standard or backup file type", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if (FileID.getText().toString().length()!=2) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure the File ID is 1 byte", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if ((ISOName.getText().toString().length()!=4) && (ISOName.getText().toString().length()!=0)) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure the optional ISO File Name must be 2 bytes ", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if (cryptoModeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a communication setting", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if (FileSize.getText().toString().length() == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure a valid file size is entered", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        try {
            iFileSize = (parseInt(FileSize.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure a number is entered in file size", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;

        }


        if (isIncompleteForm)
            return;

        // Access Rights 2 bytes
        byte ACByte1 = (byte) ((((byte) (spReadAccess.getSelectedItemPosition())) << 4) | (((byte) (spWriteAccess.getSelectedItemPosition())) & (byte) 0x0F)) ;
        byte ACByte2 = (byte) ((((byte) (spReadWriteAccess.getSelectedItemPosition())) << 4) | (((byte) (spChangeAccessRights.getSelectedItemPosition())) & (byte) 0x0F)) ;

        Log.v("bFileType", ByteArray.byteArrayToHexString(new byte[]{ bFileType}));
        Log.v("bFileID", FileID.getText().toString());
        Log.v("baISOName", ByteArray.byteArrayToHexString(ByteArray.hexStringToByteArray(ISOName.getText().toString())));
        Log.v("bCommSetting", ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.v("Access Rights", ByteArray.byteArrayToHexString(new byte[]{ACByte1, ACByte2 }));
        Log.v("iFileSize", "FileSize = " + iFileSize);

        mCallback.onCreateFileDataReturn(bFileType,
                ByteArray.hexStringToByteArray(FileID.getText().toString())[0],
                ByteArray.hexStringToByteArray(ISOName.getText().toString()),
                bCommSetting,
                new byte[]{(byte) ACByte1, (byte) ACByte2 },
                iFileSize);
    }

}