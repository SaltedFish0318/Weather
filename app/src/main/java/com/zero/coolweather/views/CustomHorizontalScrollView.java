package com.zero.coolweather.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;


public class CustomHorizontalScrollView extends HorizontalScrollView {

    private GestureDetector mGestureDetector;

    public CustomHorizontalScrollView(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new YScrollDetector());
        setFadingEdgeLength(0);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new YScrollDetector());
        setFadingEdgeLength(0);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetector(context, new YScrollDetector());
        setFadingEdgeLength(0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev) && mGestureDetector.onTouchEvent(ev);
    }

    /**
     * 竖向滑动距离<横向距离，执行横向滑动
     */
    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) < Math.abs(distanceX)) {
                return true;
            }
            return false;
        }
    }

    public interface ScrollViewListener {

        void onScrollChanged(ScrollType scrollType);

    }

    private ScrollViewListener scrollViewListener;

    public enum ScrollType{TOUCH,UP};

    /**
     * 当前状态,手指离开
     */
    private ScrollType scrollType = ScrollType.UP;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.scrollType = ScrollType.TOUCH;
                scrollViewListener.onScrollChanged(scrollType);

            case MotionEvent.ACTION_MOVE:
                this.scrollType = ScrollType.TOUCH;
                scrollViewListener.onScrollChanged(scrollType);
                break;
            case MotionEvent.ACTION_UP:
                this.scrollType = ScrollType.UP;
                scrollViewListener.onScrollChanged(scrollType);
                break;

            default:
                this.scrollType = ScrollType.UP;
                scrollViewListener.onScrollChanged(scrollType);
                break;

        }
        return super.onTouchEvent(ev);
    }

    public void setOnScrollStateChangedListener(ScrollViewListener listener){
        this.scrollViewListener = listener;
    }
}
