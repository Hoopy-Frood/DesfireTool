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
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrew on 2017/08/22.
 */

public class fGetKeyVersion  extends Fragment {
    View rootView;
    IMainActivityCallbacks mCallback;
    private Button buttonGo;
    private Spinner spKeyToInquire;



    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_getkeyversion, container, false);
        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fGetKeyVersion", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }



        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        spKeyToInquire = (Spinner) rootView.findViewById(R.id.spinner_KeyToInquire);
        populateSpinners ();


        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoGetKeyVersion();
            }
        });

        return rootView;

    }


    private void populateSpinners(){
        //List<Number> https://www.mkyong.com/android/android-spinner-drop-down-list-example/

        List<String> list = new ArrayList<String>();
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
        list.add("14");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKeyToInquire.setAdapter(dataAdapter);
        spKeyToInquire.setSelection(0);
    }


    private void onGoGetKeyVersion(){

        mCallback.onGoGetKeyVersionReturn((byte) spKeyToInquire.getSelectedItemPosition());
    }
}
