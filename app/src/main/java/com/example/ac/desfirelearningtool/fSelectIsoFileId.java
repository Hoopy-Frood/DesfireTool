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

public class fSelectIsoFileId extends Fragment {
    private Button buttonGo;
    private EditText IsoFileId;
    private byte [] IsoFileIdList;
    private boolean isIsoFileIdListPopulated;
    private ListView lvIsoFileIdList;
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
        IsoFileId = (EditText) rootView.findViewById(R.id.EditText_IsoFileId);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoSelect();
            }
        });
        buttonGo.setEnabled(false);

        IsoFileId.addTextChangedListener(watcher);

        IsoFileIdList = getArguments().getByteArray("IsoFileIdList");
        isIsoFileIdListPopulated = getArguments().getBoolean("isIsoFileIdListPopulated");

        //Log.v("Select application List", ByteArray.byteArrayToHexString(IsoFileIdList));
        populateListView ();
        //lvIsoFileIdList.setOnItemClickListener();

        lvIsoFileIdList.setClickable(true);
        lvIsoFileIdList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                if (position == 0)
                    IsoFileId.setText("000000");
                else {
                    if (isIsoFileIdListPopulated == true) {
                        if (position == lvIsoFileIdList.getAdapter().getCount() - 1){
                            Bundle appListInfo = mCallback.onFragmentGetIsoFileIds();
                            IsoFileIdList = appListInfo.getByteArray("IsoFileIdList");
                            isIsoFileIdListPopulated = appListInfo.getBoolean("isIsoFileIdListPopulated");

                            populateListView();

                        }else {
                            IsoFileId.setText(ByteArray.byteArrayToHexStringNoSpace(Arrays.copyOfRange(IsoFileIdList, (position - 1) * 3, (position - 1) * 3 + 3)));
                        }
                    } else {
                        Bundle appListInfo = mCallback.onFragmentGetIsoFileIds();
                        IsoFileIdList = appListInfo.getByteArray("IsoFileIdList");
                        isIsoFileIdListPopulated = appListInfo.getBoolean("isIsoFileIdListPopulated");

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
            if (IsoFileId.getText().toString().length()==4) {
                buttonGo.setEnabled(true);
            } else {
                buttonGo.setEnabled(false);
            }
        }
    };

    private void populateListView () {
        lvIsoFileIdList = (ListView) rootView.findViewById(R.id.lv_IsoFileIdList);

        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        List<String> ListIsoFileIdList = new ArrayList<String>();

        ListIsoFileIdList.add("00 00 00");

        if (isIsoFileIdListPopulated  == true) {
            if  (IsoFileIdList.length %3 == 0) {
                for (int i = 0; i < IsoFileIdList.length; i = i + 3) {
                    ListIsoFileIdList.add(ByteArray.byteArrayToHexString(IsoFileIdList, i, 3));
                }
                ListIsoFileIdList.add("Update Application List");
            }
        } else {
            ListIsoFileIdList.add("Call Get Application IDs");
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                ListIsoFileIdList);

        lvIsoFileIdList.setAdapter(arrayAdapter);

    }


    private void onGoSelect(){

        Log.d("onGoSelect", IsoFileId.getText().toString());

        mCallback.onSelectIsoFileIdReturn(ByteArray.hexStringToByteArray(IsoFileId.getText().toString()));

    }
}
