package com.vgtu.ekg.diagram;

import com.androidplot.xy.XYSeries;

/**
 * Primitive simulation of some kind of signal.  For this example,
 * we'll pretend its an ecg.  This class represents the data as a circular buffer;
 * data is added sequentially from left to right.  When the end of the buffer is reached,
 * i is reset back to 0 and simulated sampling continues.
 */
public class ECGModel implements XYSeries {

	private final int[] data;
	private int latestIndex = -1;

	/**
	 * @param size Sample size contained within this model
	 */
	public ECGModel(int size) {
		data = new int[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = 0;
		}
	}

	public void addNewValue(int value) {
		latestIndex++;
		if (latestIndex >= data.length) {
			latestIndex = 0;
		}
		// insert a random sample:
		data[latestIndex] = value;
	}

	public int getLatestIndex() {
		return latestIndex;
	}

	@Override
	public int size() {
		return data.length;
	}

	@Override
	public Number getX(int index) {
		return index;
	}

	@Override
	public Number getY(int index) {
		return data[index];
	}

	@Override
	public String getTitle() {
		return "Signal";
	}
}