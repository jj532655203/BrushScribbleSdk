package com.jj.brushscribblesdk;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.appcompat.app.AppCompatActivity;

import com.jj.brush_scribble_sdk.BrushScribbleView;
import com.jj.brush_scribble_sdk.data.TouchPoint;
import com.jj.brush_scribble_sdk.data.TouchPointList;
import com.jj.brush_scribble_sdk.intf.RawInputCallback;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    private SurfaceHolder.Callback holderCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");

            surfaceView.setRawInputCallback(inputCallBack).setRawDrawingEnable(true);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");

            surfaceView.setRawDrawingEnable(false);

        }
    };
    private RawInputCallback inputCallBack = new RawInputCallback() {
        @Override
        public void onBeginRawDrawing(TouchPoint var2) {
            Log.d(TAG, "onBeginRawDrawing");

        }

        @Override
        public void onEndRawDrawing(TouchPoint var2) {
            Log.d(TAG, "onEndRawDrawing");

        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint var1) {
            Log.d(TAG, "onRawDrawingTouchPointMoveReceived");

        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList var1) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived");

        }
    };
    private BrushScribbleView surfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(holderCallBack);
    }
}
