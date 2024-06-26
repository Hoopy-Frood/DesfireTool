package com.example.ac.desfirelearningtool;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Array;
import java.util.Arrays;

import static com.example.ac.desfirelearningtool.Desfire.commMode.ENCIPHERED;
import static com.example.ac.desfirelearningtool.Desfire.commMode.MAC;
import static com.example.ac.desfirelearningtool.Desfire.commMode.PLAIN;


public class MainActivity extends AppCompatActivity implements IMainActivityCallbacks {
    private AdView mAdView;

    fCommandMenu commandFragment;
    fSelectApplication selectFragment;
    fSelectIsoFileId selectIsoFragment;
    fCreateApplication createApplicationFragment;
    fGetKeyVersion getKeyVersionFragment;
    fDeleteApplication deleteApplicationFragment;
    fCreateFile createFileFragment;
    fGetFileSettings getFileSettingsFragment;
    fDeleteFile getDeleteFileFragment;
    fAuthenticate authenticateFragment;
    fReadData getReadDataFragment;
    fWriteData getWriteDataFragment;
    fReadRecords getReadRecordsFragment;
    fWriteRecord getWriteRecordFragment;
    fClearRecordFile getClearRecordFileFragment;
    fGetValue getValueFragment;
    fCredit creditFragment;
    fDebit debitFragment;
    fLimitedCredit limitedCreditFragment;
    fChangeKey changeKeyFragment;
    fChangeKeySettings changeKeySettingsFragment;

    protected PendingIntent pendingIntent;
    protected IntentFilter[] intentFiltersArray;
    protected String[][] techListsArray;
    protected NfcAdapter nfcAdapter;

    private AndroidCommunicator communicator;
    private Desfire desfireCard;

    private Button buttonClearScreen;
    private Button buttonCopyLog;
    private CheckBox checkboxWrapperMode;
    private boolean boolWrapperMode;

    private ScrollLog scrollLog;
    public ScrollView scrollViewTextLog;

