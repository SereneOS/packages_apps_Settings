/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.annotation.SuppressLint;
import android.annotation.ColorInt;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.BatteryManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;

public class BatteryMeterView extends CardView {
    WaveView waveView;
    TextView progress_Text;
    TextView charging_Text;
    IntentFilter ifilter;
    Intent batteryStatus;
    int status;

    public BatteryMeterView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BatteryMeterView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    @SuppressLint("DefaultLocale")
    private void init(Context context) {
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = context.registerReceiver(null, ifilter);
        status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        setLayoutParams(new ViewGroup.LayoutParams((int) dpToPx(232), (int) dpToPx(232)));
        waveView = new WaveView(context);
        progress_Text = new TextView(context);
        progress_Text.setTextAppearance(R.style.BatteryGauage_TextView);
        charging_Text = new TextView(context);
        charging_Text.setTextAppearance(R.style.BatteryGauage_TextView_Light);
        charging_Text.setTextSize(22);
        charging_Text.setGravity(Gravity.CENTER_HORIZONTAL);
        progress_Text.setTextSize(52);
        progress_Text.setTextColor(getColorAttr(context, android.R.attr.textColorSecondary));
        if (getCharging()) {
            charging_Text.setText("Charging");
        } else
            charging_Text.setText("Discharging");
        progress_Text.setGravity(Gravity.CENTER_HORIZONTAL);
        charging_Text.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        progress_Text.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        charging_Text.setAlpha((float) 0.7);
        progress_Text.setAlpha((float) 0.7);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.addView(progress_Text);
        linearLayout.addView(charging_Text);
        RelativeLayout layout = new RelativeLayout(context);
        layout.addView(waveView);
        layout.addView(linearLayout);
        addView(layout);
        setRadius(getLayoutParams().height);
        if (hurtEyes(getColorAttr(context, android.R.attr.colorAccent))) {
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                TextView child = (TextView) linearLayout.getChildAt(i);
                child.setTextColor(getColorAttr(context, android.R.attr.textColorSecondary));
            }
        } if (getBatteryLevel() > 70 && !hurtEyes(getColorAttr(context, android.R.attr.colorAccent))) {
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                TextView child = (TextView) linearLayout.getChildAt(i);
                child.setTextColor(Color.WHITE);
            }
        }
    }

    public boolean getCharging() {
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public void setCharging(boolean status) {
        if (status) {
            this.status = BatteryManager.BATTERY_STATUS_CHARGING;
        } else {
            this.status = BatteryManager.BATTERY_STATUS_DISCHARGING;
        }
        postInvalidate();
    }

    public void setBatteryLevel(int progress) {
        waveView.setProgress(progress);
        progress_Text.setText(String.format("%d%%", waveView.getProgress()));
    }

    public int getBatteryLevel() {
        return waveView.getProgress() <= 0 ? waveView.getProgress() : 10;
    }

    private boolean hurtEyes(int color) {
        int rgb = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
        return rgb > 210;
    }

    @ColorInt
    private int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        @ColorInt int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    class Wave extends View {
        private final int WAVE_HEIGHT_HUGE = 36;
        private final int WAVE_HEIGHT_LARGE = 16;
        private final int WAVE_HEIGHT_MIDDLE = 8;
        private final int WAVE_HEIGHT_LITTLE = 5;
        private final float WAVE_LENGTH_MULTIPLE_LARGE = 1.5f;
        private final float WAVE_LENGTH_MULTIPLE_MIDDLE = 1f;
        private final float WAVE_LENGTH_MULTIPLE_LITTLE = 0.5f;
        private final float WAVE_HZ_FAST = 0.13f;
        private final float WAVE_HZ_NORMAL = 0.09f;
        private final float WAVE_HZ_SLOW = 0.05f;

        public final int DEFAULT_ABOVE_WAVE_ALPHA = 255;
        public final int DEFAULT_BLOW_WAVE_ALPHA = 180;

        private final float X_SPACE = 20;
        private final double PI2 = 2 * Math.PI;

        private Path mAboveWavePath = new Path();
        private Path mBlowWavePath = new Path();

        private Paint mAboveWavePaint = new Paint();
        private Paint mBlowWavePaint = new Paint();

        private int mAboveWaveColor;
        private int mBlowWaveColor;

        private float mWaveMultiple;
        private float mWaveLength;
        private int mWaveHeight;
        private float mMaxRight;
        private float mWaveHz;

        private float mAboveOffset = 0.0f;
        private float mBlowOffset;

        private Wave.RefreshProgressRunnable mRefreshProgressRunnable;

        private int left, right, bottom;
        private double omega;

        public Wave(Context context, AttributeSet attrs) {
            this(context, attrs, R.attr.waveViewStyle);
        }

        public Wave(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawPath(mBlowWavePath, mBlowWavePaint);
            canvas.drawPath(mAboveWavePath, mAboveWavePaint);
        }

        public void setAboveWaveColor(int aboveWaveColor) {
            this.mAboveWaveColor = aboveWaveColor;
        }

        public void setBlowWaveColor(int blowWaveColor) {
            this.mBlowWaveColor = blowWaveColor;
        }

        public Paint getAboveWavePaint() {
            return mAboveWavePaint;
        }

        public Paint getBlowWavePaint() {
            return mBlowWavePaint;
        }

        public void initializeWaveSize(int waveMultiple, int waveHeight, int waveHz) {
            mWaveMultiple = getWaveMultiple(waveMultiple);
            mWaveHeight = getWaveHeight(waveHeight);
            mWaveHz = getWaveHz(waveHz);
            mBlowOffset = mWaveHeight * 0.4f;
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    mWaveHeight * 2);
            setLayoutParams(params);
        }

        public void initializePainters() {
            mAboveWavePaint.setColor(mAboveWaveColor);
            mAboveWavePaint.setAlpha(DEFAULT_ABOVE_WAVE_ALPHA);
            mAboveWavePaint.setStyle(Paint.Style.FILL);
            mAboveWavePaint.setAntiAlias(true);

            mBlowWavePaint.setColor(mBlowWaveColor);
            mBlowWavePaint.setAlpha(DEFAULT_BLOW_WAVE_ALPHA);
            mBlowWavePaint.setStyle(Paint.Style.FILL);
            mBlowWavePaint.setAntiAlias(true);
        }

        private float getWaveMultiple(int size) {
            switch (size) {
                case WaveView.LARGE:
                    return WAVE_LENGTH_MULTIPLE_LARGE;
                case WaveView.MIDDLE:
                    return WAVE_LENGTH_MULTIPLE_MIDDLE;
                case WaveView.LITTLE:
                    return WAVE_LENGTH_MULTIPLE_LITTLE;
            }
            return 0;
        }

        private int getWaveHeight(int size) {
            switch (size) {
                case WaveView.HUGE:
                    return WAVE_HEIGHT_HUGE;
                case WaveView.LARGE:
                    return WAVE_HEIGHT_LARGE;
                case WaveView.MIDDLE:
                    return WAVE_HEIGHT_MIDDLE;
                case WaveView.LITTLE:
                    return WAVE_HEIGHT_LITTLE;
            }
            return 0;
        }

        private float getWaveHz(int size) {
            switch (size) {
                case WaveView.LARGE:
                    return WAVE_HZ_FAST;
                case WaveView.MIDDLE:
                    return WAVE_HZ_NORMAL;
                case WaveView.LITTLE:
                    return WAVE_HZ_SLOW;
            }
            return 0;
        }

        private void calculatePath() {
            mAboveWavePath.reset();
            mBlowWavePath.reset();

            getWaveOffset();

            float y;
            mAboveWavePath.moveTo(left, bottom);
            for (float x = 0; x <= mMaxRight; x += X_SPACE) {
                y = (float) (mWaveHeight * Math.sin(omega * x + mAboveOffset) + mWaveHeight);
                mAboveWavePath.lineTo(x, y);
            }
            mAboveWavePath.lineTo(right, bottom);

            mBlowWavePath.moveTo(left, bottom);
            for (float x = 0; x <= mMaxRight; x += X_SPACE) {
                y = (float) (mWaveHeight * Math.sin(omega * x + mBlowOffset) + mWaveHeight);
                mBlowWavePath.lineTo(x, y);
            }
            mBlowWavePath.lineTo(right, bottom);
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (View.GONE == visibility) {
                removeCallbacks(mRefreshProgressRunnable);
            } else {
                removeCallbacks(mRefreshProgressRunnable);
                mRefreshProgressRunnable = new Wave.RefreshProgressRunnable();
                post(mRefreshProgressRunnable);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            if (hasWindowFocus) {
                if (mWaveLength == 0) {
                    startWave();
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (mWaveLength==0){
                startWave();
            }
        }

        private void startWave() {
            if (getWidth() != 0) {
                int width = getWidth();
                mWaveLength = width * mWaveMultiple;
                left = getLeft();
                right = getRight();
                bottom = getBottom() + 2;
                mMaxRight = right + X_SPACE;
                omega = PI2 / mWaveLength;
            }
        }

        private void getWaveOffset() {
            if (mBlowOffset > Float.MAX_VALUE - 100) {
                mBlowOffset = 0;
            } else {
                mBlowOffset += mWaveHz;
            }

            if (mAboveOffset > Float.MAX_VALUE - 100) {
                mAboveOffset = 0;
            } else {
                mAboveOffset += mWaveHz;
            }
        }

        private class RefreshProgressRunnable implements Runnable {
            public void run() {
                synchronized (this) {
                    long start = System.currentTimeMillis();

                    calculatePath();

                    invalidate();

                    long gap = 16 - (System.currentTimeMillis() - start);
                    postDelayed(this, gap < 0 ? 0 : gap);
                }
            }
        }

    }

    class WaveView extends LinearLayout {
        protected static final int HUGE = 0;
        protected static final int LARGE = 1;
        protected static final int MIDDLE = 2;
        protected static final int LITTLE = 3;

        private int mAboveWaveColor = getColorAttr(this.getContext(), android.R.attr.colorAccent);
        private int mProgress;
        private int mWaveHeight = 0;
        private int mWaveMultiple = 3;
        private int mWaveHz = 2;

        private int mWaveToTop;

        private Wave mWave;
        private Solid mSolid;

        public WaveView(Context context) {
            super(context);
            setOrientation(VERTICAL);

            mWave = new Wave(context, null);
            mWave.initializeWaveSize(mWaveMultiple, mWaveHeight, mWaveHz);
            mWave.setAboveWaveColor(mAboveWaveColor);
            mWave.setBlowWaveColor(mAboveWaveColor);
            mWave.initializePainters();

            mSolid = new Solid(context, null);
            mSolid.setAboveWavePaint(mWave.getAboveWavePaint());
            mSolid.setBlowWavePaint(mWave.getBlowWavePaint());

            addView(mWave);
            addView(mSolid);

            setProgress(mProgress);
        }

        public void setProgress(int progress) {
            this.mProgress = progress > 100 ? 100 : progress;
            computeWaveToTop();
        }

        public int getProgress() {
            return this.mProgress;
        }

        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            if (hasWindowFocus) {
                computeWaveToTop();
            }
        }

        private void computeWaveToTop() {
            mWaveToTop = (int) (getHeight() * (1f - mProgress / 100f));
            ViewGroup.LayoutParams params = mWave.getLayoutParams();
            if (params != null) {
                ((LayoutParams) params).topMargin = mWaveToTop;
            }
            mWave.setLayoutParams(params);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Parcelable superState = super.onSaveInstanceState();
            WaveView.SavedState ss = new WaveView.SavedState(superState);
            ss.progress = mProgress;
            return ss;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
            WaveView.SavedState ss = (WaveView.SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());
            setProgress(ss.progress);
        }

        private class SavedState extends BaseSavedState {
            int progress;

            SavedState(Parcelable superState) {
                super(superState);
            }

            private SavedState(Parcel in) {
                super(in);
                progress = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel out, int flags) {
                super.writeToParcel(out, flags);
                out.writeInt(progress);
            }

            public final Creator<WaveView.SavedState> CREATOR = new Creator<WaveView.SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return new WaveView.SavedState(in);
                }

                public WaveView.SavedState[] newArray(int size) {
                    return new WaveView.SavedState[size];
                }
            };
        }
    }

    class Solid extends View {

        private Paint aboveWavePaint;
        private Paint blowWavePaint;

        public Solid(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public Solid(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.weight = 1;
            setLayoutParams(params);
        }

        public void setAboveWavePaint(Paint aboveWavePaint) {
            this.aboveWavePaint = aboveWavePaint;
        }

        public void setBlowWavePaint(Paint blowWavePaint) {
            this.blowWavePaint = blowWavePaint;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(getLeft(), 0, getRight(), getBottom(), blowWavePaint);
            canvas.drawRect(getLeft(), 0, getRight(), getBottom(), aboveWavePaint);
        }
    }

}

