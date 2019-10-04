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

/**
 * Draw the weight and balance envelope using the data provided
 * @author Ron Walker
 *
 */
public class GraphWNBView extends TextView {
    AircraftSpecs mACData;
    Paint mPaint = new Paint();
    Path mPath = new Path();

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

        float maxW = 2450f;
        float minW = 1650f;
        float maxA = 50f;
        float minA = 30f;

        float cgA = 42.12f;
        float cgW = 2419.8f;

        float[] drawPoints = new float[] {
                47.3f, 1650f,
                35f,   1650f,
                35f,   1950f,
                40f,   2450f,
                47.3f, 2450f,
                47.3f, 1650f

        };

        AircraftSpecs acSpecs = new AircraftSpecs(getText().toString());
        maxW = acSpecs.gross();
        minW = acSpecs.empty();
        maxA = acSpecs.cgMax();
        minA = acSpecs.cgMin();
        cgA  = acSpecs.cg();
        cgW  = acSpecs.weight();

        mPaint.setStyle(Paint.Style.FILL);

        // Fill the background with RED
        mPaint.setColor(Color.rgb(0xC0, 0x00, 0x00));
        canvas.drawPaint(mPaint);

        // Now draw the inner GREEN based on the segments
        mPaint.setColor(Color.rgb(0x00, 0xA0, 0x00));

        float diffW = maxW - minW;          // Weight spread
        float ratioY = getHeight() / diffW; // vertical ratio

        float diffA = maxA - minA;          // ARM spread
        float ratioX = getWidth() / diffA;  // Horizontal ratio

        for(int idx = 0; idx < drawPoints.length; idx += 2) {
            drawPoints[idx]     = (drawPoints[idx]     - minA) * ratioX;
            drawPoints[idx + 1] = getHeight() - (drawPoints[idx + 1] - minW) * ratioY;
        }

        mPath.reset();
        mPath.moveTo(drawPoints[0], drawPoints[1]);
        for(int idx = 2; idx < drawPoints.length; idx += 2) {
            mPath.lineTo(drawPoints[idx], drawPoints[idx + 1]);
        }
        canvas.drawPath(mPath, mPaint);

        float cgX = (cgA - minA) * ratioX;
        float cgY = getHeight() - (cgW - minW) * ratioY;
        mPaint.setColor(Color.BLACK);
        canvas.drawCircle(cgX, cgY, 5, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(3);
        canvas.drawCircle(cgX, cgY, 8, mPaint);
    }
}

