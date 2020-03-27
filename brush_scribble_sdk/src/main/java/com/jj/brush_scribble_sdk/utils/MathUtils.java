package com.jj.brush_scribble_sdk.utils;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    private static final String TAG = "MathUtils";

    /**
     * 求2圆的2条外公切线的4个切点
     *
     * @param circleCenter0 圆0的圆心
     * @param circleCenter1 圆1的圆心
     */
    public static List<PointF[]> get4PointFOf2OutsideTan(PointF circleCenter0, PointF circleCenter1, float r) {


        if (Float.compare(circleCenter0.x, circleCenter1.x) == 0 && Float.compare(circleCenter1.x, circleCenter1.x) == 0) {
            Log.e(TAG, "2圆重合 return");
            return null;
        }

        if (r <= 0) {
            Log.e(TAG, "2圆半径必须>0 return");
            return null;
        }

        //2圆心连线的斜率
        float circleLineM = (circleCenter1.y - circleCenter0.y) / (circleCenter1.x - circleCenter0.x);
        Log.d(TAG, "get4PointFOf2OutsideTan circleLineM=" + circleLineM);

        //圆1直径线和圆2直径线(垂直于2圆心连线)的斜率:与m相乘=-1
        float m = -1 / circleLineM;
        Log.d(TAG, "get4PointFOf2OutsideTan m=" + m);

        //圆0直径线系数k0
        float k0 = circleCenter0.y + circleCenter0.x / m;
        //圆1直径线系数k1
        float k1 = circleCenter1.y + circleCenter1.x / m;
        Log.d(TAG, "get4PointFOf2OutsideTan k0=" + k0 + "?k1=" + k1);

        //圆0的圆心坐标(a0,b0)
        float a0 = circleCenter0.x;
        float b0 = circleCenter0.y;
        //圆1的圆心坐标(a1,b1)
        float a1 = circleCenter1.x;
        float b1 = circleCenter1.y;

        //圆0右侧外公切点(activeX0,activeY0)
        //圆0左侧外公切点(negativeX0,negativeY0)
        double sqrtInner0 = (Math.pow(r, 2) - Math.pow(a0, 2) - Math.pow(k0 - b0, 2)) / (Math.pow(m, 2) + 1)/*前半段*/ + Math.pow((m * a0 + k0 - b0) / (Math.pow(m, 2) + 1), 2);
        double sqrt0 = Math.sqrt(sqrtInner0);
        Log.d(TAG, "get4PointFOf2OutsideTan sqrtInner0=" + sqrtInner0 + "?sqrt0=" + sqrt0);

        double activeX0 = m * (a0 * m + k0 - b0) / (Math.pow(m, 2) + 1) /*前半段*/ + m * sqrt0;
        double activeY0 = k0 - activeX0 / m;
        Log.d(TAG, "get4PointFOf2OutsideTan activeX0=" + activeX0 + "?activeY0=" + activeY0);

        double negativeX0 = m * (a0 * m + k0 - b0) / (Math.pow(m, 2) + 1) /*前半段*/ - m * sqrt0;
        double negativeY0 = k0 - negativeX0 / m;
        Log.d(TAG, "get4PointFOf2OutsideTan negativeX0=" + negativeX0 + "?negativeY0=" + negativeY0);

        //圆1右侧外公切点(activeX1,activeY1)
        //圆1左侧外公切点(negativeX1,negativeY1)
        double sqrtInner1 = (Math.pow(r, 2) - Math.pow(a1, 2) - Math.pow(k1 - b1, 2)) / (Math.pow(m, 2) + 1)/*前半段*/ + Math.pow((m * a1 + k1 - b1) / (Math.pow(m, 2) + 1), 2);
        double sqrt1 = Math.sqrt(sqrtInner1);
        Log.d(TAG, "get4PointFOf2OutsideTan sqrtInner1=" + sqrtInner1 + "?sqrt1=" + sqrt1);

        double activeX1 = m * (a1 * m + k1 - b1) / (Math.pow(m, 2) + 1) /*前半段*/ + m * sqrt1;
        double activeY1 = k1 - activeX1 / m;
        Log.d(TAG, "get4PointFOf2OutsideTan activeX1=" + activeX1 + "?activeY1=" + activeY1);

        double negativeX1 = m * (a1 * m + k1 - b1) / (Math.pow(m, 2) + 1) /*前半段*/ - m * sqrt1;
        double negativeY1 = k1 - negativeX1 / m;
        Log.d(TAG, "get4PointFOf2OutsideTan negativeX1=" + negativeX1 + "?negativeY1=" + negativeY1);


        PointF negative0 = new PointF((float) negativeX0, (float) negativeY0);
        PointF active0 = new PointF((float) activeX0, (float) activeY0);

        PointF negative1 = new PointF((float) negativeX1, (float) negativeY1);
        PointF active1 = new PointF((float) activeX1, (float) activeY1);


        //x轴最小点
        double minx = activeX0;
        PointF fistPoint = active0;
        if (minx > activeX1) {
            minx = activeX1;
            fistPoint = active1;
        }
        if (minx > negativeX0) {
            minx = negativeX0;
            fistPoint = negative0;
        }
        if (minx > negativeX1) {
            fistPoint = negative1;
        }
        //y轴最大点
        double maxy = activeY0;
        PointF secondPoint = active0;
        if (maxy < activeY1) {
            maxy = activeY1;
            secondPoint = active1;
        }
        if (maxy < negativeY0) {
            maxy = negativeY0;
            secondPoint = negative0;
        }
        if (maxy < negativeY1) {
            secondPoint = negative1;
        }

        //x轴最大点
        double maxx = activeX0;
        PointF thirdPoint = active0;
        if (maxx < activeX1) {
            maxx = activeX1;
            thirdPoint = active1;
        }
        if (maxx < negativeX0) {
            maxx = negativeX0;
            thirdPoint = negative0;
        }
        if (maxx < negativeX1) {
            thirdPoint = negative1;
        }
        //剩下的点
        PointF fourPoint;
        ArrayList<PointF> pointFS1 = new ArrayList<>(3);
        pointFS1.add(fistPoint);
        pointFS1.add(secondPoint);
        pointFS1.add(thirdPoint);
        if (!pointFS1.contains(active0)) {
            fourPoint = active0;
        } else if (!pointFS1.contains(active1)) {
            fourPoint = active1;
        } else if (!pointFS1.contains(negative0)) {
            fourPoint = negative0;
        } else {
            fourPoint = negative1;
        }

        List<PointF[]> pointFS = new ArrayList<>(2);

        PointF[] twoPointFCircle0 = new PointF[2];
        twoPointFCircle0[0] = fistPoint;
        twoPointFCircle0[1] = secondPoint;
        PointF[] twoPointFCircle1 = new PointF[2];
        twoPointFCircle1[1] = thirdPoint;
        twoPointFCircle1[0] = fourPoint;

        pointFS.add(twoPointFCircle0);
        pointFS.add(twoPointFCircle1);

        return pointFS;
    }

}
