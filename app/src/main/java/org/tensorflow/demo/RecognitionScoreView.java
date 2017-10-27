/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.tensorflow.demo.Classifier.Recognition;
import org.tensorflow.demo.env.Utils;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.Face;

import java.util.List;

public class RecognitionScoreView extends View implements ResultsView {
//  private static final float TEXT_SIZE_DIP = 24;
  private static final float TEXT_SIZE_DIP = 12;
  private List<Recognition> results;
  private final float textSizePx;
  private final Paint fgPaint;
  private final Paint bgPaint;
  final CharSequence[] emotions = { "Anger", "Disgust", "Happy", "Sad", "Surprise", "Neutral" };
    private Context mContext;

  public RecognitionScoreView(final Context context, final AttributeSet set) {
    super(context, set);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    fgPaint = new Paint();
    fgPaint.setTextSize(textSizePx);

    bgPaint = new Paint();
//    bgPaint.setColor(0xcc4285f4);
    bgPaint.setColor(Color.TRANSPARENT);
    mContext = context;
  }

  @Override
  public void setResults(final List<Recognition> results) {
    Log.e("TW", "results = " +results);
    this.results = results;
    postInvalidate();
  }

  private static final float FACE_POSITION_RADIUS = 10.0f;
  private static final float ID_TEXT_SIZE = 40.0f;
  private static final float ID_Y_OFFSET = 50.0f;
  private static final float ID_X_OFFSET = -50.0f;
  private static final float BOX_STROKE_WIDTH = 5.0f;

  private static final int COLOR_CHOICES[] = {
          Color.BLUE,
          Color.CYAN,
          Color.GREEN,
          Color.MAGENTA,
          Color.RED,
          Color.WHITE,
          Color.YELLOW
  };

  private float mWidthScaleFactor = 1.0f;
  private float mHeightScaleFactor = 1.0f;
  private int mFacing = CameraSource.CAMERA_FACING_FRONT;


  @Override
  public void onDraw(final Canvas canvas) {
    final int x = 10;
    final int x2 = 500;
    int y = (int) (fgPaint.getTextSize() * 1.5f);
    int y2 = (int) (fgPaint.getTextSize() * 1.5f);



    canvas.drawPaint(bgPaint);
    fgPaint.setColor(Color.BLACK);

    if (results != null) {
      int idx=0;
      for (final Recognition recog : results) {
        canvas.drawText(recog.getTitle() + "_" + emotions[Integer.parseInt(recog.getTitle())].toString() + ": " + recog.getConfidence(), x, y, fgPaint);
        y += fgPaint.getTextSize() * 1.5f;
        idx++;
      }

      int maxIndex = Integer.parseInt(results.get(0).getTitle());
      for (int i = 0; i < results.size(); i++) {
        for (int j = 0; j < results.size(); j++) {
          if (results.get(j).getTitle().equals("" + i)) {//anger
            if(i == maxIndex) {
              fgPaint.setColor(Color.RED);
            } else {
              fgPaint.setColor(Color.BLACK);
            }
            canvas.drawText(i + "_" + emotions[i].toString() + ": " + results.get(j).getConfidence(), x2, y2, fgPaint);
            break;
          }
        }
        y2 += fgPaint.getTextSize() * 1.5f;
      }

      String appName = Utils.getTopActivity(getContext());

      Log.e("TW", "appName = " +appName);
      fgPaint.setColor(Color.BLUE);
      canvas.drawText("Pkg : " + appName, x, y+10, fgPaint);
    }
    Paint mBoxPaint;

    mBoxPaint = new Paint();

    mBoxPaint.setStyle(Paint.Style.STROKE);
    if( Utils.SAVE_PREVIEW_BITMAP) {
      mBoxPaint.setColor(Color.RED);
      mBoxPaint.setStrokeWidth(15);
    } else {
      mBoxPaint.setColor(Color.BLUE);
      mBoxPaint.setStrokeWidth(5);
    }

    float wh = 550;
    float left = 240;
    float top = 900;
    float right = left + wh;
    float bottom = top + wh;
    canvas.drawRect(left, top, right, bottom, mBoxPaint);
  }

}
