package com.vgtu.ekg.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.util.Locale;

public class TimerTextView extends android.support.v7.widget.AppCompatTextView {

	private Paint textPaint;
	private final Rect textBounds = new Rect(); //don't new this up in a draw method
	private boolean isRecording = false;
	private long startRecoringTime = 0;
	private long finishTime = 0;
	private long delta = 0;
	private long min = 0;
	private long sec = 0;
	private long ms = 0;
	private String resultText = "00:00:00";

	public TimerTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		textPaint = new Paint();
		textPaint.setColor(getCurrentTextColor());
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTextSize(getTextSize());
		textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		if (isRecording) {
			startRecoringTime = System.currentTimeMillis();
		} else {
			finishTime = System.currentTimeMillis();
		}
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		delta = isRecording ? System.currentTimeMillis() - startRecoringTime : finishTime - startRecoringTime;

		sec = delta / 1000 % 60;
		min = (delta / (1000 * 60)) % 60;
		ms = delta % 1000 / 10;
		resultText = String.format(Locale.ENGLISH, "%02d:%02d:%02d", min, sec, ms);

		textPaint.getTextBounds(resultText, 0, resultText.length(), textBounds);
		canvas.drawText(resultText,
				0,
				(getHeight() / 2) - textBounds.exactCenterY(),
				textPaint);

		if (isRecording) {
			invalidate();
		}
	}
}
