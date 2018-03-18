package com.vgtu.ekg;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.neurosky.thinkgear.HeartRateAcceleration;
import com.neurosky.thinkgear.NeuroSkyHeartMeters;
import com.neurosky.thinkgear.TGDevice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.KeyEvent;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;

import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static android.content.ContentValues.TAG;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

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
    private TextView recordTime;
//    private int time = 0;
    public boolean new_pacient_directory=true;
    public int bt_connected = 1;
    int heartRate_vs=0;
    int subjectContactQuality_last;
    int subjectContactQuality_cnt;
    boolean rec_per=false;
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
    private XYSeries hseries;
    public String formattedDate;
    private boolean isRecording = false;
// Объявление переменных

//Раздел рисования графика
    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    XYSeriesRenderer dxyrenderer = new XYSeriesRenderer(),hxyrenderer;
    //Создание хранилища данных
    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    //private int addX = -1, addY;
    int[] xv = new int[5000];
    int[] yv = new int[5000];

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
        Context context = getBaseContext(); //получаем контекст

        //setup the draw section
        renderer.setPointSize(3);
        renderer.setZoomButtonsVisible(true);
        //renderer.setShowGrid(show_grid);
        renderer.setXAxisMax(5000);
        renderer.setXAxisMin(0);
        renderer.setYAxisMax(1400);
        renderer.setYAxisMin(-100);
        renderer.setXLabels(10);
        renderer.setYLabels(10);
        renderer.setAxesColor(Color.WHITE);

        //set up heart rate
        hxyrenderer = new XYSeriesRenderer();
        //hxyrenderer.setColor(Color.BLUE);
        hxyrenderer.setPointStyle(PointStyle.DIAMOND);
        renderer.addSeriesRenderer(hxyrenderer);
        hseries = new XYSeries("RR интервал");
        dataset.addSeries(hseries);
        //setup the draw in screen
        LinearLayout linear = findViewById(R.id.linear1);
        chart = ChartFactory.getLineChartView(MainActivity.this, dataset, renderer);
        linear.addView(chart,new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

// Обновление графика и его сдвиг
    public void updateChart(XYSeries series,int newValue)
    {
        dataset.removeSeries(series);
        int length = series.getItemCount();
        int addX;
        int addY;
        //Длина окна графика по оси Х
        if(length>=5000)
        {
            for(int i = 0;i<length-1;i++)
            {
                xv[i] = (int)series.getX(i);
                yv[i] = (int)series.getY(i+1);
            }
            series.clear();
            addX = length-1;
            addY = newValue;
            for(int j = 0;j<length-1;j++)
            {
                series.add(xv[j], yv[j]);
            }
            series.add(addX, addY);
            dataset.addSeries(series);
        }
        else
        {
            addX = length;
            addY = newValue;
            series.add(addX, addY);
            dataset.addSeries(series);
        }
        chart.invalidate();
    }

//Раздел рисования графика

//Верхняя строка состояния
    private void InitActionBar() {

       // ActionBar bar;

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar);
        View mview = getSupportActionBar().getCustomView();
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#131313")));

        btnMinimize = mview.findViewById(R.id.action_bar_minimize);
        heart_Rate =mview.findViewById(R.id.heartRate);
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

        mImageView=mview.findViewById(R.id.action_bar_heartRateIcon);
        mImageView.setBackgroundResource(R.drawable.heart_anim);
        mAnimationDrawable = (AnimationDrawable)mImageView.getBackground();

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
                        bt_connected =2;
                        btnBluetooth.setImageResource(R.drawable.ic_connect_bt);
                        break;
                    case 2:
                        tgDevice.close();
                        bt_connected=1;
                        btnBluetooth.setImageResource(R.drawable.ic_disconnect_bt);
                        break;
                }

            }
        });





        btnSettings = mview.findViewById(R.id.action_bar_settings);
        btnSettings.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v){

                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });

        ImageButton btnRecord = mview.findViewById(R.id.action_bar_play);
        btnRecord.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!isRecording)
                    //startRecording();
                    if(rec_per) {

                        startRecording();}
                    else {
                        Toast.makeText(getApplicationContext(), "Подключите устройство!",
                                Toast.LENGTH_LONG).show();
                    }
            }
        });

        ImageButton btnStopRecording = mview.findViewById(R.id.action_bar_stop);
        btnStopRecording.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                stopRecording();
            }
        });

        recordTime = (TextView)mview.findViewById(R.id.recordTime);

    }

