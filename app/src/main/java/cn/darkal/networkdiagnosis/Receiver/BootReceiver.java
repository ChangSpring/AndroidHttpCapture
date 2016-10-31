package cn.darkal.networkdiagnosis.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.darkal.networkdiagnosis.service.MoniorService;

/**
 * Created by Alfred on 2016/10/28.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i("BootReceiver", "手机开机了....");
            startMonitorService(context);
        }
        if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            startMonitorService(context);
        }
    }

    private void startMonitorService(Context context) {
        Intent intent = new Intent(context, MoniorService.class);
        context.startService(intent);
    }
}
