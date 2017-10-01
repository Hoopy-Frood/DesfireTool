package com.example.ac.desfirelearningtool;

import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by Ac on 8/6/2017.
 */

public class ScrollLog {
    private TextView scrollLog;
    private ScrollView scrollView;
    private static int MAX_OUTPUT_LINES = 2048;

    public ScrollLog(TextView scrollID, ScrollView inScrollView){
        scrollLog = scrollID;
        scrollLog.setMovementMethod(new ScrollingMovementMethod());
        scrollLog.setText("");
        scrollLog.setTextColor(0xAA000000);
        scrollView = inScrollView;

    }

    public CharSequence getText(){
        return scrollLog.getText();
    }

    public void append(String appendText){
        appendColor (appendText, (int) 0xAA000000);  // Default color dark grey
    }

    public void clearScreen(){
        scrollLog.setText("");
    }

    public void appendTitle(String appendText){
        appendColor (appendText, Color.BLUE);
    }

    public void appendStatus(String appendText){
        appendColor (appendText, (int) 0xFF008000);  // Dark green
    }

    public void appendData(String appendText){
        appendColor (appendText, (int) 0xFF000080);  // Dark blue
    }

    public void appendWarning(String appendText){
        appendColor (appendText, (int) 0xFF800000);  // Dark red
    }

    public void appendError(String appendText){
        appendColor (appendText, Color.RED);
    }

    public void appendColor(String appendText, int color){
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString spanText= new SpannableString(appendText);
        spanText.setSpan(new ForegroundColorSpan(color), 0, appendText.length(), 0);
        builder.append(spanText);

        scrollLog.append(builder);
        scrollLog.append("\n");

        removeLinesFromTextView();

        scrollView.post(new Runnable() {
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    // remove leading lines from beginning of the output view
    private void removeLinesFromTextView() {
        int linesToRemove = scrollLog.getLineCount() - MAX_OUTPUT_LINES;
        if (linesToRemove > 0) {
            for (int i = 0; i < linesToRemove; i++) {
                Editable text = scrollLog.getEditableText();
                int lineStart = scrollLog.getLayout().getLineStart(0);
                int lineEnd = scrollLog.getLayout().getLineEnd(0);
                text.delete(lineStart, lineEnd);
            }
        }
    }


}
