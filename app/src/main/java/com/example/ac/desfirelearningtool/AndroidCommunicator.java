package com.example.ac.desfirelearningtool;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;



// Facade wrapper for android card communication logic
public class AndroidCommunicator implements ICardCommunicator {

    private final IsoDep isoDep;
    private boolean useIsoMode;
    private ScrollLog scrollLog;

    public AndroidCommunicator(IsoDep isoDep, boolean useIsoMode, ScrollLog tv_scrollLog) {
        this.isoDep = isoDep;
        this.useIsoMode = useIsoMode;
        this.scrollLog = tv_scrollLog;
    }

    // Factory method to correctly initialize a MifareDesfire card from a generic tag
    // Returns "null" upon failure (including cases where the tag is not a MifareDesfire
    // card)
    public MifareDesfire get(Tag tag) {
        try {
            String[] tagList = tag.getTechList();
            for (String tagTech : tagList) {
                if (tagTech.equals(IsoDep.class.getName())) {
                    return new MifareDesfire(this, tag.getId(),this.scrollLog);
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    public byte[] isoFrame(byte[] nativeCommand) {
        // 90, command(1), 00, 00, args.lenght(), args, 00

        byte command = nativeCommand[0];

        int length = nativeCommand.length + 4;

        // Add space for 0 termination. This should be required, but Android... does not want it!
        // go figure...
        if (nativeCommand.length > 1)
            ++length;

        byte[] isoFrame = new byte[length];
        // INF
        isoFrame[0] = (byte)0x90;
        isoFrame[1] = command;
        isoFrame[2] = 0;
        isoFrame[3] = 0;
        isoFrame[4] = (byte)(nativeCommand.length - 1); // args without command

        System.arraycopy(nativeCommand, 1, isoFrame, 5, nativeCommand.length - 1);

        // Set last to 0
        if (nativeCommand.length > 1)
            isoFrame[isoFrame.length - 1] = 0;

        return isoFrame;
    }

    public byte[] fromIsoAnswer(byte[] isoAnswer) {
        // data, 0x91, code
        // OR
        // error_code, 0x00

        if (isoAnswer.length == 1) {
            // Is this a native answer?
            // (typically: 0x1C - I don't understand your command)
            // switch back to native
            this.useIsoMode = false;
            return null;
        }

        int commandPosition = isoAnswer.length - 2;
        if (commandPosition < 0)
            return null;

        byte isoCode = isoAnswer[commandPosition];
        if (isoCode != (byte)0x91)
            return null;

        byte[] nativeFrame = new byte[isoAnswer.length - 1];
        // INF
        nativeFrame[0] = isoAnswer[commandPosition + 1];

        System.arraycopy(isoAnswer, 0, nativeFrame, 1, isoAnswer.length - 2);

        return nativeFrame;
    }

    public byte[] transceive(byte[] data) throws IOException {
        if (useIsoMode) {
            byte[] isoCommand = isoFrame(data);
            Log.v("Transceive CMD:", ByteArray.byteArrayToHexString(isoCommand));
            scrollLog.append("->" + ByteArray.byteArrayToHexString(isoCommand));
            byte[] isoAnswer = isoDep.transceive(isoCommand);
            Log.v("Transceive RSP:", ByteArray.byteArrayToHexString(isoAnswer));
            scrollLog.append("<-" + ByteArray.byteArrayToHexString(isoAnswer));
            return fromIsoAnswer(isoAnswer);
        } else {
            scrollLog.append("->" + ByteArray.byteArrayToHexString(data));
            byte [] answer = isoDep.transceive(data);
            scrollLog.append("<-" + ByteArray.byteArrayToHexString(answer));
            return answer;
        }
    }

    public void connect() throws IOException {
        isoDep.connect();
        isoDep.setTimeout(500);
    }

    public boolean isConnected() throws IOException {
        return isoDep.isConnected();
    }

    public void close() throws IOException {
        isoDep.close();
    }
}

