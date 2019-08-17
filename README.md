＃app
一个权限申请管理类，可自动获取所需权限，支持其它应用，需要root设备支持。

主要功能如下：

支持运行时权限申请
支持高级权限申请，包括悬浮窗，修改设置，查看应用使用情况以及截屏等。
支持激活设备管理器
支持开启无障碍
支持激活设备所有者
支持修改系统安全设置和修改系统全局设置
判断某个应用的某项权限是否已获取
可得知权限申请是否成功

	例子：
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
