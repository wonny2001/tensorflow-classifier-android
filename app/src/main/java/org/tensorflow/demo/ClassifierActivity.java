/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.env.Utils;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // These are the settings for the original v1 Inception model. If you want to
  // use a model that's been produced from the TensorFlow for Poets codelab,
  // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
  // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
  // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
  // the ones you produced.
  //
  // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
  // model first:
  //kucc
  // python strip_unused.py \
  // --input_graph=<retrained-pb-file> \
  // --output_graph=<your-stripped-pb-file> \
  // --input_node_names="Mul" \
  // --output_node_names="final_result" \
  // --input_binary=true
//  private static final int INPUT_SIZE = 224;
//  private static final int IMAGE_MEAN = 117;
//  private static final float IMAGE_STD = 1;
//  private static final String INPUT_NAME = "input";
//  private static final String OUTPUT_NAME = "output";
//
//  private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
//  private static final String LABEL_FILE =
//      "file:///android_asset/imagenet_comp_graph_label_strings.txt";

  private static final int INPUT_SIZE = 299;
//  private static final int INPUT_SIZE = 120;
  private static final int IMAGE_MEAN = 128;
  private static final float IMAGE_STD = 128f;
  private static final String INPUT_NAME = "Mul";
  private static final String OUTPUT_NAME = "final_result";

  private static final String MODEL_FILE =
          "file:///android_asset/rounded_graph.pb"; // or optimized_graph.pb
  private static final String LABEL_FILE =
          "file:///android_asset/retrained_labels.txt";

//  private static final int INPUT_SIZE = 128;
//  //  private static final int INPUT_SIZE = 120;
//  private static final int IMAGE_MEAN = 128;
//  private static final float IMAGE_STD = 128f;
//  private static final String INPUT_NAME = "input";
//  private static final String OUTPUT_NAME = "final_result";
//
//  private static final String MODEL_FILE =
//          "file:///android_asset/retrained_graph.pb"; // or optimized_graph.pb
//  private static final String LABEL_FILE =
//          "file:///android_asset/retrained_labels.txt";

  private static final boolean MAINTAIN_ASPECT = true;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 640);
//  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
//  private static final Size DESIRED_PREVIEW_SIZE = new Size(100, 100);

  private Classifier classifier;

  private Integer sensorOrientation;

  private int previewWidth = 0;
  private int previewHeight = 0;
  private byte[][] yuvBytes;
  private int[] rgbBytes = null;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;

  private Bitmap cropCopyBitmap;

  private boolean computing = false;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private ResultsView resultsView;

  private BorderedText borderedText;

  private long lastProcessingTimeMs;
  private String outputString =
          "anger,disgust,happiness,sadness,suprise,neutral\n";

  private String outFileName = "";
  private int mFileCnt = 0;

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  private static final float TEXT_SIZE_DIP = 10;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    classifier =
        TensorFlowImageClassifier.create(
            getAssets(),
            MODEL_FILE,
            LABEL_FILE,
            INPUT_SIZE,
            IMAGE_MEAN,
            IMAGE_STD,
            INPUT_NAME,
            OUTPUT_NAME);

    resultsView = (ResultsView) findViewById(R.id.results);
    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    final Display display = getWindowManager().getDefaultDisplay();
    final int screenOrientation = display.getRotation();

    LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);

    sensorOrientation = rotation + screenOrientation;

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbBytes = new int[previewWidth * previewHeight];
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

    croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

//    croppedBitmap = ImageUtils.cropCenterBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            INPUT_SIZE, INPUT_SIZE,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    yuvBytes = new byte[3][];

    addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            renderDebug(canvas);
          }
        });
  }

  @Override
  public void onImageAvailable(final ImageReader reader) {

//    LOGGER.i("onImageAvailable");
    Image image = null;

    try {
      image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (computing) {
        image.close();
        return;
      }
      computing = true;

      Trace.beginSection("imageAvailable");

      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);

      final int yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();
      ImageUtils.convertYUV420ToARGB8888(
          yuvBytes[0],
          yuvBytes[1],
          yuvBytes[2],
          previewWidth,
          previewHeight,
          yRowStride,
          uvRowStride,
          uvPixelStride,
          rgbBytes);

      image.close();
    } catch (final Exception e) {
      if (image != null) {
        image.close();
      }
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }

    rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final Canvas canvas = new Canvas(croppedBitmap);

