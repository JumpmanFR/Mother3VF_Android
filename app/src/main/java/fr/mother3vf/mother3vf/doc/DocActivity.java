package fr.mother3vf.mother3vf.doc;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import fr.mother3vf.mother3vf.R;
import fr.mother3vf.mother3vf.databinding.ActivityDocBinding;

/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Developed by JumpmanFR
 * Inspired from Paul Kratt’s MultiPatch app for macOS
 ******************************************************************************/
public class DocActivity extends Activity implements View.OnLayoutChangeListener, View.OnTouchListener, View.OnClickListener {

    private static final int MIN_FONT_SIZE = 6;
    private static final int MAX_FONT_SIZE = 20;
    private static final int DEFAULT_FONT_SIZE = 14;

    public static final String DOC_FILE = "DOC_FILE";

    private static final String DOC_TEXT = "DOC_TEXT";

    private ActivityDocBinding views;

    private Paint paint;
    private int maxCharsPerLine = 0;

    private String docText;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityDocBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        setActionBar(views.toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setIcon(R.drawable.ic_actionbar);
        }

        views.toolbar.setOnClickListener(this);
        views.toolbar.setOnTouchListener(this);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String docFile = extras.getString(DOC_FILE);
                docText = loadContentFromFile(docFile);
            }
        } else {
            docText = savedInstanceState.getString(DOC_TEXT);
        }

        paint = views.docTextView.getPaint();
        views.docTextView.setVisibility(View.INVISIBLE);
        views.docTextView.addOnLayoutChangeListener(this);
        views.docTextView.setText(docText);
        Linkify.addLinks(views.docTextView, Pattern.compile("https?://\\S*"), "");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(DOC_TEXT, docText);
    }

    /**
     * Gets the doc content from text file and return it as a string
     * @param docFile the text file as an absolute path
     * @return the doc content
     */
    private String loadContentFromFile(String docFile) {
        if (docFile != null && !"".equals(docFile)) {
            StringBuilder text = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(docFile))) {

                // do reading, usually loop until end of file reading
                String mLine;
                while ((mLine = reader.readLine()) != null) {
                    text.append(mLine);
                    text.append('\n');
                }
                return text.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return getString(R.string.doc_error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        views.docTextView.removeOnLayoutChangeListener(this);
        views = null;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (oldRight == 0) {
            int fontSize = findIdealFontSize(MIN_FONT_SIZE, MAX_FONT_SIZE);
            if (fontSize == MIN_FONT_SIZE) {
                fontSize = DEFAULT_FONT_SIZE;
            }
            views.docTextView.setTextSize(fontSize);
            views.docTextView.setVisibility(View.VISIBLE);
            views.docTextView.post( new Runnable() {
                    @Override
                    public void run() {
                        views.docTextView.requestLayout();
                    }
                });
        } else {
            views.docTextView.setVisibility(View.VISIBLE);
            views.docTextView.removeOnLayoutChangeListener(this);
        }
    }

    private void updateMaxCharsPerLine() {
        maxCharsPerLine = Collections.max(Arrays.asList(docText.split("\n")), new Comparator<String>() {
            @Override
            public int compare(String str1, String str2) {
                return str1.length() - str2.length();
            }}).length() + 1;
    }

    /**
     * Finds the biggest font size for the doc (monospace, manual line breaks) before the text lines get cut in two
     * @param minSize minimum acceptable font size
     * @param maxSize maximum acceptable font size
     * @return that ideal font size
     */
    private int findIdealFontSize(int minSize, int maxSize) {
        if (maxSize - minSize <= 1) {
            return minSize;
        }
        int midSize = (maxSize + minSize) / 2;
        if (isFontSizeFitting(midSize)) {
            return findIdealFontSize(midSize, maxSize);
        } else {
            return findIdealFontSize(minSize, midSize);
        }
    }

    /**
     * Tells whether the text lines in the doc fit in the view without getting cut in two, for a specific font size
     * @param fontSize the font size to test
     * @return true if it does, false otherwise
     */
    private boolean isFontSizeFitting(int fontSize) {
        views.docTextView.setTextSize(fontSize);
        if (maxCharsPerLine == 0) {
            updateMaxCharsPerLine();
        }
        float textSize = paint.measureText("m", 0, 1) * maxCharsPerLine;
        return textSize <= ((ViewGroup) views.docTextView.getParent()).getWidth();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return view.performClick();
    }

    @Override
    public void onClick(View view) {
        views.scrollView.smoothScrollTo(0,0);
    }
}
