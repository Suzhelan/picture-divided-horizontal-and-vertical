package top.linl.imageutil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    public static final String OUTPUT_PATH = "OUTPUT_PATH";
    public static final String SOURCE_DIRECTORY = "SOURCE_DIRECTORY";
    //先定义
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE"};
    private static Context appContext;
    private ScrollView logRootView;
    private EditText srcDirView;
    private EditText outputDirView;
    private Button startButton;


    //然后通过一个函数来申请
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Context getAppContext() {
        return appContext;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getAppContext();
        //初始化查找控件
        logRootView = findViewById(R.id.logs);
        srcDirView = findViewById(R.id.source_directory);
        outputDirView = findViewById(R.id.output_directory);
        startButton = findViewById(R.id.start_run);
        initView();

    }

    private void initView() {
        onStartRunButton onClick = new onStartRunButton();
        //为控件设置属性
        startButton.setOnClickListener(onClick);
        //验证读写权限
        verifyStoragePermissions(this);
    }


    private void addLogToView(String text) {
        @SuppressLint("InflateParams")
        LinearLayout logItem = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.log_item, null);
        TextView log = logItem.findViewById(R.id.log_text);
        log.setText(text);
        logRootView.addView(logItem);
    }

    private class onStartRunButton implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            try {
                //开始运行
                Intent intent = new Intent(getApplicationContext(), OutputTask.class);
                Bundle bundle = new Bundle();
                bundle.putString(OUTPUT_PATH, outputDirView.getText().toString());
                bundle.putString(SOURCE_DIRECTORY, srcDirView.getText().toString());
                intent.putExtras(bundle);
                //启动服务
                startForegroundService(intent);

                //清理输入框
                outputDirView.setText(null);
                srcDirView.setText(null);

                Toast.makeText(MainActivity.this, "已发布任务到服务中", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();

            }
        }

    }
}