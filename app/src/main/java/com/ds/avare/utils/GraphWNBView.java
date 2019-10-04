/*
Copyright (c) 2019, Apps4Av Inc. (apps4av.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.ds.avare.R;
import com.ds.avare.flight.AircraftSpecs;

import java.util.LinkedList;

/**
 * Draw the weight and balance envelope using the data provided
 * @author Ron Walker
 *
 */
public class GraphWNBView extends TextView {
    AircraftSpecs mACSpecs = new AircraftSpecs();
    Paint mPaint = new Paint();
    Path mPath = new Path();
    float[] mCGPoints = new float[100];
    final int mMargin = 25;

    public GraphWNBView(Context ctx) {
        super(ctx);
    }

    public GraphWNBView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }

    public GraphWNBView(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Generic index local variable
        int idx = 0;

        // Fill our our native aircraft specs object from the string text that
        // was passed in to the control
        mACSpecs.fromString(getText().toString());

        // Extract individual points for the CG Envelope
        String[] envPoints = mACSpecs.getCGEnv().split(" ");

        // Parse each one for the ARM/WT location
        for(String envPoint : envPoints) {
            String[] xy = envPoint.split(",");
            mCGPoints[idx++] = (Helper.parseFloat(xy[0]));
            mCGPoints[idx++] = (Helper.parseFloat(xy[1]));
        }

        // Display metrics we need
        int iWidth  = getWidth();
        int iHeight = getHeight();

        // Default the paint brush to fill the area
        mPaint.setStyle(Paint.Style.FILL);

        // Fill the background with RED
        mPaint.setColor(Color.rgb(0xC0, 0x00, 0x00));
        canvas.drawPaint(mPaint);

        // Now draw the inner GREEN based on the segments
        mPaint.setColor(Color.rgb(0x00, 0xA0, 0x00));

        // The weight is along the vertical Y axis
        float diffW = mACSpecs.getGross() - mACSpecs.getEmpty();  // Weight spread
        float ratioY = (iHeight - 2 * mMargin) / diffW;     // vertical ratio

        // The arm is along the horizontal X axis
        float diffA = mACSpecs.getCGMax() - mACSpecs.getCGMin();  // ARM spread
        float ratioX = (iWidth - 2 * mMargin) / diffA;      // Horizontal ratio

        // Recalc the location of each point based upon the display ratio
        for(idx = 0; idx < envPoints.length * 2; idx += 2) {
            mCGPoints[idx] = (mCGPoints[idx] - mACSpecs.getCGMin()) * ratioX + mMargin;
            mCGPoints[idx + 1] = iHeight - (mCGPoints[idx + 1] - mACSpecs.getEmpty()) * ratioY - mMargin;
        }

        // Draw all the points into the Path, then give the path to the canvas
        mPath.reset();
        mPath.moveTo(mCGPoints[0], mCGPoints[1]);
        for(idx = 2; idx < envPoints.length * 2; idx += 2) {
            mPath.lineTo(mCGPoints[idx], mCGPoints[idx + 1]);
        }
        canvas.drawPath(mPath, mPaint);

        // Draw a circle at the point of our calculated CG
        float cgX = (mACSpecs.getCG() - mACSpecs.getCGMin()) * ratioX + mMargin;
        float cgY = iHeight - (mACSpecs.getWeight() - mACSpecs.getEmpty()) * ratioY - mMargin;
        mPaint.setColor(Color.BLACK);
        canvas.drawCircle(cgX, cgY, 5, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(3);
        canvas.drawCircle(cgX, cgY, 8, mPaint);
    }
}

