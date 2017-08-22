package com.example.ac.desfirelearningtool;


import android.support.v4.app.Fragment;

import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2017/08/21.
 */


public class fCreateApplication extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGo;
    private Spinner spinnerNumberOfKeys,spinnerChangeUsingSpecificKey;
    private EditText appID;
    private EditText ISOName;
    private EditText DFName;
    private byte KeySettingByte1;
    private byte KeySettingByte2;
    private RadioGroup cryptoMethodGroup;
    private RadioGroup changeKeyAccessGroup;
    private CheckBox cbAllowAMKChange;
    private CheckBox cbFreeDirListAccess;
    private CheckBox cbFreeCreateDeleteFiles;
    private CheckBox cbAMKSettingChangeable;
    private CheckBox cbUser2ByteFileID;



    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_createapplication, container, false);
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
        spinnerNumberOfKeys = (Spinner) rootView.findViewById(R.id.spinner_NumOfKeys);
        spinnerChangeUsingSpecificKey =  (Spinner) rootView.findViewById(R.id.spinner_ChangeUsingSpecificKey );
        appID = (EditText) rootView.findViewById(R.id.EditText_ApplicationID);
        ISOName = (EditText) rootView.findViewById(R.id.EditText_ISOFileID);
        DFName = (EditText) rootView.findViewById(R.id.EditText_DFName);

        cryptoMethodGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_Crypto);
        changeKeyAccessGroup =(RadioGroup) rootView.findViewById(R.id.radioGroup_KeyAccessRights);

        cbAllowAMKChange = (CheckBox) rootView.findViewById(R.id.CheckBox_AllowAMKChange);
        cbFreeDirListAccess = (CheckBox) rootView.findViewById(R.id.CheckBox_FreeDirListAccess);
        cbFreeCreateDeleteFiles = (CheckBox) rootView.findViewById(R.id.CheckBox_FreeCreateDeleteFiles);
        cbAMKSettingChangeable = (CheckBox) rootView.findViewById(R.id.CheckBox_AMKSettingChangeable);
        cbUser2ByteFileID = (CheckBox) rootView.findViewById(R.id.CheckBox_Use2ByteFileID);

        populateSpinners ();



        KeySettingByte1 = 0x00;
        KeySettingByte2 = 0x00;


        changeKeyAccessGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("changeKeyAccessGroup:", "Radio button clicked");
                // checkedId is the RadioButton selected
                onRadioButtonClicked(group, checkedId);
            }
        });

        cryptoMethodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("cryptoMethodGroup:", "Radio button clicked");
                // checkedId is the RadioButton selected
                onRadioButtonClicked(group, checkedId);
            }
        });

        cbAllowAMKChange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("cbEnableFreeGV:", "Check Box checked ");
                // checkedId is the RadioButton selected
                onCheckBoxClicked(cb.getId(), IsChecked);
            }
        });


        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoCreateApplication();
            }
        });

        return rootView;

    }


    private void populateSpinners(){
        //List<Number> https://www.mkyong.com/android/android-spinner-drop-down-list-example/

        List<String> list = new ArrayList<String>();  // There must be at least 1 key
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
        list.add("14");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNumberOfKeys.setAdapter(dataAdapter);
        spinnerNumberOfKeys.setSelection(2);

        List<String> specificKeylist = new ArrayList<String>();
        specificKeylist.add("1");
        specificKeylist.add("2");
        specificKeylist.add("3");
        specificKeylist.add("4");
        specificKeylist.add("5");
        specificKeylist.add("6");
        specificKeylist.add("7");
        specificKeylist.add("8");
        specificKeylist.add("9");
        specificKeylist.add("10");
        specificKeylist.add("11");
        specificKeylist.add("12");
        specificKeylist.add("13");
        ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, specificKeylist);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChangeUsingSpecificKey.setAdapter(dataAdapter2);
        spinnerChangeUsingSpecificKey.setEnabled(false);
    }



    public void onRadioButtonClicked(RadioGroup group, int checkedId) {
        // Is the button now checked?
        Log.d("onRadioButtonClicked", "initiated");

        // Check which radio button was clicked
        switch(checkedId) {
            case R.id.radio_ChangeKeyAfterAuthMasterKey:
                KeySettingByte1 &= 0x0F;
                spinnerChangeUsingSpecificKey.setEnabled(false);
                break;
            case R.id.radio_ChangeKeyAfterAuthSameKey:
                    KeySettingByte1 &= 0x0F;
                    KeySettingByte1 |= 0xE0;
                    spinnerChangeUsingSpecificKey.setEnabled(false);
                break;
            case R.id.radio_ChangeKeyAfterAuthSpecificKey:
                    spinnerChangeUsingSpecificKey.setEnabled(true);
                    KeySettingByte1 &= 0x0F;
                    KeySettingByte1 |= 0x10;  // Placeholder to check during button press
                break;
            case R.id.radio_ChangeKeyFrozen:
                    KeySettingByte1 &= 0x0F;
                    KeySettingByte1 |= 0xF0;
                    spinnerChangeUsingSpecificKey.setEnabled(false);
                break;
            case R.id.radio_DES:
                    KeySettingByte2 &= 0x3F;
                break;
            case R.id.radio_3K3DES:
                    KeySettingByte2 &= 0x3F;
                    KeySettingByte2 |= 0x40;
                break;
            case R.id.radio_AES:
                    KeySettingByte2 &= 0x3F;
                    KeySettingByte2 |= 0x80;
                break;
        }
    }
    public void onCheckBoxClicked(int checkId, boolean isChecked) {
        // Is the button now checked?

        // Check which radio button was clicked
        switch(checkId) {
            case R.id.CheckBox_AllowAMKChange:
                if (isChecked) {
                    KeySettingByte1 |= 0x01;
                    //Toast.makeText(getActivity().getApplicationContext(), "Allow AMK Change " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }else {
                    KeySettingByte1 &= 0xFE;
                    //Toast.makeText(getActivity().getApplicationContext(), "Allow AMK Change " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.CheckBox_FreeDirListAccess:
                if (isChecked) {
                    KeySettingByte1 |= 0x02;
                    // Toast.makeText(getApplicationContext(), "CheckBox_FreeDirListAccess " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }else {
                    KeySettingByte1 &= 0xFD;
                    // Toast.makeText(getApplicationContext(), "CheckBox_FreeDirListAccess " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.CheckBox_FreeCreateDeleteFiles:
                if (isChecked) {
                    KeySettingByte1 |= 0x04;
                    // Toast.makeText(getApplicationContext(), "CheckBox_FreeCreateDeleteFiles " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }else {
                    KeySettingByte1 &= 0xFB;
                    // Toast.makeText(getApplicationContext(), "CheckBox_FreeCreateDeleteFiles " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.CheckBox_AMKSettingChangeable:
                if (isChecked) {
                    KeySettingByte1 |= 0x08;
                    // Toast.makeText(getApplicationContext(), "CheckBox_AMKSettingChangeable " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }else {
                    KeySettingByte1 &= 0xF7;
                    // Toast.makeText(getApplicationContext(), "CheckBox_AMKSettingChangeable " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.CheckBox_Use2ByteFileID:
                if (isChecked) {
                    KeySettingByte2 |= 0x20;
                    // Toast.makeText(getApplicationContext(), "CheckBox_Use2ByteFileID " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte2}), Toast.LENGTH_SHORT).show();
                }else {
                    KeySettingByte2 &= 0xDF;
                    // Toast.makeText(getApplicationContext(), "CheckBox_Use2ByteFileID " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte2}), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    private void onGoCreateApplication(){
        boolean isIncompleteForm = false;
        if (appID.getText().toString().length()!=6) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure the Application ID is 3 bytes", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if ((ISOName.getText().toString().length()!=4) && (ISOName.getText().toString().length()!=0)) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure the Optional ISO File Name must be 2 bytes ", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if (DFName.getText().toString().length()%2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure Optional DF Name is 0-16 bytes", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (cryptoMethodGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a crypto method", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }
        if (changeKeyAccessGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please select a change key access right", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }


        KeySettingByte2 &= 0xF0;
        KeySettingByte2 |= (byte) (spinnerNumberOfKeys.getSelectedItemPosition() + 1);
        if ( (byte) (KeySettingByte1 & 0xF0) == (byte) 0x10) {
            KeySettingByte1 = (byte) (((byte) (spinnerChangeUsingSpecificKey.getSelectedItemPosition() + 1)) << 4);
            //Toast.makeText(getApplicationContext(), "spinnerChangeUsingSpecificKey " + ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}), Toast.LENGTH_SHORT).show();
        }

        if (isIncompleteForm)
            return;

        Log.d("CreateApplication", "Input OK");

        mCallback.onCreateApplicationReturn(
                ByteArray.hexStringToByteArray(appID.getText().toString()),
                KeySettingByte1,
                KeySettingByte2,
                ByteArray.hexStringToByteArray(ISOName.getText().toString()),
                ByteArray.hexStringToByteArray(DFName.getText().toString())
        );

        /*
        ByteArray returnCreateFileByteArray = new ByteArray();

        returnCreateFileByteArray.append(ByteArray.hexStringToByteArray(appID.getText().toString()))
                .append(KeySettingByte1)
                .append(KeySettingByte1);

        if (ISOName.getText().toString().length() == 4) {
            returnCreateFileByteArray.append(ByteArray.hexStringToByteArray(ISOName.getText().toString()));
        }
        if (DFName.getText().toString().length() != 0) {
            returnCreateFileByteArray.append(ByteArray.hexStringToByteArray(DFName.getText().toString()));
        }
        Log.v("appID", appID.getText().toString());
        Log.v("KeySettingByte1", ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte1}));
        Log.v("KeySettingByte2", ByteArray.byteArrayToHexString(new byte[]{(byte) KeySettingByte2}));

        Log.v("onGoCreateApplication", ByteArray.byteArrayToHexString(returnCreateFileByteArray.toArray()));

        Intent resultIntent = new Intent();


        resultIntent.putExtra("CREATEAPPLICATIONSTRING",returnCreateFileByteArray.toArray() );
        setResult(Activity.RESULT_OK, resultIntent);

        finish();
        */

    }

}