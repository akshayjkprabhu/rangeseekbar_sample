package com.ajp.rangeseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

/*
 * Created by akshay on 25/11/18.
 * Copyright © 2018, Huffy
 * Written under contract by Robosoft Technologies Pvt. Ltd.
 */
public class RangeSeekBar extends View {

    private static final String TAG = RangeSeekBar.class.getSimpleName();

    /* todo RangeSeekBar
     * 1. Support padding for knob for touch area
     * 2. Add listener that gives the progress on moved - done
     * 3. getSelectableRange values in percentage - done
     * 4. Extend this to support max,min and seek indication - not done
     * 6. pass color, knob radius and through attributes - only color is done
     * 8. reduce member variables -rectify
     **/

    private Paint mSeekBarPaint, mRangePaint;
    private Paint mSeekKnobPaint, mSeekKnobSmallPaint;

    private RectF mSeekBarRect, mProgressRect;
    private RectF mLeftKnobRect, mRightKnobRect, mLeftSmallRect, mRightSmallRect;

    private boolean leftKnobTouched, rightKnobTouched;
    private boolean mLeftKnobEnabled, mRightKnobEnabled;
    private float mKnobRadius;
    private float mViewHeight;
    private float mSeekBarWidth;

    private float mLeftKnobPos;
    private float mRightKnobPos;

    private float mSeekBarStart;
    private float mSeekBarEnd;
    private float mCentreVertical;
    private float mSmallKnobRadius;

    private RangeChangeListener mRangeListener;

    @ColorInt
    private int mSeekColor, mRangeColor, mKnobColor, mKnobTint;

    private int mRangeMinPercent, mRangeMaxPercent;

