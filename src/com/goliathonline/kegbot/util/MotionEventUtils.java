package com.goliathonline.kegbot.util;

import android.view.MotionEvent;

/**
 * A utility class that emulates multitouch APIs available in Android 2.0+.
 */
public class MotionEventUtils {
    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_POINTER_UP = 0x6;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    public static boolean sMultiTouchApiAvailable;

    static {
        try {
            MotionEvent.class.getMethod("getPointerId", new Class[]{int.class});
            sMultiTouchApiAvailable = true;
        } catch (NoSuchMethodException nsme) {
            sMultiTouchApiAvailable = false;
        }
    }

    public static float getX(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getX(ev, pointerIndex);
        } else {
            return ev.getX();
        }
    }

    public static float getY(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getY(ev, pointerIndex);
        } else {
            return ev.getY();
        }
    }

    public static int getPointerId(MotionEvent ev, int pointerIndex) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.getPointerId(ev, pointerIndex);
        } else {
            return 0;
        }
    }

    public static int findPointerIndex(MotionEvent ev, int pointerId) {
        if (sMultiTouchApiAvailable) {
            return WrappedStaticMotionEvent.findPointerIndex(ev, pointerId);
        } else {
            return (pointerId == 0) ? 0 : -1;
        }
    }

    /**
     * A wrapper around newer (SDK level 5) MotionEvent APIs. This class only gets loaded
     * if it is determined that these new APIs exist on the device.
     */
    private static class WrappedStaticMotionEvent {
        public static float getX(MotionEvent ev, int pointerIndex) {
            return ev.getX(pointerIndex);
        }

        public static float getY(MotionEvent ev, int pointerIndex) {
            return ev.getY(pointerIndex);
        }

        public static int getPointerId(MotionEvent ev, int pointerIndex) {
            return ev.getPointerId(pointerIndex);
        }

        public static int findPointerIndex(MotionEvent ev, int pointerId) {
            return ev.findPointerIndex(pointerId);
        }
    }
}
