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
    bgPaint.setColor(0xfff1f1f1);
    mContext = context;
  }

  @Override
  public void setResults(final List<Recognition> results) {
    Log.e("TW", "results = " +results);
    this.results = results;
    postInvalidate();
  }

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

  }
}
