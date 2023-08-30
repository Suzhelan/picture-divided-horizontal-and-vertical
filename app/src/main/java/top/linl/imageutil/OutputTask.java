package top.linl.imageutil;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class OutputTask extends Service {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final static String channelId = "ID";
    private final Set<Runnable> TaskStack = new HashSet<>();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //第一次创建服务时执行的方法，且只执行一次
    @Override
    public void onCreate() {
        super.onCreate();
        // 创建一个通知频道 NotificationChannel
        NotificationChannel channel = new NotificationChannel(channelId, "频道名称", NotificationManager.IMPORTANCE_HIGH);
        //桌面小红点
        channel.enableLights(false);
        //通知显示
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    //客户端通过调用startService()方法启动服务时执行该方法，可以执行多次
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Bundle bundle = intent.getExtras();
            //消息传入
            Runnable task = new Task(bundle);
            executorService.submit(task);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        return super.onStartCommand(intent, flags, startId);
    }


    //客户端调用unBindeService()方法断开服务绑定时执行该方法
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //服务被销毁时执行的方法，且只执行一次 （如果绑定了IBind则需要解绑后才销毁）
    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    //判断任务栈中是否是空的
    private boolean isAllTaskExecutionEnd() {
        return TaskStack.isEmpty();
    }

    private class Task implements Runnable {
        public static final String FORMAT = "%d/%d 当前 : %s";
        public final int flagId = newRandomNum(1, 7342734);//唯一通知标识 等会要拿来更新通知文本
        private final NotificationManager manager = (NotificationManager) OutputTask.this.getSystemService(Context.NOTIFICATION_SERVICE);//通知管理器
        private final AtomicInteger num = new AtomicInteger();//计数器
        private final NotificationCompat.Builder mBuilder;//通知构造器/编辑器
        private final File source_directory;
        private final File outputPath;

        @SuppressLint("DefaultLocale")
        public Task(Bundle bundle) {
            //信息
            //目标文件夹
            source_directory = new File(bundle.getString(MainActivity.SOURCE_DIRECTORY));
            //输出文件夹
            outputPath = new File(bundle.getString(MainActivity.OUTPUT_PATH));
            //通知构造器
            mBuilder = new NotificationCompat.Builder(OutputTask.this, channelId) // 在API16之后，可以使用build()来进行Notification的构建 Notification
                    .setContentTitle("正在分类 " + source_directory.getName())
//                    .setContentText("到"+outputPath.getAbsolutePath())//会占通知位置
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true);
            Notification notification = mBuilder.build();
            // 参数一：唯一的通知标识；参数二：通知消息。
            startForeground(flagId, notification);// 开始前台服务
        }

        public int newRandomNum(int min, int max) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            //添加到任务栈
            TaskStack.add(this);
            //创建输出文件夹
            File horizontalOutputPath = new File(outputPath.getAbsolutePath() + "/横屏壁纸/");
            File verticalOutputPath = new File(outputPath.getAbsolutePath() + "/竖屏壁纸/");
            File otherPath = new File(outputPath.getAbsolutePath() + "/例外");
            if (!horizontalOutputPath.exists())
                horizontalOutputPath.mkdirs();
            if (!verticalOutputPath.exists())
                verticalOutputPath.mkdirs();

            //所有子文件
            File[] fileList = source_directory.listFiles();

            for (File file : fileList) {
                if (file.isDirectory()) continue;
                try {
                    //读成字节数组 这样可以避免流只能被读一次的问题 就不用读两次(读大小和复制)
                    byte[] bitmapByteArray = FileUtils.readByByteArrayOutputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapByteArray, 0, bitmapByteArray.length);
                    //图片宽度
                    int width = bitmap.getWidth();
                    //图片高度
                    int height = bitmap.getHeight();

                    float result = ((float) height) / width;
                    String fileName = file.getName();
                    File outputFile;
                    //比例误差接近1:1 鉴定为头像
                    if (Math.max(0.8, result) == Math.min(result, 1.2)) {
                        outputFile =  new File(otherPath, fileName);
                        if (!outputFile.getParentFile().exists()) {
                            outputFile.mkdirs();
                        }
                        FileUtils.writeFileByBytes(outputFile, bitmapByteArray);
                    }
                    //高度比宽度小 鉴定为横屏壁纸
                    else if (height < width) {
                        outputFile = new File(horizontalOutputPath, fileName);
                        FileUtils.writeFileByBytes(outputFile, bitmapByteArray);
                    } else {
                        outputFile = new File(verticalOutputPath, fileName);
                        FileUtils.writeFileByBytes(outputFile, bitmapByteArray);
                    }

                    mBuilder//.setContentText(String.format(FORMAT, num.getAndIncrement(), fileList.length - 1, outputFile.getAbsolutePath()))
                            .setProgress(fileList.length - 1, num.get(), false)
                            //长文本通知
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(String.format(FORMAT, num.getAndIncrement(), fileList.length - 1, outputFile.getAbsolutePath())));
                    manager.notify(flagId, mBuilder.build());
                    //手动释放
                    bitmap.recycle();
                } catch (Exception e) {
                    FileUtils.writeTextToFile(OutputTask.this.getExternalFilesDir("Log").getAbsolutePath() + "/log.txt", Log.getStackTraceString(e) + "\n", true);
                }
            }

            //执行完毕从任务栈中移除
            TaskStack.remove(this);
            //从通知移除
            manager.cancel(flagId);
            //通知完成
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(OutputTask.this,"\""+source_directory.getName()+"\" 已分类完毕",Toast.LENGTH_LONG).show();
                }
            });
            //判断所有任务是否执行完毕 完毕了关闭服务
            if (isAllTaskExecutionEnd()) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(OutputTask.this,"所有任务已执行完毕",Toast.LENGTH_LONG).show();
                    }
                });
                stopService(new Intent(getApplicationContext(), OutputTask.class));
            }
        }

    }

}
