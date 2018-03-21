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
import java.util.Arrays;

import static com.example.ac.desfirelearningtool.MifareDesfire.commMode.ENCIPHERED;
import static com.example.ac.desfirelearningtool.MifareDesfire.commMode.MAC;
import static com.example.ac.desfirelearningtool.MifareDesfire.commMode.PLAIN;


public class MainActivity extends AppCompatActivity implements IMainActivityCallbacks{
    private AdView mAdView;

    fCommandMenu commandFragment;
    fSelectApplication selectFragment;
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

    protected PendingIntent pendingIntent;
    protected IntentFilter[] intentFiltersArray;
    protected String[][] techListsArray;
    protected NfcAdapter nfcAdapter;

    private AndroidCommunicator communicator;
    private MifareDesfire desfireCard;

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
        checkboxWrapperMode = (CheckBox) findViewById(R.id.CheckBox_WrapAPDU);
        scrollViewTextLog = findViewById(R.id.scrollview_TextLog);
        scrollLog = new ScrollLog((TextView) findViewById(R.id.textView_scrollLog),scrollViewTextLog);

        applicationList = null;
        baFileIDList = null;
        boolWrapperMode = false;

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
            }
        });
        checkboxWrapperMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean IsChecked) {
                Log.d("MainActivity:", "Wrap APDU Check Box checked ");
                // checkedId is the RadioButton selected
                if (IsChecked)
                    boolWrapperMode = true;
                else
                    boolWrapperMode = false;
            }
        });

        // Initialize additional data for foreground dispatching of Nfc intents
        this.pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        this.intentFiltersArray = new IntentFilter[]{ new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) };
        this.techListsArray = new String[][] {
                new String[] { android.nfc.tech.IsoDep.class.getName() },
                new String[] { android.nfc.tech.MifareClassic.class.getName() },
                new String[] { android.nfc.tech.NfcV.class.getName() }
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
     *  Rearm Thread
     *
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
                }
                catch (Exception e) {
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
            communicator = new AndroidCommunicator(IsoDep.get(tag), boolWrapperMode,scrollLog);

            desfireCard = communicator.get(tag); // we do not specify a key here!
            if (desfireCard == null) {
                Log.d("onCardDetection", "Not a DESFire card");
                new Exception().printStackTrace();
            }

            desfireCard.connect();
            Log.d("onCardDetection:"," CONNECTED");
            //scrollLog.setTextColor(0xAAFF0000);
            scrollLog.appendTitle("DESFire Connected\n");
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

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            Log.e("onCardDetection", e.getMessage(), e);
        }
    }
    //endregion

    //region Miscellaneous Helpers
    @Override
    public void onBackPressed() {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {

            getSupportActionBar().setTitle("DESFire Tool");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportFragmentManager().popBackStack();
            Log.d("onBackPressed", "popBackStack");
        } else {
            super.onBackPressed();
            getSupportActionBar().setTitle("DESFire Tool");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            Log.d("onBackPressed", "onBackPressed()");
        }
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
        }
        catch (Exception ex) {
            Log.e("Tag_getTagService", ex.getMessage());
            return null;
        }
    }


    public static int Tag_getServiceHandle(Tag that) {
        try {
            Class c = Tag.class;
            //c = Class.forName("android.nfc.Tag");
            Method m = c.getMethod("getServiceHandle");
            int serviceHandle = (Integer)m.invoke(that);
            return serviceHandle;
        }
        catch (Exception ex) {
            Log.e("Tag_getServiceHandle", ex.getMessage());
            return 0;
        }
    }

    public static int INfcTag_connect(Object that, int nativeHandle, int technology) {
        try {
            Class c = Class.forName("android.nfc.INfcTag$Stub$Proxy");

            Method m = c.getMethod("connect", int.class, int.class);
            return (Integer)m.invoke(that, nativeHandle, technology);
        }
        catch (Exception ex) {
            Log.e("INfcTag_connect", ex.getMessage());
            return -1;
        }
    }

    //public boolean isPresent(int nativeHandle) throws RemoteException
    public static boolean INfcTag_isPresent(Object that, int nativeHandle) throws Exception {
        Class c = Class.forName("android.nfc.INfcTag$Stub$Proxy");

        Method m = c.getMethod("isPresent", int.class);
        boolean result = (Boolean)m.invoke(that, nativeHandle);
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

    public ScrollLog getScrollLogObject () {
        return scrollLog;
    }

    //region Get Version
    public void onGetVersion (){

        try {
            scrollLog.appendTitle("Get Version");
            byte [] hwInfo = desfireCard.getVersion().data;
            byte [] swInfo = desfireCard.getMoreData().data;
            byte [] UIDInfo = desfireCard.getMoreData().data;

            scrollLog.appendStatus("Hardware related information:");
            scrollLog.appendData("Vendor ID:           " + ByteArray.byteArrayToHexString(hwInfo,0,1));
            scrollLog.appendData("Type:                " + ByteArray.byteArrayToHexString(hwInfo,1,1));
            scrollLog.appendData("Sub-type             " + ByteArray.byteArrayToHexString(hwInfo,2,1));
            scrollLog.appendData("Major/minor version: " + ByteArray.byteArrayToHexString(hwInfo,3,1) + "/ " + ByteArray.byteArrayToHexString(hwInfo,4,1) );
            scrollLog.appendData("Storage size:        " + ByteArray.byteArrayToHexString(hwInfo,5,1));
            scrollLog.appendData("Protocol:            " + ByteArray.byteArrayToHexString(hwInfo,6,1));
            scrollLog.appendStatus("Software related information:");
            scrollLog.appendData("Vendor ID:           " + ByteArray.byteArrayToHexString(swInfo,0,1));
            scrollLog.appendData("Type:                " + ByteArray.byteArrayToHexString(swInfo,1,1));
            scrollLog.appendData("Sub-type             " + ByteArray.byteArrayToHexString(swInfo,2,1));
            scrollLog.appendData("Major/minor version: " + ByteArray.byteArrayToHexString(swInfo,3,1) + "/ " + ByteArray.byteArrayToHexString(swInfo,4,1) );
            scrollLog.appendData("Storage size:        " + ByteArray.byteArrayToHexString(swInfo,5,1));
            scrollLog.appendData("Protocol:            " + ByteArray.byteArrayToHexString(swInfo,6,1));
            scrollLog.appendStatus("UID and manufacturer information:");
            scrollLog.appendData("UID:                 " + ByteArray.byteArrayToHexString(UIDInfo,0,7));
            scrollLog.appendData("Batch number:        " + ByteArray.byteArrayToHexString(UIDInfo,7,5));
            scrollLog.appendData("Cal week of prod:    " + ByteArray.byteArrayToHexString(UIDInfo,12,1));
            scrollLog.appendData("Production year:     " + ByteArray.byteArrayToHexString(UIDInfo,13,1));
            // Parse all info
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Card UID
    public void onGetCardUID (){

        try {
            scrollLog.appendTitle("Get Card UID");
            MifareDesfire.DesfireResponse res = desfireCard.getCardUID();
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get Card UID Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }
            scrollLog.appendStatus("Decrypted Card UID: " + ByteArray.byteArrayToHexString(res.data));
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Application IDs
    public void onGetApplicationIDs (){

        try {
            scrollLog.appendTitle("Get Application ID");
            MifareDesfire.DesfireResponse res = desfireCard.getApplicationIDs();
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get Application ID Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }
            applicationList = res.data;
            applicationListPopulated = true;

            if (applicationList.length == 0) {
                scrollLog.appendTitle ("No application on card " );
                return;
            }
            scrollLog.appendTitle ("getAppIDs : " + ByteArray.byteArrayToHexString(applicationList));

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }

    public Bundle onFragmentGetApplicationIDs (){

        onGetApplicationIDs();

        Bundle appListInfo = new Bundle();
        appListInfo.putByteArray("applicationList",applicationList);
        appListInfo.putBoolean("applicationListPopulated", applicationListPopulated);

        return appListInfo;

    }
    //endregion

    //region Get Free Memory
    public void onGetFreeMem (){

        try {
            scrollLog.appendTitle("Get Get Free Memory");
            MifareDesfire.DesfireResponse res = desfireCard.getFreeMem();

            ByteBuffer bb = ByteBuffer.wrap(res.data);
            bb.order( ByteOrder.LITTLE_ENDIAN);

            scrollLog.appendTitle("Free Memory: " + bb.getShort() + "B");
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Key Settings
    public void onGetKeySettings (){

        try {
            scrollLog.appendTitle("Get Get Key Settings");
            MifareDesfire.DesfireResponse res = desfireCard.getKeySettings();

            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get Key Settins Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }
            scrollLog.appendTitle("Key Settings: " + ByteArray.byteArrayToHexString(res.data));

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
                scrollLog.appendData("- Configuration changeable");
            }
            if ((res.data[0] & (byte) 0x04) != (byte) 0x00) {
                scrollLog.appendData("- Master key not required for create/delete");
            }
            if ((res.data[0] & (byte) 0x02) != (byte) 0x00) {
                scrollLog.appendData("- Free directory list access");
            }
            if ((res.data[0] & (byte) 0x01) != (byte) 0x00) {
                scrollLog.appendData("- Allow change of master key");
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
            scrollLog.appendData("- No. of keys: " + ByteArray.byteToHexString( (byte) (res.data[1] & (byte) 0x07)));

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Get DF Names
    public void onGetDFNames (){

        try {
            scrollLog.appendTitle("Get DF Names");
            MifareDesfire.DesfireResponse res = desfireCard.getDFNames();
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME))
                scrollLog.appendTitle("Application 1: " + ByteArray.byteArrayToHexString(res.data));
            int i = 2;
            while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                res = desfireCard.getMoreData();
                scrollLog.appendTitle("Application " + i + ": " + ByteArray.byteArrayToHexString(res.data));
                i++;
            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Set Configuration
    public void onSetConfiguration (){

        try {
            scrollLog.appendTitle("Set Configuration");
            // TODO Set Configuration

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Authenticate
    public void onAuthenticate (){
        getSupportActionBar().setTitle("Authenticate");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        authenticateFragment = new fAuthenticate();
        authenticateFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,authenticateFragment).addToBackStack("commandview").commit();

    }

    public void onAuthenticateReturn(byte bAuthCmd, byte bKeyNo, byte[] key) {
        scrollLog.appendTitle("Authenticate");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        getSupportFragmentManager().popBackStack();

        try {
            MifareDesfire.statusType res = desfireCard.authenticate(bAuthCmd, bKeyNo, key);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");

            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }
    //endregion

    //region Authentication Test
    public void onAuthenticateTest (){

        byte[] zeroKey = new byte[8];
        Arrays.fill(zeroKey, (byte)0);

        try {
            scrollLog.appendTitle("Authentication");
            MifareDesfire.statusType res = desfireCard.authenticate((byte) 0x0A, (byte) 0, zeroKey);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
            } else{

                scrollLog.appendStatus("Authentication Successful");
            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }

    public void onAuthISOTest (){

        byte[] zeroKey = new byte[24];
        Arrays.fill(zeroKey, (byte)0);

        try {
            scrollLog.appendTitle("Authentication");
            MifareDesfire.statusType res = desfireCard.authenticate((byte) 0x1A, (byte) 0, zeroKey);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }

    public void onAuthAESTest (){

        byte[] zeroKey = new byte[16];
        Arrays.fill(zeroKey, (byte)0);

        try {
            scrollLog.appendTitle("Authentication");
            MifareDesfire.statusType res = desfireCard.authenticate((byte) 0xAA, (byte) 0, zeroKey);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }


        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }
    //endregion

    //region Select Application
    public void onSelectApplication (){
        scrollLog.appendTitle("Select Application");

        getSupportActionBar().setTitle("Select Application");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("applicationList", applicationList);
        bundle.putBoolean("applicationListPopulated", applicationListPopulated);
        selectFragment = new fSelectApplication();
        selectFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, selectFragment).addToBackStack("commandview").commit();

    }

    public void onSelectApplicationReturn(byte [] baAppId) {
        scrollLog.appendTitle("SelectApplication Return");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        baFileIDList = null;

        scrollLog.appendTitle("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            MifareDesfire.statusType retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
            else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Create Application
    public void onCreateApplication (){
        scrollLog.appendTitle("Create Application");

        getSupportActionBar().setTitle("Create Application");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        createApplicationFragment = new fCreateApplication();
        createApplicationFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,createApplicationFragment).addToBackStack("commandview").commit();

    }

    public void onCreateApplicationReturn(byte [] baAppId, byte bKeySetting1, byte bKeySetting2, byte [] baISOName, byte [] DFName){
        scrollLog.appendTitle("Create Application Return");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        getSupportFragmentManager().popBackStack();
        scrollLog.appendTitle("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            MifareDesfire.statusType mfResult = desfireCard.createApplication(baAppId, bKeySetting1, bKeySetting2, baISOName, DFName);
            if (mfResult != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Create Application Failed: " + desfireCard.DesFireErrorMsg(mfResult));
            else
                scrollLog.appendTitle("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Delete Application
    public void onDeleteApplication (){
        scrollLog.appendTitle("Delete Application");

        getSupportActionBar().setTitle("Delete Application");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("applicationList", applicationList);
        deleteApplicationFragment = new fDeleteApplication();
        deleteApplicationFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, deleteApplicationFragment).addToBackStack("commandview").commit();

    }

    public void onDeleteApplicationReturn(byte [] baAppId) {
        scrollLog.appendTitle("Delete Application Return");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        scrollLog.appendTitle("Application ID returned = " + ByteArray.byteArrayToHexString(baAppId));
        if (baAppId.length != 3) {
            scrollLog.appendError("Application ID too short");
            return;
        }

        try {
            MifareDesfire.statusType retValue = desfireCard.deleteApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Delete Application Failed: " + desfireCard.DesFireErrorMsg(retValue));
            else
                scrollLog.appendTitle("Delete Application OK: " + ByteArray.byteArrayToHexString(baAppId));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Get Key Version
    public void onGetKeyVersion (){
        scrollLog.appendTitle("Get Key Version");

        getSupportActionBar().setTitle("Get Key Version");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getKeyVersionFragment = new fGetKeyVersion();


        getKeyVersionFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getKeyVersionFragment).addToBackStack("commandview").commit();

    }

    public void onGoGetKeyVersionReturn(byte iKeyToInquire) {
        scrollLog.appendTitle("Get Key Version Return");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();
        commandFragment.enableAllButtons();
        

        scrollLog.appendTitle("Key to inquire = " + iKeyToInquire);

        try {
            MifareDesfire.DesfireResponse retValue = desfireCard.getKeyVersion(iKeyToInquire);
            if (retValue.status != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Get Key Version Failed: " + desfireCard.DesFireErrorMsg(retValue.status));
            else
                scrollLog.appendTitle("Key Version: " + ByteArray.byteArrayToHexString(retValue.data));
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Format PICC
    public void onFormatPICC (){

        try {
            scrollLog.appendTitle("Format PICC");
            MifareDesfire.statusType res = desfireCard.formatPICC();

            if (res  == MifareDesfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion

    //region Commit Transaction
    public void onCommitTransaction (){

        try {
            scrollLog.appendTitle("Commit Transaction");
            MifareDesfire.DesfireResponse res = desfireCard.commitTransaction();

            if (res.status  == MifareDesfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onCommitTransaction", e.getMessage(), e);
        }
    }
    //endregion

    //region Abort Transaction
    public void onAbortTransaction (){

        try {
            scrollLog.appendTitle("Abort Transaction");
            MifareDesfire.DesfireResponse res = desfireCard.abortTransaction();

            if (res.status  == MifareDesfire.statusType.AUTHENTICATION_ERROR)
                scrollLog.appendError("Authentication Error: PICC Master Key is not authenticated");
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAbortTransaction", e.getMessage(), e);
        }
    }
    //endregion


    //region create File
    public void onCreateFile (){
        scrollLog.appendTitle("Create File");

        getSupportActionBar().setTitle("Create File");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        createFileFragment = new fCreateFile();

        createFileFragment.setArguments(getIntent().getExtras());
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,createFileFragment).addToBackStack("commandview").commit();
    }

    public void onCreateFileDataReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iFileSize) {
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();


        Log.d("MainActivity", "bFileType " + ByteArray.byteArrayToHexString(new byte[] {bFileType}));
        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[] {bFileId}));
        Log.d("MainActivity", "baISOName " + ByteArray.byteArrayToHexString(baISOName));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[] {bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iFileSize = " + iFileSize);
        try {
            MifareDesfire.statusType retValue = desfireCard.createDataFile(bFileType, bFileId, baISOName, bCommSetting, baAccessBytes, iFileSize);
            if (retValue != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Create Data File Failed: " + desfireCard.DesFireErrorMsg(retValue));
            else
                scrollLog.appendTitle("Create Data File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    public void onCreateFileRecordReturn (byte bFileType, byte bFileId, byte[] baISOName, byte bCommSetting, byte[] baAccessBytes, int iRecordSize, int iNumOfRecords) {
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        Log.d("MainActivity", "bFileType " + ByteArray.byteArrayToHexString(new byte[]{bFileType}));
        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[]{bFileId}));
        Log.d("MainActivity", "ISOName " + ByteArray.byteArrayToHexString(baISOName));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iRecordSize = " + iRecordSize);
        Log.d("MainActivity", "iNumOfRecords = " + iNumOfRecords);
        try {
            MifareDesfire.statusType retValue = desfireCard.createRecordFile(bFileType, bFileId, baISOName, bCommSetting, baAccessBytes, iRecordSize, iNumOfRecords);
            if (retValue != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Create Record File Failed: " + desfireCard.DesFireErrorMsg(retValue));
            else
                scrollLog.appendTitle("Create Record File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    public void onCreateFileValueReturn (byte bFileId,byte bCommSetting, byte[] baAccessBytes, int iLowerLimit, int iUpperLimit, int iValue, byte bOptionByte) {
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        Log.d("MainActivity", "bFileID " + ByteArray.byteArrayToHexString(new byte[]{bFileId}));
        Log.d("MainActivity", "bCommSetting " + ByteArray.byteArrayToHexString(new byte[]{bCommSetting}));
        Log.d("MainActivity", "AccessRights " + ByteArray.byteArrayToHexString(baAccessBytes));
        Log.d("MainActivity", "iLowerLimit = " + iLowerLimit);
        Log.d("MainActivity", "iUpperLimit = " + iUpperLimit);
        Log.d("MainActivity", "iValue = " + iValue);
        Log.d("MainActivity", "bOptionByte " + ByteArray.byteArrayToHexString(new byte[]{bOptionByte}));

        try {
            MifareDesfire.statusType retValue = desfireCard.createValueFile(bFileId, bCommSetting, baAccessBytes, iLowerLimit, iUpperLimit, iValue, bOptionByte);
            if (retValue != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Create Value File Failed: " + desfireCard.DesFireErrorMsg(retValue));
            else
                scrollLog.appendTitle("Create Value File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion

    //region Get File IDs
    public void onGetFileIDs() {
        try {
            scrollLog.appendTitle("Get FileIDs");
            MifareDesfire.DesfireResponse res = desfireCard.getFileIDs();
            if (res.status != MifareDesfire.statusType.SUCCESS)
                scrollLog.appendError("Get FileIDs Failed: " + desfireCard.DesFireErrorMsg(res.status));

            if (res.data != null) {
                if (res.data.length > 0) {
                    scrollLog.appendTitle("FileIDs :" + ByteArray.byteArrayToHexString(res.data));
                    baFileIDList = res.data;
                }
            } else {
                scrollLog.appendTitle("No file in directory.");
                baFileIDList = null;
            }
            bFileIDListPopulated = true;
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    public Bundle onFragmentGetFileIDs (){

        onGetFileIDs();

        Bundle FileListInfo = new Bundle();
        FileListInfo.putByteArray("baFileIDList",baFileIDList);
        FileListInfo.putBoolean("bFileIDListPopulated", bFileIDListPopulated);

        return FileListInfo;

    }

    public void onGetISOFileIDs(){
        try {

            ByteArray baISOFileIDs = new ByteArray();

            scrollLog.appendTitle("Get ISO File IDs");
            MifareDesfire.DesfireResponse res = desfireCard.getISOFileIDs();
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                baISOFileIDs.append(res.data);

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData();
                    baISOFileIDs.append(res.data);
                }
            }
            if (baISOFileIDs.toArray().length > 0)
                scrollLog.appendTitle("ISO FileIDs :" + ByteArray.byteArrayToHexString(baISOFileIDs.toArray()));
            else
                scrollLog.appendTitle("No ISO FileIDs");

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }
    //endregion



    //region Get File Settings
    public void onGetFileSettings() {
        scrollLog.appendTitle("Get File Settings");

        getSupportActionBar().setTitle("Get File Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getFileSettingsFragment = new fGetFileSettings();

        getFileSettingsFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getFileSettingsFragment).addToBackStack("commandview").commit();
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
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            MifareDesfire.DesfireResponse res = desfireCard.getFileSettings(bFileID);
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get File Settings Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }

            scrollLog.appendTitle("File Settings of file 0x" + ByteArray.byteToHexString(bFileID) + ": " + ByteArray.byteArrayToHexString(res.data));
            // File Type
            switch (res.data[0]) {
                case (byte) 0x00:
                    scrollLog.appendData("- Standard data file");
                    break;
                case (byte) 0x01:
                    scrollLog.appendData("- Backup data file");
                    break;
                case (byte) 0x02:
                    scrollLog.appendData("- Value file with backup");
                    break;
                case (byte) 0x03:
                    scrollLog.appendData("- Linear record file with backup");
                    break;
                case (byte) 0x04:
                    scrollLog.appendData("- Cyclic record file with backup");
                    break;
                default:
                    scrollLog.appendData("- Unknown File type: 0x" + ByteArray.byteToHexString(res.data[0]));
                    break;
            }
            // Comm Setting
            switch (res.data[1]) {
                case (byte) 0x00:
                    scrollLog.appendData("- Plain communication");
                    break;
                case (byte) 0x01:
                    scrollLog.appendData("- Plain communication secured with MAC");
                    break;
                case (byte) 0x03:
                    scrollLog.appendData("- Fully enciphered communication");
                    break;
                default:
                    scrollLog.appendData("- Unknown communication setting: 0x" + ByteArray.byteToHexString(res.data[1]));
                    break;
            }

            // Separate parsing of the three types of files
            // Standard or backup file type
            if ((res.data[0] == (byte) 0x00) || (res.data[0] == (byte) 0x01)) {
                parseAccessRight("Read Access         ", (byte)((res.data[2] >> 4) & (byte) 0x0F));
                parseAccessRight("Write Access        ", (byte)(res.data[2] & (byte) 0x0F));
                parseAccessRight("Read & Write Access ", (byte)((res.data[3] >> 4) & (byte) 0x0F));
                parseAccessRight("Change Access Rights", (byte)(res.data[3] & (byte) 0x0F));
                scrollLog.appendData("- File size           : " + ByteBuffer.wrap(res.data,4,3).order(ByteOrder.LITTLE_ENDIAN).getShort() + " B");

            } else if ((res.data[0] == (byte) 0x02) ) {
                parseAccessRight("GetValue/Debit Access          ", (byte)((res.data[2] >> 4) & (byte) 0x0F));
                parseAccessRight("GetValue/Debit/LtdCredit Access", (byte)(res.data[2] & (byte) 0x0F));
                parseAccessRight("GetValue/Debit/LtdCredit/Credit", (byte)((res.data[3] >> 4) & (byte) 0x0F));
                parseAccessRight("Change Access Rights           ", (byte)(res.data[3] & (byte) 0x0F));
                scrollLog.appendData("- Lower Limit              : " + ByteBuffer.wrap(res.data,4,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                scrollLog.appendData("- Upper Limit              : " + ByteBuffer.wrap(res.data,8,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                scrollLog.appendData("- Limited Credit Value     : " + ByteBuffer.wrap(res.data,12,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                if ((res.data[16] & 0x01) == 1) {
                    scrollLog.appendData("- Limited Credit enabled    ");
                } else {
                    scrollLog.appendData("- Limited Credit disabled   ");
                }
                if ((res.data[16] & 0x02) == 2) {
                    scrollLog.appendData("- Free GetValue    ");
                } else {
                    scrollLog.appendData("- Free GetValue disabled   ");
                }

            } else if ((res.data[0] == (byte) 0x03) || (res.data[0] == (byte) 0x04)) {
                parseAccessRight("Read Access          ", (byte)((res.data[2] >> 4) & (byte) 0x0F));
                parseAccessRight("Write Access         ", (byte)(res.data[2] & (byte) 0x0F));
                parseAccessRight("Read & Write Access  ", (byte)((res.data[3] >> 4) & (byte) 0x0F));
                parseAccessRight("Change Access Rights ", (byte)(res.data[3] & (byte) 0x0F));

                scrollLog.appendData("- Record size          : " + ByteBuffer.wrap(res.data, 4,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                scrollLog.appendData("- Max no of records    : " + ByteBuffer.wrap(res.data, 7,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                scrollLog.appendData("- Current no of records: " + ByteBuffer.wrap(res.data,10,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
            }



        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }

    public Bundle onFragmentGetFileSettings (byte bFileID){
        Log.d("onFragmentGetFileSet", "on FragementGetFileSettings Start");

        Bundle bundleFileSettings = new Bundle();
        try {

            MifareDesfire.DesfireResponse res = desfireCard.getFileSettings(bFileID);
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get File Settings Failed: " + desfireCard.DesFireErrorMsg(res.status));
                bundleFileSettings.putBoolean("boolCommandSuccess", false);
                return bundleFileSettings;
            }


            bundleFileSettings.putBoolean("boolCommandSuccess", true);

            bundleFileSettings.putInt("currentAuthenticatedKey", desfireCard.currentAuthenticatedKey());
            bundleFileSettings.putByte("fileType",res.data[0]);
            bundleFileSettings.putByte("commSetting",res.data[1]);


            // Separate parsing of the three types of files
            // Standard or backup file type
            if ((res.data[0] == (byte) 0x00) || (res.data[0] == (byte) 0x01)) {

                bundleFileSettings.putByte("readAccess",(byte)((res.data[2] >>> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("writeAccess", (byte)(res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("readWriteAccess", (byte)((res.data[3] >>> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte)(res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("fileSize", ByteBuffer.wrap(res.data,4,3).order(ByteOrder.LITTLE_ENDIAN).getInt());

            } else if ((res.data[0] == (byte) 0x02) ) {
                bundleFileSettings.putByte("GVD", (byte)((res.data[2] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("GVDLC", (byte)(res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("GVDLCC", (byte)((res.data[3] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte)(res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("lowerLimit",         ByteBuffer.wrap(res.data,4,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                bundleFileSettings.putInt("upperLimit",         ByteBuffer.wrap(res.data,8,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                bundleFileSettings.putInt("limitedCreditValue", ByteBuffer.wrap(res.data,12,4).order(ByteOrder.LITTLE_ENDIAN).getInt());

                bundleFileSettings.putByte("LC_FreeGV_Flag",(res.data[16]));


            } else if ((res.data[0] == (byte) 0x03) || (res.data[0] == (byte) 0x04)) {

                bundleFileSettings.putByte("readAccess",(byte)((res.data[2] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("writeAccess", (byte)(res.data[2] & (byte) 0x0F));
                bundleFileSettings.putByte("readWriteAccess ", (byte)((res.data[3] >> 4) & (byte) 0x0F));
                bundleFileSettings.putByte("changeAccessRights", (byte)(res.data[3] & (byte) 0x0F));

                bundleFileSettings.putInt("recordSize",         ByteBuffer.wrap(res.data,4,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                bundleFileSettings.putInt("MaxNumOfRecords",    ByteBuffer.wrap(res.data,7,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
                bundleFileSettings.putInt("currentNumOfRecords",ByteBuffer.wrap(res.data,10,3).order(ByteOrder.LITTLE_ENDIAN).getShort());
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

        getSupportActionBar().setTitle("Delete File");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getDeleteFileFragment = new fDeleteFile();

        getDeleteFileFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getDeleteFileFragment).addToBackStack("commandview").commit();
    }


    public void onDeleteFileReturn(byte bFileID) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            MifareDesfire.DesfireResponse res = desfireCard.deleteFile(bFileID);
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Delete File Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }

            scrollLog.appendTitle("Delete File " + ByteArray.byteArrayToHexString(new byte [] {bFileID}) + " Ok ");

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

        getSupportActionBar().setTitle("Read Data");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getReadDataFragment = new fReadData();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getReadDataFragment).addToBackStack("commandview").commit();
    }



    public void onReadDataReturn(byte bFileID, int iOffset, int iLength, MifareDesfire.commMode selCommMode) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            ByteArray baRecvData = new ByteArray();

            MifareDesfire.DesfireResponse res = desfireCard.readData(bFileID, iOffset, iLength, selCommMode);
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(selCommMode);
                }
                baRecvData.append(res.data);
            }

            //
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Data:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read File Failed: " + desfireCard.DesFireErrorMsg(res.status));
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

    //region Read Records
    public void onReadRecords() {
        scrollLog.appendTitle("Read Records");

        getSupportActionBar().setTitle("Read Records");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getReadRecordsFragment = new fReadRecords();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getReadRecordsFragment).addToBackStack("commandview").commit();
    }



    public void onReadRecordsReturn(byte bFileID, int iOffsetRecord, int iNumOfRecords, MifareDesfire.commMode selCommMode) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            ByteArray baRecvData = new ByteArray();

            MifareDesfire.DesfireResponse res = desfireCard.readRecords(bFileID, iOffsetRecord, iNumOfRecords, selCommMode);
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(selCommMode);
                }
                baRecvData.append(res.data);
            }

            // Output
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Record:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read Record File Failed: " + desfireCard.DesFireErrorMsg(res.status));
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

        getSupportActionBar().setTitle("Write Data");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getWriteDataFragment = new fWriteData();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getWriteDataFragment).addToBackStack("commandview").commit();
    }



    public void onWriteDataReturn(byte bFileID, int iOffset, int iLength, byte [] bDataToWrite, MifareDesfire.commMode selCommMode) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {

            // TODO: separate data blocks if too long
            MifareDesfire.DesfireResponse res = desfireCard.writeData(bFileID, iOffset, iLength, bDataToWrite, selCommMode);
            if ((res.status == MifareDesfire.statusType.SUCCESS) ) {
                scrollLog.appendStatus("Write Data File Success");
            } else {
                scrollLog.appendError("WriteFile Failed: " + desfireCard.DesFireErrorMsg(res.status));
                Log.d("onWriteDataReturn", "writeData return: " + desfireCard.DesFireErrorMsg(res.status));
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

        getSupportActionBar().setTitle("Write Record");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getWriteRecordFragment = new fWriteRecord();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getWriteRecordFragment).addToBackStack("commandview").commit();
    }



    public void onWriteRecordReturn(byte bFileID, int iRecordNum, int iSizeToWrite, byte [] bDataToWrite, MifareDesfire.commMode selCommMode) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {

            // TODO: separate data blocks if too long
            MifareDesfire.DesfireResponse res = desfireCard.writeRecord(bFileID, iRecordNum, iSizeToWrite, bDataToWrite, selCommMode);
            if ((res.status == MifareDesfire.statusType.SUCCESS) ) {
                scrollLog.appendStatus("Write Record File Success");
            } else {
                scrollLog.appendError("WriteFile Failed: " + desfireCard.DesFireErrorMsg(res.status));
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

        getSupportActionBar().setTitle("Clear Record File");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getClearRecordFileFragment = new fClearRecordFile();

        getClearRecordFileFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getClearRecordFileFragment).addToBackStack("commandview").commit();
    }

    //
    public void onClearRecordFileReturn(byte bFileID) {
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            MifareDesfire.DesfireResponse res = desfireCard.clearRecordFile(bFileID);
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Clear Record Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
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

        getSupportActionBar().setTitle("Get Value");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        getValueFragment = new fGetValue();

        getValueFragment.setArguments(bundle);
        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,getValueFragment).addToBackStack("commandview").commit();
    }

    //
    public void onGetValueReturn(byte bFileID, MifareDesfire.commMode curCommMode) {
        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {
            MifareDesfire.DesfireResponse res = desfireCard.getValue(bFileID, curCommMode);
            if (res.status != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Get File Settings Failed: " + desfireCard.DesFireErrorMsg(res.status));
                return;
            }

            if (res.data.length > 0){
                Log.d("onGetValueReturn", "Value Byte: " + ByteArray.byteArrayToHexString(res.data));
                scrollLog.appendData("Value:" + ByteBuffer.wrap(res.data).order(ByteOrder.LITTLE_ENDIAN).getInt());
            } else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Get Value Failed: " + desfireCard.DesFireErrorMsg(res.status));
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

    //region Credit
    public void onCredit() {
        scrollLog.appendTitle("Credit");

        getSupportActionBar().setTitle("Credit");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = new Bundle();
        bundle.putByteArray("baFileIDList", baFileIDList);

        creditFragment = new fCredit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,creditFragment).addToBackStack("commandview").commit();
    }



    public void onCreditReturn(byte bFileID, int iCreditValue, MifareDesfire.commMode selCommMode) {

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();

        try {

            // TODO: separate data blocks if too long
            MifareDesfire.DesfireResponse res = desfireCard.credit(bFileID, iCreditValue, selCommMode);
            if ((res.status == MifareDesfire.statusType.SUCCESS) ) {
                scrollLog.appendStatus("Credit Success");
            } else {
                scrollLog.appendError("Credit Failed: " + desfireCard.DesFireErrorMsg(res.status));
                Log.d("onCreditReturn", "Error returned: " + desfireCard.DesFireErrorMsg(res.status));
            }

        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onActivityResult", e.getMessage(), e);
        }
    }
    //endregion



    //region Read Data Test

    public void onReadDataTest(byte fileID) {
        try {

            int offset = 0;
            int length = 3;
            ByteArray baRecvData = new ByteArray();

            scrollLog.appendTitle("Read Data Test");
            MifareDesfire.DesfireResponse res = desfireCard.readData(fileID, offset, length, PLAIN);
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                baRecvData.append(res.data);

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData();
                    baRecvData.append(res.data);
                }
            }
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Data Test:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read File Failed: " + desfireCard.DesFireErrorMsg(res.status));
                    return;
                }

                scrollLog.appendData("No data returned");
            }


        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }

    public void onReadDataEncryptedTest(byte fileID, int bytesToRead) {
        try {

            int offset = 0;
            ByteArray baRecvData = new ByteArray();

            scrollLog.appendTitle("Read Data Encrypted Test");
            MifareDesfire.DesfireResponse res = desfireCard.readData(fileID, offset, bytesToRead, MifareDesfire.commMode.ENCIPHERED);
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(MifareDesfire.commMode.ENCIPHERED);
                }
                baRecvData.append(res.data);
            }
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Data Test:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read File Failed: " + desfireCard.DesFireErrorMsg(res.status));
                    return;
                }

                scrollLog.appendData("No data returned");
            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }

    public void onReadDataMACTest(byte fileID) {
        try {

            int offset = 0;
            int length = 8;
            ByteArray baRecvData = new ByteArray();

            scrollLog.appendTitle("Read Data MAC Test");
            MifareDesfire.DesfireResponse res = desfireCard.readData(fileID, offset, length, MifareDesfire.commMode.MAC);
            if ((res.status == MifareDesfire.statusType.SUCCESS) || (res.status == MifareDesfire.statusType.ADDITONAL_FRAME)) {

                while (res.status == MifareDesfire.statusType.ADDITONAL_FRAME) {
                    res = desfireCard.getMoreData(MifareDesfire.commMode.ENCIPHERED);
                }
                baRecvData.append(res.data);
            }
            if (baRecvData.toArray().length > 0)
                scrollLog.appendData("Read Data MAC Test:" + ByteArray.byteArrayToHexString(baRecvData.toArray()));
            else {
                if (res.status != MifareDesfire.statusType.SUCCESS) {
                    scrollLog.appendError("Read File Failed: " + desfireCard.DesFireErrorMsg(res.status));
                    return;
                }
                scrollLog.appendData("No data returned");
            }

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onReadDataMACTest", e.getMessage(), e);
        }
    }
    //endregion



    //region Write Data Test

    //region Create Test Perso
    public void onCreateTestPerso() {
        scrollLog.appendTitle("Create Test Personalization File System");

        try {
            scrollLog.appendTitle("Authentication");
            byte [] key = new byte[8];
            Arrays.fill(key,(byte)0);
            byte [] T3DESKey = new byte [24];
            Arrays.fill(T3DESKey,(byte)0);
            byte [] AESKey = new byte [16];
            Arrays.fill(AESKey,(byte)0);

            MifareDesfire.statusType res = desfireCard.authenticate((byte) 0x0A, (byte)0, key);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            //region  Create Application D40 DES (D4 0D E5)
            byte[] baAppId = new byte[] {(byte) 0xD4,(byte) 0x0D,(byte) 0xE5 };
            byte [] baNull = new byte[] {};
            MifareDesfire.statusType mfResult = desfireCard.createApplication(baAppId, (byte)0x0F, (byte)0x03, baNull, baNull);
            if (mfResult != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + desfireCard.DesFireErrorMsg(mfResult));
                return;
            } else {
                scrollLog.appendTitle("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            MifareDesfire.statusType retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0x0A, (byte)0, key);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            createTestPersoFiles ();
            //endregion

            //region  Create Application ISO DES (15 0D E5)
            // Select Application
            baAppId = new byte[] {(byte) 0x00,(byte) 0x00,(byte) 0x00 };
            retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate main key
            res = desfireCard.authenticate((byte) 0x0A, (byte)0, key);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            baAppId = new byte[] {(byte) 0x15,(byte) 0x0D,(byte) 0xE5 };
            mfResult = desfireCard.createApplication(baAppId, (byte)0x0F, (byte)0x43, baNull, baNull);
            if (mfResult != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + desfireCard.DesFireErrorMsg(mfResult));
                return;
            } else {
                scrollLog.appendTitle("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0x1A, (byte)0, T3DESKey);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }
            createTestPersoFiles ();
            //endregion

            //region  Create Application ISO AES (15 0A E5)
            // Select Application
            baAppId = new byte[] {(byte) 0x00,(byte) 0x00,(byte) 0x00 };
            retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate main key
            res = desfireCard.authenticate((byte) 0x0A, (byte)0, key);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }

            baAppId = new byte[] {(byte) 0x15,(byte) 0x0A,(byte) 0xE5 };
            mfResult = desfireCard.createApplication(baAppId, (byte)0x0F, (byte)0x83, baNull, baNull);
            if (mfResult != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Application Failed: " + desfireCard.DesFireErrorMsg(mfResult));
                return;
            } else {
                scrollLog.appendTitle("Create Application OK: " + ByteArray.byteArrayToHexString(baAppId));
            }

            // Select Application
            retValue = desfireCard.selectApplication(baAppId);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            } else
                scrollLog.appendTitle("Select OK: " + ByteArray.byteArrayToHexString(baAppId));

            // Authenticate
            res = desfireCard.authenticate((byte) 0xAA, (byte)0, AESKey);
            if (res != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Authentication Error: " + desfireCard.DesFireErrorMsg(res));
                return;
            } else {
                scrollLog.appendStatus("Authentication Successful");
            }
            createTestPersoFiles ();
            //endregion
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }
    //endregion


    public void createTestPersoFiles () {
        try {
            byte[] baNull = new byte[]{};

            MifareDesfire.statusType retValue;
            // Create Data File
            retValue = desfireCard.createStdDataFile((byte) 0x01, baNull, MifareDesfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 32);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createStdDataFile((byte) 0x02, baNull, MifareDesfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 32);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }

            retValue = desfireCard.createStdDataFile((byte) 0x03, baNull, MifareDesfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 32);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Data File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x04, baNull, MifareDesfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 8, 3);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x05, baNull, MifareDesfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 8, 3);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createLinearRecordFile((byte) 0x06, baNull, MifareDesfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 8, 3);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Record File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x07, MifareDesfire.commMode.getSetting(PLAIN), new byte[]{(byte) 0xEE, (byte) 0xEE}, 0, 1000, 0, (byte) 0x00);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x08, MifareDesfire.commMode.getSetting(MAC), new byte[]{(byte) 0x00, (byte) 0x00}, 0, 1000, 0, (byte) 0x00);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            retValue = desfireCard.createValueFile((byte) 0x09, MifareDesfire.commMode.getSetting(ENCIPHERED), new byte[]{(byte) 0x00, (byte) 0x00}, 0, 1000, 0, (byte) 0x00);
            if (retValue != MifareDesfire.statusType.SUCCESS) {
                scrollLog.appendError("Create Value File Failed: " + desfireCard.DesFireErrorMsg(retValue));
                return;
            }
            scrollLog.appendTitle("Create Data File OK");
        } catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("createTestPersoFiles", e.getMessage(), e);
        }
    }



    public void onTestAll() {

        // Select preset app D40
        Log.d("TestAll", "*** Test D40 ****************************");
        scrollLog.appendTitle("***** TEST D40 ");
        onSelectApplicationReturn(new byte[] { (byte) 0xD4, (byte) 0x0D, (byte) 0xE5});


        scrollLog.appendTitle("***** TEST Plain Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onWriteDataReturn((byte) 0x01, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, PLAIN);
        Log.d("TestAll", "*** Read Plain Data **************************");
        onReadDataReturn((byte) 0x01,0,0,PLAIN);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Plain Record");
        onClearRecordFileReturn((byte) 0x04);
        onCommitTransaction();
        Log.d("TestAll", "*** Write Plain Record **************************");
        onWriteRecordReturn((byte) 0x04, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, PLAIN);
        onCommitTransaction();
        Log.d("TestAll", "*** Read Plain Record **************************");
        onReadRecordsReturn((byte) 0x04, 0, 0, PLAIN);

        onAuthenticateTest ();

        scrollLog.appendTitle("***** TEST MAC Data");
        Log.d("TestAll", "*** Write MAC Data **************************");
        onWriteDataReturn((byte) 0x02, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, MifareDesfire.commMode.MAC);
        Log.d("TestAll", "*** Read MAC Data **************************");
        onReadDataMACTest((byte) 0x02);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Encrypted Data");
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 10);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0
        Log.d("TestAll", "*** Write Encrypted Data **************************");
        onWriteDataReturn((byte) 0x03, 0, 4, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x00}, MifareDesfire.commMode.ENCIPHERED);
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 4);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0


        scrollLog.appendTitle("***** TEST MAC Record");
        onClearRecordFileReturn((byte) 0x05);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x05, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, MAC);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x05, 0, 0, MAC);


        scrollLog.appendTitle("***** TEST Encrypted Record");
        onClearRecordFileReturn((byte) 0x06);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x06, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, ENCIPHERED);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x06, 0, 0, ENCIPHERED);

        scrollLog.appendTitle("***** TEST Get Value - Plain");
        onGetValueReturn((byte) 0x07, PLAIN);
        scrollLog.appendTitle("***** TEST Get Value - MAC");
        onGetValueReturn((byte) 0x08, MAC);
        scrollLog.appendTitle("***** TEST Get Value - Enciphered");
        onGetValueReturn((byte) 0x09, ENCIPHERED);



        // Test ISO DES
        Log.d("TestAll", "*** Test ISO DES ****************************");
        scrollLog.appendTitle("***** TEST ISO AES ");
        onSelectApplicationReturn(new byte[] { (byte) 0x15, (byte) 0x0D, (byte) 0xE5});

        scrollLog.appendTitle("***** TEST Plain Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onWriteDataReturn((byte) 0x01, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, PLAIN);
        Log.d("TestAll", "*** Read Plain Data **************************");
        onReadDataReturn((byte) 0x01,0,0,PLAIN);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Plain Record");
        onClearRecordFileReturn((byte) 0x04);
        onCommitTransaction();
        Log.d("TestAll", "*** Write Plain Record **************************");
        onWriteRecordReturn((byte) 0x04, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, PLAIN);
        onCommitTransaction();
        Log.d("TestAll", "*** Read Plain Record **************************");
        onReadRecordsReturn((byte) 0x04, 0, 0, PLAIN);

        onAuthISOTest ();

        scrollLog.appendTitle("***** TEST MAC Data");
        Log.d("TestAll", "*** Write MAC Data **************************");
        onWriteDataReturn((byte) 0x02, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, MifareDesfire.commMode.MAC);
        Log.d("TestAll", "*** Read MAC Data **************************");
        onReadDataMACTest((byte) 0x02);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Encrypted Data");
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 10);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0
        Log.d("TestAll", "*** Write Encrypted Data **************************");
        onWriteDataReturn((byte) 0x03, 0, 4, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x00}, MifareDesfire.commMode.ENCIPHERED);
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 4);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST MAC Record");
        onClearRecordFileReturn((byte) 0x05);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x05, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, MAC);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x05, 0, 0, MAC);

        scrollLog.appendTitle("***** TEST Encrypted Record");
        onClearRecordFileReturn((byte) 0x06);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x06, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, ENCIPHERED);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x06, 0, 0, ENCIPHERED);

        scrollLog.appendTitle("***** TEST Get Value - Plain");
        onGetValueReturn((byte) 0x07, PLAIN);
        scrollLog.appendTitle("***** TEST Get Value - MAC");
        onGetValueReturn((byte) 0x08, MAC);
        scrollLog.appendTitle("***** TEST Get Value - Enciphered");
        onGetValueReturn((byte) 0x09, ENCIPHERED);




        Log.d("TestAll", "*** Test AES ****************************");
        scrollLog.appendTitle("***** TEST ISO AES ");
        onSelectApplicationReturn(new byte[] { (byte) 0x15, (byte) 0x0A, (byte) 0xE5});

        scrollLog.appendTitle("***** TEST Plain Data");
        Log.d("TestAll", "*** Write Plain Data **************************");
        onWriteDataReturn((byte) 0x01, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, PLAIN);
        Log.d("TestAll", "*** Read Plain Data **************************");
        onReadDataReturn((byte) 0x01,0,0,PLAIN);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Plain Record");
        onClearRecordFileReturn((byte) 0x04);
        onCommitTransaction();
        Log.d("TestAll", "*** Write Plain Record **************************");
        onWriteRecordReturn((byte) 0x04, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, PLAIN);
        onCommitTransaction();
        Log.d("TestAll", "*** Read Plain Record **************************");
        onReadRecordsReturn((byte) 0x04, 0, 0, PLAIN);

        onAuthAESTest ();

        scrollLog.appendTitle("***** TEST MAC Data");
        Log.d("TestAll", "*** Write MAC Data **************************");
        onWriteDataReturn((byte) 0x02, 0, 3, new byte [] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc}, MifareDesfire.commMode.MAC);
        Log.d("TestAll", "*** Read MAC Data **************************");
        onReadDataMACTest((byte) 0x02);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST Encrypted Data");
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 10);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0
        Log.d("TestAll", "*** Write Encrypted Data **************************");
        onWriteDataReturn((byte) 0x03, 0, 4, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x00}, MifareDesfire.commMode.ENCIPHERED);
        Log.d("TestAll", "*** Read Encrypted Data **************************");
        onReadDataEncryptedTest((byte) 0x03, 4);  // Enc   Key 2 / 0 (Should be encrypted after auth key 0

        scrollLog.appendTitle("***** TEST MAC Record");
        onClearRecordFileReturn((byte) 0x05);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x05, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, MAC);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x05, 0, 0, MAC);

        scrollLog.appendTitle("***** TEST Encrypted Record");
        onClearRecordFileReturn((byte) 0x06);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onWriteRecordReturn((byte) 0x06, 0, 3, new byte [] {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC}, ENCIPHERED);
        onCommitTransaction();
        Log.d("TestAll", "*** Write MAC Record **************************");
        onReadRecordsReturn((byte) 0x06, 0, 0, ENCIPHERED);

        scrollLog.appendTitle("***** TEST Get Value - Plain");
        onGetValueReturn((byte) 0x07, PLAIN);
        scrollLog.appendTitle("***** TEST Get Value - MAC");
        onGetValueReturn((byte) 0x08, MAC);
        scrollLog.appendTitle("***** TEST Get Value - Enciphered");
        onGetValueReturn((byte) 0x09, ENCIPHERED);



                /*
*/
    }




}
