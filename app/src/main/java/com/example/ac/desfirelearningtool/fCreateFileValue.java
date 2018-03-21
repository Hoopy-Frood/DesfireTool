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

import static java.lang.Integer.parseInt;

/**
 * Created by andrew on 2017/08/22.
 */

public class fCreateFileValue extends Fragment {
    IMainActivityCallbacks mCallback;
        Button buttonGo;
        EditText etFileID, etLowerLimit, etUpperLimit, etValue;
        Spinner spGVD, spGVDLC, spGVDLCC, spChangeAccessRights;
        RadioGroup cryptoModeGroup, recordFileType;
        byte bCommSetting, bOptionByte;
        int iLowerLimit, iUpperLimit, iValue;
        CheckBox cbEnableLC, cbEnableFreeGV;

        public fCreateFileValue() {
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
            View rootView = inflater.inflate(R.layout.f_createfilevalue, container, false);
            try {
                mCallback = (IMainActivityCallbacks) getActivity();
                if (mCallback == null){
                    Log.d("fCreateFileValue", "Cannot initialize callback interface");
                }
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().toString()
                        + " must implement IMainActivityCallbacks");
            }

            buttonGo = (Button) rootView.findViewById(R.id.button_Go);
            spGVD = (Spinner) rootView.findViewById(R.id.spinner_GVD);
            spGVDLC = (Spinner) rootView.findViewById(R.id.spinner_GVDLC);
            spGVDLCC = (Spinner) rootView.findViewById(R.id.spinner_GVDLCC);
            spChangeAccessRights = (Spinner) rootView.findViewById(R.id.spinner_ChangeAccessRights);
            etFileID = (EditText) rootView.findViewById(R.id.EditText_FileID);
            etLowerLimit = (EditText) rootView.findViewById(R.id.EditText_LowerLimit);
            etUpperLimit = (EditText) rootView.findViewById(R.id.EditText_UpperLimit);
            etValue = (EditText) rootView.findViewById(R.id.EditText_Value);
            cbEnableFreeGV = (CheckBox) rootView.findViewById(R.id.CheckBox_EnableFreeGetValue);
            cbEnableLC = (CheckBox) rootView.findViewById(R.id.CheckBox_EnableLimitedCredit);

            cryptoModeGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup_CryptoMode);
            populateSpinners ();

            bCommSetting = (byte) 0xff;
            iLowerLimit = 0;
            iUpperLimit = 0;
            iValue = 0;
            bOptionByte = (byte) 0x00;

            cryptoModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    Log.d("cryptoModeGroup:", "Radio button clicked");
                    // checkedId is the RadioButton selected
                    onRadioButtonClicked(group, checkedId);
                }
            });

            cbEnableFreeGV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                    Log.d("cbEnableFreeGV:", "Check Box checked ");
                    // checkedId is the RadioButton selected
                    onEnableFreeGVChecked( IsChecked);
                }
            });

            cbEnableLC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                    Log.d("cbEnableLC:", "Check Box checked ");
                    // checkedId is the RadioButton selected
                    onEnableLimitedCreditChecked(IsChecked);
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
            Log.d("Populate Spinner", "1");
            spGVD.setAdapter(dataAdapter);
            spGVD.setSelection(14);
            Log.d("Populate Spinner", "2");
            spGVDLC.setAdapter(dataAdapter);
            spGVDLC.setSelection(14);
            Log.d("Populate Spinner", "3");
            spGVDLCC.setAdapter(dataAdapter);
            spGVDLCC.setSelection(14);
            Log.d("Populate Spinner", "4");
            spChangeAccessRights.setAdapter(dataAdapter);
            spChangeAccessRights.setSelection(14);
        }

        public void onRadioButtonClicked(RadioGroup group, int checkedId) {
            Log.d("cdfOnRadioButtonClicked", "initiated");
            // Is the button now checked?
            //boolean checked = ((RadioButton) view).isChecked();
            //Log.d("cdfOnRadioButtonClicked", "got ischecked");
            // Check which radio button was clicked
            switch(checkedId) {
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

        public void onEnableLimitedCreditChecked(boolean isChecked) {
            if (isChecked) {
                bOptionByte |= 0x01;
                //Toast.makeText(getActivity().getApplicationContext(), "Allow CheckBox_EnableLimitedCredit " + ByteArray.byteArrayToHexString(new byte[]{(byte) bOptionByte}), Toast.LENGTH_SHORT).show();
            } else {
                bOptionByte &= 0xFE;
                //Toast.makeText(getActivity().getApplicationContext(), "Allow CheckBox_EnableLimitedCredit" + ByteArray.byteArrayToHexString(new byte[]{(byte) bOptionByte}), Toast.LENGTH_SHORT).show();
            }
        }

        public void onEnableFreeGVChecked(boolean isChecked) {
            if (isChecked) {
                bOptionByte |= 0x02;
                //Toast.makeText(getActivity().getApplicationContext(), "CheckBox_FreeDirListAccess " + ByteArray.byteArrayToHexString(new byte[]{(byte) bOptionByte}), Toast.LENGTH_SHORT).show();
            }else {
                bOptionByte &= 0xFD;
                //Toast.makeText(getActivity().getApplicationContext(), "CheckBox_FreeDirListAccess " + ByteArray.byteArrayToHexString(new byte[]{(byte) bOptionByte}), Toast.LENGTH_SHORT).show();
            }

        }




        private void onGoCreateFile(){
            boolean isIncompleteForm = false;

            if (etFileID.getText().toString().length()!=2) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure the File ID is 1 byte", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }
            if (etLowerLimit.getText().toString().length() == 0) {
                etLowerLimit.setText(R.string.lower_limit); //getResources().getString(R.string.lower_limit));
            }
            if (etUpperLimit.getText().toString().length() == 0) {
                etUpperLimit.setText(R.string.upper_limit); //getResources().getString(R.string.lower_limit));
            }
            if (etValue.getText().toString().length() == 0) {
                etValue.setText(R.string.value); //getResources().getString(R.string.lower_limit));
            }

            try {
                iLowerLimit = (parseInt(etLowerLimit.getText().toString()));
                iUpperLimit = (parseInt(etUpperLimit.getText().toString()));
                iValue = (parseInt(etValue.getText().toString()));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Please ensure valid numbers are entered in records", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;

            }


            if (cryptoModeGroup.getCheckedRadioButtonId() == -1) {
                Toast.makeText(getActivity().getApplicationContext(), "Please select a communication setting", Toast.LENGTH_SHORT).show();
                isIncompleteForm = true;
            }



            if (isIncompleteForm)
                return;

            // Access Rights 2 bytes
            byte ACByte1 = (byte) ((((byte) (spGVD.getSelectedItemPosition())) << 4) | (((byte) (spGVDLC.getSelectedItemPosition())) & (byte) 0x0F)) ;
            byte ACByte2 = (byte) ((((byte) (spGVDLCC.getSelectedItemPosition())) << 4) | (((byte) (spChangeAccessRights.getSelectedItemPosition())) & (byte) 0x0F)) ;

            Log.v("bFileType", ByteArray.byteArrayToHexString(new byte[]{ (byte) 0xCC}));
            Log.v("etFileID", etFileID.getText().toString());
            Log.v("bCommSetting", ByteArray.byteArrayToHexString(new byte[]{ bCommSetting}));
            Log.v("Access Rights", ByteArray.byteArrayToHexString(new byte[]{ ACByte1, (byte) ACByte2 }));
            Log.v("iLowerLimit", "iLowerLimit = " + iLowerLimit);
            Log.v("iUpperLimit", "iUpperLimit = " + iUpperLimit);
            Log.v("iValue", "iValue = " + iValue);
            Log.v("bOptionByte", ByteArray.byteArrayToHexString(new byte[]{ bOptionByte}));

            mCallback.onCreateFileValueReturn(
                    ByteArray.hexStringToByteArray(etFileID.getText().toString())[0],
                    bCommSetting,
                    new byte[]{(byte) ACByte1, (byte) ACByte2 },
                    iLowerLimit,
                    iUpperLimit,
                    iValue,
                    bOptionByte);
        }

    }