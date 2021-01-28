/*******************************************************************************
 * This file is part of MOTHER 3 VF for Android (2017, JumpmanFR)
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * Paul Kratt - main MultiPatch application for macOS
 * xperia64 - port to Android support
 * JumpmanFR - adaptation for MOTHER3VF
 ******************************************************************************/
package fr.mother3vf.mother3vf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;


@SuppressLint("AppCompatCustomView")
public class PixelatedImageView extends ImageView {
    private RectF drawingRect;

    public PixelatedImageView(Context context) {
        super(context);
    }

    public PixelatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PixelatedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setDither(false);
        paint.setAntiAlias(false);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inScaled = false;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image, options);

        float widthScale = ((float) getWidth()) / bitmap.getWidth();
        float heightScale = ((float) getHeight()) / bitmap.getHeight();

        float scale = Math.min(widthScale, heightScale);

        drawingRect = new RectF(getRight() -  bitmap.getWidth() * scale, getTop(), getRight(), getTop() + bitmap.getHeight() * scale);

        canvas.drawBitmap(bitmap, null, drawingRect, paint);
    }

    public RectF getDrawingRect() {
        return drawingRect;
    }
}
