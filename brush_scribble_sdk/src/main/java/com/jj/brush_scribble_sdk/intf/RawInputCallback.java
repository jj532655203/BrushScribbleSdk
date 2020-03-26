package com.jj.brush_scribble_sdk.intf;

import com.jj.brush_scribble_sdk.data.TouchPoint;
import com.jj.brush_scribble_sdk.data.TouchPointList;

public abstract class RawInputCallback {

    public RawInputCallback() {
    }

    public abstract void onBeginRawDrawing(TouchPoint var2);

    public abstract void onEndRawDrawing(TouchPoint var2);

    public abstract void onRawDrawingTouchPointMoveReceived(TouchPoint var1);

    public abstract void onRawDrawingTouchPointListReceived(TouchPointList var1);

}
