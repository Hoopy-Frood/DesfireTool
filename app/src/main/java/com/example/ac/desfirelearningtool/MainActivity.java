package com.example.ac.desfirelearningtool;

import android.app.PendingIntent;
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
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
    implements IMainActivityCallbacks{
    private AdView mAdView;

    fCommandMenu commandFragment;
    fSelectApplication selectFragment;



    protected PendingIntent pendingIntent;
    protected IntentFilter[] intentFiltersArray;
    protected String[][] techListsArray;
    protected NfcAdapter nfcAdapter;

    private AndroidCommunicator communicator;
    private MifareDesfire desfireCard;
    private ScrollLog scrollLog;

    private byte[] applicationList;
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

        scrollLog = new ScrollLog((TextView) findViewById(R.id.textView_scrollLog));

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
                    .add(R.id.fragment_container, commandFragment).commit();

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

            Object nfcTag = Tag_getTagService(currentTag);
            int nativeHandle = Tag_getServiceHandle(currentTag);

            new CardWatchdogRearm(nfcTag, nativeHandle).start();

            onCardDetection(currentTag);
        }
    }


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
            for (int i = 0; i < 20; ++i) {  //250 orig
                try {
                    int result = INfcTag_connect(nfcTag, nativeHandle, 0);
                    boolean present = INfcTag_isPresent(nfcTag, nativeHandle);
                    if (!present) {
                        Log.d("CardWatchdogRearm", "INfcTag_connect: " + result);
                        break;
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
            communicator = new AndroidCommunicator(IsoDep.get(tag), true,scrollLog);

            desfireCard = communicator.get(tag); // we do not specify a key here!
            if (desfireCard == null) {
                Log.d("onCardDetection", "Not a DESFire card");
                new Exception().printStackTrace();
            }

            desfireCard.connect();
            Log.d("onCardDetection:"," CONNECTED");
            //scrollLog.setTextColor(0xAAFF0000);
            scrollLog.appendTitle("DESFire Connected\n");

            commandFragment.enableAllButtons();


        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            Log.e("onCardDetection", e.getMessage(), e);
        }
    }

    public void onGetVersion (){

        try {
            scrollLog.appendTitle("Get Version");
            scrollLog.appendTitle("Hardware Info: " + ByteArray.byteArrayToHexString(desfireCard.getVersion().data));
            scrollLog.appendTitle("Software Info: " + ByteArray.byteArrayToHexString(desfireCard.getMoreData().data));
            scrollLog.appendTitle("UID Info: " + ByteArray.byteArrayToHexString(desfireCard.getMoreData().data));
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }


    public void onGetCardUID (){

        try {
            scrollLog.appendTitle("Get Card UID");
            desfireCard.getCardUID();

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }

    public void onGetApplicationIDs (){

        try {
            scrollLog.appendTitle("Get Application ID");
            applicationList = desfireCard.getApplicationIDs();

            scrollLog.appendTitle ("getAppIDs : " + ByteArray.byteArrayToHexString(applicationList));

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }


    public void onGetFreeMem (){

        try {
            scrollLog.appendTitle("Get Get Free Memory");
            MifareDesfire.MifareResult res = desfireCard.getFreeMem();

            ByteBuffer bb = ByteBuffer.wrap(res.data);
            bb.order( ByteOrder.LITTLE_ENDIAN);

            scrollLog.appendTitle("Free Memory: " + bb.getShort());
        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }

    public void onGetKeySettings (){

        try {
            scrollLog.appendTitle("Get Get Key Settings");
            MifareDesfire.MifareResult res = desfireCard.getKeySettings();

            if (res.resultType != MifareDesfire.MifareResultType.SUCCESS)
                scrollLog.appendError("Select Failed: " + desfireCard.DesFireErrorMsg(res.resultType));
            else
                scrollLog.appendTitle("Key Settings: " + ByteArray.byteArrayToHexString(res.data));

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onGetVersion", e.getMessage(), e);
        }
    }



    public void onGetDFNames (){

        try {
            scrollLog.appendTitle("Get DF Names");
            MifareDesfire.MifareResult res = desfireCard.getDFNames();
            if ((res.resultType == MifareDesfire.MifareResultType.SUCCESS) || (res.resultType == MifareDesfire.MifareResultType.ADDITONAL_FRAME))
                scrollLog.appendTitle("Application 1: " + ByteArray.byteArrayToHexString(res.data));
            int i = 2;
            while (res.resultType == MifareDesfire.MifareResultType.ADDITONAL_FRAME) {
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
    public void onAuthenticate (){
        byte[] zeroKey = new byte[8];
        Arrays.fill(zeroKey, (byte)0);

        try {
            scrollLog.appendTitle("Authentication");
            boolean authenticated = desfireCard.authenticate((byte)0, zeroKey);
            if (authenticated)
                scrollLog.appendTitle("Authentication Successful");
            else
                scrollLog.appendError("Authentication Failed");

        }
        catch (Exception e) {
            commandFragment.disableAllButtons();
            scrollLog.appendError("DESFire Disconnected\n");
            Log.e("onAuthenticate", e.getMessage(), e);
        }
    }

    public void onSelectApplication (){
        scrollLog.appendTitle("Select Application");

        Bundle bundle = new Bundle();
        bundle.putByteArray("applicationList", applicationList);

        getSupportActionBar().setTitle("Select Application");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        selectFragment = new fSelectApplication();


        selectFragment.setArguments(bundle);

        //getSupportFragmentManager().addToBackStack(null);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, selectFragment).addToBackStack("commandview").commit();

    }


    public void onSelectReturn(byte [] baAppId){
        scrollLog.appendTitle("Select Application Return");

        getSupportActionBar().setTitle("DESFire Tool");
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportFragmentManager().popBackStack();



    }

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

}