//    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    Bitmap bb = ImageUtils.cropCenterBitmap(rgbFrameBitmap, INPUT_SIZE, INPUT_SIZE);
    bb = RotateBitmap(bb, 270);
    canvas.drawBitmap(bb, null, new Rect(0,0,INPUT_SIZE,INPUT_SIZE), null);

    // For examining the actual TF input.
    Log.e("TW", "SAVE_PREVIEW_BITMAP : "+Utils.SAVE_PREVIEW_BITMAP);
    if (Utils.SAVE_PREVIEW_BITMAP) {

      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("YYMMdd_HHmmsss");
      String date = sdf.format(cal.getTime());
      ImageUtils.saveBitmap(bb, "preview_" + date + ".png");
    } else {

    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            resultsView.setResults(results);

            requestRender();
            computing = false;
            makeCSVData(results);

          }
        });

    Trace.endSection();
  }

  public static Bitmap RotateBitmap(Bitmap source, float angle)
  {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  private void makeCSVData(List<Classifier.Recognition> results) {
    Log.e("TW", "results.size() : "+results.size());

    String appName = Utils.getTopActivity(getApplicationContext());
    Log.e("TW", "results.size() : "+results.size());

    for(int i=0;i<results.size();i++) {
      for (int j = 0; j < results.size(); j++) {
        if (results.get(j).getTitle().equals("" + i)) {//anger
          outputString = outputString + results.get(j).getConfidence();
          break;
        }
      }
      if( i == results.size()-1 ) {
        outputString = outputString + "," + appName + "\n";
      } else {
        outputString = outputString + ",";
      }
    }

    mFileCnt += 1;
    Log.e("TW", "output : "+outputString);
  }

//  public static String getTopActivity(Context mContext) {
//    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//      //For third party app, bcz cannot use Activity Manager.
//      //Need : Settings->Security->Apps with usage access
//
//      String currentApp = null;
//      UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
//      long time = System.currentTimeMillis();
//      List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 10 * 1000, time);
//      if (applist != null && applist.size() > 0) {
//        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
//        for (UsageStats usageStats : applist) {
//          mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
//
//        }
//        if (mySortedMap != null && !mySortedMap.isEmpty()) {
//          currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
//        }
//      }
//      return currentApp;
//    } else {
//      //For preload map only can use ActivityManager
//      ActivityManager activityManager = (ActivityManager) mContext
//              .getSystemService(Context.ACTIVITY_SERVICE);
//      List<ActivityManager.RunningTaskInfo> info;
//      info = activityManager.getRunningTasks(1);
//      return info != null && info.size() > 0 && info.get(0) != null
//              ? info.get(0).topActivity.getClassName() : "";
//    }
//  }
  @Override
  public void onSetDebug(boolean debug) {
    classifier.enableStatLogging(debug);
  }

  private void renderDebug(final Canvas canvas) {
    if (!isDebug()) {
      return;
    }
    final Bitmap copy = cropCopyBitmap;
    if (copy != null) {
      final Matrix matrix = new Matrix();
      final float scaleFactor = 2;
      matrix.postScale(scaleFactor, scaleFactor);
      matrix.postTranslate(
          canvas.getWidth() - copy.getWidth() * scaleFactor,
          canvas.getHeight() - copy.getHeight() * scaleFactor);
      canvas.drawBitmap(copy, matrix, new Paint());

      final Vector<String> lines = new Vector<String>();
      if (classifier != null) {
        String statString = classifier.getStatString();
        String[] statLines = statString.split("\n");
        for (String line : statLines) {
          lines.add(line);
        }
      }

      lines.add("Frame: " + previewWidth + "x" + previewHeight);
      lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
      lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
      lines.add("Rotation: " + sensorOrientation);
      lines.add("Inference time: " + lastProcessingTimeMs + "ms");

      borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }
  }


  @Override
  public synchronized void onDestroy() {
    super.onDestroy();
    outputCSVFile();
  }

  private void outputCSVFile() {
    Toast.makeText(getApplicationContext(),"csv file saved",Toast.LENGTH_SHORT).show();

    try {
      // Creates a file in the primary external storage space of the
      // current application.
      // If the file does not exists, it is created.
      File testFile = new File(this.getExternalFilesDir(null), outFileName/*"TestFile.csv"*/);
      if (!testFile.exists())
        testFile.createNewFile();

      // Adds a line to the file
      BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, false /*true*/ /*append*/));
//      writer.write("This is a test file.");
      writer.write(outputString);
      writer.close();
      // Refresh the data so it can seen when the device is plugged in a
      // computer. You may have to unplug and replug the device to see the
      // latest changes. This is not necessary if the user should not modify
      // the files.
      MediaScannerConnection.scanFile(this,
              new String[]{testFile.toString()},
              null,
              null);
    } catch (IOException e) {
      Log.e("ReadWriteFile", "Unable to write to the TestFile.txt file.");
    }
  }

  public void showDialog(Context context, String title, String[] btnText,
                         DialogInterface.OnClickListener listener) {

    final CharSequence[] items = { "Clock", "Gallery", "SNS", "MobileOffice", "Game", "Etc" };

//    if (listener == null)
//      listener = new DialogInterface.OnClickListener() {
//        @Override
//        public void onClick(DialogInterface paramDialogInterface,
//                            int paramInt) {
//          Log.e("TW", "OK clicked");
//          finish();
//          paramDialogInterface.dismiss();
//        }
//      };
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);

    builder.setSingleChoiceItems(items, -1,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int item) {
                Log.e("TW", "item : "+item);
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("YYMMdd_HHmmsss");
                String date = sdf.format(cal.getTime());

                outFileName = items[item].toString() + "_" + date + "_line_" + mFileCnt + ".csv";

                Log.e("TW", "outFileName : "+outFileName);
              }
            });
    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int whichButton) {
//        Toast.makeText(WeightTestActivity.this,
//                "Now" + selected[0], Toast.LENGTH_SHORT).show();
          Log.e("TW", "OK clicked");
          finish();
      }
    });
//    if (btnText.length != 1) {
//      builder.setNegativeButton(btnText[1], listener);
//    }
    builder.show();
  }

  boolean doubleBackToExitPressedOnce = false;
  @Override
  public void onBackPressed() {
    if (doubleBackToExitPressedOnce) {
      super.onBackPressed();
      return;
    }

    showDialog(this, "Your Title", new String[] { "Ok" },
            new DialogInterface.OnClickListener() {

              @Override
              public void onClick(DialogInterface dialog, int which) {
                if(which==-1)
                  Log.d("Neha", "On button click : " + which);
              }
            });


    this.doubleBackToExitPressedOnce = true;
    Toast.makeText(this, "Please click BACK again to exit. " + outFileName + "will be saved", Toast.LENGTH_SHORT).show();

    new Handler().postDelayed(new Runnable() {

      @Override
      public void run() {
        doubleBackToExitPressedOnce=false;
      }
    }, 2000);
  }

}
