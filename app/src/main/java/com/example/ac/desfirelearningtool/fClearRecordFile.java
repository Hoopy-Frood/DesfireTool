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
import java.util.List;

/**
 * Created by andrew on 2018/03/02.
 */

public class fClearRecordFile extends Fragment {

    private Button buttonGo;
    private EditText etFileID;
    private byte [] baFileIDList;
    private boolean bFileIDListPopulated;
    private ListView lvFileIDList;
    View rootView;
    IMainActivityCallbacks mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_clearrecordfile, container, false);



        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fClearRecordFile", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }

        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        etFileID = (EditText) rootView.findViewById(R.id.EditText_FileID);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoClearRecordFile();
            }
        });
        buttonGo.setEnabled(false);

        etFileID.addTextChangedListener(watcher);

        baFileIDList = getArguments().getByteArray("baFileIDList");
        bFileIDListPopulated = getArguments().getBoolean("bFileIDListPopulated");

        lvFileIDList = (ListView) rootView.findViewById(R.id.lv_FileList);
        //Log.v("Select application List", ByteArray.byteArrayToHexString(baFileIDList));
        populateListView ();
        //lvFileIDList.setOnItemClickListener();

        lvFileIDList.setClickable(true);
        lvFileIDList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {

                if (bFileIDListPopulated == true) {
                    if (position == lvFileIDList.getAdapter().getCount() - 1) {
                        Bundle fileListInfo = mCallback.onFragmentGetFileIDs();
                        baFileIDList = fileListInfo.getByteArray("baFileIDList");
                        bFileIDListPopulated = fileListInfo.getBoolean("bFileIDListPopulated");

                        populateListView();

                    } else {
                        etFileID.setText(ByteArray.byteToHexString(baFileIDList[position]));
                    }
                } else {
                    Bundle fileListInfo = mCallback.onFragmentGetFileIDs();
                    baFileIDList = fileListInfo.getByteArray("baFileIDList");
                    bFileIDListPopulated = fileListInfo.getBoolean("bFileIDListPopulated");

                    populateListView();

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
            if (etFileID.getText().toString().length()==2) {
                buttonGo.setEnabled(true);
            } else {
                buttonGo.setEnabled(false);
            }
        }
    };

    private void populateListView () {
        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        List<String> ListFileIDList = new ArrayList<String>();

        if (bFileIDListPopulated == true) {
            if  (baFileIDList != null) {
                for (int i = 0; i < baFileIDList.length; i = i + 1) {
                    ListFileIDList.add(ByteArray.byteArrayToHexString(baFileIDList, i, 1));
                }
            }
            ListFileIDList.add("Update FileIDs");
        } else {
            ListFileIDList.add("Call Get FileIDs");
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                ListFileIDList);

        lvFileIDList.setAdapter(arrayAdapter);

    }


    private void onGoClearRecordFile(){

        Log.d("onGoClearRecordFile", etFileID.getText().toString());

        mCallback.onClearRecordFileReturn(ByteArray.hexStringToByteArray(etFileID.getText().toString())[0]);

    }
}
