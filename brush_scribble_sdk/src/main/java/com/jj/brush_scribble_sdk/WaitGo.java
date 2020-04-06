package com.jj.brush_scribble_sdk;

/**
 * Jay
 * 让当前线程等待或唤醒
 */

public class WaitGo {
    public synchronized void wait1() throws InterruptedException {
        wait();
    }

    public synchronized void wait1(long timeout) throws InterruptedException {
        wait(timeout);
    }

    public synchronized void go() {
        notify();
    }

}