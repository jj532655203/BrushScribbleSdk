package com.jj.brush_scribble_sdk;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import com.blankj.utilcode.util.ObjectUtils;
import com.jj.brush_scribble_sdk.data.TouchPoint;

import java.util.ArrayList;
import java.util.List;

public class BrushRender {

    private static final String TAG = "BrushRender";
    private static final float SIZE_RATIO = 1f;

    //t<0-1>
    private static final float[] T = new float[]{0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F, 0.7F, 0.8F, 0.9F};

    private static Paint eraserPaint;

    /**
     * 用加密过的笔迹点集合绘制
     *
     * @param intensivePoints 加密过的点集合
     */
    public static void drawStroke(Canvas canvas, Paint paint, List<TouchPoint> intensivePoints, float strokeWidth, float maxTouchPressure) {
        if (ObjectUtils.isEmpty(intensivePoints) || intensivePoints.size() < 6 || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
            Log.e(TAG, "drawStroke 有传参为空");
            return;
        }
        Log.d(TAG, "drawStroke");


        Paint.Style var5 = paint.getStyle();
        float var6 = paint.getStrokeWidth();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1.0F);

        for (int i = 0; i < intensivePoints.size() - 1; ++i) {
            TouchPoint pointI = intensivePoints.get(i);
            TouchPoint pointI1 = intensivePoints.get(i + 1);
            paint.setStrokeWidth(pointI1.size);
            canvas.drawLine(pointI.x, pointI.y, pointI1.x, pointI1.y, paint);
        }

        paint.setStyle(var5);
        paint.setStrokeWidth(var6);

    }

    /**
     * intensivePoints经过加密并oldPoints.size>=3时使用
     *
     * @param increasePoint   当前笔迹线新增点
     * @param oldPoints       当前笔迹线原有点集合
     * @param intensivePoints oldPoints经过加密后的点集合
     */
    public static List<List<TouchPoint>> intensiveByIncrease(Canvas canvas, Paint paint, TouchPoint increasePoint, List<TouchPoint> oldPoints, List<TouchPoint> intensivePoints, float strokeWidth, float maxTouchPressure) {
        if (ObjectUtils.isEmpty(intensivePoints) || intensivePoints.size() < 6 || ObjectUtils.isEmpty(oldPoints) || oldPoints.size() < 3
                || ObjectUtils.isEmpty(increasePoint) || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
            Log.e(TAG, "intensiveByIncrease 有传参为空");
            return null;
        }
        Log.d(TAG, "intensiveByIncrease");

        //将intensivePoints中最后bezier后半段加密点都去掉
        List<TouchPoint> oldLastBezierLastHalfSegment = new ArrayList<>(6);
        int intensivePointSize = intensivePoints.size();
        for (int i = 0; i < 6; i++) {
            TouchPoint lasti = intensivePoints.remove(intensivePointSize - i - 1);
            oldLastBezierLastHalfSegment.add(lasti);
        }

        List<TouchPoint> increaseIntensivePoints = new ArrayList<>(intensivePoints);

        List<List<TouchPoint>> bezier = intensiveBezier(oldPoints.get(oldPoints.size() - 2), oldPoints.get(oldPoints.size() - 1), increasePoint);
        List<TouchPoint> mergeBezierOverlap = mergeBezierOverlap(oldLastBezierLastHalfSegment, bezier.get(0));

        increaseIntensivePoints.addAll(mergeBezierOverlap);

        increaseIntensivePoints.addAll(bezier.get(1));

        List<List<TouchPoint>> arr = new ArrayList<>(2);
        arr.add(increaseIntensivePoints);
        arr.add(oldLastBezierLastHalfSegment);
        return arr;
    }

    /**
     * points未经过加密时使用;
     * 或points.size<=3时使用
     *
     * @return 加密后的点集合
     */
//    public static List<TouchPoint> drawStroke(Canvas canvas, Paint paint, List<TouchPoint> points, float strokeWidth, float maxTouchPressure) {
//        if (ObjectUtils.isEmpty(points) || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
//            Log.e(TAG, "drawStroke 有传参为空");
//            return null;
//        }
//        Log.d(TAG, "drawStroke");
//
//        int pointSize = points.size();
//        if (pointSize < 1) {
//            return null;
//        } else if (pointSize == 1 || pointSize == 2) {
//            TouchPoint point = points.get(0);
//            Paint.Style oldStyle = paint.getStyle();
//            float oldStrokeWidth = paint.getStrokeWidth();
//            paint.setStyle(Paint.Style.FILL);
//
//            if (pointSize == 1) {
//                //单点
//                canvas.drawPoint(point.x, point.y, paint);
//            } else {
//                //2点
//                canvas.drawLine(point.x, point.y, points.get(1).x, points.get(1).y, paint);
//            }
//
//            paint.setStyle(oldStyle);
//            paint.setStrokeWidth(oldStrokeWidth);
//            return points;
//        }
//
//
//        List<TouchPoint> var7 = computeStrokePoints(points);
//
//        drawStroke(canvas, paint, var7, strokeWidth, maxTouchPressure);
//
//        return var7;
//    }
//    public static List<TouchPoint> computeStrokePoints(List<TouchPoint> points) {
//        long millis = System.currentTimeMillis();
//        List<TouchPoint> var4 = intensive(points);
//
//        Log.d(TAG, "intensive 耗时=" + (System.currentTimeMillis() - millis));
//
//        return var4;
//    }

