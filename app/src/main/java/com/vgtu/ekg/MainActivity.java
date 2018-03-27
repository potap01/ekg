package com.vgtu.ekg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.neurosky.thinkgear.TGDevice;
import com.vgtu.ekg.util.PermissionUtil;
import com.vgtu.ekg.view.TimerTextView;

import org.achartengine.GraphicalView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_CODE_STORAGE = 1;
	private static final int RC_SETTINGS = 2;

	// Объявление переменных
	BluetoothAdapter bluetoothAdapter;
	TGDevice tgDevice;
	//    private ActionBar bar;
//    private View mview;
//    HeartRateAcceleration  heartRateAcceleration;
//    int smoothedHeartRate;
//    private static final String DIRECTORY_DOCS = "/documents";
	private String mCurFileName = ""; // имя текущего файла для работы
	private ImageButton btnMinimize;
	private ImageButton btnBluetooth;
	//    private ImageButton btnImageUsers;
	private ImageButton btnSettings;
	//    private ImageButton btnRecord;
//    private ImageButton btnStopRecording;
	private TextView heart_Rate;
	//    private TextView pacient;
	public ImageView mImageView;
	//    public ImageView mImageBT;
	public AnimationDrawable mAnimationDrawable;
	private TimerTextView recordTime;
	//    private int time = 0;
	public boolean new_pacient_directory = true;
	public int bt_connected = 1;
	int heartRate_vs = 0;
	int subjectContactQuality_last;
	int subjectContactQuality_cnt;
	boolean rec_per = false;
	//    public  TextView tv_HeartRate,tv_HeartAge,tv_RespirationRate,tv_RelaxationLevel,tv_5minHeartAge,tv_rrInterval;
//    TextView tv_Title;
//    EditText et_age;
	int heartRate;
	int rr_interval;
	double RawValue;
	int Raw_Value;
	//    byte poorSignal;
//    public int average_heartrate = 0;
//    int len = 0;
//    int tem_heartrate_difference = 0;
//    int tem_sum = 0;//sum of heart rate difference
//    int value = 0;//new point
//    int tem_value = 0;
	private GraphicalView chart;
	private LinearLayout linear;
	//private XYSeries hseries;
	public String formattedDate;

	private boolean isRecording = false;

	//Раздел рисования графика
	//XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	//XYSeriesRenderer dxyrenderer = new XYSeriesRenderer(), hxyrenderer;
	//Создание хранилища данных
	//XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
	//private int addX = -1, addY;
	int[] xv = new int[5000];
	int[] yv = new int[5000];


	private Queue<Integer> unwritedRRvalues = new ConcurrentLinkedQueue<>();
	private Queue<Integer> unwritedRawValues = new ConcurrentLinkedQueue<>();

	private XYPlot xyPlotView;
	/**
	 * Uses a separate thread to modulate redraw frequency.
	 */
	private Redrawer redrawer;
	ECGModel ecgSeries;

	//Начальные установки для графика
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		InitActionBar();
		// pacient=(TextView)findViewById(R.id.patient);
		subjectContactQuality_last = -1; /* Начало с невозможного значения */
		subjectContactQuality_cnt = 200; /* start over the limit, so it gets reported the 1st time */

		Intent intent = getIntent();
		//получаем строку и формируем имя ресурса
		String resName = "n" + intent.getIntExtra("head", 0);
		Log.i("name", resName);

