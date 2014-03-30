////////////////////////////////////////////////////////////////////////////////
//
//  Tuner - An Android Tuner written in Java.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
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
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;

// Display

public class Display extends TunerView
        implements ValueAnimator.AnimatorUpdateListener {
    private static final int OCTAVE = 12;

    private int large;
    private int medium;
    private int small;

    private int margin;
    private double cents;

    private Rect barRect;
    private Path path;
    private Matrix matrix;
    private ValueAnimator animator;


    // Note values for display

    private static final String notes[] =
            {"C", "C", "D", "E", "E", "F",
                    "F", "G", "A", "A", "B", "B"};

    private static final String sharps[] =
            {"", "\u266F", "", "\u266D", "", "",
                    "\u266F", "", "\u266D", "", "\u266D", ""};

    // Constructor

    public Display(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMeter();
    }

    private void initMeter() {
        path = new Path();
        path.moveTo(0, -1);
        path.lineTo(1, 0);
        path.lineTo(1, 1);
        path.lineTo(-1, 1);
        path.lineTo(-1, 0);
        path.close();
        matrix = new Matrix();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
        // Do the inertia calculation
        if (audio != null) {
            cents = ((cents * 19.0) + audio.cents) / 20.0;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        setNoteSize();
        setMeterSize();
    }

    private void setNoteSize() {
        // Recalculate dimensions

        width = clipRect.right - clipRect.left;
        height = clipRect.bottom - clipRect.top;

        // Calculate text sizes

        large = height / 3;
        medium = height / 5;
        small = height / 9;
        margin = width / 32;

        // Make sure the text will fit the width

        paint.setTextSize(medium);
        // Scale text if necessary to fit it in

        float dx = paint.measureText("50");
        if (dx >= width / 11)
            paint.setTextScaleX((width / 12) / dx);
    }

    private void setMeterSize() {
        // Create a rect for the horizontal bar

        barRect = new Rect(width / 36 - width / 2, -height / 64,
                width / 2 - width / 36, height / 64);

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
    @SuppressLint("DefaultLocale")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // No display if no audio

        if (audio == null) {
            return;
        }
        drawNotes(canvas);

        drawMeter(canvas);
    }

    private void drawNotes(Canvas canvas) {
        // Set up paint

        paint.setStrokeWidth(1);
        paint.setColor(resources.getColor(android.R.color.primary_text_dark));
        paint.setTextAlign(Align.LEFT);
        paint.setStyle(Style.FILL);

        String s;

        // Set up text

        paint.setTextSize(large);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        // Move down

        canvas.translate((width/2) - (large/2), large);

        // Draw note

        canvas.drawText(notes[audio.note % OCTAVE], margin, 0, paint);

        // Measure text

        float dx = paint.measureText(notes[audio.note % OCTAVE]);

        // Draw sharps/flats

        paint.setTextSize(large / 2);
        s = String.format("%s", sharps[audio.note % OCTAVE]);
        canvas.translate(0, paint.ascent());
        canvas.drawText(s, margin + dx, 0, paint);

        // Draw octave

        s = String.format("%d", audio.note / OCTAVE);
        canvas.translate(0, -paint.ascent());
        canvas.drawText(s, margin + dx, 0, paint);

    }

    private void drawMeter(Canvas canvas) {
        // Reset the paint to black

        paint.setStrokeWidth(1);
        paint.setColor(resources.getColor(android.R.color.primary_text_dark));
        paint.setStyle(Style.FILL);

        // Translate the canvas down
        // and to the centre
        canvas.translate(large/2, medium);

        // Calculate x scale

        float xscale = width / 11;

        // Draw the scale legend

        for (int i = 0; i <= 5; i++) {
            String s = String.format("%d", i * 10);
            float x = i * xscale;

            paint.setTextAlign(Align.CENTER);
            canvas.drawText(s, x, 0, paint);
            canvas.drawText(s, -x, 0, paint);
        }

        // Wider lines for the scale

        paint.setStrokeWidth(3);
        paint.setStyle(Style.STROKE);
        canvas.translate(0, medium / 1.5f);

        // Draw the scale

        for (int i = 0; i <= 5; i++) {
            float x = i * xscale;

            canvas.drawLine(x, 0, x, -medium / 2, paint);
            canvas.drawLine(-x, 0, -x, -medium / 2, paint);
        }

        // Draw the fine scale

        for (int i = 0; i <= 25; i++) {
            float x = i * xscale / 5;

            canvas.drawLine(x, 0, x, -medium / 4, paint);
            canvas.drawLine(-x, 0, -x, -medium / 4, paint);
        }

        // Transform the canvas down
        // for the meter pointer

        canvas.translate(0, medium / 2.0f);

        // Set the paint colour to grey

        paint.setColor(resources.getColor(android.R.color.darker_gray));

        // Draw the bar outline

        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(barRect, paint);

        // Translate the canvas to
        // the scaled cents value

        canvas.translate((float) cents * (xscale / 10), -height / 64);

        // Set up the paint for
        // rounded corners

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // Set fill style and fill
        // the thumb

        paint.setColor(resources.getColor(android.R.color.background_light));
        paint.setStyle(Style.FILL);
        canvas.drawPath(path, paint);

        // Draw the thumb outline

        paint.setStrokeWidth(3);
        paint.setColor(resources.getColor(android.R.color.primary_text_dark));
        paint.setStyle(Style.STROKE);
        canvas.drawPath(path, paint);
    }
}
