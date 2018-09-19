package com.example.arthur.anti_asp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import static java.lang.Math.sqrt;

//TODO arffをwekaのデータセットに設定, サンプリングを10秒程度に設定

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager manager;
    private TextView values;
    private TextView status;
    float combined_acc;
    int counter = 0;
    boolean click_flag = false;
    boolean dflag = false;
    boolean stand = false;
    boolean walk = false;
    boolean run = false;
    Instances instances;
    Classifier classifier;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        values = (TextView)findViewById(R.id.text_view);
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        status = (TextView)findViewById(R.id.status_view);
        status.setText("待機中");

    //サンプリングボタン
        //立ち状態
        Button buttonSave_s = findViewById(R.id.button_l_stand);
        buttonSave_s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog();
                stand = true;
            }
        });
        //歩き状態
        Button buttonSave_w = findViewById(R.id.button_l_walk);
        buttonSave_w.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog();
                walk = true;
            }
        });
        //走り状態
        Button buttonSave_r = findViewById(R.id.button_l_run);
        buttonSave_r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog();
                run = true;
            }
        });

        //学習ボタン: csvからarff生成 & wekaの学習フェーズだけ実行
        Button button_learn = findViewById(R.id.button_learn);
        button_learn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = "acc_stand.csv";
                CsvToArff(/*path, "stand"*/);

                //weka
                try {
                    //分類器生成
                    ConverterUtils.DataSource source = new ConverterUtils.DataSource("/storage/emulated/0/anti_asp/acc.arff");
                    instances = source.getDataSet();
                    instances.setClassIndex(1);
                    classifier = new J48();
                    classifier.buildClassifier(instances);
                    System.out.println("分類器の作成に成功しました");

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("分類器の作成に失敗しました");
                }

            }

        });
        //推定ボタン　実測値をwekaに渡して結果を表示
        Button button_estimate = findViewById(R.id.button_estimate);
        button_estimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                click_flag = true;//wekaに現在の3軸加速度を引き渡し, 推定

            }
        });
        //削除ボタン
        Button Delete_button = findViewById(R.id.delete_button);
        Delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file1 = new File("/storage/emulated/0/anti_asp/acc.arff");
                file1.delete();
                File file2 = new File("/storage/emulated/0/anti_asp/acc_stand.csv");
                file2.delete();
                File file3 = new File("/storage/emulated/0/anti_asp/acc_walk.csv");
                file3.delete();
                File file4 = new File("/storage/emulated/0/anti_asp/acc_run.csv");
                file4.delete();
                //Toast.makeText(this, "学習データの削除が完了しました", Toast.LENGTH_SHORT).show();
                delete_Dialog();
            }});
    }

    //ファイル保存
    public void saveFile(String file, String str/*float value*/) {

        // try-with-resources
        try {
            FileOutputStream fileOutputstream = new FileOutputStream(new File("/storage/emulated/0/anti_asp/" + file), true);
            fileOutputstream.write(str.getBytes());
            //FileWriter f = new FileWriter("/storage/emulated/0/anti_asp/" + file, true);
            //PrintWriter p = new PrintWriter(new BufferedWriter(f));

            System.out.println("csv出力が完了しました。");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //csv to arff converter
    public void  CsvToArff(/*String file, String STATUS*/) {

        // try-with-resources
        String data = null;
        try {
            // 出力ファイルの作成
            FileOutputStream fos = new FileOutputStream(new File("/storage/emulated/0/anti_asp/acc.arff"),false);

            // ヘッダーを設定する
            String hedder =("@relation acceleration\n\n" + "@attribute acc real\n" + "@attribute state  {stand,walk,run}\n\n" + "@data\n");
            fos.write(hedder.getBytes());


            //acc_stand.csv -> acc.arff
            try {
                FileInputStream read_file = new FileInputStream("/storage/emulated/0/anti_asp/acc_stand.csv");
                byte[] size = new byte[read_file.available()];
                read_file.read(size);
                data = new String(size);

            } catch (IOException ex){
                ex.printStackTrace();
                System.out.println("acc_stand.csvのオープンに失敗しました");
            }
            fos.write(data.getBytes());
            System.out.println("acc_standをarffに出力しました");

            //acc_walk.csv -> acc.arff
            try {
                FileInputStream read_file = new FileInputStream("/storage/emulated/0/anti_asp/acc_walk.csv");
                byte[] size = new byte[read_file.available()];
                read_file.read(size);
                data = new String(size);

            } catch (IOException ex){
                ex.printStackTrace();
                System.out.println("acc_walk.csvのオープンに失敗しました");
            }
            fos.write(data.getBytes());
            System.out.println("acc_walkをarffに出力しました");

            //acc_run.csv -> acc.arff
            try {
                FileInputStream read_file = new FileInputStream("/storage/emulated/0/anti_asp/acc_run.csv");
                byte[] size = new byte[read_file.available()];
                read_file.read(size);
                data = new String(size);

            } catch (IOException ex){
                ex.printStackTrace();
                System.out.println("acc_wrun.csvのオープンに失敗しました");
            }
            fos.write(data.getBytes());
            System.out.println("acc_runをarffに出力しました");

        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("arffの出力に失敗しました");
        }

        Toast.makeText(this, "arff出力が完了しました", Toast.LENGTH_SHORT).show();

    }



    @Override
    protected void onStop() {
        super.onStop();
        // Listenerの登録解除
        manager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() > 0) {
            Sensor s = sensors.get(0);
            manager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float accX = event.values[SensorManager.DATA_X];
            float accY = event.values[SensorManager.DATA_Y];
            float accZ = event.values[SensorManager.DATA_Z];
            combined_acc = (float) sqrt(accX*accX + accY*accY + accZ*accZ);

            String str = "ACCELEROMETER"
                    + "\nX: " + accX
                    + "\nY: " + accY
                    + "\nZ: " + accZ
                    + "\n合成加速度: " + combined_acc;
            values.setText(str);
            counter++;

            //stand
            if(dflag == true && stand == true){
                saveFile("acc_stand.csv", String.valueOf(combined_acc + "," + "stand\n"));
            }
            //walk
            else if(dflag == true && walk == true){
                saveFile("acc_walk.csv", String.valueOf(combined_acc + "," + "walk\n"));
            }
            //run
            else if(dflag == true && run == true){
                saveFile("acc_run.csv", String.valueOf(combined_acc + "," + "run\n"));
            }
            //負荷軽減のために推定の間隔をセンサの更新間隔の1/10にする
            if(click_flag == true && counter%10 == 0){
                weka();
            }
        }
    }

    public void weka() {
        try {
            Evaluation eval = new Evaluation(instances);
            eval.evaluateModel(classifier, instances);
            System.out.println(eval.toSummaryString());

            FastVector out = new FastVector(2);
            out.addElement("stand");
            out.addElement("walk");
            out.addElement("run");
            Attribute acc = new Attribute("acc", 0);
            Attribute state = new Attribute("state",out, 1);
            FastVector win = new FastVector(2);

            //分類したいデータを与える
            Instance instance = new DenseInstance(3);
            instance.setValue(acc, combined_acc);//combined_acc:MainActivityから引き渡す現在の3軸合成加速度
            instance.setDataset(instances);

            double result = classifier.classifyInstance(instance);
            System.out.println(result);

            if (result == 0.0){
                //stand
                status.setText("STANDING");
            }else if(result == 1.0){
                //walk
                status.setText("WALKING\n\n歩きスマホはやめましょう！");
            }else if(result == 2.0){
                //run
                status.setText("RUNNING\n\n危険！走りスマホ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("weka分類に失敗しました");
        }

    }


    //外部ストレージに保存するためのパーミッション確認
    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private static void verifyStoragePermissions(Activity activity) {
        int readPermission = ContextCompat.checkSelfPermission(activity, mPermissions[0]);
        int writePermission = ContextCompat.checkSelfPermission(activity, mPermissions[1]);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }
    public void Dialog(){
        dflag = true;
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("---サンプリング中---\n\n10秒程度経過したら終了してください")
                .setPositiveButton("終了", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dflag = false;
                        walk = false;
                        stand = false;
                        run = false;
                    }
                });
        builder.show();
    }
    public void delete_Dialog(){
        dflag = true;
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("学習データを削除しました")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dflag = false;
                    }
                });
        builder.show();
    }
}

