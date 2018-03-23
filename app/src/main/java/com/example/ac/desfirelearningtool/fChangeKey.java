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

import static com.example.ac.desfirelearningtool.ByteArray.hexStringToByteArray;

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
    private ListView lvKeyList;
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

        tvCurrentAuthenticatedKey = (TextView) rootView.findViewById(R.id.tv_CurrentAuthenticatedKey);
        lvKeyList = rootView.findViewById(R.id.lv_KeyList);

        populateSpinners (spKeyToChange,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14"});
        populateSpinners (spChangeKeyKey,new String[] {"0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","Same","Never"});

        populateListView ();
        if (currAuthKey == -1)
            tvCurrentAuthenticatedKey.setText("None");
        else
            tvCurrentAuthenticatedKey.setText(String.valueOf(currAuthKey));

        lvKeyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                etNewKey.setText((String)lvKeyList.getItemAtPosition(position));
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

    }

    private void onGoChangeKey(){
        boolean isIncompleteForm = false;

        int inputKeyLength = etNewKey.getText().toString().length();
        if (inputKeyLength%2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is hexadecimal", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (inputKeyLength == 0) {
            switch(currAuthMode) {
                case (byte) 0x0A:
                    etNewKey.setText("0000000000000000");
                    inputKeyLength = 16;
                    break;
                case (byte) 0x1A:
                    etNewKey.setText("000000000000000000000000000000000000000000000000");
                    inputKeyLength = 48;
                    break;
                case (byte) 0xAA:
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


        switch (currAuthMode){
            case (byte)0x0A:
                if ((inputKeyLength != 16) && (inputKeyLength != 32) && (inputKeyLength != 48)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
            case (byte)0x1A:
                if ((inputKeyLength != 16) && (inputKeyLength != 32) && (inputKeyLength != 48)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key 8, 16 or 24 bytes", Toast.LENGTH_SHORT).show();
                    isIncompleteForm = true;
                }
                break;
            case (byte)0xAA:
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



        if (isIncompleteForm)
            return;

        Log.d("ChangeKey", "Input OK");
        byte bKeyChosen = (byte) (spKeyToChange.getSelectedItemPosition());

        byte [] baNewKey = ByteArray.hexStringToByteArray(etNewKey.getText().toString());
        byte [] baOldKey = null;

        mCallback.onChangeKeyReturn(bKeyChosen,(byte)0x00, baNewKey, baOldKey);

    }
}
