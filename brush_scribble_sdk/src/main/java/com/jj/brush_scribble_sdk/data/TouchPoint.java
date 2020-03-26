package com.jj.brush_scribble_sdk.data;

import android.view.MotionEvent;

import java.io.Serializable;

public class TouchPoint implements Serializable, Cloneable {
    public float x;
    public float y;
    public float pressure;
    public float size;
    public long timestamp;

    public TouchPoint() {
    }

    public TouchPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public TouchPoint(float px, float py, float p, float s, long ts) {
        this.x = px;
        this.y = py;
        this.pressure = p;
        this.size = s;
        this.timestamp = ts;
    }

    public TouchPoint(MotionEvent motionEvent) {
        this.x = motionEvent.getX();
        this.y = motionEvent.getY();
        this.pressure = motionEvent.getPressure();
        this.size = motionEvent.getSize();
        this.timestamp = motionEvent.getEventTime();
    }

    public TouchPoint(TouchPoint source) {
        this.x = source.getX();
        this.y = source.getY();
        this.pressure = source.getPressure();
        this.size = source.getSize();
        this.timestamp = source.getTimestamp();
    }

    public void set(TouchPoint point) {
        this.x = point.x;
        this.y = point.y;
        this.pressure = point.pressure;
        this.size = point.size;
        this.timestamp = point.timestamp;
    }

    public void offset(int dx, int dy) {
        this.x += (float) dx;
        this.y += (float) dy;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getPressure() {
        return this.pressure;
    }

    public float getSize() {
        return this.size;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public TouchPoint clone() throws CloneNotSupportedException {
        TouchPoint var1 = (TouchPoint) super.clone();
        return var1;
    }
    public String toString() {
        return "x:" + this.x + " y:" + this.y;
    }

}
