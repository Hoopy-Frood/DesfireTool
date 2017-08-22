package com.example.ac.desfirelearningtool;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by andrew on 2017/08/22.
 */

public class fDeleteApplication extends Fragment {
        private Button buttonGo;
        private EditText applicationID;
        private byte [] applicationList;
        private ListView lvApplicationList;
        View rootView;
        IMainActivityCallbacks mCallback;

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                 Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.f_selectapplication, container, false);



            try {
                mCallback = (IMainActivityCallbacks) getActivity();
                if (mCallback == null){
                    Log.d("fDeleteApplication", "Cannot initialize callback interface");
                }
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().toString()
                        + " must implement IMainActivityCallbacks");
            }


            buttonGo = (Button) rootView.findViewById(R.id.button_Go);
            applicationID = (EditText) rootView.findViewById(R.id.EditText_ApplicationID);

            buttonGo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onGoDeleteApplication();
                }
            });
            buttonGo.setEnabled(false);

            applicationID.addTextChangedListener(watcher);

            applicationList = getArguments().getByteArray("applicationList");

            //Log.v("Select application List", ByteArray.byteArrayToHexString(applicationList));
            populateListView ();
            //lvApplicationList.setOnItemClickListener();

            lvApplicationList.setClickable(true);
            lvApplicationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
                {
                    Toast.makeText(getContext(), "Clicked Item " + position, Toast.LENGTH_SHORT).show();
                    applicationID.setText(ByteArray.byteArrayToHexStringNoSpace(Arrays.copyOfRange(applicationList,(position)*3,(position)*3+3)));

                }
            });

            return rootView;
        }

        private final TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {}
            @Override
            public void afterTextChanged(Editable s) {
                if (applicationID.getText().toString().length()==6) {
                    buttonGo.setEnabled(true);
                } else {
                    buttonGo.setEnabled(false);
                }
            }
        };

        private void populateListView () {
            lvApplicationList = (ListView) rootView.findViewById(R.id.lv_ApplicationList);

            // Instanciating an array list (you don't need to do this,
            // you already have yours).
            List<String> ListApplicationList = new ArrayList<String>();

            if (applicationList != null) {
                if  (applicationList.length %3 == 0) {
                    for (int i = 0; i < applicationList.length; i = i + 3) {
                        ListApplicationList.add(ByteArray.byteArrayToHexString(applicationList, i, 3));
                    }
                }
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_1,
                    ListApplicationList);

            lvApplicationList.setAdapter(arrayAdapter);

        }


        private void onGoDeleteApplication(){

            Log.d("onGoDeleteApplication", applicationID.getText().toString());

            mCallback.onDeleteApplicationReturn(ByteArray.hexStringToByteArray(applicationID.getText().toString()));

        }
    }
