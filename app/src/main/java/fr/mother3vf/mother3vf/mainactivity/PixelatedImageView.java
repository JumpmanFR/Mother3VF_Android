package fr.mother3vf.mother3vf.mainactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.Locale;

import androidx.core.view.ViewCompat;
import fr.mother3vf.mother3vf.R;


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
@SuppressLint("AppCompatCustomView")
public class PixelatedImageView extends ImageView {
    private RectF drawingRect;
    Paint paint;
    BitmapFactory.Options options;
    Bitmap bitmap;
    boolean isRtl;

    public PixelatedImageView(Context context) {
        super(context);
        init();
    }

    public PixelatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelatedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setDither(false);
        paint.setAntiAlias(false);
        options = new BitmapFactory.Options();
        options.inScaled = false;
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image, options);
        drawingRect = new RectF();
        isRtl = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float widthScale = ((float) getWidth()) / bitmap.getWidth();
        float heightScale = ((float) getHeight()) / bitmap.getHeight();
        float scale = Math.min(widthScale, heightScale);
        drawingRect.top = getTop();
        drawingRect.bottom = getTop() + bitmap.getHeight() * scale;
        if (isRtl) {
            drawingRect.left = getLeft();
            drawingRect.right = getLeft() +  bitmap.getWidth() * scale;
        } else {
            drawingRect.left = getRight() -  bitmap.getWidth() * scale;
            drawingRect.right = getRight();
        }
        canvas.drawBitmap(bitmap, null, drawingRect, paint);
    }
}
