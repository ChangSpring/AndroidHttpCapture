package cn.darkal.networkdiagnosis.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import net.lightbody.bmp.core.har.HarEntry;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import cn.darkal.networkdiagnosis.Activity.MainActivity;
import cn.darkal.networkdiagnosis.R;
import cn.darkal.networkdiagnosis.SysApplication;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static cn.darkal.networkdiagnosis.service.MoniorService.CommandResult.upgradeRootPermission;

/**
 * Created by Alfred on 2016/10/27.
 */

public class MoniorService extends Service {
    public static final String COMMAND_SU = "su";
    public static final String COMMAND_SH = "sh";
    public static final String COMMAND_EXIT = "exit\n";
    public static final String COMMAND_LINE_END = "\n";

    /**
     * {专车,出发地输入栏,出发地,目的地输入栏,目的地,价格"我知道了"按钮,价格,计价规则详情(两个,为了加价,第二个坐标下移),详细说明}
     */
    private String[] commands = {"input tap 800 300",
            "input tap 440 1640", "input tap 640 340", "input tap 440 1800",
            "input tap 340 340", "input tap 340 1540", "input tap 540 1640",
            "input tap 540 1240", "input tap 540 1440", "input tap 540 1840",
            "input keyevent 4", "input keyevent 4", "input keyevent 4",
            "input keyevent 4", "input keyevent 4"};

    private Handler mHandler = new Handler();

    private final String TAG = "MoniorService";

    @Override

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() executed");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                notificationIntent, 0);
//        Notification notification = new Notification(R.mipmap.ic_launcher,
//                "监控数据", System.currentTimeMillis());
//        notification.setLatestEventInfo(this, "监控标题", "监控内容",
//                pendingIntent);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("监控数据");
        builder.setContentTitle("监控标题");
        builder.setContentText("监控内容");
        builder.setContentIntent(pendingIntent);

        startForeground(1, builder.getNotification());
        upgradeRootPermission(getPackageCodePath());

//        new Thread(new MyThread(), "thread").start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.postDelayed(runnable, 0);
        Log.d(TAG, "onStartCommand executed ! ");
        Toast.makeText(getApplicationContext(), "MonitorService onStartCommand exected ", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, MoniorService.class);
        startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            mHandler.postDelayed(this, 5 * 1000 * 1000);
            new Thread(new MyThread(), "thread").start();
        }
    };


    class MyThread implements Runnable {
        @Override
        public void run() {
            Intent mIntent = new Intent();
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.sdu.didi.psnger", "com.didi.sdk.app.DidiLoadDexActivity");
            mIntent.setComponent(comp);
            mIntent.setAction("android.intent.action.VIEW");
            startActivity(mIntent);

            try {
                Thread.sleep(10 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            CommandResult commandResult = execCommand(commands, true, true);
            uploadGrabbingData();
//            Message message = mHandler.obtainMessage();
//            message.what = 1;
//            message.obj = commandResult;
//            mHandler.sendMessage(message);
        }
    }

    private void uploadGrabbingData() {
        List<HarEntry> list = ((SysApplication) getApplication()).getHarEntryList();

        StringBuilder stringBuilder = new StringBuilder();
        for (HarEntry harEntry : list){
            stringBuilder.append(harEntry.getRequest().getUrl()).append("\\x01");
        }

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("",stringBuilder.toString())
                .build();

        Request request = new Request.Builder().url("").post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //在子线程中执行
                Log.i(TAG,"response = " + response.body().string());
            }
        });

    }

    /**
     * execute shell commands
     *
     * @param commands        command array
     * @param isRoot          whether need to run with root
     * @param isNeedResultMsg whether need result msg
     * @return if isNeedResultMsg is false, {@link CommandResult#successMsg} is null and
     * {@link CommandResult#errorMsg} is null.
     * <p>
     * if {@link CommandResult#result} is -1, there maybe some excepiton.
     */
    public CommandResult execCommand(String[] commands, boolean isRoot, boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }


        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;


        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(isRoot ? COMMAND_SU : COMMAND_SH);
            os = new DataOutputStream(process.getOutputStream());
            for (int i = 0; i < commands.length; i++) {
                if (commands[i] == null) {
                    continue;
                }
                Log.i(TAG, "current command = " + commands[i]);
                os.write(commands[i].getBytes());
                os.writeBytes(COMMAND_LINE_END);
                os.flush();

                if (i != commands.length - 2) {
                    Thread.sleep(6000);
                }
            }
            os.writeBytes(COMMAND_EXIT);
            os.flush();


            result = process.waitFor();
// get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (process != null) {
                process.destroy();
            }

        }
        return new CommandResult(result, successMsg == null ? null : successMsg.toString(), errorMsg == null ? null : errorMsg.toString());
    }

    /**
     * result of command
     * <p>
     * <p>
     * <p>
     * {@link CommandResult#result} means result of command, 0 means normal, else means error, same to excute in
     * linux shell
     * <p>
     * {@link CommandResult#successMsg} means success message of command result
     * <p>
     * {@link CommandResult#errorMsg} means error message of command result
     *
     * @author Trinea 2013-5-16
     */
    public static class CommandResult {


        /**
         * result of command
         **/
        public int result;
        /**
         * success message of command result
         **/
        public String successMsg;
        /**
         * error message of command result
         **/
        public String errorMsg;


        public CommandResult(int result) {
            this.result = result;
        }


        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        /**
         * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
         *
         * @return 应用程序是/否获取Root权限
         */
        public static boolean upgradeRootPermission(String pkgCodePath) {
            Process process = null;
            DataOutputStream os = null;
            try {
                String cmd = "chmod 777 " + pkgCodePath;
                process = Runtime.getRuntime().exec("su"); //切换到root帐号
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(cmd + "\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                    process.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

    }
}