// Включение записи в файл
    private void startRecording() {
        isRecording = true;
        Thread timer = new Thread() {
            public void run() {
                Long startTime = System.currentTimeMillis();
                Long endTime;
                Long delta;
                //String seconds, minutes, milliseconds;
                while (isRecording) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    endTime = System.currentTimeMillis();
                    delta = endTime - startTime;
                    final int seconds = Long.valueOf(delta / 1000 % 60).intValue();
                    final int minutes = Long.valueOf(delta / 1000 / 60).intValue();
                    final int milliseconds = Long.valueOf(delta % 1000).intValue();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String s1 = ":";
                            Locale l = Locale.ENGLISH;
                            String s = String.format(l, "%02d" , minutes) + s1 + String.format(l, "%02d" , seconds) + s1 + String.format(l, "%02d" , milliseconds);
                            recordTime.setText(s);
                            //recordTime.setText(minutes + ":" + seconds + "." + String.format("%2s",milliseconds));
                            }
                    });
                }

            }
        };
        timer.start();
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        formattedDate = df.format(calendar.getTime());
        //Date now = new Date();
        if (new_pacient_directory) {
            Thread fileWriterThread = new Thread() {
                public void run() {
                    String path = Environment.getExternalStorageDirectory() + File.separator + "ECG data" + File.separator +mCurFileName;
                    File folder = new File(path);
                    folder.mkdirs();
                    File file = new File(folder, formattedDate+ ".txt");
                    try {
                        file.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(file);
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
                        //DataOutputStream dataOut =new DataOutputStream(fOut); //Это записывает значения в файл в двоичном виде
                        while (isRecording) {
                            while (heartRate_vs != heartRate) {
                            //    Date now = new Date();
                                heartRate_vs = heartRate;
                                // запись значений RR интервалов
                                //if (rr_interval!=0){outputStreamWriter.write(String.valueOf(rr_interval) + "\n");} // запись в файл в текстовом виде
                                //dataOut.writeInt(rr_interval); //Это записывает значения в файл в двоичном виде
                                // запись значений не обработанного ЭКГ
                                outputStreamWriter.write(String.valueOf(Raw_Value) + "\n");
                               Thread.sleep(0);
                            }
                        }
                        outputStreamWriter.close();
                        fOut.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            fileWriterThread.start();
        }
        else  {
            Thread fileWriterThread = new Thread() {
                public void run() {
                    String path = Environment.getExternalStorageDirectory() + File.separator + "ECG data";
                    File folder = new File(path);
                    folder.mkdirs();
                    File file = new File(folder, formattedDate+ ".txt");
                    try {
                        file.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(file);
                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
                        while (isRecording) {
                           while (heartRate_vs != heartRate) {
                            //   Date now = new Date();
                                heartRate_vs = heartRate;
                             //  if (rr_interval!=0){outputStreamWriter.write(String.valueOf(rr_interval) + "\n");}
                               Thread.sleep(0);
                            }
                        }
                        outputStreamWriter.close();
                        fOut.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            fileWriterThread.start();
        }

    }

// Выключение записи в файл
    private void stopRecording(){
        isRecording = false;
        Toast.makeText(getApplicationContext(),"Запись завершена!",Toast.LENGTH_LONG).show();
    }

//обработчик настроек в меню
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // читаем цвет фона из ListPreference для цвета фона
        int color;
        String regular = prefs.getString(getString(R.string.pref_style), "");
        if (regular.contains("Черный")) {
            color = Color.BLACK;
            renderer.setBackgroundColor(color);

        } else if (regular.contains("Белый")) {
            color = Color.WHITE;
            renderer.setBackgroundColor(color);

        }
        renderer.setApplyBackgroundColor(true);
        // читаем цвет фона из ListPreference для цвета кривой графика
        int color_curve;
        String regular_curve = prefs.getString(getString(R.string.pref_mesh), "");
        if (regular_curve.contains("Синий")) {
            color_curve = Color.BLUE;
            hxyrenderer.setColor(color_curve);

        } else if (regular_curve.contains("Зеленый")) {
            color_curve = Color.GREEN;
            hxyrenderer.setColor(color_curve);

        } else if (regular_curve.contains("Красный")) {
            color_curve = Color.RED;
            hxyrenderer.setColor(color_curve);

        }
        // читаем установленное значение из CheckBoxPreference
        if (prefs.getBoolean(getString(R.string.pref_curve), true))
        {

            renderer.setShowGrid(true);
        }
        else {
            renderer.setShowGrid(false);
        }
        // читаем размер шрифта из EditTextPreference
        mCurFileName = prefs.getString(getString(R.string.new_pacient), "20");
        // читаем установленное значение из CheckBoxPreference
        new_pacient_directory = prefs.getBoolean(getString(R.string.pacient_directory), true);
    }

//Выключение приложения при нажатии кнопки назад на телефоне
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event)
    {
        if(keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0)
        {
            tgDevice.close();
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



//Прием сообщений от TGDevice
    @SuppressLint("HandlerLeak")
    private final  Handler handler = new Handler() {
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
                    Toast.makeText(MainActivity.this, "Модель идентифицирована",Toast.LENGTH_LONG).show();
                    tgDevice.setBlinkDetectionEnabled(true); // not allowed on EKG hardware, here to show the override message
                    tgDevice.setRespirationRateEnable(true);
                    break;

                case TGDevice.MSG_STATE_CHANGE:
                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            Toast.makeText(MainActivity.this, "Установка соединения",Toast.LENGTH_LONG).show();
                            break;
                        case TGDevice.STATE_CONNECTED:
                            Toast.makeText(MainActivity.this, "Соединение установлено",Toast.LENGTH_LONG).show();
                            tgDevice.start();
                            tgDevice.pass_seconds = 15;
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            Toast.makeText(MainActivity.this, "Не найдено устройство",Toast.LENGTH_LONG).show();
                            Toast.makeText(MainActivity.this, "Должно быть сопряжено одно Bluetooth устройство",Toast.LENGTH_LONG).show();
                            break;
                        /*case TGDevice.STATE_NOT_PAIRED:
	                	tv.append("not paired\n");
	                	break;*/
                        case TGDevice.STATE_DISCONNECTED:
                            Toast.makeText(MainActivity.this, "Соединение разорвано",Toast.LENGTH_LONG).show();
                    }
                    break;

// Проверка соединены ли электроды с кожей
                case TGDevice.MSG_POOR_SIGNAL:
            	   /* Вывод на экран сообщения о качестве сигнала каждые 90 секунд */
                    if (subjectContactQuality_cnt >= 90 || msg.arg1 != subjectContactQuality_last) {
                        if (msg.arg1 == 200) { //200 is for BMD
                            Toast.makeText(MainActivity.this, "Сигнал ЭКГ хороший",Toast.LENGTH_LONG).show();
                            rec_per=true;
                            mAnimationDrawable.start();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Нет сигнала ЭКГ! Датчик надо закрепить на груди.",Toast.LENGTH_LONG).show();
                        }
                        subjectContactQuality_cnt = 0;
                        subjectContactQuality_last = msg.arg1;
                    }
                    else subjectContactQuality_cnt++;
                break;

// Вывод переменных их класса TGDevice.class

                // Не обработанные данные ЭКГ - RAW_DATA
                case TGDevice.MSG_RAW_DATA:
                     RawValue = (msg.arg1 * 18.3) / 128;
                     Raw_Value = (int)RawValue;
                     //updateChart(hseries,Raw_Value);
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
                      //updateChart(hseries,msg.arg1);
                break;

                 // Обработанные данные ЭКГ
                case TGDevice.MSG_RAW_MULTI:
                break;

                 // Пульс
                case TGDevice.MSG_HEART_RATE:
                    heartRate=msg.arg1;
                    heart_Rate.setText(msg.arg1+"");
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
                            Toast.makeText(getApplicationContext(), "Override: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getApplicationContext(), "No Support: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                break;
                 default:
                break;
            }
        }
    };
}