/*		//setup the draw section
		renderer.setPointSize(3);
		renderer.setZoomButtonsVisible(true);
		//renderer.setShowGrid(show_grid);
		renderer.setXAxisMax(500);
		renderer.setXAxisMin(0);
		renderer.setYAxisMax(5000);
		renderer.setYAxisMin(-1400);
		renderer.setXLabels(10);
		renderer.setYLabels(10);
		renderer.setAxesColor(Color.WHITE);

		//set up heart rate
		hxyrenderer = new XYSeriesRenderer();
		//hxyrenderer.setColor(Color.BLUE);
		hxyrenderer.setPointStyle(PointStyle.DIAMOND);
		renderer.addSeriesRenderer(hxyrenderer);
		hseries = new XYSeries("RR интервал");
		dataset.addSeries(hseries);*/

		//setup the draw in screen
		//chart = ChartFactory.getLineChartView(MainActivity.this, dataset, renderer);

		//((ViewGroup) findViewById(android.R.id.content)).addView(chart, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		xyPlotView = findViewById(R.id.plot);
		initPlot();
	}

	@Override
	protected void onStop() {
		super.onStop();
		redrawer.pause();
	}

	@Override
	protected void onStart() {
		super.onStart();
		redrawer.start();
	}

	private void initPlot() {
		ecgSeries = new ECGModel(4000);
		xyPlotView.setRangeBoundaries(-1000, 1500, BoundaryMode.FIXED);
		xyPlotView.setDomainBoundaries(0, 4000, BoundaryMode.FIXED);
		xyPlotView.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(format);
		xyPlotView.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(format);
		// reduce the number of range labels
		xyPlotView.setLinesPerRangeLabel(3);
		// set a redraw rate of 30hz and start immediately:
		redrawer = new Redrawer(xyPlotView, 10, true);
		setSettingsParams();
	}

	private void setSettingsParams() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// читаем цвет фона из ListPreference для цвета фона
		String regular = prefs.getString(getString(R.string.pref_style), "");
		Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bgPaint.setColor(regular.contains("Белый") ? Color.WHITE : Color.BLACK);
		xyPlotView.getGraph().setGridBackgroundPaint(bgPaint);

		// читаем цвет из ListPreference для кривой графика
		Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		String regular_curve = prefs.getString(getString(R.string.pref_mesh), "");
		if (regular_curve.contains("Синий")) {
			linePaint.setColor(Color.BLUE);
		} else if (regular_curve.contains("Зеленый")) {
			linePaint.setColor(Color.GREEN);
		} else {
			linePaint.setColor(Color.RED);
		}
		// add a new series' to the xyplot:
		MyFadeFormatter formatter = new MyFadeFormatter(4000);
		formatter.setLegendIconEnabled(false);
		formatter.setLinePaint(linePaint);
		xyPlotView.removeSeries(ecgSeries);
		xyPlotView.addSeries(ecgSeries, formatter);

		/*
		// читаем установленное значение из CheckBoxPreference
		if (prefs.getBoolean(getString(R.string.pref_curve), true)) {

			renderer.setShowGrid(true);
		} else {
			renderer.setShowGrid(false);
		}*/


		// читаем размер шрифта из EditTextPreference
		mCurFileName = prefs.getString(getString(R.string.new_pacient), "20");
		// читаем установленное значение из CheckBoxPreference
		new_pacient_directory = prefs.getBoolean(getString(R.string.pacient_directory), true);
	}

	// Обновление графика и его сдвиг
	public void updateChart(int newValue) {
		/*dataset.removeSeries(series);
		int length = series.getItemCount();
		int addX;
		int addY;
		//Длина окна графика по оси Х
		if (length >= 500) {
			for (int i = 0; i < length - 1; i++) {
				xv[i] = (int) series.getX(i);
				yv[i] = (int) series.getY(i + 1);
			}
			series.clear();
			addX = length - 1;
			addY = newValue;
			for (int j = 0; j < length - 1; j++) {
				series.add(xv[j], yv[j]);
			}
			series.add(addX, addY);
			dataset.addSeries(series);
		} else {
			addX = length;
			addY = newValue;
			series.add(addX, addY);
			dataset.addSeries(series);
		}*/

		ecgSeries.addNewValue(newValue);

/*		currentTime = System.currentTimeMillis();
		if (lastUpdateTime == 0 || currentTime - lastUpdateTime > 25) {
			lastUpdateTime = currentTime;
			//chart.invalidate();
		}*/
		xyPlotView.getRenderer(AdvancedLineAndPointRenderer.class).setLatestIndex(ecgSeries.getLatestIndex());

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RC_SETTINGS) {
			setSettingsParams();
		}
	}

	//Раздел рисования графика

	//Верхняя строка состояния
	private void InitActionBar() {

		// ActionBar bar;

		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		View toolbar = LayoutInflater.from(this).inflate(R.layout.custom_action_bar, null);
		getSupportActionBar().setCustomView(toolbar, new ActionBar.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		View mview = getSupportActionBar().getCustomView();
		ActionBar bar = getSupportActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#131313")));

		btnMinimize = mview.findViewById(R.id.action_bar_minimize);
		heart_Rate = mview.findViewById(R.id.heartRate);
		btnMinimize.setColorFilter(Color.parseColor("#c6c5c5"));
		btnMinimize.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			    /*LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

                View popupView = layoutInflater.inflate(R.layout.popup, null);

                final PopupWindow popupWindow = new PopupWindow(popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);



                Button dismissButton = (Button) popupView.findViewById(R.id.button_close);
                tv_HeartAge = (TextView)popupView.findViewById(R.id.HEART_AGE_TEXT1);
                tv_HeartAge.setText("");
                tv_5minHeartAge = (TextView)popupView.findViewById(R.id.HEART_AGE_5MIN_TEXT1);
                tv_5minHeartAge.setText("");
                tv_rrInterval = (TextView)popupView.findViewById(R.id.RR_INTERVAL_TEXT1);
                tv_rrInterval.setText( "" );
                dismissButton.setOnClickListener(new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
                popupWindow.showAsDropDown(btnMinimize, 50, -30);*/
			}
		});

		mImageView = mview.findViewById(R.id.action_bar_heartRateIcon);
		mImageView.setBackgroundResource(R.drawable.heart_anim);
		mAnimationDrawable = (AnimationDrawable) mImageView.getBackground();

		//mImageBT=mview.findViewById(R.id.action_bar_bluetooth);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		btnBluetooth = mview.findViewById(R.id.action_bar_bluetooth);
		btnBluetooth.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (bt_connected) {
					case 1:
						tgDevice = new TGDevice(bluetoothAdapter, handler);
						tgDevice.connect(true);
						bt_connected = 2;
						btnBluetooth.setImageResource(R.drawable.ic_connect_bt);
						break;
					case 2:
						tgDevice.close();
						bt_connected = 1;
						btnBluetooth.setImageResource(R.drawable.ic_disconnect_bt);
						break;
				}

			}
		});


		btnSettings = mview.findViewById(R.id.action_bar_settings);
		btnSettings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent();
				intent.setClass(MainActivity.this, SettingActivity.class);
				startActivityForResult(intent, RC_SETTINGS);
			}
		});

		ImageButton btnRecord = mview.findViewById(R.id.action_bar_play);
		btnRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tryStartRecording();
			}
		});

		ImageButton btnStopRecording = mview.findViewById(R.id.action_bar_stop);
		btnStopRecording.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopRecording();
			}
		});

		recordTime = (TimerTextView) mview.findViewById(R.id.recordTime);

	}

	private void tryStartRecording() {
		if (!isRecording) {
			if (rec_per) {
				if (PermissionUtil.hasWriteExtStoragePermission(this)) {
					startRecording();
				} else {
					requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
				}
			} else {
				Toast.makeText(getApplicationContext(), "Подключите устройство!",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	public byte[] byteArrayFromRR(int value) {
		value = value / 4;
		int mod = value % 256;
		int div = value / 256;
		value = mod * 256 + div;

		byte[] bytes = ByteBuffer.allocate(2).putShort((short) value).array();
		byte[] resultBytes = new byte[2];
		resultBytes[1] = bytes[0];
		resultBytes[0] = bytes[1];
		return resultBytes;
	}

	public byte[] byteArrayFromRaw(int value) {
		value = value * 10 + 28000;
		return ByteBuffer.allocate(2).putShort((short) value).array();
	}

	// Включение записи в файл
	private void startRecording() {
		ecgSeries = new ECGModel(2000);

		isRecording = true;
		unwritedRRvalues.clear();
		unwritedRawValues.clear();
		recordTime.setRecording(true);
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		formattedDate = df.format(new Date(System.currentTimeMillis()));
		Thread fileWriterThread = new Thread() {
			public void run() {
				String folderName = new_pacient_directory && !TextUtils.isEmpty(mCurFileName) ? mCurFileName : "default";
				String path = Environment.getExternalStorageDirectory() + File.separator + "ECG data" + File.separator + folderName;
				File folder = new File(path);
				folder.mkdirs();
				File rrFile = new File(folder, formattedDate + "_ritm.rtm");
				File rawFile = new File(folder, formattedDate + "_kardio.krd");
				BufferedOutputStream rrBos = null;
				BufferedOutputStream rawBos = null;
				try {
					rrFile.createNewFile();
					rrBos = new BufferedOutputStream(new FileOutputStream(rrFile));
					rawFile.createNewFile();
					rawBos = new BufferedOutputStream(new FileOutputStream(rawFile));
					while (isRecording
							|| !unwritedRRvalues.isEmpty()
							|| !unwritedRawValues.isEmpty()) {

						if (!unwritedRRvalues.isEmpty()) {
							Integer intValue = unwritedRRvalues.poll();
							rrBos.write(byteArrayFromRR(intValue));
						}
						if (!unwritedRawValues.isEmpty()) {
							Integer intValue = unwritedRawValues.poll();
							rawBos.write(byteArrayFromRaw(intValue));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, e.toString());
				} finally {
					if (rrBos != null) {
						try {
							rrBos.flush();
							rrBos.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		fileWriterThread.start();

	}

	// Выключение записи в файл
	private void stopRecording() {
		if (isRecording) {
			Toast.makeText(getApplicationContext(), "Запись завершена!", Toast.LENGTH_LONG).show();
		}
		isRecording = false;
		recordTime.setRecording(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (tgDevice != null) {
			tgDevice.close();
		}
	}


	private long lastUpdateTime = 0;
	private long currentTime = 0;
	//Прием сообщений от TGDevice
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
		@SuppressLint("SetTextI18n")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case TGDevice.MSG_MODEL_IDENTIFIED:
				/*
				 * now there is something connected,
        		 * time to set the configurations we need
        		 */
					//tv.append("Model Identified\n");
					Toast.makeText(MainActivity.this, "Модель идентифицирована", Toast.LENGTH_LONG).show();
					tgDevice.setBlinkDetectionEnabled(true); // not allowed on EKG hardware, here to show the override message
					tgDevice.setRespirationRateEnable(true);
					break;

				case TGDevice.MSG_STATE_CHANGE:
					switch (msg.arg1) {
						case TGDevice.STATE_IDLE:
							break;
						case TGDevice.STATE_CONNECTING:
							Toast.makeText(MainActivity.this, "Установка соединения", Toast.LENGTH_LONG).show();
							break;
						case TGDevice.STATE_CONNECTED:
							Toast.makeText(MainActivity.this, "Соединение установлено", Toast.LENGTH_LONG).show();
							tgDevice.start();
							tgDevice.pass_seconds = 15;
							break;
						case TGDevice.STATE_NOT_FOUND:
							Toast.makeText(MainActivity.this, "Не найдено устройство", Toast.LENGTH_LONG).show();
							Toast.makeText(MainActivity.this, "Должно быть сопряжено одно Bluetooth устройство", Toast.LENGTH_LONG).show();
							break;
					    /*case TGDevice.STATE_NOT_PAIRED:
	                	tv.append("not paired\n");
	                	break;*/
						case TGDevice.STATE_DISCONNECTED:
							Toast.makeText(MainActivity.this, "Соединение разорвано", Toast.LENGTH_LONG).show();
					}
					break;

// Проверка соединены ли электроды с кожей
				case TGDevice.MSG_POOR_SIGNAL:
            	   /* Вывод на экран сообщения о качестве сигнала каждые 90 секунд */
					if (subjectContactQuality_cnt >= 90 || msg.arg1 != subjectContactQuality_last) {
						if (msg.arg1 == 200) { //200 is for BMD
							Toast.makeText(MainActivity.this, "Сигнал ЭКГ хороший", Toast.LENGTH_LONG).show();
							rec_per = true;
							mAnimationDrawable.start();
						} else {
							mAnimationDrawable.stop();
							Toast.makeText(MainActivity.this, "Нет сигнала ЭКГ! Датчик надо закрепить на груди.", Toast.LENGTH_LONG).show();
						}
						subjectContactQuality_cnt = 0;
						subjectContactQuality_last = msg.arg1;
					} else subjectContactQuality_cnt++;
					break;

// Вывод переменных их класса TGDevice.class

				// Не обработанные данные ЭКГ - RAW_DATA
				case TGDevice.MSG_RAW_DATA:
					RawValue = (msg.arg1 * 18.3) / 128;
					Raw_Value = (int) RawValue;
					if (isRecording) {
						unwritedRawValues.add(Raw_Value);
					}
					Log.d("TAG ", "RAW " + Raw_Value);
					updateChart(Raw_Value);
					break;

				// RR интервалы
				case TGDevice.MSG_EKG_RRINT:
					//  if(buff==0 || Math.abs(buff-msg.arg1)<100){buff = msg.arg1; rr_interval = buff; updateChart(hseries,rr_interval);}else{rr_interval = 0; artef++;}
					//  if(artef > 5){Toast.makeText(MainActivity.this, "Большое количество артефактов",Toast.LENGTH_LONG).show(); artef =0;}

					// rr_interval = msg.arg1;
					// updateChart(hseries,msg.arg1);

					//rr_interval = (int) RawValue;
					//rr_interval = msg.arg1;

					//updateChart(hseries,rr_interval);

					//                    if(i<40)
					//{
					// bufer[i] = msg.arg1;
					//   rr_interval = msg.arg1;
					//   updateChart(hseries,msg.arg1);
					//  rr_interval = i;
					//   i++;
					//} else{
					//     if(k<40){rr_interval = bufer[k]; k++;}else{rr_interval = 222;}
					//                   }
					//else{
					//   bufer_sr = (bufer[0]+bufer[1]+bufer[2]+bufer[3])/4;
					//   if(Math.abs(bufer_sr-msg.arg1)<100)
					//   {
                     /*       bufer[0] = bufer[1];
                            bufer[1] = bufer[2];
                            bufer[2] = bufer[3];
                            bufer[3] = msg.arg1;
                            rr_interval = msg.arg1;
                            updateChart(hseries,msg.arg1);
                        }
                      }*/
					rr_interval = msg.arg1;
					if (rr_interval != 0 && isRecording) {
						unwritedRRvalues.add(rr_interval);
					}
					//updateChart(hseries, msg.arg1);
					break;

				// Обработанные данные ЭКГ
				case TGDevice.MSG_RAW_MULTI:
					Log.d("TAG LOG", "MSG_RAW_MULTI" + msg);
					break;
				case TGDevice.MSG_RAW_MULTI_NEW:
					Log.d("TAG LOG", "MSG_RAW_MULTI_NEW" + msg);
					break;

				// Пульс
				case TGDevice.MSG_HEART_RATE:
					heartRate = msg.arg1;
					heart_Rate.setText(msg.arg1 + "");
					//updateChart(hseries,msg.arg1);
					break;

				case TGDevice.MSG_ATTENTION:
					break;

				case TGDevice.MSG_MEDITATION:
					break;

				case TGDevice.MSG_BLINK:
					break;

				case TGDevice.MSG_RAW_COUNT:
					break;

				// Разряжен аккумулятор
				case TGDevice.MSG_LOW_BATTERY:
					Toast.makeText(getApplicationContext(), "Низкий заряд батареи!", Toast.LENGTH_SHORT).show();
					break;

				case TGDevice.MSG_RELAXATION:
					break;

				case TGDevice.MSG_RESPIRATION:
					//print out about 64s after touching, then update per 10s
					//Float r = (Float)msg.obj;
					//tv_RespirationRate.setText(String.valueOf(msg.obj));
					//Toast.makeText(getApplicationContext(), "Resp Rate: "+String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
					break;

				case TGDevice.MSG_HEART_AGE:
					//tv_HeartAge.setText( msg.arg1+"" );
					break;

				case TGDevice.MSG_HEART_AGE_5MIN:
					//tv_5minHeartAge.setText( msg.arg1+"" );
					break;

				case TGDevice.MSG_EKG_IDENTIFIED:
					//updateChart(hseries,msg.arg1);
					break;

				case TGDevice.MSG_ERR_CFG_OVERRIDE:
					switch (msg.arg1) {
						case TGDevice.ERR_MSG_BLINK_DETECT:
							Toast.makeText(getApplicationContext(), "Override: blinkDetect", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_TASKFAMILIARITY:
							Toast.makeText(getApplicationContext(), "Override: Familiarity", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_TASKDIFFICULTY:
							Toast.makeText(getApplicationContext(), "Override: Difficulty", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_POSITIVITY:
							Toast.makeText(getApplicationContext(), "Override: Positivity", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_RESPIRATIONRATE:
							Toast.makeText(getApplicationContext(), "Override: Resp Rate", Toast.LENGTH_SHORT).show();
							break;
						default:
							Toast.makeText(getApplicationContext(), "Override: code: " + msg.arg1 + "", Toast.LENGTH_SHORT).show();
							break;
					}
					break;
				case TGDevice.MSG_ERR_NOT_PROVISIONED:
					switch (msg.arg1) {
						case TGDevice.ERR_MSG_BLINK_DETECT:
							Toast.makeText(getApplicationContext(), "No Support: blinkDetect", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_TASKFAMILIARITY:
							Toast.makeText(getApplicationContext(), "No Support: Familiarity", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_TASKDIFFICULTY:
							Toast.makeText(getApplicationContext(), "No Support: Difficulty", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_POSITIVITY:
							Toast.makeText(getApplicationContext(), "No Support: Positivity", Toast.LENGTH_SHORT).show();
							break;
						case TGDevice.ERR_MSG_RESPIRATIONRATE:
							Toast.makeText(getApplicationContext(), "No Support: Resp Rate", Toast.LENGTH_SHORT).show();
							break;
						default:
							Toast.makeText(getApplicationContext(), "No Support: code: " + msg.arg1 + "", Toast.LENGTH_SHORT).show();
							break;
					}
					break;
				default:
					break;
			}
		}
	};

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (!PermissionUtil.verifyPermissions(grantResults)) {
			return;
		}
		if (requestCode == REQUEST_CODE_STORAGE) {
			tryStartRecording();
		}
	}

	/**
	 * Special {@link AdvancedLineAndPointRenderer.Formatter} that draws a line
	 * that fades over time.  Designed to be used in conjunction with a circular buffer model.
	 */
	public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

		private int trailSize;

		public MyFadeFormatter(int trailSize) {
			this.trailSize = trailSize;
		}

		@Override
		public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
			// offset from the latest index:
			int offset;
			if (thisIndex > latestIndex) {
				offset = latestIndex + (seriesSize - thisIndex);
			} else {
				offset = latestIndex - thisIndex;
			}

			float scale = 255f / trailSize;
			int alpha = (int) (255 - (offset * scale));
			getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
			return getLinePaint();
		}
	}

	/**
	 * Primitive simulation of some kind of signal.  For this example,
	 * we'll pretend its an ecg.  This class represents the data as a circular buffer;
	 * data is added sequentially from left to right.  When the end of the buffer is reached,
	 * i is reset back to 0 and simulated sampling continues.
	 */
	public static class ECGModel implements XYSeries {

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

	private Format format = new Format() {
		@Override
		public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo,
		                           @NonNull FieldPosition pos) {
			Number num = (Number) obj;
			toAppendTo.append(String.valueOf(num.intValue()));
			return toAppendTo;
		}

		@Override
		public Object parseObject(String source, @NonNull ParsePosition pos) {
			return null;
		}
	};

}

