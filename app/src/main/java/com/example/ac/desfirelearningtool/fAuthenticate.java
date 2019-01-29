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
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2017/09/20.
 */


public class fAuthenticate extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGo;
    private Spinner spKeyNo;
    private RadioGroup rgAuthenticateGroup;
    private EditText etKey;
    private ListView lvKeyList;
    byte bAuthCmd;


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_authenticate, container, false);
        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("onCreateView", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }

        bAuthCmd = (byte)0x00;

        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        spKeyNo = (Spinner) rootView.findViewById(R.id.spinner_KeyID);
        etKey = rootView.findViewById(R.id.EditText_Key);
        lvKeyList = rootView.findViewById(R.id.lv_KeyList);
        rgAuthenticateGroup = rootView.findViewById(R.id.radioGroup_Authentication);

        rgAuthenticateGroup.check(R.id.radio_authD40);
        populateSpinners ();
        rgAuthenticateGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("rgRuthenticateGroup:", "Radio button clicked");
                // checkedId is the RadioButton selected
                onRadioButtonClicked(group, checkedId);
            }
        });
        populateListView ();
        //lvFileIDList.setOnItemClickListener();

        lvKeyList.setClickable(true);
        lvKeyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                etKey.setText((String)lvKeyList.getItemAtPosition(position));
            }
        });

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoAuthenticate();
            }
        });

        return rootView;

    }


    private void populateSpinners(){
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

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKeyNo.setAdapter(dataAdapter);
        spKeyNo.setSelection(0);

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

    }

    public void onRadioButtonClicked(RadioGroup group, int checkedId) {
        // Is the button now checked?
        Log.d("onRadioButtonClicked", "initiated");

        // Check which radio button was clicked
        switch(checkedId) {
            case R.id.radio_authD40:
                bAuthCmd = (byte)0x0A;
                break;
            case R.id.radio_authISO:
                bAuthCmd = (byte)0x1A;
                break;
            case R.id.radio_authAES:
                bAuthCmd = (byte)0xAA;
                break;
            case R.id.radio_authEV2:
                bAuthCmd = (byte)0x71;
                break;
        }
    }


    private void onGoAuthenticate(){
        boolean isIncompleteForm = false;

        int inputKeyLength = etKey.getText().toString().length();
        if (inputKeyLength%2 == 1) {
            Toast.makeText(getActivity().getApplicationContext(), "Please ensure Key is hexadecimal", Toast.LENGTH_SHORT).show();
            isIncompleteForm = true;
        }

        if (inputKeyLength == 0) {
            switch(bAuthCmd) {
                case (byte) 0x0A:
                    etKey.setText("0000000000000000");
                    inputKeyLength = 16;
                    break;
                case (byte) 0x1A:
                    etKey.setText("000000000000000000000000000000000000000000000000");
                    inputKeyLength = 48;
                    break;
                case (byte) 0xAA:
                case (byte) 0x71:
                    etKey.setText("00000000000000000000000000000000");
                    inputKeyLength = 32;
                    break;
                default:
                    etKey.setText("0000000000000000");
                    inputKeyLength = 16;
                    break;
            }
            Toast.makeText(getActivity().getApplicationContext(), "Using Default Key of 0x00 bytes", Toast.LENGTH_SHORT).show();
        }

        switch (bAuthCmd){
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
            case (byte)0x71:
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
                bAuthCmd = (byte) 0x0A;
                break;
        }



        if (isIncompleteForm)
            return;

        Log.d("CreateApplication", "Input OK");
        byte bKeyChosen = (byte) (spKeyNo.getSelectedItemPosition());

        mCallback.onAuthenticateReturn(
                bAuthCmd,
                bKeyChosen,
                ByteArray.hexStringToByteArray(etKey.getText().toString())
        );
    }
}