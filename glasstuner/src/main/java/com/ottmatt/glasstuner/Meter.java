////////////////////////////////////////////////////////////////////////////////
//
//  Tuner - An Android Tuner written in Java.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version  of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package com.ottmatt.glasstuner;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

// Meter

public class Meter extends TunerView
        implements AnimatorUpdateListener {
    private Matrix matrix;
    private Bitmap bitmap;
    private Rect barRect;
    private RectF meterRect;
    private Path path;

    private ValueAnimator animator;

    private double cents;
    private float medium;
    private int margin;

    // Constructor

    public Meter(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create a path for the thumb

        path = new Path();

        path.moveTo(0, -1);
        path.lineTo(1, 0);
        path.lineTo(1, 1);
        path.lineTo(-1, 1);
        path.lineTo(-1, 0);
        path.close();

        // Create a matrix for scaling

        matrix = new Matrix();
        bitmap = BitmapFactory.decodeResource(resources,
                org.billthefarmer.tuner.R.drawable.ic_pref_screen);
    }

    // OnSizeChanged

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Recalculate dimensions

        width = clipRect.right - clipRect.left;
        height = clipRect.bottom - clipRect.top;
        margin = width / 32;

        // Recalculate text size

        medium = height / 3.0f;
        paint.setTextSize(medium);

        // Scale text if necessary to fit it in

        float dx = paint.measureText("50");
        if (dx >= width / 11)
            paint.setTextScaleX((width / 12) / dx);

        // Create a rect for the horizontal bar

        barRect = new Rect(width / 36 - width / 2, -height / 64,
                width / 2 - width / 36, height / 64);

        meterRect = new RectF(margin, -width/2 + margin,
                width - margin, width/2 - margin);

        // Create a matrix to scale the path,
        // a bit narrower than the height

        matrix.setScale(height / 24, height / 8);

        // Scale the path

        path.transform(matrix);

        // Create animator

        animator = ValueAnimator.ofInt(0, 10000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setDuration(10000);

        animator.addUpdateListener(this);
        animator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        // Do the inertia calculation
        if (audio != null)
            cents = ((cents * 19.0) + audio.cents) / 20.0;

        invalidate();
    }

    // OnDraw

    @Override
    @SuppressLint("DefaultLocale")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStrokeWidth(6);
        paint.setStyle(Style.STROKE);
        paint.setColor(resources.getColor(android.R.color.primary_text_dark));

        canvas.translate(0, height/2 + height/3);

        float xscale = width / 11;
        float xLocation = (float) cents * (xscale / 10);
        float radius = width/2 + 20 + margin;
        float theta = (float) Math.asin(xLocation / radius);
        float yLocation = (float)(radius * Math.cos(theta));

        // Draw the scale
        canvas.drawArc(meterRect, 0, -180, false, paint);
        canvas.drawLine(width/2, width/2 - 20, width/2, width/2 +20, paint);

        // Draw the needle
        canvas.drawLine(width/2, 0, xLocation + width/2, -yLocation, paint);
        paint.setStyle(Style.FILL);
        canvas.drawCircle(width/2, 0, 5, paint);
        // Draw the needle indicator
        if (cents < 15 && cents > -15) {
            paint.setColor(resources.getColor(android.R.color.holo_green_light));
        } else {
            paint.setColor(resources.getColor(android.R.color.holo_red_light));
        }
        paint.setStyle(Style.STROKE);
        canvas.drawCircle(width/2, 0, 10, paint);
    }
}
