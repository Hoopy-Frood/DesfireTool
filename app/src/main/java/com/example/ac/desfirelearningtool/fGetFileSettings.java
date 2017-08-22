package com.example.ac.desfirelearningtool;

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

public class fGetFileSettings extends Fragment {
    private Button buttonGo;
    private EditText etFileID;
    private byte [] baFileIDList;
    private ListView lvFileIDList;
    View rootView;
    IMainActivityCallbacks mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.f_getfilesettings, container, false);



        try {
            mCallback = (IMainActivityCallbacks) getActivity();
            if (mCallback == null){
                Log.d("fGetFileSettings", "Cannot initialize callback interface");
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement IMainActivityCallbacks");
        }

        buttonGo = (Button) rootView.findViewById(R.id.button_Go);
        etFileID = (EditText) rootView.findViewById(R.id.EditText_FileID);

        buttonGo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onGoGetFileSettings();
            }
        });
        buttonGo.setEnabled(false);

        etFileID.addTextChangedListener(watcher);

        baFileIDList = getArguments().getByteArray("baFileIDList");

        //Log.v("Select application List", ByteArray.byteArrayToHexString(baFileIDList));
        populateListView ();
        //lvFileIDList.setOnItemClickListener();

        lvFileIDList.setClickable(true);
        lvFileIDList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0,View arg1, int position, long arg3)
            {
                Toast.makeText(getContext(), "Clicked Item " + position, Toast.LENGTH_SHORT).show();
                etFileID.setText(ByteArray.byteArrayToHexStringNoSpace(Arrays.copyOfRange(baFileIDList,(position)*3,(position)*3+3)));

            }
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d("onOptionsItemSelected", "Back pressed");
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
            if (etFileID.getText().toString().length()==6) {
                buttonGo.setEnabled(true);
            } else {
                buttonGo.setEnabled(false);
            }
        }
    };

    private void populateListView () {
        lvFileIDList = (ListView) rootView.findViewById(R.id.lv_ApplicationList);

        // Instanciating an array list (you don't need to do this,
        // you already have yours).
        List<String> ListFileIDList = new ArrayList<String>();

        if (baFileIDList != null) {
            if  (baFileIDList.length %3 == 0) {
                for (int i = 0; i < baFileIDList.length; i = i + 3) {
                    ListFileIDList.add(ByteArray.byteArrayToHexString(baFileIDList, i, 3));
                }
            }
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                ListFileIDList);

        lvFileIDList.setAdapter(arrayAdapter);

    }


    private void onGoGetFileSettings(){

        Log.d("onGoGetFileSettings", etFileID.getText().toString());

        mCallback.onGetFileSettingsReturn(ByteArray.hexStringToByteArray(etFileID.getText().toString())[0]);

    }
}
