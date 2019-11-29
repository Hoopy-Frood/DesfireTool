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

public class fChangeKey  extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGoChangeKey;
    private Button buttonGetKeySettings;

    private Spinner spKeyToChange;
    private Spinner spChangeKeyKey;
    private TextView tvCurrentAuthenticatedKey;
    private EditText etNewKey;
    private EditText etOldKey;
    private EditText etKeyVersion;
    private ListView lvKeyList;
    private TextView tvOldKey;
    private int currAuthKey;
    private byte currAuthMode;




    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_changekey, container, false);
        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fGetKeyVersion", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }


        currAuthKey = getArguments().getInt("currentAuthenticatedKey");
        currAuthMode = getArguments().getByte("currentAuthenticationMode");

        buttonGoChangeKey = (Button) rootView.findViewById(R.id.button_Go);
        buttonGetKeySettings = (Button) rootView.findViewById(R.id.button_GetKeySettings);

        spKeyToChange = (Spinner) rootView.findViewById(R.id.spinner_KeyToChange);
        spChangeKeyKey = (Spinner) rootView.findViewById(R.id.spinner_ChangeKeyKey);

        etNewKey = (EditText) rootView.findViewById(R.id.EditText_NewKey);
        etOldKey = (EditText) rootView.findViewById(R.id.EditText_OldKey);
        tvOldKey = (TextView) rootView.findViewById(R.id.tv_OldKeyText);
        etKeyVersion = (EditText) rootView.findViewById(R.id.EditText_NewKeyVersion);

        tvCurrentAuthenticatedKey = (TextView) rootView.findViewById(R.id.tv_CurrentAuthenticatedKey);
        lvKeyList = rootView.findViewById(R.id.lv_KeyList);

        populateSpinners (spKeyToChange,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14"});
        populateSpinners (spChangeKeyKey,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","Same Key","Frozen"});

        populateListView ();
        if (currAuthKey == -1) {
            tvCurrentAuthenticatedKey.setText("None");
            Toast.makeText(getActivity().getApplicationContext(), "Please perform authentication before change key", Toast.LENGTH_SHORT).show();
        } else {
            tvCurrentAuthenticatedKey.setText(String.valueOf(currAuthKey));
        }

        lvKeyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                etNewKey.setText((String)lvKeyList.getItemAtPosition(position));
            }
        });

        spKeyToChange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currAuthKey != spKeyToChange.getSelectedItemPosition()-1) {
                    // Set visibility of old key
                    etOldKey.setVisibility(View.VISIBLE);
                    tvOldKey.setVisibility(View.VISIBLE);
                } else {
                    etOldKey.setVisibility(View.GONE);
                    tvOldKey.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                etOldKey.setVisibility(View.GONE);
            }
        });


        buttonGoChangeKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoChangeKey();
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

    private void populateListView () {
        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        List<String> ListKeyText = new ArrayList<String>();

        ListKeyText.add("0000000000000000");
        ListKeyText.add("00000000000000000000000000000000");
        ListKeyText.add("000000000000000000000000000000000000000000000000");
        ListKeyText.add("00112233445566778899AABBCCDDEEFF");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                ListKeyText);

        lvKeyList.setAdapter(arrayAdapter);
        lvKeyList.setClickable(true);
    }

    private void onGoGetKeySettings () {

        Log.d("fChangeKey", "onGoGetKeySettings");

        byte[] bKeySettings = mCallback.onFragmentGetKeySettings();
        if (bKeySettings == null){
            return;
        }

        spChangeKeyKey.setSelection((bKeySettings[0] >> 4)+1);

        Log.d("onGoGetKeySettings", "Change Key Key = " + ByteArray.byteToHexString((byte)(bKeySettings[0] >> 4)));
        if ((spChangeKeyKey.getSelectedItemPosition()-1) != currAuthKey) {
            Toast.makeText(getActivity().getApplicationContext(), "Current Authentication Key is not change key key", Toast.LENGTH_SHORT).show();
        }

    }

    private void onGoChangeKey() {
        boolean isIncompleteForm = false;

        byte [] baNewKey = null;
        byte [] baOldKey = null;
        byte bKeyChosen = (byte) (spKeyToChange.getSelectedItemPosition()-1);

        // NEW KEY INPUT
        int inputKeyLength = etNewKey.getText().toString().length();
        if (inputKeyLength % 2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is hexadecimal", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (inputKeyLength == 0) {
            switch (currAuthMode) {
                case (byte) 0x0A:
                    etNewKey.setText("0000000000000000");
                    inputKeyLength = 16;
                    break;
                case (byte) 0x1A:
                    etNewKey.setText("000000000000000000000000000000000000000000000000");
                    inputKeyLength = 48;
                    break;
                case (byte) 0xAA:
                case (byte) 0x71:
                case (byte) 0x77:
                    etNewKey.setText("00000000000000000000000000000000");
                    inputKeyLength = 32;
                    break;
                default:
                    etNewKey.setText("0000000000000000");
                    inputKeyLength = 16;
                    break;
            }
            Toast.makeText(getActivity().getApplicationContext(), "Using Default Key of 0x00 bytes", Toast.LENGTH_SHORT).show();
        }


        switch (currAuthMode) {
            case (byte) 0x0A:
                if ((inputKeyLength != 16) && (inputKeyLength != 32) && (inputKeyLength != 48)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
            case (byte) 0x1A:
                if ((inputKeyLength != 16) && (inputKeyLength != 32) && (inputKeyLength != 48)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
            case (byte) 0xAA:
            case (byte) 0x71:
            case (byte) 0x77:
                if ((inputKeyLength != 32)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is 16 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
            default:
                if ((inputKeyLength != 16) && (inputKeyLength != 32) && (inputKeyLength != 48)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
        }


        // OLD KEY INPUT
        if (currAuthKey != bKeyChosen) {
            int inputOldKeyLength = etOldKey.getText().toString().length();
            if (inputOldKeyLength % 2 == 1) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is hexadecimal", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }

            if (inputOldKeyLength == 0) {
                switch (currAuthMode) {
                    case (byte) 0x0A:
                        etOldKey.setText("0000000000000000");
                        inputOldKeyLength = 16;
                        break;
                    case (byte) 0x1A:
                        etOldKey.setText("000000000000000000000000000000000000000000000000");
                        inputOldKeyLength = 48;
                        break;
                    case (byte) 0xAA:
                    case (byte) 0x71:
                    case (byte) 0x77:
                        etOldKey.setText("00000000000000000000000000000000");
                        inputOldKeyLength = 32;
                        break;
                    default:
                        etOldKey.setText("0000000000000000");
                        inputOldKeyLength = 16;
                        break;
                }
                Toast.makeText(getActivity().getApplicationContext(), "Using Default Key of 0x00 bytes", Toast.LENGTH_SHORT).show();
            }


            switch (currAuthMode) {
                case (byte) 0x0A:
                    if ((inputOldKeyLength != 16) && (inputOldKeyLength != 32) && (inputOldKeyLength != 48)) {
                        Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                        isIncompleteForm = true;
                    }
                    break;
                case (byte) 0x1A:
                    if ((inputOldKeyLength != 16) && (inputOldKeyLength != 32) && (inputOldKeyLength != 48)) {
                        Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                        isIncompleteForm = true;
                    }
                    break;
                case (byte) 0xAA:
                case (byte) 0x71:
                case (byte) 0x77:
                    if ((inputOldKeyLength != 32)) {
                        Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is 16 bytes", Toast.LENGTH_SHORT).show();
                        isIncompleteForm = true;
                    }
                    break;
                default:
                    if ((inputOldKeyLength != 16) && (inputOldKeyLength != 32) && (inputOldKeyLength != 48)) {
                        Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                        isIncompleteForm = true;
                    }
                    break;
            }
        }

        // KEY VERSION INPUT
        int versionLength = etKeyVersion.getText().toString().length();
        if (versionLength % 2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is hexadecimal", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (versionLength == 0) {
            etKeyVersion.setText("00");
            versionLength = 2;
            Toast.makeText(getActivity().getApplicationContext(), "Using Default Key Version of 00", Toast.LENGTH_SHORT).show();
        } else if (versionLength != 2) {
            Toast.makeText(getActivity().getApplicationContext(), "Please input 1 byte hexidecimal key version", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }



        if (isIncompleteForm)
            return;

        Log.d("ChangeKey", "Input OK");
        baNewKey = ByteArray.hexStringToByteArray(etNewKey.getText().toString());
        baOldKey = ByteArray.hexStringToByteArray(etOldKey.getText().toString());
        byte bKeyVersion = ByteArray.hexStringToByte(etKeyVersion.getText().toString());

        mCallback.onChangeKeyReturn(bKeyChosen, bKeyVersion, baNewKey, baOldKey);

    }
}
