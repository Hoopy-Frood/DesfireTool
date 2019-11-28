package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2018/03/23.
 */

public class fChangeKeySettings extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGoChangeKeySettings;
    private Button buttonGetKeySettings;

    private Spinner spChangeKeyKey;
    private CheckBox cbAllowAMKChange;
    private CheckBox cbFreeDirListAccess;
    private CheckBox cbFreeCreateDeleteFiles;
    private CheckBox cbKeySettingsChangeable;
    private int currAuthKey;
    private byte currAuthMode;
    private boolean boolAllowAMKChange;
    private boolean boolFreeDirListAccess;
    private boolean boolFreeCreateDeleteFiles;
    private boolean boolKeySettingsChangeable;



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


        currAuthKey = getArguments().getInt("currentAuthenticatedKey");
        currAuthMode = getArguments().getByte("currentAuthenticationMode");

        buttonGoChangeKeySettings = (Button) rootView.findViewById(R.id.button_Go);
        buttonGetKeySettings = (Button) rootView.findViewById(R.id.button_GetKeySettings);

        spChangeKeyKey = (Spinner) rootView.findViewById(R.id.spinner_ChangeKeyKey);
        cbAllowAMKChange = (CheckBox) rootView.findViewById(R.id.CheckBox_AllowAMKChange);
        cbFreeDirListAccess = (CheckBox) rootView.findViewById(R.id.CheckBox_FreeDirListAccess);
        cbFreeCreateDeleteFiles = (CheckBox) rootView.findViewById(R.id.CheckBox_FreeCreateDeleteFiles);
        cbKeySettingsChangeable = (CheckBox) rootView.findViewById(R.id.CheckBox_KeySettingsChangeable);


        cbAllowAMKChange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("cbAllowAMKChange:", "Check Box checked ");
                // checkedId is the RadioButton selected
                onCheckBoxClicked(cb.getId(), IsChecked);
            }
        });

        cbFreeDirListAccess.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("cbFreeDirListAccess:", "Check Box checked ");
                // checkedId is the RadioButton selected
                onCheckBoxClicked(cb.getId(), IsChecked);
            }
        });
        cbFreeCreateDeleteFiles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("cbFreeCreateDelFiles:", "Check Box checked ");
                // checkedId is the RadioButton selected
                onCheckBoxClicked(cb.getId(), IsChecked);
            }
        });
        cbKeySettingsChangeable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("cbAMKSettingChangeable:", "Check Box checked ");
                // checkedId is the RadioButton selected
                onCheckBoxClicked(cb.getId(), IsChecked);
            }
        });

        populateSpinners (spChangeKeyKey,new String[] {"" +
                "0 - AMK to change keys",
                "1 - Key 1 to change keys",
                "2 - Key 2 to change keys",
                "3 - Key 3 to change keys",
                "4 - Key 4 to change keys",
                "5 - Key 5 to change keys",
                "6 - Key 6 to change keys",
                "7 - Key 7 to change keys",
                "8 - Key 8 to change keys",
                "9 - Key 9 to change keys",
                "10 - Key 10 to change keys",
                "11 - Key 11 to change keys",
                "12 - Key 12 to change keys",
                "13 - Key 13 to change keys",
                "14 - Key 14 to change keys",
                "Same - Auth with same key",
                "All Keys in application are frozen"});


        buttonGoChangeKeySettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoChangeKeySettings();
            }
        });
        buttonGetKeySettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoGetKeySettings();
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

    public void onCheckBoxClicked(int checkId, boolean isChecked) {
        // Is the button now checked?

        // Check which radio button was clicked
        switch(checkId) {
            case R.id.CheckBox_AllowAMKChange:
                boolAllowAMKChange = isChecked;
                Toast.makeText(getActivity().getApplicationContext(), "Allow Changing Master Key =" + isChecked,   Toast.LENGTH_SHORT).show();
                break;
            case R.id.CheckBox_FreeDirListAccess:
                boolFreeDirListAccess = isChecked;
                Toast.makeText(getActivity().getApplicationContext(), "Free Directory Access = " + isChecked,   Toast.LENGTH_SHORT).show();
                break;
            case R.id.CheckBox_FreeCreateDeleteFiles:
                boolFreeCreateDeleteFiles = isChecked;
                Toast.makeText(getActivity().getApplicationContext(), "Free Create / Delete Files = " + isChecked,   Toast.LENGTH_SHORT).show();
                break;
            case R.id.CheckBox_KeySettingsChangeable:
                boolKeySettingsChangeable = isChecked;
                Toast.makeText(getActivity().getApplicationContext(), "Key Settings is changable = " + isChecked,   Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void onGoGetKeySettings () {

        Log.d("fChangeKey", "onGoGetKeySettings");

        byte[] bKeySettings = mCallback.onFragmentGetKeySettings();
        if (bKeySettings == null){
            return;
        }

        spChangeKeyKey.setSelection((bKeySettings[0] >> 4)+1);
        Log.d("onGoGetKeySettings", "Change Key Key = " + ByteArray.byteToHexString((byte)(bKeySettings[0] >> 4)));

    }

    private void onGoChangeKeySettings(){
        boolean isIncompleteForm = false;

        int iChangeKeyKey = (spChangeKeyKey.getSelectedItemPosition()-1);

        mCallback.onChangeKeySettingsReturn(iChangeKeyKey,boolAllowAMKChange, boolFreeCreateDeleteFiles, boolFreeDirListAccess, boolKeySettingsChangeable);

    }
}
