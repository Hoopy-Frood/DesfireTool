package com.example.ac.desfirelearningtool;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

/**
 * Created by Ac on 8/6/2017.
 */

public class ScrollLog {
    private TextView scrollLog;

    public ScrollLog(TextView scrollID){
        scrollLog = scrollID;
        scrollLog.setMovementMethod(new ScrollingMovementMethod());
        scrollLog.setText("");
        scrollLog.setTextColor(0xAA000000);

    }

    public CharSequence getText(){
        return scrollLog.getText();
    }

    public void append(String appendText){
        scrollLog.append(appendText + "\n");
    }

    public void clearScreen(){
        scrollLog.setText("");
    }

    public void appendTitle(String appendText){
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString spanText= new SpannableString(appendText);
        spanText.setSpan(new ForegroundColorSpan(Color.BLUE), 0, appendText.length(), 0);
        builder.append(spanText);

        scrollLog.append(builder);
        scrollLog.append("\n");

    }

    public void appendStatus(String appendText){
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString spanText= new SpannableString(appendText);
        spanText.setSpan(new ForegroundColorSpan((int) 0xFF008000), 0, appendText.length(), 0);  // Dark Green
        builder.append(spanText);

        scrollLog.append(builder);
        scrollLog.append("\n");

    }

    public void appendData(String appendText){
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString spanText= new SpannableString(appendText);
        spanText.setSpan(new ForegroundColorSpan((int) 0xFF000080), 0, appendText.length(), 0);
        builder.append(spanText);

        scrollLog.append(builder);
        scrollLog.append("\n");

    }

    public void appendError(String appendText){
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString spanText= new SpannableString(appendText);
        spanText.setSpan(new ForegroundColorSpan(Color.RED), 0, appendText.length(), 0);
        builder.append(spanText);

        scrollLog.append(builder);
        scrollLog.append("\n");

    }

}
