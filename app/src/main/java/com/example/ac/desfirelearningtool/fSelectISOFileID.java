package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Ac on 8/19/2017.
 */

public class fSelectISOFileID extends Fragment {
    private Button buttonGo;
    private EditText ISOFileID;
    private byte [] ISOFileIDList;
    private boolean isISOFileIDListPopulated;
    private ListView lvISOFileIDList;
    View rootView;
    IMainActivityCallbacks mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                                Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_isoselectapplication, container, false);



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
        ISOFileID = (EditText) rootView.findViewById(R.id.EditText_ISOFileID);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoSelect();
            }
        });
        buttonGo.setEnabled(false);

        ISOFileID.addTextChangedListener(watcher);

        ISOFileIDList = getArguments().getByteArray("ISOFileIDList");
        isISOFileIDListPopulated = getArguments().getBoolean("isISOFileIDListPopulated");

        //Log.v("Select application List", ByteArray.byteArrayToHexString(ISOFileIDList));
        populateListView ();
        //lvISOFileIDList.setOnItemClickListener();

        lvISOFileIDList.setClickable(true);
        lvISOFileIDList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                if (position == 0)
                    ISOFileID.setText("000000");
                else {
                    if (isISOFileIDListPopulated == true) {
                        if (position == lvISOFileIDList.getAdapter().getCount() - 1){
                            Bundle appListInfo = mCallback.onFragmentGetISOFileIDs();
                            ISOFileIDList = appListInfo.getByteArray("ISOFileIDList");
                            isISOFileIDListPopulated = appListInfo.getBoolean("isISOFileIDListPopulated");

                            populateListView();

                        }else {
                            ISOFileID.setText(ByteArray.byteArrayToHexStringNoSpace(Arrays.copyOfRange(ISOFileIDList, (position - 1) * 3, (position - 1) * 3 + 3)));
                        }
                    } else {
                        Bundle appListInfo = mCallback.onFragmentGetISOFileIDs();
                        ISOFileIDList = appListInfo.getByteArray("ISOFileIDList");
                        isISOFileIDListPopulated = appListInfo.getBoolean("isISOFileIDListPopulated");

                        populateListView();
                    }
                }

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
            if (ISOFileID.getText().toString().length()==6) {
                buttonGo.setEnabled(true);
            } else {
                buttonGo.setEnabled(false);
            }
        }
    };

    private void populateListView () {
        lvISOFileIDList = (ListView) rootView.findViewById(R.id.lv_ISOFileIDList);

        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        List<String> ListISOFileIDList = new ArrayList<String>();

        ListISOFileIDList.add("00 00 00");

        if (isISOFileIDListPopulated  == true) {
            if  (ISOFileIDList.length %3 == 0) {
                for (int i = 0; i < ISOFileIDList.length; i = i + 3) {
                    ListISOFileIDList.add(ByteArray.byteArrayToHexString(ISOFileIDList, i, 3));
                }
                ListISOFileIDList.add("Update Application List");
            }
        } else {
            ListISOFileIDList.add("Call Get Application IDs");
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                ListISOFileIDList);

        lvISOFileIDList.setAdapter(arrayAdapter);

    }


    private void onGoSelect(){

        Log.d("onGoSelect", ISOFileID.getText().toString());

        mCallback.onSelectApplicationReturn(ByteArray.hexStringToByteArray(ISOFileID.getText().toString()));

    }
}
