package com.jj.brush_scribble_sdk;

import android.graphics.Canvas;
import android.graphics.Paint;
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

    /**
     * 用加密过的笔迹点集合绘制
     */
    public static void drawStrokeWithoutIntensive(Canvas canvas, Paint paint, List<TouchPoint> intensivePoints, float strokeWidth, float maxTouchPressure) {
        if (ObjectUtils.isEmpty(intensivePoints) || intensivePoints.size() < 6 || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
            Log.e(TAG, "drawStrokeWithoutIntensive 有传参为空");
            return;
        }
        Log.d(TAG, "drawStrokeWithoutIntensive");


        Paint.Style var5 = paint.getStyle();
        float var6 = paint.getStrokeWidth();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1.0F);

        for (int var8 = 0; var8 < intensivePoints.size() - 1; ++var8) {
            paint.setStrokeWidth(intensivePoints.get(var8 + 1).size);
            canvas.drawLine(intensivePoints.get(var8).x, intensivePoints.get(var8).y, intensivePoints.get(var8 + 1).x, intensivePoints.get(var8 + 1).y, paint);
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
    public static List<TouchPoint> computeStrokePoints(Canvas canvas, Paint paint, TouchPoint increasePoint, List<TouchPoint> oldPoints, List<TouchPoint> intensivePoints, float strokeWidth, float maxTouchPressure) {
        if (ObjectUtils.isEmpty(intensivePoints) || intensivePoints.size() < 6 || ObjectUtils.isEmpty(oldPoints) || oldPoints.size() < 3
                || ObjectUtils.isEmpty(increasePoint) || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
            Log.e(TAG, "computeStrokePoints 有传参为空");
            return null;
        }
        Log.d(TAG, "computeStrokePoints");

        //将intensivePoints中最后bezier后半段加密点都去掉
        float[] oldLastBezierLastHalfSegment = new float[18];
        int intensivePointSize = intensivePoints.size();
        for (int i = 0; i < 6; i++) {
            TouchPoint lasti = intensivePoints.remove(intensivePointSize - i - 1);
            oldLastBezierLastHalfSegment[i * 3] = lasti.x;
            oldLastBezierLastHalfSegment[i * 3 + 1] = lasti.y;
            oldLastBezierLastHalfSegment[i * 3 + 2] = lasti.size;
        }

        List<float[]> bezier = intensiveBezier(oldPoints.get(oldPoints.size() - 2), oldPoints.get(oldPoints.size() - 1), increasePoint);
        float[] mergeBezierOverlap = mergeBezierOverlap(oldLastBezierLastHalfSegment, bezier.get(0));

        List<TouchPoint> var7 = new ArrayList<>(intensivePoints);

        ArrayList<Float> mergeBezierOverlapFloats = new ArrayList<>();
        for (Float _float : mergeBezierOverlap) {
            mergeBezierOverlapFloats.add(_float);
        }
        var7.addAll(createTouchPointsByFloats(mergeBezierOverlapFloats));

        ArrayList<Float> bezierFloats = new ArrayList<>();
        for (Float _float : bezier.get(1)) {
            bezierFloats.add(_float);
        }
        var7.addAll(createTouchPointsByFloats(bezierFloats));

//        drawStrokeWithoutIntensive(canvas, paint, var7, strokeWidth, maxTouchPressure);

        return var7;
    }

    /**
     * points未经过加密时使用;
     * 或points.size<=3时使用
     *
     * @return 加密后的点集合
     */
    public static List<TouchPoint> drawStroke(Canvas canvas, Paint paint, List<TouchPoint> points, float strokeWidth, float maxTouchPressure) {
        if (ObjectUtils.isEmpty(points) || ObjectUtils.isEmpty(canvas) || ObjectUtils.isEmpty(paint)) {
            Log.e(TAG, "drawStroke 有传参为空");
            return null;
        }
        Log.d(TAG, "drawStroke");

        int pointSize = points.size();
        if (pointSize < 1) {
            return null;
        } else if (pointSize == 1 || pointSize == 2) {
            TouchPoint point = points.get(0);
            Paint.Style oldStyle = paint.getStyle();
            float oldStrokeWidth = paint.getStrokeWidth();
            paint.setStyle(Paint.Style.FILL);

            if (pointSize == 1) {
                //单点
                canvas.drawPoint(point.x, point.y, paint);
            } else {
                //2点
                canvas.drawLine(point.x, point.y, points.get(1).x, points.get(1).y, paint);
            }

            paint.setStyle(oldStyle);
            paint.setStrokeWidth(oldStrokeWidth);
            return points;
        }


        List<TouchPoint> var7 = computeStrokePoints(points);

        drawStrokeWithoutIntensive(canvas, paint, var7, strokeWidth, maxTouchPressure);

        return var7;
    }

    public static List<TouchPoint> computeStrokePoints(List<TouchPoint> points) {
        long millis = System.currentTimeMillis();
        List<Float> var3 = intensive(points);
        ArrayList<TouchPoint> var4 = createTouchPointsByFloats(var3);

        Log.d(TAG, "intensive 耗时=" + (System.currentTimeMillis() - millis));

        return var4;
    }

    private static ArrayList<TouchPoint> createTouchPointsByFloats(List<Float> var3) {
        ArrayList<TouchPoint> var4 = new ArrayList<>();

        for (int var5 = 0; var5 < var3.size(); var5 += 3) {
            float var6 = var3.get(var5);
            float var7 = var3.get(var5 + 1);
            float var8 = var3.get(var5 + 2);
            var4.add(new TouchPoint(var6, var7, 0.0F, var8, 0L));
        }
        return var4;
    }

    private static List<Float> intensive(List<TouchPoint> points) {

        //将所有bezier加密
        List<List<float[]>> bezierList = new ArrayList<>();
        for (int i = 0; i < points.size() - 2; i++) {
            bezierList.add(intensiveBezier(points.get(i), points.get(i + 1), points.get(i + 2)));
        }

        List<float[]> mergeBezierList = new ArrayList<>();

        //将2根相邻的bezier重叠处(依权重)合并
        for (int i = 0; i < bezierList.size() - 1; i++) {
            float[] iLastHalfSegment = bezierList.get(i).get(1);
            float[] iPlusFistHalfSegment = bezierList.get(i + 1).get(0);

            //4-9merge
            float[] mergeFloats = mergeBezierOverlap(iLastHalfSegment, iPlusFistHalfSegment);

            mergeBezierList.add(mergeFloats);
        }

        List<Float> computeResult = new ArrayList<>();

        //添加首bezier前半段
        for (float _float : bezierList.get(0).get(0)) {
            computeResult.add(_float);
        }
        //添加合并bezier
        for (float[] arr : mergeBezierList) {
            for (float _float : arr) {
                computeResult.add(_float);
            }
        }
        //添加尾bezier后半段
        for (float _float : bezierList.get(bezierList.size() - 1).get(1)) {
            computeResult.add(_float);
        }
        return computeResult;
    }

    /**
     * merge相邻两根bezier重叠的部分
     *
     * @param iLastHalfSegment     第一根bezier的后半段
     * @param iPlusFistHalfSegment 第二个bezier的前半段
     * @return merge后的合并点
     */
    private static float[] mergeBezierOverlap(float[] iLastHalfSegment, float[] iPlusFistHalfSegment) {
        float[] mergeFloats = new float[12];
        mergeFloats[0] = iLastHalfSegment[3] * 0.8F + iPlusFistHalfSegment[3] * 0.2F;
        mergeFloats[1] = iLastHalfSegment[4] * 0.8F + iPlusFistHalfSegment[4] * 0.2F;
        mergeFloats[2] = iLastHalfSegment[5] * 0.8F + iPlusFistHalfSegment[5] * 0.2F;
        mergeFloats[3] = iLastHalfSegment[6] * 0.6F + iPlusFistHalfSegment[6] * 0.4F;
        mergeFloats[4] = iLastHalfSegment[7] * 0.6F + iPlusFistHalfSegment[7] * 0.4F;
        mergeFloats[5] = iLastHalfSegment[8] * 0.6F + iPlusFistHalfSegment[8] * 0.4F;
        mergeFloats[6] = iLastHalfSegment[9] * 0.4F + iPlusFistHalfSegment[9] * 0.6F;
        mergeFloats[7] = iLastHalfSegment[10] * 0.4F + iPlusFistHalfSegment[10] * 0.6F;
        mergeFloats[8] = iLastHalfSegment[11] * 0.4F + iPlusFistHalfSegment[11] * 0.6F;
        mergeFloats[9] = iLastHalfSegment[12] * 0.2F + iPlusFistHalfSegment[12] * 0.8F;
        mergeFloats[10] = iLastHalfSegment[13] * 0.2F + iPlusFistHalfSegment[13] * 0.8F;
        mergeFloats[11] = iLastHalfSegment[14] * 0.2F + iPlusFistHalfSegment[14] * 0.8F;
        return mergeFloats;
    }

    private static List<float[]> intensiveBezier(TouchPoint point0, TouchPoint point1, TouchPoint point2) {

        //曲线公式 =  Math.pow(1 - t,2) * p0 + 2 * t * (1-t) *p1 + Math.bow(t,2) * p3;

        //0-5
        float[] fistHalfSegment = new float[18];
        fistHalfSegment[0] = point0.x;
        fistHalfSegment[1] = point0.y;
        fistHalfSegment[2] = point0.size;
        //5-10
        float[] lastHalfSegment = new float[18];
        lastHalfSegment[15] = point2.x;
        lastHalfSegment[16] = point2.y;
        lastHalfSegment[17] = point2.size;

        float[] data = new float[27];
        float sizeOffset = point2.size - point0.size;
        for (int i = 0; i < T.length; i++) {
            data[3 * i] = (float) (Math.pow(1 - T[i], 2) * point0.x + 2 * T[i] * (1 - T[i]) * point1.x + Math.pow(T[i], 2) * point2.x);
            data[3 * i + 1] = (float) (Math.pow(1 - T[i], 2) * point0.y + 2 * T[i] * (1 - T[i]) * point1.y + Math.pow(T[i], 2) * point2.y);
            data[3 * i + 2] = point0.size + sizeOffset * i / 10;
        }
        for (int i = 0; i < data.length; i++) {
            if (i >= 12 && i < 15) {
                //5
                fistHalfSegment[i + 3] = data[i];
                lastHalfSegment[i - 12] = data[i];
            } else if (i < 12) {
                //1-4
                fistHalfSegment[i + 3] = data[i];
            } else {
                //6-9
                lastHalfSegment[i - 12] = data[i];
            }
        }

        ArrayList<float[]> list = new ArrayList<>();
        list.add(fistHalfSegment);
        list.add(lastHalfSegment);

        return list;
    }

}