    public RangeSeekBar(Context context) {
        super(context);
        init();
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs)
            throws InvalidRangeException {
        super(context, attrs);
        readFromAttributes(attrs);
        init();
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
            throws InvalidRangeException {
        super(context, attrs, defStyleAttr);
        readFromAttributes(attrs);
        init();
    }

    private void readFromAttributes(AttributeSet attrs) throws InvalidRangeException {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar,
                    0, 0);
            mSeekColor = typedArray.getColor(R.styleable.RangeSeekBar_seekBarColor, getColor(R.color.seekBarColor));
            mRangeColor = typedArray.getColor(R.styleable.RangeSeekBar_rangeColor, getColor(R.color.rangeColor));
            mKnobColor = typedArray.getColor(R.styleable.RangeSeekBar_knobColor, getColor(R.color.knobColor));
            mKnobTint = typedArray.getColor(R.styleable.RangeSeekBar_knobTint, getColor(R.color.knobTint));
            mRangeMaxPercent = typedArray.getInt(R.styleable.RangeSeekBar_maxPercentage, 100);
            mRangeMinPercent = typedArray.getInt(R.styleable.RangeSeekBar_minPercentage, 0);
            mLeftKnobEnabled = typedArray.getBoolean(R.styleable.RangeSeekBar_leftKnobEnabled, true);
            mRightKnobEnabled = typedArray.getBoolean(R.styleable.RangeSeekBar_rightKnobEnabled, true);
            typedArray.recycle();
            if (mRangeMaxPercent < mRangeMinPercent) throw new InvalidRangeException();
        }
    }

    @ColorInt
    private int getColor(@ColorRes int colorId) {
        return ContextCompat.getColor(getContext(), colorId);
    }


    private void init() {
        mSeekBarPaint = new Paint();
        mSeekBarPaint.setAntiAlias(true);
        mSeekBarPaint.setStyle(Paint.Style.FILL);
        mSeekBarPaint.setColor(mSeekColor);

        mRangePaint = new Paint();
        mRangePaint.setAntiAlias(true);
        mRangePaint.setStyle(Paint.Style.FILL);
        mRangePaint.setColor(mRangeColor);

        mSeekKnobPaint = new Paint();
        mSeekKnobPaint.setAntiAlias(true);
        mSeekKnobPaint.setStyle(Paint.Style.FILL);
        mSeekKnobPaint.setColor(mKnobTint);

        mSeekKnobSmallPaint = new Paint();
        mSeekKnobSmallPaint.setAntiAlias(true);
        mSeekKnobSmallPaint.setStyle(Paint.Style.FILL);
        mSeekKnobSmallPaint.setColor(mKnobColor);

        mSeekBarRect = new RectF();
        mProgressRect = new RectF();
        mLeftKnobRect = new RectF();
        mRightKnobRect = new RectF();
        mLeftSmallRect = new RectF();
        mRightSmallRect = new RectF();

        mKnobRadius = getResources().getDimension(R.dimen.default_knob_radius);
        mSeekBarWidth = getResources().getDimension(R.dimen.default_seek_bar_width);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        mSeekBarStart = getPaddingStart() + mKnobRadius;
        mSeekBarEnd = width - getPaddingEnd() - mKnobRadius;
        mViewHeight = mKnobRadius * 2;
        mCentreVertical = getPaddingTop() + mViewHeight / 2;

        mLeftKnobPos = getHorizontalPosition(mRangeMinPercent);
        mRightKnobPos = getHorizontalPosition(mRangeMaxPercent);
        mSmallKnobRadius = mKnobRadius / 2;

        mSeekBarRect.set(mSeekBarStart, mCentreVertical - mSeekBarWidth / 2,
                mSeekBarEnd, mCentreVertical + mSeekBarWidth / 2);

        updateLeftKnobPosition();
        updateRightKnobPosition();
        updateSelectedRange();
        setMeasuredDimension(width + getPaddingStart() + getPaddingEnd(),
                (int) (mViewHeight + getPaddingTop() + getPaddingBottom()));
    }

    private void updateRightKnobPosition() {
        mRightKnobRect.set(mRightKnobPos - mKnobRadius,
                mCentreVertical - mKnobRadius,
                mRightKnobPos + mKnobRadius,
                mCentreVertical + mKnobRadius);

        mRightSmallRect.set(mRightKnobPos - mSmallKnobRadius,
                mCentreVertical - mSmallKnobRadius,
                mRightKnobPos + mSmallKnobRadius,
                mCentreVertical + mSmallKnobRadius);
    }

    private void updateLeftKnobPosition() {
        mLeftKnobRect.set(mLeftKnobPos - mKnobRadius,
                mCentreVertical - mKnobRadius,
                mLeftKnobPos + mKnobRadius,
                mCentreVertical + mKnobRadius);

        mLeftSmallRect.set(mLeftKnobPos - mSmallKnobRadius,
                mCentreVertical - mSmallKnobRadius,
                mLeftKnobPos + mSmallKnobRadius,
                mCentreVertical + mSmallKnobRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(mSeekBarRect, 10, 10, mSeekBarPaint);
        canvas.drawRoundRect(mProgressRect, 10, 10, mRangePaint);
        canvas.drawArc(mLeftKnobRect, 0, 360, true, mSeekKnobPaint);
        canvas.drawArc(mLeftSmallRect, 0, 360, true, mSeekKnobSmallPaint);
        canvas.drawArc(mRightKnobRect, 0, 360, true, mSeekKnobPaint);
        canvas.drawArc(mRightSmallRect, 0, 360, true, mSeekKnobSmallPaint);
        Log.d(TAG, "onDraw");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "onLayout");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                leftKnobTouched = false;
                rightKnobTouched = false;
                if (mRangeListener != null) {
                    mRangeListener.onChangeDone();
                }
                break;
            case MotionEvent.ACTION_DOWN:
                leftKnobTouched = mLeftKnobEnabled && event.getX() <= mLeftKnobRect.right && event.getX() >= mLeftKnobRect.left;
                rightKnobTouched = mRightKnobEnabled && event.getX() <= mRightKnobRect.right && event.getX() >= mRightKnobRect.left;
                break;
            case MotionEvent.ACTION_MOVE:
                if (leftKnobTouched && event.getX() >= mSeekBarStart && event.getX() <= mRightKnobRect.centerX()) {
                    rightKnobTouched = false; // for when both the knobs in same position
                    moveKnob(mLeftKnobRect, mLeftSmallRect, event.getX());
                    onRangeStartValueChange(event.getX());
                }
                if (rightKnobTouched && event.getX() <= mSeekBarEnd && event.getX() >= mLeftKnobRect.centerX()) {
                    leftKnobTouched = false;// for when both the knobs in same position
                    moveKnob(mRightKnobRect, mRightSmallRect, event.getX());
                    onRangeEndValueChange(event.getX());
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                leftKnobTouched = false;
                rightKnobTouched = false;
                break;
        }
        return true;
    }

    private void moveKnob(@NonNull RectF bigKnob, @NonNull RectF smallKnob, float movementX) {
        bigKnob.left = movementX - mKnobRadius;
        bigKnob.right = movementX + mKnobRadius;
        smallKnob.left = movementX - mKnobRadius / 2;
        smallKnob.right = movementX + mKnobRadius / 2;
        updateSelectedRange();
        invalidate();
    }

    private void updateSelectedRange() {
        mProgressRect.set(mLeftKnobRect.centerX(),
                mCentreVertical - mSeekBarWidth / 2,
                mRightKnobRect.centerX(),
                mCentreVertical + mSeekBarWidth / 2);
    }

    /**
     * (y - y') = m(x - x')
     * m = (y" - y') / (x" - x')
     *
     * @param x is the horizontal position of knob
     * @return position in percentage
     */
    private int getPercentageProgress(float x) {
        float slope = (float) (100.0 / (mSeekBarEnd - mSeekBarStart));
        return Math.round(slope * (x - mSeekBarStart));
    }

    /**
     * y = mx + c
     * m = (y" - y') / (x" - x')
     * c = mSeekBarStart
     *
     * @param percentage is percentage equivalent of position
     * @return horizontal position
     */
    private float getHorizontalPosition(int percentage) {
        float slope = (float) ((mSeekBarEnd - mSeekBarStart) / 100.0);
        return slope * percentage + mSeekBarStart;
    }

    protected void onRangeEndValueChange(float x) {
        int progress = getPercentageProgress(x);
        Log.d(TAG, "onRangeEndValueChange : " + progress);
        if (mRangeListener != null) {
            mRangeListener.onRangeChanged(x, progress);
        }
    }

    protected void onRangeStartValueChange(float x) {
        int progress = getPercentageProgress(x);
        Log.d(TAG, "onRangeStartValueChange : " + progress);
        if (mRangeListener != null) {
            mRangeListener.onRangeChanged(x, progress);
        }
    }

    public void setRangeChangeListener(RangeChangeListener listener) {
        this.mRangeListener = listener;
    }

    public int getMinValue() {
        int progress = getPercentageProgress(mLeftKnobRect.centerX());
        Log.d(TAG, "Min percentage : " + progress);
        return progress;
    }

    public int getMaxValue() {
        int progress = getPercentageProgress(mRightKnobRect.centerX());
        Log.d(TAG, "Max progress : " + progress);
        return progress;
    }

    public @NonNull
    Pair<Integer, Integer> getRangeValue() {
        return new Pair<>(getMinValue(), getMaxValue());
    }

    public void setLeftKnobEnabled(boolean lefknobEnabled) {
        this.mLeftKnobEnabled = lefknobEnabled;
    }

    public void setRightKnobEnabled(boolean rightKnobEnabled) {
        this.mRightKnobEnabled = rightKnobEnabled;
    }

    public void setRangeValue(@NonNull Pair<Integer, Integer> percentMinMax) throws InvalidRangeException {
        if (percentMinMax.second < percentMinMax.first) throw new InvalidRangeException();
        mRangeMinPercent = percentMinMax.first;
        mRangeMaxPercent = percentMinMax.second;
        requestLayout(); //to measure once again
    }

    public static class InvalidRangeException extends Exception {
        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }

    public interface RangeChangeListener {
        void onRangeChanged(float x, int percent);

        void onChangeDone();
    }
}
