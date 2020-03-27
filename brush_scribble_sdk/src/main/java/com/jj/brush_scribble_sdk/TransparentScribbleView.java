package com.jj.brush_scribble_sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.blankj.utilcode.util.ObjectUtils;
import com.jj.brush_scribble_sdk.data.TouchPoint;
import com.jj.brush_scribble_sdk.data.TouchPointList;
import com.jj.brush_scribble_sdk.intf.RawInputCallback;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TransparentScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 32;
    private WaitGo waitGo = new WaitGo();
    private boolean is2StopRender;
    private boolean isRenderRunning;
    private boolean isRefresh;
    private Paint renderPaint;
    private float strokeWidth = 12f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedDeque<TouchPointList> mLast16PathQueue = new ConcurrentLinkedDeque<>();
    private ConcurrentHashMap<TouchPointList, List<TouchPoint>> mPathIntensivePointsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TouchPointList, List<TouchPoint>> mPathPreviousBezierLastHalfMap = new ConcurrentHashMap<>();

    public int getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public TransparentScribbleView setRawInputCallback(RawInputCallback rawInputCallback) {
        this.rawInputCallback = rawInputCallback;
        return this;
    }

    public TransparentScribbleView(Context context) {
        this(context, null);
    }

    public TransparentScribbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransparentScribbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundResource(R.color.transparent);
        setZOrderOnTop(true);
        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);

        initRenderPaint();

    }


    /**
     * 监听surfaceView的motionEvent
     * 只监听第一根手指
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouch");

        TouchPoint activeTouchPoint = new TouchPoint(event.getX(), event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                activeTouchPointList.getPoints().clear();
                activeTouchPointList.setTimeStamp(System.currentTimeMillis());
                activeTouchPointList.add(activeTouchPoint);
                renderPath();

                if (rawInputCallback != null) rawInputCallback.onBeginRawDrawing(activeTouchPoint);
            }
            break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                if (event.getPointerId(event.getActionIndex()) != ACTIVE_POINTER_ID) {
                    break;
                }

                activeTouchPointList.add(activeTouchPoint);
                renderPath();

                TouchPointList touchPointList = new TouchPointList();
                touchPointList.addAll(activeTouchPointList);

                activeTouchPointList.getPoints().clear();

                if (rawInputCallback != null) {
                    rawInputCallback.onRawDrawingTouchPointListReceived(touchPointList);

                    rawInputCallback.onEndRawDrawing(activeTouchPoint);
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                int actionIndex = event.getActionIndex();
                if (event.getPointerId(actionIndex) != ACTIVE_POINTER_ID) {
                    break;
                }

                activeTouchPointList.add(activeTouchPoint);

                renderPath();

                if (rawInputCallback != null)
                    rawInputCallback.onRawDrawingTouchPointMoveReceived(activeTouchPoint);
            }
            break;
        }
        return true;
    }


    synchronized void startRenderThread() {
        is2StopRender = false;
        if (isRenderRunning) {
            return;
        }
        isRenderRunning = true;

        JobExecutor.getInstance().execute(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "startRenderThread ThreadName=" + Thread.currentThread().getName());

                while (!is2StopRender) {

                    Canvas canvas = null;
                    try {
                        if (!isRefresh) {
                            waitGo.waitOne();
                            continue;
                        }
                        isRefresh = false;


                        long startDoRenderTime = System.currentTimeMillis();

                        if (getHolder() != null && getHolder().getSurface().isValid()) {

                            canvas = getHolder().lockCanvas();

                            //由于双缓冲机制,得绘制最近几根笔迹

                            int size = mLast16PathQueue.size();
                            TouchPointList previousPath = null;
                            for (TouchPointList _toDrawPath : mLast16PathQueue) {
                                if (_toDrawPath.size() == 0) {
                                    continue;
                                }

                                if (is2StopRender) break;

                                List<TouchPoint> _toDrawPathPoints = _toDrawPath.getPoints();
                                int _toDrawPathPointsSize = _toDrawPathPoints.size();
                                if (_toDrawPathPointsSize <= 3) {
                                    addAPath2Canvas(_toDrawPathPoints, canvas);
                                    if (_toDrawPathPointsSize == 3) {
                                        mPathIntensivePointsMap.put(_toDrawPath, BrushRender.intensive(_toDrawPathPoints));
                                        previousPath = _toDrawPath;
                                    }
                                    continue;
                                }

                                List<TouchPoint> intensivePoints = mPathIntensivePointsMap.get(_toDrawPath);
                                if (intensivePoints == null) {
                                    //新线
                                    if (previousPath == null || _toDrawPath.getTimeStamp() != previousPath.getTimeStamp()) {
                                        Log.e(TAG, "遍历绘制 mLast16PathQueue 逻辑异常 前一根线没加密成功?");
                                        continue;
                                    }

                                    TouchPoint increasePoint = _toDrawPathPoints.get(_toDrawPathPointsSize - 1);
                                    List<List<TouchPoint>> increaseComputeResult = BrushRender.intensiveByIncrease(
                                            canvas, renderPaint, increasePoint, previousPath.getPoints(), mPathIntensivePointsMap.get(previousPath), strokeWidth, 0
                                    );
                                    if (ObjectUtils.isEmpty(increaseComputeResult)) {
                                        Log.e(TAG, "遍历绘制 mLast16PathQueue 逻辑异常 增量加密失败");
                                        continue;
                                    }

                                    intensivePoints = increaseComputeResult.get(0);
                                    mPathIntensivePointsMap.put(_toDrawPath, intensivePoints);

                                    List<TouchPoint> oldLastBezierLastHalfSegmentPoints = increaseComputeResult.get(1);

                                    if (ObjectUtils.isEmpty(oldLastBezierLastHalfSegmentPoints)) {
                                        Log.e(TAG, "遍历绘制 mLast16PathQueue 逻辑异常 去毛边失败");
                                    } else {
                                        mPathPreviousBezierLastHalfMap.put(_toDrawPath, oldLastBezierLastHalfSegmentPoints);
                                    }

                                }

                                //擦除上一根线的毛边
                                BrushRender.eraseStroke(canvas, mPathPreviousBezierLastHalfMap.get(_toDrawPath), strokeWidth, 0);

                                //绘制加密后的线
                                BrushRender.drawStroke(canvas, renderPaint, intensivePoints, strokeWidth, 0);

                                previousPath = _toDrawPath;

                            }

                            Log.d(TAG, "doRender consume time=" + (System.currentTimeMillis() - startDoRenderTime) + "?mLast16PathQueue.size=" + size);

                        } else {
                            Log.e(TAG, "surfaceView released return");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (canvas != null) {
                            getHolder().unlockCanvasAndPost(canvas);
                        }
                    }

                }

                isRenderRunning = false;
                Log.d(TAG, "startRenderThread 停止笔划渲染线程成功 ThreadName=" + Thread.currentThread().getName());
            }
        });
    }

    private void addAPath2Canvas(List<TouchPoint> points, Canvas canvas) {

        if (points == null || points.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        int size = points.size();
        if (size == 1 || size == 2) {
            TouchPoint touchPoint = points.get(0);
            if (size == 1) {
                canvas.drawPoint(touchPoint.x, touchPoint.y, renderPaint);
            } else {
                TouchPoint touchPoint1 = points.get(1);
                canvas.drawLine(touchPoint.x, touchPoint.y, touchPoint1.x, touchPoint1.y, renderPaint);
            }
        } else {

            Path activePath = new Path();
            TouchPoint touchPoint0 = points.get(0);
            activePath.moveTo(touchPoint0.x, touchPoint0.y);
            for (int i = 0; i < size - 1; i++) {
                TouchPoint touchPointi = points.get(i);
                TouchPoint touchPointiPlus = points.get(i + 1);
                activePath.quadTo(touchPointi.x, touchPointi.y, touchPointiPlus.x, touchPointiPlus.y);
            }
            canvas.drawPath(activePath, renderPaint);

        }

        Log.d(TAG, "addAPath2Canvas consume time=" + (System.currentTimeMillis() - startTime));

    }


    void stopRenderThread() {
        if (!isRenderRunning) return;
        is2StopRender = true;
        waitGo.go();
    }

    private void initRenderPaint() {
        if (renderPaint != null) return;
        renderPaint = new Paint();
        renderPaint.setStrokeWidth(strokeWidth);
        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setColor(strokeColor);
    }


    private void renderPath() {
        Log.d(TAG, "renderPath start");

        if (mLast16PathQueue.size() == FRAME_CACHE_SIZE) {
            TouchPointList touchPointList = mLast16PathQueue.removeFirst();
            mPathIntensivePointsMap.remove(touchPointList);
        }
        TouchPointList lastTouchPointList = new TouchPointList(activeTouchPointList.size());
        lastTouchPointList.addAll(activeTouchPointList);
        lastTouchPointList.setTimeStamp(activeTouchPointList.getTimeStamp());
        mLast16PathQueue.add(lastTouchPointList);

        isRefresh = true;
        if (!waitGo.isGo()) waitGo.go();
    }


    /**
     * start scribble thread when onResume!stop it when onPause!
     *
     * @param enable true to start,otherwise to top.
     */
    public void setRawDrawingEnable(boolean enable) {
        Log.d(TAG, "setRawDrawingEnable enable=" + enable);

        if (enable) {
            startRenderThread();
            reproduceScribblesAfterSurfaceRecreated();
        } else {
            stopRenderThread();
        }
    }

    /**
     * when this TransparentScribbleView need swipe up(case : used to a new page),you need call this function .
     * call this function must surfaceCreated!
     */
    public void clearScreenAfterSurfaceViewCreated() {
        Log.d(TAG, "clearScreenAfterSurfaceViewCreated ");

        mLast16PathQueue.clear();
        mPathIntensivePointsMap.clear();
        mPathPreviousBezierLastHalfMap.clear();

        activeTouchPointList.getPoints().clear();

        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            getHolder().unlockCanvasAndPost(canvas);
        } else {
            Log.e(TAG, "clearScreenAfterSurfaceViewCreated 失败!");
        }

    }

    /**
     * reproduce scribbles after surface recreate,but need setRawDrawingEnable(true) first;
     * setRawDrawingEnable(true) is a perfect trigger,since at the time is onResumed(as well as surface created)
     */
    public void reproduceScribblesAfterSurfaceRecreated() {
        Log.d(TAG, "reproduceScribblesAfterSurfaceRecreated ");
        if (!isRenderRunning) {
            Log.e(TAG, "reproduceScribblesAfterSurfaceRecreated --> need setRawDrawingEnable(true) first!");
            return;
        }
        isRefresh = true;
        if (!waitGo.isGo()) waitGo.go();
    }

}
