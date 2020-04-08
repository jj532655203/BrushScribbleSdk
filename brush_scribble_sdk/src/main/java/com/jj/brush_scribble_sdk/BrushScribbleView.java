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
import java.util.concurrent.ConcurrentLinkedQueue;

public class BrushScribbleView extends SurfaceView {

    private static final String TAG = "TransparentScribbleView";
    private static final int FRAME_CACHE_SIZE = 48;
    private WaitGo renderWaitGo = new WaitGo();
    private WaitGo eraserWaitGo = new WaitGo();
    private boolean is2StopRender, is2StopEraser;
    private boolean isRenderRunning, isEraserRunning;
    private boolean isRendering, isErase;
    private Paint renderPaint;
    private float strokeWidth = 6f;
    private int strokeColor = Color.BLACK;
    private RawInputCallback rawInputCallback;
    private static final int ACTIVE_POINTER_ID = 0;
    private TouchPointList activeTouchPointList = new TouchPointList();
    private ConcurrentLinkedQueue<TouchPointList> mLast16PathQueue = new ConcurrentLinkedQueue<>();
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

    public BrushScribbleView setRawInputCallback(RawInputCallback rawInputCallback) {
        this.rawInputCallback = rawInputCallback;
        return this;
    }

    public BrushScribbleView(Context context) {
        this(context, null);
    }

    public BrushScribbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrushScribbleView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        Log.d(TAG, "onTouchEvent event.size=" + event.getSize() + "pressure=" + event.getPressure());

        TouchPoint activeTouchPoint = new TouchPoint(event.getX(), event.getY(), event.getPressure(), event.getSize(), 0);

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
                        if (!isRendering) {
                            renderWaitGo.wait1();
                            continue;
                        }
                        isRendering = false;


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

                                    long intensiveStartTime = System.currentTimeMillis();
                                    TouchPoint increasePoint = _toDrawPathPoints.get(_toDrawPathPointsSize - 1);
                                    List<List<TouchPoint>> increaseComputeResult = BrushRender.intensiveByIncrease(
                                            canvas, renderPaint, increasePoint, previousPath.getPoints(), mPathIntensivePointsMap.get(previousPath), strokeWidth, 0
                                    );
                                    if (ObjectUtils.isEmpty(increaseComputeResult)) {
                                        Log.e(TAG, "遍历绘制 mLast16PathQueue 逻辑异常 增量加密失败");
                                        continue;
                                    }
                                    Log.d(TAG, "增量加密 耗时=" + (System.currentTimeMillis() - intensiveStartTime));

                                    intensivePoints = increaseComputeResult.get(0);

                                    //由于现在是使用以点绘实心圆的方式,所以可以将intensivePoints倒数第50点之前的点删除
                                    int intensivePointsSize = intensivePoints.size();
                                    if (intensivePointsSize > 50) {
                                        intensivePoints = intensivePoints.subList(intensivePointsSize - 50, intensivePointsSize);
                                    }

                                    mPathIntensivePointsMap.put(_toDrawPath, intensivePoints);

                                    List<TouchPoint> oldLastBezierLastHalfSegmentPoints = increaseComputeResult.get(1);

                                    if (ObjectUtils.isEmpty(oldLastBezierLastHalfSegmentPoints)) {
                                        Log.e(TAG, "遍历绘制 mLast16PathQueue 逻辑异常 去毛边失败");
                                    } else {
                                        mPathPreviousBezierLastHalfMap.put(_toDrawPath, oldLastBezierLastHalfSegmentPoints);
                                    }

                                }

                                long drawStartTime = System.currentTimeMillis();

                                //擦除上一根线的毛边
                                BrushRender.eraseStroke(canvas, mPathPreviousBezierLastHalfMap.get(_toDrawPath), strokeWidth, 0);

                                //绘制加密后的线
                                BrushRender.drawStroke(canvas, renderPaint, intensivePoints, strokeWidth, 0);

                                Log.d(TAG, "擦除+绘制一次 耗时=" + (System.currentTimeMillis() - drawStartTime));

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

    synchronized void startEraserThread() {
        is2StopEraser = false;
        if (isEraserRunning) {
            return;
        }
        isEraserRunning = true;

        JobExecutor.getInstance().execute(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "startEraserThread ThreadName=" + Thread.currentThread().getName());

                while (!is2StopEraser) {

                    try {
                        if (!isErase) {
                            eraserWaitGo.wait1();
                            continue;
                        }
                        isErase = false;

                        long millis = System.currentTimeMillis();

                        for (int i = 0; i < FRAME_CACHE_SIZE; i++) {
                            Canvas canvas = getHolder().lockCanvas();
                            if (canvas != null) {
                                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                getHolder().unlockCanvasAndPost(canvas);
                            } else {
                                Log.e(TAG, "clearScreenAfterSurfaceViewCreated 失败!");
                            }
                        }

                        Log.d(TAG, "doErase consuming time " + (System.currentTimeMillis() - millis));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                isRenderRunning = false;
                Log.d(TAG, "startEraserThread 停止擦除线程成功 ThreadName=" + Thread.currentThread().getName());
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


    synchronized void stopRenderThread() {
        if (!isRenderRunning) return;
        is2StopRender = true;
        renderWaitGo.go();
    }

    synchronized void stopEraserThread() {
        if (!isEraserRunning) return;
        is2StopEraser = true;
        eraserWaitGo.go();
    }

    private void initRenderPaint() {
        if (renderPaint != null) return;
        renderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        renderPaint.setStrokeJoin(Paint.Join.ROUND);
        renderPaint.setStrokeCap(Paint.Cap.ROUND);
        renderPaint.setStrokeWidth(strokeWidth);
        renderPaint.setStyle(Paint.Style.STROKE);
        renderPaint.setColor(strokeColor);
    }


    private void renderPath() {
        Log.d(TAG, "renderPath start");

        if (mLast16PathQueue.size() == FRAME_CACHE_SIZE) {
            TouchPointList touchPointList = mLast16PathQueue.poll();
            mPathIntensivePointsMap.remove(touchPointList);
            mPathPreviousBezierLastHalfMap.remove(touchPointList);
        }
        TouchPointList lastTouchPointList = new TouchPointList(activeTouchPointList.size());
        lastTouchPointList.addAll(activeTouchPointList);
        lastTouchPointList.setTimeStamp(activeTouchPointList.getTimeStamp());
        mLast16PathQueue.add(lastTouchPointList);

        isRendering = true;
        renderWaitGo.go();
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
            startEraserThread();
            reproduceScribblesAfterSurfaceRecreated();
        } else {
            stopRenderThread();
            stopEraserThread();
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

        clearScreen();

    }

    private void clearScreen() {
        isErase = true;
        eraserWaitGo.go();
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
        isRendering = true;
        renderWaitGo.go();
    }

}