    private byte[] applicationList;
    private boolean applicationListPopulated;
    private byte[] baFileIDList;
    private boolean bFileIDListPopulated;
    private byte[] baIsoFileIdList;
    private boolean bIsoFileIdListPopulated;
    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("DESFire Tool");


        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.i("Ads", "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.i("Ads", "onAdFailedToLoad");
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.i("Ads", "onAdOpened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.i("Ads", "onAdLeftApplication");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Log.i("Ads", "onAdClosed");
            }
        });

        // Buttons - Log Management
        buttonClearScreen = (Button) findViewById(R.id.button_ClearScreen);
        buttonCopyLog = (Button) findViewById(R.id.button_CopyLog);
        checkboxWrapperMode = (CheckBox) findViewById(R.id.CheckBox_ISOWrap);
        scrollViewTextLog = findViewById(R.id.scrollview_TextLog);
        scrollLog = new ScrollLog((TextView) findViewById(R.id.textView_scrollLog), scrollViewTextLog);

        applicationList = null;
        baFileIDList = null;
        baIsoFileIdList = null;
        boolWrapperMode = false;
        bIsoFileIdListPopulated = false;

        buttonClearScreen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scrollLog.clearScreen();
            }
        });
        buttonCopyLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("DESFire Tool", scrollLog.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        checkboxWrapperMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("MainActivity:", "Wrap APDU Check Box checked ");
                // checkedId is the RadioButton selected
                if (IsChecked)
                    boolWrapperMode = true;
                else
                    boolWrapperMode = false;

                if (communicator != null ) {
                    communicator.setIsoMode(boolWrapperMode);
                }
             }
        });

        // Initialize additional data for foreground dispatching of Nfc intents
        this.pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        this.intentFiltersArray = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)};
        this.techListsArray = new String[][]{
                new String[]{android.nfc.tech.IsoDep.class.getName()},
                new String[]{android.nfc.tech.MifareClassic.class.getName()},
                new String[]{android.nfc.tech.NfcV.class.getName()}
        };

        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);


        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            commandFragment = new fCommandMenu();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            commandFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, commandFragment).commit();

        }

    }

    @Override
    public void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (currentTag != null) {

            // Object nfcTag = Tag_getTagService(currentTag);
            // int nativeHandle = Tag_getServiceHandle(currentTag);

            //new CardWatchdogRearm(nfcTag, nativeHandle).start();

            onCardDetection(currentTag);
        }
    }

    //region Card Detection methods

    /**
     * Rearm Thread
     */
    private static class CardWatchdogRearm extends Thread {

        private final Object nfcTag;
        private final int nativeHandle;

        CardWatchdogRearm(Object nfcTag, int nativeHandle) {
            this.nfcTag = nfcTag;
            this.nativeHandle = nativeHandle;
        }

        @Override
        public void run() {
            // After 10 seconds, die anyway
            for (int i = 0; i < 2; ++i) {  //250 orig
                try {
                    int result = INfcTag_connect(nfcTag, nativeHandle, 0);
                    boolean present = INfcTag_isPresent(nfcTag, nativeHandle);
                    if (!present) {
                        Log.d("CardWatchdogRearm", "INfcTag_connect: " + result);
                        break;
                    } else {
                        Log.d("CardWatchdogRearm", "PRESENT INfcTag_connect: " + result);
                    }

                    Thread.sleep(40);
                } catch (Exception e) {
                    Log.e("CardWatchdogRearm", e.getMessage());
                    break;
                }
            }
        }
    }

    private void onCardDetection(Tag tag) {
        // You may want to use the basic IsoDep/Tag classes
        // final IsoDep communicator = IsoDep.get(tag);
        // communicator.transceive();
        // ...

        try {
            communicator = new AndroidCommunicator(IsoDep.get(tag), boolWrapperMode, scrollLog);

            desfireCard = communicator.get(tag); // we do not specify a key here!
            if (desfireCard == null) {
                Log.d("onCardDetection", "Not a DESFire card");
                new Exception().printStackTrace();
            }

            desfireCard.connect();
            Log.d("onCardDetection:", " CONNECTED");
            //scrollLog.setTextColor(0xAAFF0000);
            scrollLog.appendStatus("DESFire Connected\n");
            applicationListPopulated = false;


            commandFragment.enableAllButtons();

            /*  CRC Test
            scrollLog.appendData(ByteArray.byteArrayToHexString(new byte[] {(byte) 0x00,(byte) 0x09,(byte) 0x10,(byte) 0x01,(byte) 0x01,(byte) 0x7C,(byte) 0xF4,(byte) 0xB8,(byte) 0x00}));
            scrollLog.appendData(ByteArray.byteArrayToHexString(desfireCard.dfCrypto.calcCRC(new byte[] {(byte) 0x00,(byte) 0x09,(byte) 0x10,(byte) 0x01,(byte) 0x01,(byte) 0x7C,(byte) 0xF4,(byte) 0xB8,(byte) 0x00})));
            scrollLog.appendData("CRC16 Check Needs to be A3 5E");

            scrollLog.appendData(ByteArray.byteArrayToHexString(new byte[] {(byte) 0x00}));
            scrollLog.appendData(ByteArray.byteArrayToHexString(desfireCard.dfCrypto.calcCRC(new byte[] {(byte) 0x00})));
            scrollLog.appendData("Needs to be FE 51");

            scrollLog.appendData(ByteArray.byteArrayToHexString(new byte[] {(byte) 0x00,(byte) 0x00}));
            scrollLog.appendData(ByteArray.byteArrayToHexString(desfireCard.dfCrypto.calcCRC(new byte[] {(byte) 0x00,(byte) 0x00})));
            scrollLog.appendData("Needs to be A0 1E");*/

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            Log.e("onCardDetection", e.getMessage(), e);
        }
    }
    //endregion

    //region Miscellaneous Helpers
    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {

            setHomeStatusBar();
            Log.d("onBackPressed", "popBackStack");
        } else {
            super.onBackPressed();
            getSupportActionBar().setTitle("DESFire Tool");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            Log.d("onBackPressed", "onBackPressed()");
        }
    }

    private void setHomeStatusBar (){
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();
    }

    private void setStatusBar (String title) {
        try {
            getSupportActionBar().setTitle(title);
        } catch (NullPointerException e) {
            Log.e("setStatusBar", "Null title");
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static Object Tag_getTagService(Tag that) {
        try {
            Class c = Tag.class;
            //c = Class.forName("android.nfc.Tag");
            Method m = c.getMethod("getTagService");
            Object nfcTag = m.invoke(that);
            return nfcTag;
        } catch (Exception ex) {
            Log.e("Tag_getTagService", ex.getMessage());
            return null;
        }
    }


    public static int Tag_getServiceHandle(Tag that) {
        try {
            Class c = Tag.class;
            //c = Class.forName("android.nfc.Tag");
            Method m = c.getMethod("getServiceHandle");
            int serviceHandle = (Integer) m.invoke(that);
            return serviceHandle;
        } catch (Exception ex) {
            Log.e("Tag_getServiceHandle", ex.getMessage());
            return 0;
        }
    }

    public static int INfcTag_connect(Object that, int nativeHandle, int technology) {
        try {
            Class c = Class.forName("android.nfc.INfcTag$Stub$Proxy");

            Method m = c.getMethod("connect", int.class, int.class);
            return (Integer) m.invoke(that, nativeHandle, technology);
        } catch (Exception ex) {
            Log.e("INfcTag_connect", ex.getMessage());
            return -1;
        }
    }

    //public boolean isPresent(int nativeHandle) throws RemoteException
    public static boolean INfcTag_isPresent(Object that, int nativeHandle) throws Exception {
        Class c = Class.forName("android.nfc.INfcTag$Stub$Proxy");

        Method m = c.getMethod("isPresent", int.class);
        boolean result = (Boolean) m.invoke(that, nativeHandle);
        return result;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    //endregion

    public ScrollLog getScrollLogObject() {
        return scrollLog;
    }

    //region Get Version
    public void onGetVersion() {

        try {
            scrollLog.appendTitle("Get Version");
            Desfire.DesfireResponse dfresp = desfireCard.getVersion();
            if (dfresp.status != Desfire.statusType.ADDITONAL_FRAME) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(dfresp.status) );
                return;
            }
            byte[] hwInfo = dfresp.data;
            dfresp = desfireCard.getMoreData();
            if (dfresp.status != Desfire.statusType.ADDITONAL_FRAME) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(dfresp.status) );
                return;
            }
            byte[] swInfo = dfresp.data;
            dfresp = desfireCard.getMoreData();
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(dfresp.status) );
                return;
            }
            byte[] UIDInfo = dfresp.data;

                    scrollLog.appendStatus("Hardware related information:");
            scrollLog.appendData("Vendor ID:           " + ByteArray.byteArrayToHexString(hwInfo, 0, 1));
            scrollLog.appendData("Type:                " + ByteArray.byteArrayToHexString(hwInfo, 1, 1));
            scrollLog.appendData("Sub-type             " + ByteArray.byteArrayToHexString(hwInfo, 2, 1));
            scrollLog.appendData("Major/minor version: " + ByteArray.byteArrayToHexString(hwInfo, 3, 1) + "/ " + ByteArray.byteArrayToHexString(hwInfo, 4, 1));
            scrollLog.appendData("Storage size:        " + ByteArray.byteArrayToHexString(hwInfo, 5, 1));
            scrollLog.appendData("Protocol:            " + ByteArray.byteArrayToHexString(hwInfo, 6, 1));
            scrollLog.appendStatus("Software related information:");
            scrollLog.appendData("Vendor ID:           " + ByteArray.byteArrayToHexString(swInfo, 0, 1));
            scrollLog.appendData("Type:                " + ByteArray.byteArrayToHexString(swInfo, 1, 1));
            scrollLog.appendData("Sub-type             " + ByteArray.byteArrayToHexString(swInfo, 2, 1));
            scrollLog.appendData("Major/minor version: " + ByteArray.byteArrayToHexString(swInfo, 3, 1) + "/ " + ByteArray.byteArrayToHexString(swInfo, 4, 1));
            scrollLog.appendData("Storage size:        " + ByteArray.byteArrayToHexString(swInfo, 5, 1));
            scrollLog.appendData("Protocol:            " + ByteArray.byteArrayToHexString(swInfo, 6, 1));
            scrollLog.appendStatus("UID and manufacturer information:");
            scrollLog.appendData("UID:                 " + ByteArray.byteArrayToHexString(UIDInfo, 0, 7));
            scrollLog.appendData("Batch number:        " + ByteArray.byteArrayToHexString(UIDInfo, 7, 5));
            scrollLog.appendData("Cal week of prod:    " + ByteArray.byteArrayToHexString(UIDInfo, 12, 1));
            scrollLog.appendData("Production year:     " + ByteArray.byteArrayToHexString(UIDInfo, 13, 1));
            // Parse all info
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Card UID
    public void onGetCardUID() {
        scrollLog.appendTitle("Get Card UID");
        try {

            Desfire.DesfireResponse res = desfireCard.getCardUID();
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }
            scrollLog.appendData("Decrypted Card UID: " + ByteArray.byteArrayToHexString(res.data));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetCardUID", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Application IDs
    public void onGetApplicationIDs() {

        try {
            scrollLog.appendTitle("Get Application ID");
            Desfire.DesfireResponse res = desfireCard.getApplicationIDs();
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }
            applicationList = res.data;
            applicationListPopulated = true;

            if (applicationList.length == 0) {
                scrollLog.appendData("No application on card ");
                return;
            }
            scrollLog.appendData("getAppIDs : " + ByteArray.byteArrayToHexString(applicationList));

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetApplicationIDs", e.getMessage(), e);
        }
    }

    public Bundle onFragmentGetApplicationIDs() {

        onGetApplicationIDs();

        Bundle appListInfo = new Bundle();
        appListInfo.putByteArray("applicationList", applicationList);
        appListInfo.putBoolean("applicationListPopulated", applicationListPopulated);

        return appListInfo;

    }
    //endregion


    //region Get Free Memory
    public void onGetFreeMem() {

        try {
            scrollLog.appendTitle("Get Free Memory");
            Desfire.DesfireResponse res = desfireCard.getFreeMem();
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }

            ByteBuffer bb = ByteBuffer.wrap(res.data);
            bb.order(ByteOrder.LITTLE_ENDIAN);

            scrollLog.appendData("Free Memory: " + bb.getShort() + "B");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetFreeMem", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Key Settings
    public void onGetKeySettings() {
        onFragmentGetKeySettings();
    }

    public byte[] onFragmentGetKeySettings() {
        Desfire.DesfireResponse res;
        try {
            scrollLog.appendTitle("Get Key Settings");
            res = desfireCard.getKeySettings();

            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Get Key Settings Failed: " + Desfire.DesfireErrorMsg(res.status));
                return null;
            }
            scrollLog.appendStatus("Key Settings: " + ByteArray.byteArrayToHexString(res.data));

            if ((res.data[0] & (byte) 0xF0) == (byte) 0x00) {
                scrollLog.appendData("- Change key access: Master Key");
            } else if ((res.data[0] & (byte) 0xF0) == (byte) 0xE0) {
                scrollLog.appendData("- Change key access right: Same Key");
            } else if ((res.data[0] & (byte) 0xF0) == (byte) 0xF0) {
                scrollLog.appendData("- Change key access right: Frozen");
            } else {
                scrollLog.appendData("Change key access right: Key " + ByteArray.byteToHexString((byte) (res.data[0] >> 4)));
            }
            if ((res.data[0] & (byte) 0x08) != (byte) 0x00) {
                scrollLog.appendData("- Configuration changeable with master key auth");
            } else {
                scrollLog.appendData("- Configuration not changeable");
            }
            if ((res.data[0] & (byte) 0x04) != (byte) 0x00) {
                scrollLog.appendData("- Master key not required for create/delete");
            } else {
                scrollLog.appendData("- Master key is required for create/delete");
            }
            if ((res.data[0] & (byte) 0x02) != (byte) 0x00) {
                scrollLog.appendData("- Free file directory access");
            } else {
                scrollLog.appendData("- Master key auth needed before directory access");
            }
            if ((res.data[0] & (byte) 0x01) != (byte) 0x00) {
                scrollLog.appendData("- Master key is changeable");
            } else {
                scrollLog.appendData("- Master Key is frozen");
            }
            if ((res.data[1] & (byte) 0xC0) == (byte) 0x00) {
                scrollLog.appendData("- Crypto method = DES/3DES");
            } else if ((res.data[1] & (byte) 0xC0) == (byte) 0x40) {
                scrollLog.appendData("- Crypto method = 3K3DES");
            } else if ((res.data[1] & (byte) 0xC0) == (byte) 0x80) {
                scrollLog.appendData("- Crypto method = AES");
            }
            if ((res.data[1] & (byte) 0x20) != (byte) 0x00) {
                scrollLog.appendData("- Support 2 byte file identifiers");
            }
            scrollLog.appendData("- No. of keys: " + ByteArray.byteToHexString((byte) (res.data[1] & (byte) 0x07)));

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetKeySettings", e.getMessage(), e);
            return null;
        }
        return res.data;
    }
    //endregion

    //region Get DF Names
    public void onGetDFNames() {

        try {
            scrollLog.appendTitle("Get DF Names");
            Desfire.DesfireResponse res = desfireCard.getDFNames();

            if ((res.status == Desfire.statusType.SUCCESS) || (res.status == Desfire.statusType.ADDITONAL_FRAME)) {
                if ((res.data == null) || (res.data.length == 0)) {
                    scrollLog.appendData("No DF name returned");
                    return;
                }

                scrollLog.appendData("Application 1: " + ByteArray.byteArrayToHexString(res.data));
            } else {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }

            int i = 2;
            while (res.status == Desfire.statusType.ADDITONAL_FRAME) {
                res = desfireCard.getMoreData();
                scrollLog.appendData("Application " + i + ": " + ByteArray.byteArrayToHexString(res.data));
                i++;
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetDFNames", e.getMessage(), e);
        }
    }
    //endregion

    //region Set Configuration
    public void onSetConfiguration() {

    }
    //endregion

    //region Change Key
    public void onChangeKey() {
        scrollLog.appendTitle("Change Key");

        setStatusBar("Change Key");
        
        changeKeyFragment = new fChangeKey();

        Bundle bundle = new Bundle();
        bundle.putInt("currentAuthenticatedKey", desfireCard.currentAuthenticatedKey());
        bundle.putByte("currentAuthenticationMode", desfireCard.currentAuthenticationMode());

        //changeKeyFragment.setArguments(getIntent().getExtras());
        changeKeyFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, changeKeyFragment).addToBackStack("commandview").commit();

    }

    public void onChangeKeyReturn(byte bKeyToChange, byte bKeyVersion, byte[] baNewKey, byte[] baOldKey) {
        setHomeStatusBar();

        try {
            Desfire.statusType retValue = desfireCard.changeKey(bKeyToChange, bKeyVersion, baNewKey, baOldKey);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Change Key Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendStatus("Change Key Success");

        } catch (Exception e) {
            scrollLog.appendError("DESFire Error\n");
            Log.e("onChangeKeyReturn", e.getMessage(), e);
        }
    }
    //endregion

    //region Change Key
    public void onChangeKeySettings() {
        scrollLog.appendTitle("Change Key Settings");

        setStatusBar("Change Key Settings");

        changeKeySettingsFragment = new fChangeKeySettings();

/*        Bundle bundle = new Bundle();
        bundle.putInt("currentAuthenticatedKey", desfireCard.currentAuthenticatedKey());
        bundle.putByte("currentAuthenticationMode", desfireCard.currentAuthenticationMode());

        changeKeySettingsFragment.setArguments(bundle);
  */      getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, changeKeySettingsFragment).addToBackStack("commandview").commit();

    }

    public void onChangeKeySettingsReturn(int iChangeKeyKey, boolean boolKeySettingChangeable, boolean boolFreeCreateDelete, boolean boolFreeDirInfoAccess, boolean boolMasterKeyChangable) {
        setHomeStatusBar();

        try {
            Desfire.statusType retValue = desfireCard.changeKeySettings(iChangeKeyKey, boolKeySettingChangeable, boolFreeCreateDelete, boolFreeDirInfoAccess, boolMasterKeyChangable);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Error : " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendStatus("Change Key Settings Success");

        } catch (Exception e) {
            scrollLog.appendError("DESFire Error\n");
            Log.e("onChangeKeySettingsRet", e.getMessage(), e);
        }
    }
    //endregion


    //region Authenticate
    public void onAuthenticate() {
        setStatusBar("Authenticate");


        authenticateFragment = new fAuthenticate();
        authenticateFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, authenticateFragment).addToBackStack("commandview").commit();

    }

    public void onAuthenticateReturn(byte bAuthCmd, byte bKeyNo, byte[] key) {
        scrollLog.appendTitle("Authenticate");

        setHomeStatusBar();

        getSupportFragmentManager().popBackStack();

        try {
            Desfire.statusType res = desfireCard.authenticate(bAuthCmd, bKeyNo, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");

            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }
    //endregion

    //region Select Application
    public void onSelectApplication() {
        scrollLog.appendTitle("Select Application");

        setStatusBar("Select Application");


        Bundle bundle = new Bundle();
        bundle.putByteArray("applicationList", applicationList);
        bundle.putBoolean("applicationListPopulated", applicationListPopulated);
        selectFragment = new fSelectApplication();
        selectFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, selectFragment).addToBackStack("commandview").commit();

    }

    public void onSelectApplicationReturn(byte[] baAppId) {
        scrollLog.appendTitle("SelectApplication Return");

        setHomeStatusBar();

        baFileIDList = null;
        bFileIDListPopulated = false;

        scrollLog.appendData("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            Desfire.statusType retValue = desfireCard.selectApplication(baAppId);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendData("Select OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Select ISO File ID
    public void onSelectIsoFileId() {
        scrollLog.appendTitle("Select ISO File ID");

        setStatusBar("Select ISO File ID");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baIsoFileIdList", baIsoFileIdList);
        bundle.putBoolean("bIsoFileIdListPopulated", bIsoFileIdListPopulated);
        selectIsoFragment = new fSelectIsoFileId();
        selectIsoFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, selectIsoFragment).addToBackStack("commandview").commit();

    }

    public void onSelectIsoFileIdReturn(byte[] baIsoFileId) {
        scrollLog.appendTitle("Select ISO File ID Return");

        setHomeStatusBar();

        baIsoFileIdList = null;
        bIsoFileIdListPopulated = false;

        scrollLog.appendData("ISO File ID returned = " + ByteArray.byteArrayToHexString(baIsoFileId));
        if (baIsoFileId.length != 2) {
            scrollLog.appendError("File ID not correct");
        }

        try {
            Desfire.ISOResponse retValue = desfireCard.selectIsoFileId(baIsoFileId);
            if (retValue.status != Desfire.statusWord.SUCCESS)
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(retValue.status));
            else
                scrollLog.appendData("Select OK: " + ByteArray.byteArrayToHexString(baIsoFileId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion


    //region Create Application
    public void onCreateApplication() {
        scrollLog.appendTitle("Create Application");

        setStatusBar("Create Application");


        createApplicationFragment = new fCreateApplication();
        createApplicationFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, createApplicationFragment).addToBackStack("commandview").commit();

    }

    public void onCreateApplicationReturn(byte[] baAppId, byte bKeySetting1, byte bKeySetting2, byte[] baISOName, byte[] DFName) {
        scrollLog.appendTitle("Create Application Return");

        setHomeStatusBar();
        scrollLog.appendData("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            Desfire.statusType mfResult = desfireCard.createApplication(baAppId, bKeySetting1, bKeySetting2, baISOName, DFName);
            if (mfResult != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Create Application Failed: " + Desfire.DesfireErrorMsg(mfResult));
            else
                scrollLog.appendData("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Delete Application
    public void onDeleteApplication() {
        scrollLog.appendTitle("Delete Application");

        setStatusBar("Delete Application");


        Bundle bundle = new Bundle();
        bundle.putByteArray("applicationList", applicationList);
        deleteApplicationFragment = new fDeleteApplication();
        deleteApplicationFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, deleteApplicationFragment).addToBackStack("commandview").commit();

    }

    public void onDeleteApplicationReturn(byte[] baAppId) {
        scrollLog.appendTitle("Delete Application Return");

        setHomeStatusBar();

        scrollLog.appendData("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            Desfire.statusType retValue = desfireCard.deleteApplication(baAppId);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Delete Application Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendData("Delete Application OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Key Version
    public void onGetKeyVersion() {
        scrollLog.appendTitle("Get Key Version");

        setStatusBar("Get Key Version");


        getKeyVersionFragment = new fGetKeyVersion();


        getKeyVersionFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getKeyVersionFragment).addToBackStack("commandview").commit();

    }

    public void onGoGetKeyVersionReturn(byte iKeyToInquire) {
        setHomeStatusBar();
        commandFragment.enableAllButtons();

        try {
            Desfire.DesfireResponse retValue = desfireCard.getKeyVersion(iKeyToInquire);
            if (retValue.status != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Get Key Version Failed: " + Desfire.DesfireErrorMsg(retValue.status));
            else
                scrollLog.appendData("Key " + iKeyToInquire + " version is " + ByteArray.byteArrayToHexString(retValue.data));

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Format PICC
    public void onFormatPICC() {

        try {
            scrollLog.appendTitle("Format PICC");
            Desfire.statusType res = desfireCard.formatPICC();

            if (res == Desfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
            if (res == Desfire.statusType.SUCCESS)
                scrollLog.appendStatus("Format PICC Successful");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onFormatPICC", e.getMessage(), e);
        }
    }
    //endregion

    //region Commit Transaction
    public void onCommitTransaction() {

        try {
            scrollLog.appendTitle("Commit Transaction");
            Desfire.DesfireResponse res = desfireCard.commitTransaction();

            if (res.status == Desfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
            if (res.status == Desfire.statusType.SUCCESS)
                scrollLog.appendStatus("Commit Successful");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onCommitTransaction", e.getMessage(), e);
        }
    }
    //endregion

    //region Abort Transaction
    public void onAbortTransaction() {

        try {
            scrollLog.appendTitle("Abort Transaction");
            Desfire.DesfireResponse res = desfireCard.abortTransaction();

            if (res.status == Desfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
            if (res.status == Desfire.statusType.SUCCESS)
                scrollLog.appendStatus("Abort Successful");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAbortTransaction", e.getMessage(), e);
        }
    }
    //endregion

    //region Create File
    public void onCreateFile() {
        scrollLog.appendTitle("Create File");

        setStatusBar("Create File");

        createFileFragment = new fCreateFile();

        createFileFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, createFileFragment).addToBackStack("commandview").commit();
    }

    public void onCreateFileDataReturn(byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iFileSize) {
        setHomeStatusBar();

        Log.d("MainActivity", "bFileType " + ByteArray.byteArrayToHexString(new byte[]{bFileType}));
        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[]{bFileId}));
        Log.d("MainActivity", "baISOName " + ByteArray.byteArrayToHexString(baISOName));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iFileSize = " + iFileSize);
        try {
            Desfire.statusType retValue = desfireCard.createDataFile(bFileType, bFileId, baISOName, bCommSetting, baAccessBytes, iFileSize);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Create Data File Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendData("Create Data File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    public void onCreateFileRecordReturn(byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iRecordSize, int iNumOfRecords) {
        setHomeStatusBar();
        
        Log.d("MainActivity", "bFileType " + ByteArray.byteArrayToHexString(new byte[]{bFileType}));
        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[]{bFileId}));
        Log.d("MainActivity", "ISOName " + ByteArray.byteArrayToHexString(baISOName));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iRecordSize = " + iRecordSize);
        Log.d("MainActivity", "iNumOfRecords = " + iNumOfRecords);
        try {
            Desfire.statusType retValue = desfireCard.createRecordFile(bFileType, bFileId, baISOName, bCommSetting, baAccessBytes, iRecordSize, iNumOfRecords);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Create Record File Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendData("Create Record File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    public void onCreateFileValueReturn(byte bFileId, byte bCommSetting, byte[] baAccessBytes, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) {
        setHomeStatusBar();

        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[]{bFileId}));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iLowerLimit = " + iLowerLimit);
        Log.d("MainActivity", "iUpperLimit = " + iUpperLimit);
        Log.d("MainActivity", "iValue = " + iValue);
        Log.d("MainActivity", "bOptionByte " + ByteArray.byteArrayToHexString(new byte[]{bOptionByte}));

        try {
            Desfire.statusType retValue = desfireCard.createValueFile(bFileId, bCommSetting, baAccessBytes, iLowerLimit, iUpperLimit, iValue, bOptionByte);
            if (retValue != Desfire.statusType.SUCCESS)
                scrollLog.appendError("Create Value File Failed: " + Desfire.DesfireErrorMsg(retValue));
            else
                scrollLog.appendData("Create Value File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Get File IDs
    public void onGetFileIds() {
        try {
            scrollLog.appendTitle("Get FileIds");
            Desfire.DesfireResponse res = desfireCard.getFileIds();
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                baFileIDList = null;
                return;
            }

            if (res.data != null) {
                if (res.data.length > 0) {
                    scrollLog.appendData("FileIds :" + ByteArray.byteArrayToHexString(res.data));
                    baFileIDList = res.data;
                }
            } else {
                scrollLog.appendData("No file in directory.");
                baFileIDList = null;
            }
            bFileIDListPopulated = true;
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetFileIds", e.getMessage(), e);
        }
    }

    public Bundle onFragmentGetFileIds() {

        onGetFileIds();

        Bundle FileListInfo = new Bundle();
        FileListInfo.putByteArray("baFileIDList", baFileIDList);
        FileListInfo.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        return FileListInfo;

    }

    public void onGetIsoFileIds() {
        try {
            ByteArray baIsoFileIds = new ByteArray();

            scrollLog.appendTitle("Get ISO File IDs");
            Desfire.DesfireResponse res = desfireCard.getIsoFileIds();
            if ((res.status == Desfire.statusType.SUCCESS) || (res.status == Desfire.statusType.ADDITONAL_FRAME)) {

                //baISOFileIDList;
                baIsoFileIds.append(res.data);
                bIsoFileIdListPopulated = true;

                while (res.status == Desfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData();
                    baIsoFileIds.append(res.data);
                }
            } else {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }
            baIsoFileIdList = baIsoFileIds.toArray();

            if (baIsoFileIds.toArray().length > 0) {

                scrollLog.appendData("ISO FileIds :" + ByteArray.byteArrayToHexString(baIsoFileIdList));
            } else {
                scrollLog.appendData("No ISO FileIds");
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetIsoFileIds", e.getMessage(), e);
        }
    }


    public Bundle onFragmentGetIsoFileIds() {

        onGetIsoFileIds();

        Bundle appListInfo = new Bundle();
        appListInfo.putByteArray("ISOFileIDList", baIsoFileIdList);
        appListInfo.putBoolean("isISOFileIDListPopulated", bIsoFileIdListPopulated);

        return appListInfo;

    }
    //endregion


    //region Get File Settings
    public void onGetFileSettings() {
        scrollLog.appendTitle("Get File Settings");

        setStatusBar("Get File Settings");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getFileSettingsFragment = new fGetFileSettings();

        getFileSettingsFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getFileSettingsFragment).addToBackStack("commandview").commit();
    }

    private void parseAccessRight(String sAccessType, Byte accesss) {
        switch (accesss) {
            case (byte) 0x0E:
                scrollLog.appendData("- " + sAccessType + ": Free");
                break;
            case (byte) 0x0F:
                scrollLog.appendData("- " + sAccessType + ": Locked");
                break;
            default:
                scrollLog.appendData("- " + sAccessType + ": Key " + (int) accesss);
                break;
        }
    }

    //
    public void onGetFileSettingsReturn(byte bFileID) {
        setHomeStatusBar();
        onFragmentGetFileSettings(bFileID);
    }

    public Bundle onFragmentGetFileSettings(byte bFileID) {
        Log.d("onFragmentGetFileSet", "on FragementGetFileSettings Start");

        Bundle bundleFileSettings = new Bundle();
        try {

            Desfire.DesfireResponse res = desfireCard.getFileSettings(bFileID);
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Get File Settings Failed: " + Desfire.DesfireErrorMsg(res.status));
                bundleFileSettings.putBoolean("boolCommandSuccess", false);
                return bundleFileSettings;
            }


            bundleFileSettings.putBoolean("boolCommandSuccess", true);

            bundleFileSettings.putInt("currentAuthenticatedKey", desfireCard.currentAuthenticatedKey());
            bundleFileSettings.putByte("fileType", res.data[0]);
            bundleFileSettings.putByte("commSetting", res.data[1]);


            // Separate parsing of the three types of files
            // Standard or backup file type
            if ((res.data[0] == (byte) 0x00) || (res.data[0] == (byte) 0x01)) {

                bundleFileSettings.putByte("readAccess", (byte) ((res.data[2] >>> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("writeAccess", (byte) (res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("readWriteAccess", (byte) ((res.data[3] >>> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte) (res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("fileSize", ByteBuffer.wrap(res.data, 4, 3).order(ByteOrder.LITTLE_ENDIAN).getInt());

            } else if ((res.data[0] == (byte) 0x02)) {
                bundleFileSettings.putByte("GVD", (byte) ((res.data[2] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("GVDLC", (byte) (res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("GVDLCC", (byte) ((res.data[3] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte) (res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("lowerLimit", ByteBuffer.wrap(res.data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                bundleFileSettings.putInt("upperLimit", ByteBuffer.wrap(res.data, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                bundleFileSettings.putInt("limitedCreditValue", ByteBuffer.wrap(res.data, 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());

                bundleFileSettings.putByte("LC_FreeGV_Flag", (res.data[16]));


            } else if ((res.data[0] == (byte) 0x03) || (res.data[0] == (byte) 0x04)) {

                bundleFileSettings.putByte("readAccess", (byte) ((res.data[2] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("writeAccess", (byte) (res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("readWriteAccess ", (byte) ((res.data[3] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte) (res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("recordSize", ByteBuffer.wrap(res.data, 4, 3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                bundleFileSettings.putInt("MaxNumOfRecords", ByteBuffer.wrap(res.data, 7, 3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                bundleFileSettings.putInt("currentNumOfRecords", ByteBuffer.wrap(res.data, 10, 3).order(ByteOrder.LITTLE_ENDIAN).getShort());
            }


        } catch (Exception e) {
            bundleFileSettings.putBoolean("boolCommandSuccess", false);
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);

        }
        return bundleFileSettings;
    }
    //endregion

    //region Delete File
    public void onDeleteFile() {
        scrollLog.appendTitle("Delete File");

        setStatusBar("Delete File");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getDeleteFileFragment = new fDeleteFile();

        getDeleteFileFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getDeleteFileFragment).addToBackStack("commandview").commit();
    }


    public void onDeleteFileReturn(byte bFileID) {
        setHomeStatusBar();

        try {
            Desfire.DesfireResponse res = desfireCard.deleteFile(bFileID);
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Delete File Failed: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }

            scrollLog.appendData("Delete File " + ByteArray.byteArrayToHexString(new byte[]{bFileID}) + " Ok ");

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Read Data
    public void onReadData() {
        scrollLog.appendTitle("Read Data");

        setStatusBar("Read Data");

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        getReadDataFragment = new fReadData();
        getReadDataFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getReadDataFragment).addToBackStack("commandview").commit();
    }


    public void onReadDataReturn(byte bFileID, int iOffset, int iLength, Desfire.commMode selCommMode) {
        setHomeStatusBar();

        try {
            ByteArray baRecvData = new ByteArray();

            Desfire.DesfireResponse res = desfireCard.readData(bFileID, iOffset, iLength, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS) || (res.status == Desfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == Desfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(selCommMode);
                }
                baRecvData.append(res.data);
            }

            //
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Data:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != Desfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read File Failed: " + Desfire.DesfireErrorMsg(res.status));
                    return;
                }

                scrollLog.appendData("No data returned");
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Write Data
    public void onWriteData() {
        scrollLog.appendTitle("Write Data");

        setStatusBar("Write Data");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);
        getWriteDataFragment = new fWriteData();
        getWriteDataFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getWriteDataFragment).addToBackStack("commandview").commit();
    }

    public void onWriteDataReturn(byte bFileID, int iOffset, int iLength, byte[] bDataToWrite, Desfire.commMode selCommMode) {
        setHomeStatusBar();

        try {

            // TODO: separate data blocks if too long
            Desfire.DesfireResponse res = desfireCard.writeData(bFileID, iOffset, iLength, bDataToWrite, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS)) {
                scrollLog.appendStatus("Write Data File Success");
            } else {
                scrollLog.appendError("WriteFile Failed: " + Desfire.DesfireErrorMsg(res.status));
                Log.d("onWriteDataReturn", "writeData return: " + Desfire.DesfireErrorMsg(res.status));
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Read Records
    public void onReadRecords() {
        scrollLog.appendTitle("Read Records");

        setStatusBar("Read Records");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        getReadRecordsFragment = new fReadRecords();
        getReadRecordsFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getReadRecordsFragment).addToBackStack("commandview").commit();
    }


    public void onReadRecordsReturn(byte bFileID, int iOffsetRecord, int iNumOfRecords, Desfire.commMode selCommMode) {
        setHomeStatusBar();

        int recordLength = 0;

        try {
            ByteArray baRecvData = new ByteArray();

            if (selCommMode == Desfire.commMode.ENCIPHERED) {
                recordLength = desfireCard.getRecordLength(bFileID);
            }

            Desfire.DesfireResponse res = desfireCard.readRecords(bFileID, iOffsetRecord, iNumOfRecords, selCommMode, recordLength);
            if ((res.status == Desfire.statusType.SUCCESS) || (res.status == Desfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == Desfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(selCommMode);
                }
                baRecvData.append(res.data);
            }

            // Output
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Record:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != Desfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read Record File Failed: " + Desfire.DesfireErrorMsg(res.status));
                    return;
                }
                scrollLog.appendData("No data returned");
            }
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Write Record
    public void onWriteRecord() {
        scrollLog.appendTitle("Write Record");

        setStatusBar("Write Record");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);
        getWriteRecordFragment = new fWriteRecord();
        getWriteRecordFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getWriteRecordFragment).addToBackStack("commandview").commit();
    }


    public void onWriteRecordReturn(byte bFileID, int iRecordNum, int iSizeToWrite, byte[] bDataToWrite, Desfire.commMode selCommMode) {
        setHomeStatusBar();

        try {
            // TODO: separate data blocks if too long
            Desfire.DesfireResponse res = desfireCard.writeRecord(bFileID, iRecordNum, iSizeToWrite, bDataToWrite, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS)) {
                scrollLog.appendStatus("Write Record File Success");
            } else {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
            }
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Clear Record File
    public void onClearRecordFile() {
        scrollLog.appendTitle("Clear Record File");

        setStatusBar("Clear Record File");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        getClearRecordFileFragment = new fClearRecordFile();
        getClearRecordFileFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getClearRecordFileFragment).addToBackStack("commandview").commit();
    }

    //
    public void onClearRecordFileReturn(byte bFileID) {
        setHomeStatusBar();

        try {
            Desfire.DesfireResponse res = desfireCard.clearRecordFile(bFileID);
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                }
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Value
    public void onGetValue() {
        scrollLog.appendTitle("Get Value");

        setStatusBar("Get Value");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        getValueFragment = new fGetValue();

        getValueFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, getValueFragment).addToBackStack("commandview").commit();
    }

    //
    public void onGetValueReturn(byte bFileID, Desfire.commMode curCommMode) {
        setHomeStatusBar();
        try {
            Desfire.DesfireResponse res = desfireCard.getValue(bFileID, curCommMode);
            if (res.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Error: " + Desfire.DesfireErrorMsg(res.status));
                return;
            }

            if (res.data.length > 0) {
                Log.d("onGetValueReturn", "Value Byte: " + ByteArray.byteArrayToHexString(res.data));
                scrollLog.appendData("Value:" + ByteBuffer.wrap(res.data).order(ByteOrder.LITTLE_ENDIAN).getInt());
            } else {
                scrollLog.appendData("No data returned");
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion`

    //region Credit
    public void onCredit() {
        scrollLog.appendTitle("Credit");

        setStatusBar("Credit");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);
        creditFragment = new fCredit();
        creditFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, creditFragment).addToBackStack("commandview").commit();
    }


    public void onCreditReturn(byte bFileID, int iCreditValue, Desfire.commMode selCommMode) {
        setHomeStatusBar();

        try {
            Desfire.DesfireResponse res = desfireCard.credit(bFileID, iCreditValue, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS)) {
                scrollLog.appendStatus("Credit Success");
            } else {
                scrollLog.appendError("Credit Failed: " + Desfire.DesfireErrorMsg(res.status));
                Log.d("onCreditReturn", "Error returned: " + Desfire.DesfireErrorMsg(res.status));
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Debit
    public void onDebit() {
        scrollLog.appendTitle("Debit");

        setStatusBar("Debit");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        debitFragment = new fDebit();
        debitFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, debitFragment).addToBackStack("commandview").commit();
    }


    public void onDebitReturn(byte bFileID, int iDebitValue, Desfire.commMode selCommMode) {
        setHomeStatusBar();
        try {
            Desfire.DesfireResponse res = desfireCard.debit(bFileID, iDebitValue, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS)) {
                scrollLog.appendStatus("Debit Success");
            } else {
                scrollLog.appendError("Debit Failed: " + Desfire.DesfireErrorMsg(res.status));
                Log.d("onDebitReturn", "Error returned: " + Desfire.DesfireErrorMsg(res.status));
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Limited Credit
    public void onLimitedCredit() {
        scrollLog.appendTitle("Limited Credit");

        setStatusBar("Limited Credit");


        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);
        bundle.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        limitedCreditFragment = new fLimitedCredit();
        limitedCreditFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, limitedCreditFragment).addToBackStack("commandview").commit();
    }


    public void onLimitedCreditReturn(byte bFileID, int iLCValue, Desfire.commMode selCommMode) {
        setHomeStatusBar();
        try {
            Desfire.DesfireResponse res = desfireCard.limitedCredit(bFileID, iLCValue, selCommMode);
            if ((res.status == Desfire.statusType.SUCCESS)) {
                scrollLog.appendStatus("Limited Credit Success");
            } else {
                scrollLog.appendError("Limited Credit Failed: " + Desfire.DesfireErrorMsg(res.status));
                Log.d("onLimitedCreditReturn", "Error returned: " + Desfire.DesfireErrorMsg(res.status));
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Authentication Tests
    public void onAuthenticateTest() {

        byte[] zeroKey = new byte[8];
        Arrays.fill(zeroKey, (byte) 0);

        try {
            scrollLog.appendTitle("Authentication");
            Desfire.statusType res = desfireCard.authenticate((byte) 0x0A, (byte) 0, zeroKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
            } else {

                scrollLog.appendStatus("Authentication Successful");
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticateTest", e.getMessage(), e);
        }
    }

    public void onAuthISOTest(int keySize) {
        byte[] zeroKey = new byte[keySize];
        Arrays.fill(zeroKey, (byte) 0);

        onAuthISOTest(keySize, zeroKey);
    }

    public void onAuthISOTest(int keySize, byte[] key) {
        scrollLog.appendTitle("AuthenticateISO Test");

        try {
            Desfire.statusType res = desfireCard.selectApplication(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00});
            res = desfireCard.selectApplication(new byte[]{(byte) 0x15, (byte) 0x0D, (byte) 0xE5});
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendData("Select OK: 15 0D E5");


            res = desfireCard.authenticate((byte) 0x1A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthISOTest", e.getMessage(), e);
        }
    }

    public void onAuthAESTest() {
        scrollLog.appendTitle("AuthenticateISO Test");

        try {
            // Select Application
            Desfire.statusType res = desfireCard.selectApplication(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00});
            res = desfireCard.selectApplication(new byte[]{(byte) 0x15, (byte) 0x0A, (byte) 0xE5});
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: 15 0A E5");


            byte[] zeroKey = new byte[16];
            Arrays.fill(zeroKey, (byte) 0);

            scrollLog.appendTitle("Authentication");
            res = desfireCard.authenticate((byte) 0xAA, (byte) 0, zeroKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }


        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthAESTest", e.getMessage(), e);
        }
    }

    public void onAuthEV2Test() {
        scrollLog.appendTitle("AuthenticateEV2 Test");

        try {
            // Select Application

            Desfire.statusType res = desfireCard.selectApplication(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00});
            res = desfireCard.selectApplication(new byte[]{(byte) 0x15, (byte) 0x0A, (byte) 0xE5});
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: 15 0A E5");


            byte[] zeroKey = new byte[16];
            Arrays.fill(zeroKey, (byte) 0);

            scrollLog.appendTitle("Authentication");
            res = desfireCard.authenticate((byte) 0x71, (byte) 0, zeroKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }


        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthAESTest", e.getMessage(), e);
        }
    }
    //endregion

    //region Create Test Perso
    public void onCreateTestPerso() {
        scrollLog.appendTitle("Create Test Personalization File System");

        try {
            scrollLog.appendTitle("Authentication");
            byte[] key = new byte[8];
            Arrays.fill(key, (byte) 0);
            byte[] T3DESKey = new byte[24];
            Arrays.fill(T3DESKey, (byte) 0);
            byte[] AESKey = new byte[16];
            Arrays.fill(AESKey, (byte) 0);

            Desfire.statusType res = desfireCard.authenticate((byte) 0x0A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            res = desfireCard.formatPICC();
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Format PICC Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Format PICC Successful");
            }
            //region  Create Application D40 DES (D4 0D E5)
            byte[] baAppId = new byte[]{(byte) 0xD4, (byte) 0x0D, (byte) 0xE5};
            byte[] baNull = new byte[]{};
            res = desfireCard.createApplication(baAppId, (byte) 0x0F, (byte) 0x03, baNull, baNull);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0x0A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            createTestDirFiles();
            //endregion

            //region  Create Application ISO DES (15 0D E5)
            // Select Application
            baAppId = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate main key
            res = desfireCard.authenticate((byte) 0x0A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            baAppId = new byte[]{(byte) 0x15, (byte) 0x0D, (byte) 0xE5};
            res = desfireCard.createApplication(baAppId, (byte) 0x0F, (byte) 0x43, baNull, baNull);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0x1A, (byte) 0, T3DESKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }
            createTestDirFiles();
            //endregion

            //region  Create Application ISO AES (15 0A E5)
            // Select Application
            baAppId = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate main key
            res = desfireCard.authenticate((byte) 0x0A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            baAppId = new byte[]{(byte) 0x15, (byte) 0x0A, (byte) 0xE5};
            res = desfireCard.createApplication(baAppId, (byte) 0x0F, (byte) 0x83, baNull, baNull);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0xAA, (byte) 0, AESKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }
            createTestDirFiles();
            //endregion

            //region  Create Application AuthEV2 AES (AE 2A E5)
            // Select Application
            baAppId = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate main key
            res = desfireCard.authenticate((byte) 0x0A, (byte) 0, key);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            baAppId = new byte[]{(byte) 0xAE, (byte) 0x2A, (byte) 0xE5};
            res = desfireCard.createApplication(baAppId, (byte) 0x0F, (byte) 0x83, baNull, baNull);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            res = desfireCard.selectApplication(baAppId);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + Desfire.DesfireErrorMsg(res));
                return;
            } else
                scrollLog.appendStatus("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0xAA, (byte) 0, AESKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }
            createTestDirFiles();
            //endregion
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }
    //endregion

    //region Desfire Light Test
    public void onTestDesfireLight() {
        scrollLog.appendTitle("Test DESFire Light Default configuration");

        try {
            scrollLog.appendTitle("Select DF 01");

            byte[] baFileId = new byte[]{(byte) 0xDF, (byte) 0x01};
            byte[] baNull = new byte[]{};
            Desfire.ISOResponse isoResp = desfireCard.selectIsoFileId(baFileId);
            if (isoResp.status != Desfire.statusWord.SUCCESS) {
                scrollLog.appendError("Select DF01 Failed: " + Desfire.DesfireErrorMsg(isoResp.status));
                return;
            } else {
                scrollLog.appendStatus("Select DF01 OK: " + ByteArray.byteArrayToHexString(baFileId));
            }

            scrollLog.appendTitle("Authentication EV2");
            byte[] AESKey = new byte[16];
            Arrays.fill(AESKey, (byte) 0);

            Desfire.statusType res = desfireCard.authenticate((byte) 0x71, (byte) 0x03, AESKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            scrollLog.appendTitle("Get Card UID");
            Desfire.DesfireResponse dfresp = desfireCard.getCardUID();
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("GetCardUID Error: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            }
            byte [] cardUID = new byte[7];
            System.arraycopy(dfresp.data,0,cardUID,0,7);

            scrollLog.appendTitle("Get Value");
            dfresp = desfireCard.getValue((byte)0x03, PLAIN);
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Get Value: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            } else {
                scrollLog.appendStatus("Get Value: " + ByteBuffer.wrap(dfresp.data).order(ByteOrder.LITTLE_ENDIAN).getInt());
            }

            scrollLog.appendTitle("Credit - 100");
            dfresp = desfireCard.credit((byte)0x03, 100, ENCIPHERED);
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Credit: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            } else {
                scrollLog.appendStatus("Credit Ok ");
            }

            scrollLog.appendTitle("" + "Debit - 100");
            dfresp = desfireCard.debit((byte)0x03, 100, ENCIPHERED);
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Debit: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            } else {
                scrollLog.appendStatus("Debit Ok ");
            }

            scrollLog.appendTitle("Authenticate EV2 Non-First");
            res = desfireCard.authenticate((byte) 0x77, (byte) 0x01, AESKey);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            scrollLog.appendTitle("Commit Reader ID");
            dfresp = desfireCard.commitReaderID(AESKey);
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("CommitReaderID: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            } else {
                scrollLog.appendStatus("CommitReaderID Ok ");
            }

            scrollLog.appendTitle("Commit Transaction");
            dfresp = desfireCard.commitTransaction((byte) 0x01);
            if (dfresp.status != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("CommitTransaction: " + Desfire.DesfireErrorMsg(dfresp.status));
                return;
            } else {
                scrollLog.appendStatus("CommitTransacation Ok ");
            }

            scrollLog.appendTitle("Transaction MAC Verification");
            res = desfireCard.txnMacVerification(AESKey, cardUID, dfresp.data);
            if (res != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("txnMacVerification: " + Desfire.DesfireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("txnMacVerification Ok ");
            }


        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onTestDesfireLight", e.getMessage(), e);
        }
    }
    //endregion

    //region Testing
    public void createTestDirFiles () {
        try {
            byte[] baNull = new byte[]{};

            Desfire.statusType retValue;
            // Create Data File
            retValue = desfireCard.createStdDataFile((byte) 0x01, baNull, Desfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 32);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createStdDataFile((byte) 0x02, baNull, Desfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 32);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }

            retValue = desfireCard.createStdDataFile((byte) 0x03, baNull, Desfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 32);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x04, baNull, Desfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 8, 3);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x05, baNull, Desfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 8, 3);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x06, baNull, Desfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 8, 3);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x07, Desfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 0, 1000, 0, (byte) 0x01);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x08, Desfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 0, 1000, 0, (byte) 0x01);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x09, Desfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 0, 1000, 0, (byte) 0x01);
            if (retValue != Desfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + Desfire.DesfireErrorMsg(retValue));
                return;
            }
            scrollLog.appendStatus("Create Data File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("createTestDirFiles", e.getMessage(), e);
        }
    }

    public void onApplicationTest () {

        scrollLog.appendTitle("***** TEST Plain Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onWriteDataReturn((byte) 0x01, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, PLAIN);
        Log.d("TestAll", "*** Read Plain Data **************************");
        onReadDataReturn((byte) 0x01,0,3,PLAIN);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Plain Record");
        onClearRecordFileReturn((byte) 0x04);
        onCommitTransaction();
        Log.d("TestAll", "*** Write Plain Record **************************");
        onWriteRecordReturn((byte) 0x04, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, PLAIN);
        onCommitTransaction();
        Log.d("TestAll", "*** Read Plain Record **************************");
        onReadRecordsReturn((byte) 0x04, 0, 0, PLAIN);

        scrollLog.appendTitle("***** TEST MAC Data");
        Log.d("TestAll", "*** Write MAC Data **************************");
        onWriteDataReturn((byte) 0x02, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, Desfire.commMode.MAC);
        Log.d("TestAll", "*** Read MAC Data **************************");
        onReadDataReturn((byte) 0x02, 0, 0, MAC);

        scrollLog.appendTitle("***** TEST Encrypted Data");
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataReturn((byte) 0x03, 0, 10, ENCIPHERED);
        Log.d("TestAll", "*** Write Encrypted Data **************************");
        onWriteDataReturn((byte) 0x03, 0, 4, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x00}, Desfire.commMode.ENCIPHERED);
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataReturn((byte) 0x03, 0, 10, ENCIPHERED);


        scrollLog.appendTitle("***** TEST MAC Record");
        onClearRecordFileReturn((byte) 0x05);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x05, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, MAC);
        onCommitTransaction();
        Log.d("TestAll", "*** Read MAC Record **************************");
        onReadRecordsReturn((byte) 0x05, 0, 0, MAC);


        scrollLog.appendTitle("***** TEST Encrypted Record");
        onClearRecordFileReturn((byte) 0x06);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x06, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, ENCIPHERED);
        onCommitTransaction();
        Log.d("TestAll", "*** Read MAC Record **************************");
        onReadRecordsReturn((byte) 0x06, 0, 0, ENCIPHERED);

        scrollLog.appendTitle("***** TEST Get Value - Plain");
        onGetValueReturn((byte) 0x07, PLAIN);
        onCreditReturn((byte)0x07,80,PLAIN);
        onCommitTransaction();
        onGetValueReturn((byte) 0x07, PLAIN);
        onDebitReturn((byte)0x07,80,PLAIN);
        onCommitTransaction();
        onGetValueReturn((byte) 0x07, PLAIN);
        onLimitedCreditReturn((byte)0x07, 20, PLAIN);
        onCommitTransaction();
        onGetValueReturn((byte) 0x07, PLAIN);
        scrollLog.appendTitle("***** TEST Get Value - MAC");
        onGetValueReturn((byte) 0x08, MAC);
        onCreditReturn((byte)0x08,90,MAC);
        onCommitTransaction();
        onGetValueReturn((byte) 0x08, MAC);
        onDebitReturn((byte)0x08,90,MAC);
        onCommitTransaction();
        onGetValueReturn((byte) 0x08, MAC);
        onLimitedCreditReturn((byte)0x08, 20, MAC);
        onCommitTransaction();
        onGetValueReturn((byte) 0x08, MAC);
        scrollLog.appendTitle("***** TEST Get Value - Enciphered");
        onGetValueReturn((byte) 0x09, ENCIPHERED);
        onCreditReturn((byte)0x09,100,ENCIPHERED);
        onCommitTransaction();
        onGetValueReturn((byte) 0x09, ENCIPHERED);
        onDebitReturn((byte)0x09,100,ENCIPHERED);
        onCommitTransaction();
        onGetValueReturn((byte) 0x09, ENCIPHERED);
        onLimitedCreditReturn((byte)0x09, 20, ENCIPHERED);
        onCommitTransaction();
        onGetValueReturn((byte) 0x09, ENCIPHERED);
    }

    public void onTestAll() {

        // Select preset app D40
        Log.d("TestAll", "*** Test D40 ****************************");
        scrollLog.appendTitle("***** TEST D40 ***** ");
        onSelectApplicationReturn(new byte[] { (byte) 0xD4, (byte) 0x0D, (byte) 0xE5});

        onAuthenticateTest ();

        onApplicationTest();
        /*
        // Test ISO DES
        Log.d("TestAll", "*** Test ISO DES ****************************");
        scrollLog.appendTitle("***** TEST ISO DES ***** ");
        onAuthISOTest (24);
        onApplicationTest ();

        Log.d("TestAll", "*** Test AES ****************************");
        scrollLog.appendTitle("***** TEST ISO AES ***** ");
        onAuthAESTest ();
        onApplicationTest ();


        Log.d("TestAll", "*** Test EV2 ****************************");
        scrollLog.appendTitle("***** TEST EV2 AES ***** ");
        onAuthEV2Test ();
        onApplicationTest ();
*/
    }

    public void onTestCurrent() {

        byte [] key00_8 = new byte [] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte [] keyAA_8 = new byte [] {(byte)0xAA,(byte)0xBB,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        byte [] key00_16 = new byte [] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte [] keyEX_16 = new byte [] {(byte)0xB0,(byte)0xB1,(byte)0xB2,(byte)0xB3,(byte)0xB4,(byte)0xB5,(byte)0xB6,(byte)0xB7,(byte)0xB8,(byte)0xB9,(byte)0xBA,(byte)0xBB,(byte)0xBC,(byte)0xBD,(byte)0xBE,(byte)0xBF};
        byte [] key01_16 = new byte [] {(byte)0x12,(byte)0x22,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte [] key0A_16 = new byte [] {(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A};
        byte [] key0B_16 = new byte [] {(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B};
        byte [] key00_24 = new byte [] {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte [] key0A_24 = new byte [] {(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A,(byte)0x0A};
        byte [] key0B_24 = new byte [] {(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B,(byte)0x0B};


/*
        Log.d("onSetConfiguration", "*** Test Change Key ****************************");
        scrollLog.appendTitle("***** TEST D40 DES CHANGE KEY ");
        onSelectApplicationReturn(new byte[] { (byte) 0xD4, (byte) 0x0D, (byte) 0xE5});

        onAuthenticateTest();

        scrollLog.appendTitle("***** TEST Change Same key Data");
        Log.d("TestAll", "*** TEST Change Same key Data **************************");
        onChangeKeyReturn((byte) 0x00, (byte) 0x00, key00_16, null);


        onAuthenticateTest ();

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
         onChangeKeyReturn((byte) 0x01, (byte) 0x00, key00_16, key00_16);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x01, (byte) 0x00, key00_16, key0A_16);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x00, key0B_16, key00_16);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x00, key00_16, key0B_16);
        Log.d("TestAll", "*** Test Change Key ****************************");

*/

        Log.d("TestAll", "*** Test Change Key ****************************");
        scrollLog.appendTitle("***** TEST ISO DES CHANGE KEY ");
        onSelectApplicationReturn(new byte[] { (byte) 0x15, (byte) 0x0D, (byte) 0xE5});

        onAuthISOTest (24,key00_24);

        scrollLog.appendTitle("***** TEST Change Same key Data");
        Log.d("TestAll", "*** TEST Change Same key Data **************************");
        onChangeKeyReturn((byte) 0x00, (byte) 0x0A, key00_24, null);


        onAuthISOTest (24);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x01, (byte) 0x00, key0A_24, key00_24);
/*
        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x01, (byte) 0x00, key00_24, key0A_24);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x00, key0B_24, key00_24);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** TEST Change different key Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x00, key00_24, key0B_24);
        Log.d("TestAll", "*** Test Change Key ****************************");


        scrollLog.appendTitle("***** TEST ISO AES CHANGE KEY ");
        onSelectApplicationReturn(new byte[] { (byte) 0x15, (byte) 0x0A, (byte) 0xE5});

        onAuthAESTest ();

        scrollLog.appendTitle("***** TEST Change Same key Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onChangeKeyReturn((byte) 0x00, (byte) 0x0A, key00_16, null);


        onAuthAESTest ();

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onChangeKeyReturn((byte) 0x01, (byte) 0x1A, key0A_16, key00_16);
*/
/*
        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onChangeKeyReturn((byte) 0x01, (byte) 0x1A, key00_16, key0B_16);

        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x2A, key0B_16, key00_16);
        scrollLog.appendTitle("***** TEST Change different key Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onChangeKeyReturn((byte) 0x02, (byte) 0x2A, key00_16, key0B_16);

/**/
    }



        //endregion



}
