package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2018/03/23.
 */

public class fChangeFileSettings extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGoChangeFileSettings;

    private Button buttonGetFileIDs;
    private Spinner spFileIDs;
    private Button buttonGetFileSettings;
    RadioGroup rgSecondAppIndicator, rgCryptoMode, rgAddArPresent;
    Spinner spReadAccess, spWriteAccess, spReadWriteAccess, spChangeAccessRights;
    boolean boolTargetSecondaryApp;
    boolean boolAddArPrsent;
    byte bCommSetting;
    private byte [] fileList;
    private boolean fileListPopulated;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_changekeysettings, container, false);
        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fChangeKeySettings", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }

        spFileIDs = rootView.findViewById(R.id.spinner_FileID);
        buttonGoChangeFileSettings =  rootView.findViewById(R.id.button_Go);
        buttonGetFileIDs = rootView.findViewById(R.id.button_GetFiles);
        buttonGetFileSettings = rootView.findViewById(R.id.button_GetFileSettings);

        rgSecondAppIndicator = rootView.findViewById(R.id.radioGroup_SecondAppIndicator);
        rgCryptoMode = rootView.findViewById(R.id.radioGroup_CommMode);
        rgAddArPresent = rootView.findViewById(R.id.radioGroup_AdditionalAccessRights);

        spReadAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAccess);
        spWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_WriteAccess);
        spReadWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAndWriteAccess);
        spChangeAccessRights = (Spinner) rootView.findViewById(R.id.spinner_ChangeAccessRights);




        populateSpinners (spFileIDs,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14"});
        populateSpinners (spReadAccess,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","Free","Never"});
        populateSpinners (spWriteAccess,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","Free","Never"});
        populateSpinners (spReadWriteAccess,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","Free","Never"});
        populateSpinners (spChangeAccessRights,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","Free","Never"});

        rgSecondAppIndicator.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("fChangeFileSettings:", "Radio button clicked");
                // checkedId is the RadioButton selected
                cdfOnRadioButtonClicked(group, checkedId);
            }
        });
        rgCryptoMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("fChangeFileSettings:", "Radio button clicked");
                // checkedId is the RadioButton selected
                cdfOnRadioButtonClicked(group, checkedId);
            }
        });
        rgAddArPresent.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("fChangeFileSettings:", "Radio button clicked");
                // checkedId is the RadioButton selected
                cdfOnRadioButtonClicked(group, checkedId);
            }
        });

        buttonGoChangeFileSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonGoChangeFileSettings();
            }
        });

        buttonGetFileIDs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGetFileIDs();
            }
        });
        buttonGetFileSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoGetFileSettings();
            }
        });



        return rootView;

    }

    private void populateSpinners (Spinner targetSpinner, String[] sKeyIDs) {
        List <String> list = new ArrayList<>();

        list.add("--");
        for (int i = 0; i < sKeyIDs.length; i++) {
            list.add(sKeyIDs[i]);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetSpinner.setAdapter(dataAdapter);
        targetSpinner.setSelection(0);

    }

    public void cdfOnRadioButtonClicked(RadioGroup group, int checkedId) {
        Log.d("cdfOnRadioButtonClicked", "initiated");
        // Is the button now checked?
        //boolean checked = ((RadioButton) view).isChecked();
        //Log.d("cdfOnRadioButtonClicked", "got ischecked");
        // Check which radio button was clicked
        switch(checkedId) {
            case R.id.radio_TargetPrimaryApplication:
                boolTargetSecondaryApp = false;
                Toast.makeText(getActivity().getApplicationContext(), "Target Primary Application", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_TargetSecondaryApplication:
                boolTargetSecondaryApp = true;
                Toast.makeText(getActivity().getApplicationContext(), "Target Secondary Application", Toast.LENGTH_SHORT).show();
                break;
            case R.id.radio_AddArNotPresent:
                boolAddArPrsent = false;
                break;
            case R.id.radio_AddArPresent:
                boolAddArPrsent = true;
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


    public void onGetFileIDs () {
        Log.d("fReadData", "onGetFileIDs");

        Bundle fileListInfo = mCallback.onFragmentGetFileIds();
        fileList = fileListInfo.getByteArray("baFileIDList");
        fileListPopulated = fileListInfo.getBoolean("bFileIDListPopulated");


        if (fileList.length > 0) {
            Log.d("fileList", "File list: " + ByteArray.byteArrayToHexString(fileList));
            populateFileIDs(fileList);
        }
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
        spFileIDs.setAdapter(dataAdapter);
        spFileIDs.setSelection(0);

    }



    private void onGoGetFileSettings () {

        Log.d("fChangeFileSettings", "onGoGetFileSettings");

        byte bFileID = (byte) spFileIDs.getSelectedItemId();
        if (bFileID == (byte) 0x00) {

        }

        Bundle  bKeySettings = mCallback.onFragmentGetFileSettings(bFileID);
        if (bKeySettings == null){
            return;
        }

//        spChangeKeyKey.setSelection((bKeySettings[0] >> 4)+1);
//        Log.d("onGoGetKeySettings", "Change Key Key = " + ByteArray.byteToHexString((byte)(bKeySettings[0] >> 4)));
//
//        boolAllowAMKChange = (bKeySettings[0] & 0x01) == 0x01;
//        cbMKChangeable.setChecked(boolAllowAMKChange);
//        boolFreeCreateDeleteFiles = (bKeySettings[0] & 0x02) == 0x02;
//        cbFreeCreateDelete.setChecked(boolFreeCreateDeleteFiles);
//        boolFreeDirAccess = (bKeySettings[0] & 0x04) == 0x04;
//        cbFreeDirAccess.setChecked(boolFreeDirAccess);
//        boolKeySettingsChangeable = (bKeySettings[0] & 0x08) == 0x08;
//        cbKeySettingsChangeable.setChecked(boolKeySettingsChangeable);

    }

    private void buttonGoChangeFileSettings(){
//
//        int iChangeKeyKey = (spChangeKeyKey.getSelectedItemPosition() - 1);
//        Log.i("onGoChangeKeySettings", "Change Key Key index" + iChangeKeyKey);
//
//        mCallback.onChangeKeySettingsReturn(iChangeKeyKey,boolAllowAMKChange, boolFreeCreateDeleteFiles, boolFreeDirAccess, boolKeySettingsChangeable);

    }
}