//    private static ArrayList<TouchPoint> createTouchPointsByFloats(List<Float> var3) {
//        ArrayList<TouchPoint> var4 = new ArrayList<>();
//
//        for (int var5 = 0; var5 < var3.size(); var5 += 3) {
//            float var6 = var3.get(var5);
//            float var7 = var3.get(var5 + 1);
//            float var8 = var3.get(var5 + 2);
//            var4.add(new TouchPoint(var6, var7, 0.0F, var8, 0L));
//        }
//        return var4;
//    }
    public static List<TouchPoint> intensive(List<TouchPoint> points) {

        //将所有bezier加密
        List<List<List<TouchPoint>>> bezierList = new ArrayList<>();
        for (int i = 0; i < points.size() - 2; i++) {
            bezierList.add(intensiveBezier(points.get(i), points.get(i + 1), points.get(i + 2)));
        }

        List<List<TouchPoint>> mergeBezierList = new ArrayList<>();

        //将2根相邻的bezier重叠处(依权重)合并
        for (int i = 0; i < bezierList.size() - 1; i++) {
            List<TouchPoint> iLastHalfSegment = bezierList.get(i).get(1);
            List<TouchPoint> iPlusFistHalfSegment = bezierList.get(i + 1).get(0);

            //4-9merge
            List<TouchPoint> mergeFloats = mergeBezierOverlap(iLastHalfSegment, iPlusFistHalfSegment);

            mergeBezierList.add(mergeFloats);
        }

        List<TouchPoint> computeResult;
        //添加首bezier前半段
        computeResult = new ArrayList<>(bezierList.get(0).get(0));
        //添加合并bezier
        for (List<TouchPoint> list : mergeBezierList) {
            computeResult.addAll(list);
        }
        //添加尾bezier后半段
        computeResult.addAll(bezierList.get(bezierList.size() - 1).get(1));

        return computeResult;
    }

    /**
     * merge相邻两根bezier重叠的部分
     * 原理:2重叠bezier重心与起始点再绘bezier
     *
     * @param iLastHalfSegment     第一根bezier的后半段
     * @param iPlusFistHalfSegment 第二个bezier的前半段
     * @return merge后的合并点
     */
    private static List<TouchPoint> mergeBezierOverlap(List<TouchPoint> iLastHalfSegment, List<TouchPoint> iPlusFistHalfSegment) {

        TouchPoint iLastHalfSegmentPoint0 = iLastHalfSegment.get(0);
        TouchPoint iLastHalfSegmentPoint6 = iLastHalfSegment.get(5);
        TouchPoint iPlusFistHalfSegmentPoint0 = iPlusFistHalfSegment.get(0);
        TouchPoint iPlusFistHalfSegmentPoint6 = iPlusFistHalfSegment.get(5);

        TouchPoint p1 = new TouchPoint();
        p1.x = (iLastHalfSegmentPoint0.x + iLastHalfSegmentPoint6.x + iPlusFistHalfSegmentPoint0.x + iPlusFistHalfSegmentPoint6.x) / 4;
        p1.y = (iLastHalfSegmentPoint0.y + iLastHalfSegmentPoint6.y + iPlusFistHalfSegmentPoint0.y + iPlusFistHalfSegmentPoint6.y) / 4;
        p1.size = (iLastHalfSegmentPoint0.size + iLastHalfSegmentPoint6.size + iPlusFistHalfSegmentPoint0.size + iPlusFistHalfSegmentPoint6.size) / 4;

        TouchPoint p0 = new TouchPoint(iLastHalfSegmentPoint0.x, iLastHalfSegmentPoint0.y, 0, iLastHalfSegmentPoint0.size, 0);

        TouchPoint p2 = new TouchPoint(iPlusFistHalfSegmentPoint6.x, iPlusFistHalfSegmentPoint6.y, 0, iPlusFistHalfSegmentPoint6.size, 0);

        List<List<TouchPoint>> intensiveBezier = intensiveBezier(p0, p1, p2);

        List<TouchPoint> mergeFloats = new ArrayList<>();
        mergeFloats.addAll(intensiveBezier.get(0));
        mergeFloats.addAll(intensiveBezier.get(1));
//        List<TouchPoint> firstHalfFloats = intensiveBezier.get(0);
//        System.arraycopy(firstHalfFloats, 0, mergeFloats, 0, firstHalfFloats.size());
//        List<TouchPoint> lastHalfFloats = intensiveBezier.get(1);
//        System.arraycopy(lastHalfFloats, 0, mergeFloats, firstHalfFloats.size(), lastHalfFloats.size());

        return mergeFloats;
    }

    /**
     * merge相邻两根bezier重叠的部分
     * 权重思想(有bug)
     *
     * @param iLastHalfSegment     第一根bezier的后半段
     * @param iPlusFistHalfSegment 第二个bezier的前半段
     * @return merge后的前半段点集合和后半段点集合
     */
