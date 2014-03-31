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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;

// Display

public class Display extends TunerView {
    private static final int OCTAVE = 12;

    private int larger;
    private int large;
    private int medium;
    private int small;

    private int margin;
    private Bitmap bitmap;

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

        bitmap = BitmapFactory.decodeResource(resources,
                org.billthefarmer.tuner.R.drawable.ic_locked);
    }

    // On size changed

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Recalculate dimensions

        width = clipRect.right - clipRect.left;
        height = clipRect.bottom - clipRect.top;

        // Calculate text sizes

        larger = height / 2;
        large = height / 3;
        medium = height / 5;
        small = height / 9;
        margin = width / 32;

        // Make sure the text will fit the width

        paint.setTextSize(medium);
        float dx = paint.measureText("0000.00Hz");

        // Scale the text if it won't fit

        if (dx + (margin * 2) >= width / 2) {
            float xscale = (width / 2) / (dx + (margin * 2));
            paint.setTextScaleX(xscale);
        }
    }

    // On draw

    @Override
    @SuppressLint("DefaultLocale")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // No display if no audio

        if (audio == null)
            return;

        // Set up paint

        paint.setStrokeWidth(1);
        paint.setColor(resources.getColor(android.R.color.primary_text_dark));
        paint.setTextAlign(Align.LEFT);
        paint.setStyle(Style.FILL);

        String s;

        // Set up text

        paint.setTextSize(larger);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        // Move down

        canvas.translate(0, larger);

        // Draw note
        String note = notes[audio.note % OCTAVE];
        float xTranslate = width/2 - paint.measureText(note)/2;
        canvas.drawText(notes[audio.note % OCTAVE], xTranslate, 0, paint);

        // Measure text

        float dx = paint.measureText(notes[audio.note % OCTAVE]);

        // Draw sharps/flats

        paint.setTextSize(larger / 2);
        s = String.format("%s", sharps[audio.note % OCTAVE]);
        canvas.translate(0, paint.ascent());
        canvas.drawText(s, xTranslate + dx, 0, paint);

        // Draw octave

        s = String.format("%d", audio.note / OCTAVE);
        canvas.translate(0, -paint.ascent());
        canvas.drawText(s, xTranslate + dx, 0, paint);


        paint.setStrokeWidth(2);
        canvas.translate(0, large);
        s = String.format("%4.2fHz", audio.frequency);
        canvas.drawText(s, width/2 - paint.measureText(s)/2, 0, paint);

    }

    // Log2

    protected double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }
}
