package de.nproth.pin;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class RotaryControlView extends View {

    private Drawable mScaleDrawable;
    private Drawable mBarDrawable;

    private float mStartAngle = 90;
    private float mEndAngle = 90;

    private int mMinValue = 0;
    private int mMaxValue = 100;
    private int mStepValue = 1;
    private int mValue = 0;

    private Path mClip = new Path();
    private Rect mBounds = new Rect();
    private RectF mBoundsF = new RectF();

    private PointF mTouch = new PointF();
    private double mAngleLastTouch;
    private double mLastValue;

    private OnValueChangedListener mValListener;

    public interface OnValueChangedListener {

        public void onValueChanged(int val, int min, int max, int stepVal);

        public void onUserChangeBegin(int val);

        public void onUserChange(int val);

        public void onUserChangeEnd(int val);
    }

    public RotaryControlView(Context context) {
        super(context);
        init(null, 0);
    }

    public RotaryControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public RotaryControlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.RotaryControlView, defStyle, 0);

        setScaleDrawable(a.getDrawable(R.styleable.RotaryControlView_scaleDrawable));
        setBarDrawable(a.getDrawable(R.styleable.RotaryControlView_barDrawable));
        setStartAngle(a.getFloat(R.styleable.RotaryControlView_startAngle, 90));
        setEndAngle(a.getFloat(R.styleable.RotaryControlView_endAngle, 90));
        setMinValue(a.getInt(R.styleable.RotaryControlView_min, 0));
        setMaxValue(a.getInt(R.styleable.RotaryControlView_max, 100));
        setStepValue(a.getInt(R.styleable.RotaryControlView_step, 1));
        setValue(a.getInt(R.styleable.RotaryControlView_value, 0));

        a.recycle();


        //Hw support for Canvas#clipPath not present before
        if(Build.VERSION.SDK_INT < 18)
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int right = getWidth() - paddingRight;
        int bottom = getHeight() - paddingBottom;

        int contentWidth = right - paddingLeft;
        int contentHeight = bottom - paddingTop;

        mBounds.set(paddingLeft, paddingTop, right, bottom);
        mBoundsF.set(paddingLeft, paddingTop, right, bottom);

        float centerX = paddingLeft + (0.5f * contentWidth);
        float centerY = paddingTop + (0.5f * contentHeight);

        float angle = (mEndAngle - mStartAngle);
        while(angle <= 0)
            angle += 360;
        angle %= 360;
        float sweep = angle * (mValue - mMinValue) / (mMaxValue - mMinValue);

        if(mScaleDrawable != null) {
            mScaleDrawable.setBounds(mBounds);
            mScaleDrawable.draw(canvas);
        }

        if(mBarDrawable != null) {
            mBarDrawable.setBounds(mBounds);
            canvas.save();

            mClip.reset();
            mClip.moveTo(centerX, centerY);
            mClip.arcTo(mBoundsF, mStartAngle, sweep, false);
            mClip.close();

            canvas.clipPath(mClip);
            mBarDrawable.draw(canvas);

            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        Log.d("RotaryControlView", "Touch Event with pointer index: " + event.getActionIndex());

        if(event.getActionIndex() != 0)
            return true;//We only care about pointer 1

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int right = getWidth() - paddingRight;
        int bottom = getHeight() - paddingBottom;

        int contentWidth = right - paddingLeft;
        int contentHeight = bottom - paddingTop;
        float cx = paddingLeft + (0.5f * contentWidth);
        float cy = paddingTop + (0.5f * contentHeight);

        float x = event.getX();
        float y = event.getY();

        double unit = ((double) mMaxValue - mMinValue) / Math.abs(mEndAngle - mStartAngle);

        float a, c;
        double angle;

        a = Math.abs(y - cy);
        c = Math.abs(x - cx);

        if(c != 0)
            angle = Math.toDegrees(Math.atan(a / c));
        else
            angle = 0;

        if(x < cx)
            angle = 180 - angle;
        if(y < cy)
            angle = 360 - angle;


        Log.d("RotaryControlView", "onTouchMove, alpha: " + angle);


        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("RotaryControlView", "onTouchDown");
                mTouch.set(x, y);
                mLastValue = mValue;

                if(mValListener != null)
                    mValListener.onUserChangeBegin(mValue);
                break;
            case MotionEvent.ACTION_MOVE:

                double diffangle;
                if(Math.abs(angle - mAngleLastTouch) > 180)
                    diffangle = (360 - angle) - mAngleLastTouch;
                else
                    diffangle = angle - mAngleLastTouch;

                double range = mEndAngle - mStartAngle;
                while(range <= 0)
                    range += 360;

                mLastValue += (int) Math.round(diffangle * (mMaxValue - mMinValue) / range);
                mLastValue = Math.min(mLastValue, mMaxValue);
                mLastValue = Math.max(mLastValue, mMinValue);

                Log.d("RotaryControlView", "onTouchMove, diffangle: " + diffangle);

                int val = (int) mLastValue;
                this.mValue = val - (val % mStepValue);

                if(mValListener != null)
                    mValListener.onUserChange(mValue);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                if(mValListener != null)
                    mValListener.onUserChangeEnd(mValue);
                Log.d("RotaryControlView", "onTouchUp");
        }

        mAngleLastTouch = angle;
        return true;
    }

    public Drawable getScaleDrawable() {
        return mScaleDrawable;
    }

    public void setScaleDrawable(Drawable mScaleDrawable) {
        this.mScaleDrawable = mScaleDrawable;
        if(mScaleDrawable != null)
            mScaleDrawable.setCallback(this);
        invalidate();
    }

    public Drawable getBarDrawable() {
        return mBarDrawable;
    }

    public void setBarDrawable(Drawable mBarDrawable) {
        this.mBarDrawable = mBarDrawable;
        if(mBarDrawable != null)
            mBarDrawable.setCallback(this);
        invalidate();
    }

    public float getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(float mStartAngle) {
        while(mStartAngle < 0)
            mStartAngle += 360;
        this.mStartAngle = mStartAngle % 360;
        invalidate();
    }

    public float getEndAngle() {
        return mEndAngle;
    }

    public void setEndAngle(float mEndAngle) {
        while(mEndAngle < 0)
            mEndAngle += 360;
        this.mEndAngle = mEndAngle % 360;
        invalidate();
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int mMinValue) throws IllegalArgumentException {
        if(mMinValue > mMaxValue)
            throw new IllegalArgumentException("min > max");
        this.mMinValue = mMinValue;
        setValue(mValue);
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int mMaxValue) throws IllegalArgumentException {
        if(mMinValue > mMaxValue)
            throw new IllegalArgumentException("max < min");
        this.mMaxValue = mMaxValue;
        setValue(mValue);
    }

    public int getStepValue() {
        return mStepValue;
    }

    public void setStepValue(int mStepValue) {
        this.mStepValue = mStepValue;
        //Align Value to steps
        setValue(mValue);
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int val) {
        val = Math.max(val, mMinValue);
        val = Math.min(val, mMaxValue);
        this.mValue = val - (val % mStepValue);
        Log.d("RotaryControlView", "Value set to " + mValue + " with steps: " + mStepValue + ", min: " + mMinValue + " and max: " + mMaxValue);
        if(mValListener != null)
            mValListener.onValueChanged(mValue, mMinValue, mMaxValue, mStepValue);
        invalidate();
    }

    public void setOnValueChangedListener(OnValueChangedListener l) {
        mValListener = l;
    }

    public OnValueChangedListener getOnValueChangedListener() {
        return mValListener;
    }
}
