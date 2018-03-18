package com.vgtu.ekg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Башлыков on 10.02.2018.
 */

public class PacientActivity extends Activity {

    // Создаём пустой массив для хранения имен
    private ArrayList<String> PacientNames = new ArrayList<>();
    private String KEY="pacient";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pacient_layout);







        // получаем экземпляр элемента ListView
        ListView listView = (ListView) findViewById(R.id.listView);
        final EditText editText = (EditText) findViewById(R.id.editPacient);




        // Создаём адаптер ArrayAdapter, чтобы привязать массив к ListView
        final ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, PacientNames);
        // Привяжем массив через адаптер к ListView
        listView.setAdapter(adapter);

        // Прослушиваем нажатия клавиш
        editText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        PacientNames.add(0, editText.getText().toString());
                        adapter.notifyDataSetChanged();
                        editText.setText("");
                        return true;
                    }
                return false;
            }
        });

        //Обрабатываем щелчки на элементах ListView:
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Intent intent = new Intent();
                intent.setClass(PacientActivity.this, MainActivity.class);

                intent.putExtra("head", position);

                //запускаем вторую активность
                startActivity(intent);
            }
        });


    }



    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {


        savedInstanceState.putStringArrayList(KEY,PacientNames);
        super.onSaveInstanceState(savedInstanceState);


    }


}
