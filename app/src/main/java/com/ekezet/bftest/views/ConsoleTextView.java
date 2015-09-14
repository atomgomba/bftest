package com.ekezet.bftest.views;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by kiri on 2015.09.11..
 */
public class ConsoleTextView extends TextView
{
    private ArrayList<String> mLines;

    public ConsoleTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setTypeface(Typeface.MONOSPACE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        setSingleLine(false);
        setLineSpacing(0.0f, 1.25f);
        mLines = new ArrayList();
    }

    public synchronized void addLine(String s)
    {
        mLines.add(s);
        update();
    }

    public synchronized void replaceLine(String s, int lineNumber)
    {
        mLines.set(lineNumber, s);
        update();
    }

    public synchronized void replaceLine(String s)
    {
        replaceLine(s, mLines.size() - 1);
    }

    public synchronized void clearLines()
    {
        mLines.clear();
        update();
    }

    private synchronized void update()
    {
        String content = "";
        int lines = mLines.size();
        if (0 < lines)
        {
            for (int i = 0; i < lines; i++)
            {
                content += mLines.get(i) + "<br>";
            }
        }
        final String s = content;
        setText(Html.fromHtml(s));
    }
}