//    private static float[] mergeBezierOverlapOld(float[] iLastHalfSegment, float[] iPlusFistHalfSegment) {
//        float[] mergeFloats = new float[12];
//        mergeFloats[0] = iLastHalfSegment[3] * 0.8F + iPlusFistHalfSegment[3] * 0.2F;
//        mergeFloats[1] = iLastHalfSegment[4] * 0.8F + iPlusFistHalfSegment[4] * 0.2F;
//        mergeFloats[2] = iLastHalfSegment[5] * 0.8F + iPlusFistHalfSegment[5] * 0.2F;
//        mergeFloats[3] = iLastHalfSegment[6] * 0.6F + iPlusFistHalfSegment[6] * 0.4F;
//        mergeFloats[4] = iLastHalfSegment[7] * 0.6F + iPlusFistHalfSegment[7] * 0.4F;
//        mergeFloats[5] = iLastHalfSegment[8] * 0.6F + iPlusFistHalfSegment[8] * 0.4F;
//        mergeFloats[6] = iLastHalfSegment[9] * 0.4F + iPlusFistHalfSegment[9] * 0.6F;
//        mergeFloats[7] = iLastHalfSegment[10] * 0.4F + iPlusFistHalfSegment[10] * 0.6F;
//        mergeFloats[8] = iLastHalfSegment[11] * 0.4F + iPlusFistHalfSegment[11] * 0.6F;
//        mergeFloats[9] = iLastHalfSegment[12] * 0.2F + iPlusFistHalfSegment[12] * 0.8F;
//        mergeFloats[10] = iLastHalfSegment[13] * 0.2F + iPlusFistHalfSegment[13] * 0.8F;
//        mergeFloats[11] = iLastHalfSegment[14] * 0.2F + iPlusFistHalfSegment[14] * 0.8F;
//        return mergeFloats;
//    }
    private static List<List<TouchPoint>> intensiveBezier(TouchPoint point0, TouchPoint point1, TouchPoint point2) {

        //曲线公式 =  Math.pow(1 - t,2) * p0 + 2 * t * (1-t) *p1 + Math.bow(t,2) * p3;

        //0-5
        List<TouchPoint> fistHalfSegment = new ArrayList<>(6);
        fistHalfSegment.add(new TouchPoint(point0.x, point0.y, 0, point0.size, 0));
        //5-10
        List<TouchPoint> lastHalfSegment = new ArrayList<>(6);

        List<TouchPoint> data = new ArrayList<>(9);
        float sizeOffset = point2.size - point0.size;
        for (int i = 0; i < T.length; i++) {
            float x = (float) (Math.pow(1 - T[i], 2) * point0.x + 2 * T[i] * (1 - T[i]) * point1.x + Math.pow(T[i], 2) * point2.x);
            float y = (float) (Math.pow(1 - T[i], 2) * point0.y + 2 * T[i] * (1 - T[i]) * point1.y + Math.pow(T[i], 2) * point2.y);
            float size = point0.size + sizeOffset * i / 10;
            data.add(new TouchPoint(x, y, 0, size, 0));
        }
        for (int i = 0; i < data.size(); i++) {
            TouchPoint pointI = data.get(i);
            if (i == 4) {
                //5
                fistHalfSegment.add(pointI);
                lastHalfSegment.add(pointI);
            } else if (i < 4) {
                //1-4
                fistHalfSegment.add(pointI);
            } else {
                //6-9
                lastHalfSegment.add(pointI);
            }
        }

        lastHalfSegment.add(new TouchPoint(point2.x, point2.y, 0, point2.size, 0));

        List<List<TouchPoint>> list = new ArrayList<>();
        list.add(fistHalfSegment);
        list.add(lastHalfSegment);

        return list;
    }

    public static void eraseStroke(Canvas canvas, List<TouchPoint> oldLastBezierLastHalfSegmentPoints, float strokeWidth, int maxPresure) {
        if (canvas == null || ObjectUtils.isEmpty(oldLastBezierLastHalfSegmentPoints)) {
            Log.e(TAG, "eraseStroke");
            return;
        }

        Log.d(TAG, "eraseStroke oldLastBezierLastHalfSegmentPoints.size=" + oldLastBezierLastHalfSegmentPoints.size());

        if (eraserPaint == null) {
            eraserPaint = getEraserPaint();
        }
        eraserPaint.setStrokeWidth(strokeWidth);

        for (int var8 = 0; var8 < oldLastBezierLastHalfSegmentPoints.size() - 1; ++var8) {
//            eraserPaint.setStrokeWidth(oldLastBezierLastHalfSegmentPoints.get(var8 + 1).size);
            canvas.drawLine(oldLastBezierLastHalfSegmentPoints.get(var8).x, oldLastBezierLastHalfSegmentPoints.get(var8).y, oldLastBezierLastHalfSegmentPoints.get(var8 + 1).x, oldLastBezierLastHalfSegmentPoints.get(var8 + 1).y, eraserPaint);
        }

    }

    public static Paint getEraserPaint() {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeMiter(4.0f);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        return paint;
    }

}
