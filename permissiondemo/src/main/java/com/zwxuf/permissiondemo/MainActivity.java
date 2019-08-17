package com.zwxuf.permissiondemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private Button bn_request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bn_request = (Button) findViewById(R.id.bn_request);

        bn_request.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bn_request:
                requestPermissions();
                break;
        }
    }

    /**
     * 主线程异步请求权限
     */
    private void requestPermissions() {

        final ProgressDialog mDialog = new ProgressDialog(this);
        mDialog.setMessage("正在请求权限...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        new PermissionManager(this)
                //组建一个任务
                .buildTask(new PermissionManager.Task()
                        .setPackageName(getPackageName())
                        //允许运行时权限
                        .allow(
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        //允许悬浮窗
                        .allowDrawOverlays(true)
                        //允许修改系统设置
                        .allowWriteSettings(true)
                        //允许投射屏幕
                        .allowProjectMedia(true)
                        //允许有权查看使用情况
                        .allowUsageStats(true)
                        //允许无障碍服务
                        .allowAccessibility(MyAccessibilityService.class, true)
                        //允许设备管理器
                        .allowDeviceAdmin(MyDeviceAdminReceiver.class, true)


                )
                //开启GPS服务
                .putSecure(Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "+gps")
                //开启wifi
                .putGlobal(Settings.Global.WIFI_ON, 1)
                //回调，用于接收执行结果, handler要放在主线程
                .setCallbacks(new PermissionManager.Callbacks() {
                    @Override
                    public void onSuccess(List<PermissionManager.Task> tasks, PermissionManager.Sets sets, boolean isAllSuccessful) {
                        if (mDialog != null) mDialog.cancel();
                        Toast.makeText(MainActivity.this, "命令执行完成", Toast.LENGTH_SHORT).show();

                        PermissionManager.Task task = tasks.get(0);
                        Log.e(TAG, "isAllSuccessful: " + isAllSuccessful + "\n"
                                + "isSuccessful: " + task.isSuccessful() + "\n"
                                + "isAllowPermissionSuccessful: " + task.isAllowPermissionSuccessful() + "\n"
                                + "isAllowOpSuccessful: " + task.isAllowOpSuccessful() + "\n"
                                + "isAccessibilitySuccessful: " + task.isAccessibilitySuccessful() + "\n"
                                + "isDeviceAdminSuccessful: " + task.isDeviceAdminSuccessful() + "\n"
                        );

                        if (sets.getSecureSetResult().get(Settings.Secure.LOCATION_PROVIDERS_ALLOWED) == Boolean.TRUE) {
                            Log.e(TAG, "open gps success");
                        } else {
                            Log.e(TAG, "open gps failure");
                        }
                        if (sets.getGlobalSetResult().get(Settings.Global.WIFI_ON) == Boolean.TRUE) {
                            Log.e(TAG, "open wifi success");
                        } else {
                            Log.e(TAG, "open wifi failure");
                        }
                    }

                    @Override
                    public void onFailure(List<PermissionManager.Task> tasks, PermissionManager.Sets sets) {
                        if (mDialog != null) mDialog.cancel();
                        Toast.makeText(MainActivity.this, "命令执行失败，设备可能没有root权限", Toast.LENGTH_SHORT).show();
                    }
                }, new Handler())
                .start();


    }


    /**
     * 同步请求权限
     */
    private void syncRequestPermissions() {
        final PermissionManager pm = new PermissionManager(this);

        //同步执行可能会影响ui，建议放在子线程执行
        PermissionManager.Result result = pm.buildTask(new PermissionManager.Task()
                .setPackageName(getPackageName())
                //允许运行时权限
                .allow(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                //允许悬浮窗
                .allowDrawOverlays(true)

        ).execute();

        //执行结果
        if (result.isExecuteSuccess()) {
            PermissionManager.Task task = result.getTasks().get(0);

            Log.e(TAG, "isAllSuccessful: " + result.isAllSuccessful() + "\n"
                    + "isSuccessful: " + task.isSuccessful() + "\n"
                    + "isAllowPermissionSuccessful: " + task.isAllowPermissionSuccessful() + "\n"
                    + "isAllowOpSuccessful: " + task.isAllowOpSuccessful() + "\n"
            );

            //Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "命令执行失败，设备可能没有root权限");
        }


    }


}
