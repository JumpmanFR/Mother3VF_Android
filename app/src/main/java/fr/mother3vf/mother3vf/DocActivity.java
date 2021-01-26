package fr.mother3vf.mother3vf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocActivity extends Activity implements View.OnLayoutChangeListener {

    public static final String DOC_FILE = "DOC_FILE";

    private static final String DOC_TEXT = "DOC_TEXT";

    private String docText;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String docFile = extras.getString(DOC_FILE);
                docText = loadContentFromFile(docFile);
            }
        } else {
            docText = savedInstanceState.getString(DOC_TEXT);
        }
        textView = ((TextView) findViewById(R.id.docText));
        textView.setText(docText.toString());
        Linkify.addLinks(textView, Pattern.compile("https?://\\S*"), "");
        textView.setVisibility(View.INVISIBLE);
        textView.addOnLayoutChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(DOC_TEXT, docText);
    }

    private String loadContentFromFile(String docFile) {
        if (docFile != null && !"".equals(docFile)) {
            BufferedReader reader = null;
            StringBuilder text = new StringBuilder();
            try {
                reader = new BufferedReader(new FileReader(docFile));

                // do reading, usually loop until end of file reading
                String mLine;
                while ((mLine = reader.readLine()) != null) {
                    text.append(mLine);
                    text.append('\n');
                }
                return text.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return getString(R.string.doc_error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textView != null) {
            textView.removeOnLayoutChangeListener(this);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (oldRight == 0) {
            int fontSize = findPerfectFontSize(6, 20);
            textView.setTextSize(fontSize);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.removeOnLayoutChangeListener(this);
        }
    }

    private int findPerfectFontSize(int minSize, int maxSize) {
        if (maxSize - minSize <= 1) {
            return minSize;
        }
        int midSize = (maxSize + minSize) / 2;
        if (isFontSizeFitting(midSize)) {
            return findPerfectFontSize(midSize, maxSize);
        } else {
            return findPerfectFontSize(minSize, midSize);
        }
    }

    private boolean isFontSizeFitting(int fontSize) {
        textView.setTextSize(fontSize);
        return textSize() <= ((ViewGroup) textView.getParent()).getWidth();
    }

    private float textSize() {
        Paint paint = textView.getPaint();
        return paint.measureText("m", 0, 1) * 75;
    }
}
