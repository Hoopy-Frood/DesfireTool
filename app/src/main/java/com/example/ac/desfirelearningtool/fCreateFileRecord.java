package com.example.ac.desfirelearningtool;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

/**
 * Created by andrew on 2017/08/22.
 */

public class fCreateFileRecord extends Fragment {
    IMainActivityCallbacks mCallback;
        Button buttonGo;
        EditText etFileID, etISOName, etRecordSize, etNumOfRecords;
        Spinner spReadAccess, spWriteAccess, spReadWriteAccess, spChangeAccessRights;
        RadioGroup cryptoModeGroup, recordFileType;
        byte bFileType, bCommSetting;
        int iRecordSize, iNumOfRecords;

        public fCreateFileRecord() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View rootView = inflater.inflate(R.layout.f_createfilerecord, container, false);

            try {
                mCallback = (IMainActivityCallbacks) getActivity();
                if (mCallback == null){
                    Log.d("fCreateFileRecord", "Cannot initialize callback interface");
                }
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().toString()
                        + " must implement IMainActivityCallbacks");
            }

            buttonGo = (Button) rootView.findViewById(R.id.button_Go);
            spReadAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAccess);
            spWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_WriteAccess);
            spReadWriteAccess = (Spinner) rootView.findViewById(R.id.spinner_ReadAndWriteAccess);
            spChangeAccessRights = (Spinner) rootView.findViewById(R.id.spinner_ChangeAccessRights);
            etFileID = (EditText) rootView.findViewById(R.id.EditText_FileID);
            etISOName = (EditText) rootView.findViewById(R.id.EditText_ISOName);
            etRecordSize = (EditText) rootView.findViewById(R.id.EditText_RecordSize);
            etNumOfRecords = (EditText) rootView.findViewById(R.id.EditText_MaxNumOfRecords);

            cryptoModeGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_CryptoMode);
            recordFileType =(RadioGroup) rootView.findViewById(R.id.radioGroup_RecordFileType);
            populateSpinners ();



            bFileType = (byte) 0x00;
            bCommSetting = (byte) 0xff;
            iRecordSize = 0;
            iNumOfRecords = 0;

            recordFileType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Log.d("recordFileType:", "Radio button clicked");
                    // checkedId is the RadioButton selected
                    cdfOnRadioButtonClicked(group, checkedId);
                }
            });

            cryptoModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Log.d("cryptoModeGroup:", "Radio button clicked");
                    // checkedId is the RadioButton selected
                    cdfOnRadioButtonClicked(group, checkedId);
                }
            });

            buttonGo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onGoCreateFile();
                }
            });

            return rootView;
        }

        private void populateSpinners() {
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
            list.add("Free");
            list.add("Never");
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, list);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spReadAccess.setAdapter(dataAdapter);
            spReadAccess.setSelection(14);
            spWriteAccess.setAdapter(dataAdapter);
            spWriteAccess.setSelection(14);
            spReadWriteAccess.setAdapter(dataAdapter);
            spReadWriteAccess.setSelection(14);
            spChangeAccessRights.setAdapter(dataAdapter);
            spChangeAccessRights.setSelection(14);
        }

        public void cdfOnRadioButtonClicked(RadioGroup group, int checkedId) {
            Log.d("cdfOnRadioButtonClicked", "initiated");
            // Is the button now checked?
            //boolean checked = ((RadioButton) view).isChecked();
            //Log.d("cdfOnRadioButtonClicked", "got ischecked");
            // Check which radio button was clicked
            switch(checkedId) {
                case R.id.radio_LinearRecordFile:
                    bFileType = (byte) 0xC1;
                    Toast.makeText(getActivity().getApplicationContext(), "Linear file checked", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.radio_CyclicRecordFile:
                    bFileType = (byte) 0xC0;
                    Toast.makeText(getActivity().getApplicationContext(), "Cyclic file checked", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.radio_PlainCommunication:
                    bCommSetting = (byte) 0x00;
                    break;
                case R.id.radio_MACCommunication:
                    bCommSetting = (byte) 0x01;

                    break;
                case R.id.radio_EncryptedCommunication:
                    bCommSetting = (byte) 0x03;

                    break;
            }
        }



        private void onGoCreateFile(){
            boolean isIncompleteForm = false;

            if (recordFileType.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getActivity().getApplicationContext(), "Please select a standard or backup file type", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
            if (etFileID.getText().toString().length()!=2) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure the File ID is 1 byte", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
            if ((etISOName.getText().toString().length()!=4) && (etISOName.getText().toString().length()!=0)) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure the optional ISO File Name must be 2 bytes ", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
            if (cryptoModeGroup.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getActivity().getApplicationContext(), "Please select a communication setting", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }


            try {
                if (etRecordSize.getText().toString().length() != 0)
                    iRecordSize = (parseInt(etRecordSize.getText().toString()));
                if (etNumOfRecords.getText().toString().length() != 0)
                    iNumOfRecords = (parseInt(etNumOfRecords.getText().toString()));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure a number is entered in record size and number of records", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }

            if (isIncompleteForm)
                return;

            if (etRecordSize.getText().toString().length() == 0) {
                Toast.makeText(getActivity().getApplicationContext(), "Using default record size of " + R.string.default_recordsize + " bytes" , Toast.LENGTH_SHORT).show();
                etRecordSize.setText(R.string.default_recordsize);
                iRecordSize = (parseInt(etRecordSize.getText().toString()));
            }
            if (etNumOfRecords.getText().toString().length() == 0) {
                Toast.makeText(getActivity().getApplicationContext(), "PUsing default record size of " + R.string.default_num_of_records + " bytes", Toast.LENGTH_SHORT).show();
                etNumOfRecords.setText(R.string.default_num_of_records);
                iNumOfRecords = (parseInt(etNumOfRecords.getText().toString()));
            }


            // Access Rights 2 bytes
            byte ACByte1 = (byte) ((((byte) (spReadAccess.getSelectedItemPosition())) << 4) | (((byte) (spWriteAccess.getSelectedItemPosition())) & (byte) 0x0F)) ;
            byte ACByte2 = (byte) ((((byte) (spReadWriteAccess.getSelectedItemPosition())) << 4) | (((byte) (spChangeAccessRights.getSelectedItemPosition())) & (byte) 0x0F)) ;

            Log.v("bFileType", ByteArray.byteArrayToHexString(new byte[]{ (byte) bFileType}));
            Log.v("etFileID", etFileID.getText().toString());
            Log.v("etISOName", ByteArray.byteArrayToHexString(ByteArray.hexStringToByteArray(etISOName.getText().toString())));
            Log.v("bCommSetting", ByteArray.byteArrayToHexString(new byte[]{(byte) bCommSetting}));
            Log.v("Access Rights", ByteArray.byteArrayToHexString(new byte[]{(byte) ACByte1, (byte) ACByte2 }));
            Log.v("iRecordSize", "iRecordSize = " + iRecordSize);
            mCallback.onCreateFileRecordReturn (bFileType,
                    ByteArray.hexStringToByteArray(etFileID.getText().toString())[0],
                    ByteArray.hexStringToByteArray(etISOName.getText().toString()),
                    bCommSetting,
                    new byte[]{(byte) ACByte1, (byte) ACByte2 },
                    iRecordSize,
                    iNumOfRecords);

        }

    }
