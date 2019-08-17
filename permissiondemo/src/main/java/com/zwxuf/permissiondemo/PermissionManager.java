package com.zwxuf.permissiondemo;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.AppOpsManager;
import android.app.Service;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Created by laiqu on 2019-08-13.
 * root权限管理
 */

public class PermissionManager {

    private static final String TAG = "fly";

    private static final int OP_MODE_INVALID = -1;

    private Context mContext;

    private List<Task> mTaskList = new ArrayList<>();

    private Sets mSets = new Sets();

    private Callbacks callbacks;

    private Handler handler;

    public PermissionManager(Context mContext) {
        this.mContext = mContext;
    }

    public PermissionManager buildTask(Task task) {
        mTaskList.add(task);
        return this;
    }

    /**
     * 是否支持设备管理器
     *
     * @return
     */
    public static boolean isSupportDeviceAdmin() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * 是否支持无障碍
     *
     * @return
     */
    public static boolean isSupportAccessibility() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT;
    }

    /**
     * 是否需要请求权限
     *
     * @return
     */
    public static boolean needRequestPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 是否支持op权限管理
     *
     * @return
     */
    public static boolean isSupportOpsManager() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * 是否获取运行时权限
     *
     * @param context
     * @param pkgName
     * @param permission
     * @return
     */
    public static boolean isAllowedPermission(Context context, String pkgName, String permission) {
        if (!needRequestPermission()) {
            return true;
        }
        if (isEmpty(pkgName)) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission(permission, pkgName);
    }

    /**
     * 是否获取所有运行时权限
     *
     * @param context
     * @param pkgName
     * @param permissions
     * @return
     */
    public static boolean isAllowedPermissionAll(Context context, String pkgName, String... permissions) {
        if (!needRequestPermission()) {
            return true;
        }
        if (hasElement(permissions)) {
            for (String permission : permissions) {
                if (!isAllowedPermission(context, pkgName, permission)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * 是否获取所有运行时权限
     *
     * @param context
     * @param pkgName
     * @param permissions
     * @return
     */
    public static boolean isAllowedPermissionAll(Context context, String pkgName, Collection<String> permissions) {
        if (hasElement(permissions)) {
            String[] array = new String[permissions.size()];
            permissions.toArray(array);
            return isAllowedPermissionAll(context, pkgName, array);
        } else {
            return true;
        }
    }

    /**
     * 是否没有获取所有运行时权限
     *
     * @param context
     * @param pkgName
     * @param permissions
     * @return
     */
    public static boolean isDeniedPermissionAll(Context context, String pkgName, String... permissions) {
        if (!needRequestPermission()) {
            return true;
        }
        if (hasElement(permissions)) {
            for (String permission : permissions) {
                if (isAllowedPermission(context, pkgName, permission)) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * 是否没有获取所有运行时权限
     *
     * @param context
     * @param pkgName
     * @param permissions
     * @return
     */
    public static boolean isDeniedPermissionAll(Context context, String pkgName, Collection<String> permissions) {
        if (hasElement(permissions)) {
            String[] array = new String[permissions.size()];
            permissions.toArray(array);
            return isDeniedPermissionAll(context, pkgName, array);
        } else {
            return true;
        }
    }

    /**
     * 是否允许op
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static boolean isAllowedOp(Context context, String packageName, String op) {
        return getOpMode(context, packageName, op) == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * 是否允许op
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static boolean isAllowedOp(Context context, String packageName, int op) {
        return isAllowedOp(context, packageName, String.valueOf(op));
    }

    /**
     * 是否忽略op
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static boolean isIgnoredOp(Context context, String packageName, String op) {
        return getOpMode(context, packageName, op) == AppOpsManager.MODE_IGNORED;
    }

    /**
     * 是否忽略op
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static boolean isIgnoredOp(Context context, String packageName, int op) {
        return isIgnoredOp(context, packageName, String.valueOf(op));
    }

    /**
     * 获取op模式
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static int getOpMode(Context context, String packageName, String op) {
        if (isEmpty(packageName)) {
            return OP_MODE_INVALID;
        }
        if (isNumeric(op)) {
            return getOpPermissionByValue(context, packageName, Integer.parseInt(op));
        } else {
            return getOpPermissionByName(context, packageName, op);
        }
    }

    /**
     * 获取op模式
     *
     * @param context
     * @param packageName
     * @param op
     * @return
     */
    public static int getOpMode(Context context, String packageName, int op) {
        return getOpMode(context, packageName, String.valueOf(op));
    }

    /**
     * 是否允许所有op
     *
     * @param context
     * @param packageName
     * @param ops
     * @return
     */
    public static boolean isAllowedOpAll(Context context, String packageName, String... ops) {
        if (hasElement(ops)) {
            for (String op : ops) {
                if (getOpMode(context, packageName, op) != AppOpsManager.MODE_ALLOWED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 是否允许所有op
     *
     * @param context
     * @param packageName
     * @param ops
     * @return
     */
    public static boolean isAllowedOpAll(Context context, String packageName, Collection<String> ops) {
        if (hasElement(ops)) {
            String[] array = new String[ops.size()];
            ops.toArray(array);
            return isAllowedOpAll(context, packageName, array);
        } else {
            return true;
        }
    }

    /**
     * 是否拒绝所有op
     *
     * @param context
     * @param packageName
     * @param ops
     * @return
     */
    public static boolean isDeniedOpAll(Context context, String packageName, String... ops) {
        if (hasElement(ops)) {
            for (String op : ops) {
                if (getOpMode(context, packageName, op) != AppOpsManager.MODE_IGNORED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 是否拒绝所有op
     *
     * @param context
     * @param packageName
     * @param ops
     * @return
     */
    public static boolean isDeniedOpAll(Context context, String packageName, Collection<String> ops) {
        if (hasElement(ops)) {
            String[] array = new String[ops.size()];
            ops.toArray(array);
            return isDeniedOpAll(context, packageName, array);
        } else {
            return true;
        }
    }

    /**
     * 是否启用无障碍
     *
     * @param context
     * @param pkgName
     * @param className
     * @return
     */
    public static boolean isAccessibilityServiceEnabled(Context context, String pkgName, String className) {
        if (!isSupportAccessibility()) {
            return false;
        }
        if (isEmpty(pkgName) || isEmpty(className)) {
            return false;
        }
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String service = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (className.startsWith(".")) {
                    className = pkgName + className;
                }
                String myServiceName = pkgName + "/" + className;
                boolean hasService = service != null && service.contains(myServiceName);
                return hasService;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 是否启用无障碍
     *
     * @param context
     * @param serviceClass
     * @return
     */
    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> serviceClass) {
        return isAccessibilityServiceEnabled(context, context.getPackageName(), serviceClass.getCanonicalName());
    }

    /**
     * 是否是设备管理器
     *
     * @param context
     * @param pkgName
     * @param className
     * @return
     */
    public static boolean isDeviceAdmin(Context context, String pkgName, String className) {
        if (!isSupportDeviceAdmin()) {
            return false;
        }
        if (isEmpty(pkgName) || isEmpty(className)) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
        return dpm.isAdminActive(new ComponentName(pkgName, className));
    }

    /**
     * 是否是设备管理器
     *
     * @param context
     * @param receiverClass
     * @return
     */
    public static boolean isDeviceAdmin(Context context, Class<? extends DeviceAdminReceiver> receiverClass) {
        if (!isSupportDeviceAdmin()) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
        return dpm.isAdminActive(new ComponentName(context, receiverClass));
    }

    /**
     * 是否是设备所有者
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isDeviceOwner(Context context, String pkgName) {
        if (!isSupportDeviceAdmin()) {
            return false;
        }
        if (isEmpty(pkgName)) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
        return dpm.isDeviceOwnerApp(pkgName);
    }

    /**
     * 取消设备管理器，仅适用于自己
     *
     * @param context
     * @param receiverClass
     */
    public static void removeDeviceAdmin(Context context, Class<? extends DeviceAdminReceiver> receiverClass) {
        if (!isSupportDeviceAdmin()) {
            return;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
        dpm.removeActiveAdmin(new ComponentName(context, receiverClass));
    }

    /**
     * 取消设备所有者，仅适用于自己
     *
     * @param context
     * @param pkgName
     */
    public static void removeDeviceOwner(Context context, String pkgName) {
        if (!isSupportDeviceAdmin()) {
            return;
        }
        if (!isDeviceOwner(context, pkgName)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
                dpm.clearDeviceOwnerApp(pkgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 是否允许出现在其它应用上层
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isAllowedDrawOverlays(Context context, String pkgName) {
        return isAllowedOp(context, pkgName, OpValue.OP_SYSTEM_ALERT_WINDOW);
    }

    /**
     * 是否允许修改系统设置
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isAllowedWriteSettings(Context context, String pkgName) {
        return isAllowedOp(context, pkgName, OpValue.OP_WRITE_SETTINGS);
    }

    /**
     * 是否开启屏幕投射
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isAllowedProjectMedia(Context context, String pkgName) {
        return isAllowedOp(context, pkgName, OpValue.OP_PROJECT_MEDIA);
    }

    /**
     * 是否有权查看使用情况
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isAllowedUsageStats(Context context, String pkgName) {
        return isAllowedOp(context, pkgName, OpValue.OP_GET_USAGE_STATS);
    }

    /**
     * 读取op设置，更新内存
     *
     * @return
     */
    public static boolean readOpSettings() {
        return execCommand("appops read-settings");
    }

    /**
     * 将内存的op设置保存
     *
     * @return
     */
    public static boolean writeOpSettings() {
        return execCommand("appops write-settings");
    }


    public static class Task {
        private String packageName;
        private Set<String> allowPermissionList;
        private Set<String> denyPermissionList;
        private Set<String> allowOpList;
        private Set<String> denyOpList;

        private AdvancedPermission accessibilityPermission;
        private AdvancedPermission deviceAdminPermission;
        private String deviceOwnerClassName;

        private boolean allowPermissionSuccessful = true;
        private boolean denyPermissionSuccessful = true;
        private boolean allowOpSuccessful = true;
        private boolean denyOpSuccessful = true;

        private boolean accessibilitySuccessful = false;
        private boolean deviceAdminSuccessful = false;
        private boolean deviceOwnerSuccessful = false;


        public Task() {
        }

        public Task(String packageName) {
            this.packageName = packageName;
        }

        public Task setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * 允许权限数组
         *
         * @param runtimePermissions
         * @return
         */
        public Task allow(String... runtimePermissions) {
            if (hasElement(runtimePermissions)) {
                if (allowPermissionList == null) {
                    allowPermissionList = new HashSet<>();
                }
                for (String runtimePermission : runtimePermissions) {
                    allowPermissionList.add(runtimePermission);
                }
            }
            return this;
        }

        /**
         * 允许权限列表
         *
         * @param runtimePermissions
         * @return
         */
        public Task allow(Collection<String> runtimePermissions) {
            if (hasElement(runtimePermissions)) {
                String[] array = new String[runtimePermissions.size()];
                runtimePermissions.toArray(array);
                return allow(array);
            }
            return this;
        }

        /**
         * 拒绝权限数组
         *
         * @param runtimePermissions
         * @return
         */
        public Task deny(String... runtimePermissions) {
            if (hasElement(runtimePermissions)) {
                if (denyPermissionList == null) {
                    denyPermissionList = new HashSet<>();
                }
                for (String runtimePermission : runtimePermissions) {
                    denyPermissionList.add(runtimePermission);
                }
            }
            return this;
        }

        /**
         * 拒绝权限列表
         *
         * @param runtimePermissions
         * @return
         */
        public Task deny(Collection<String> runtimePermissions) {
            if (hasElement(runtimePermissions)) {
                String[] array = new String[runtimePermissions.size()];
                runtimePermissions.toArray(array);
                return deny(array);
            }
            return this;
        }

        /**
         * 允许op权限：名称
         *
         * @param opNames
         * @return
         */
        public Task allowOp(String... opNames) {
            if (hasElement(opNames)) {
                if (allowOpList == null) {
                    allowOpList = new HashSet<>();
                }
                allowOpList.addAll(Arrays.asList(opNames));
            }
            return this;
        }

        /**
         * 允许op权限：数值
         *
         * @param opValues
         * @return
         */
        public Task allowOp(int... opValues) {
            return allowOp(intToString(opValues));
        }

        /**
         * 拒绝op权限：名称
         *
         * @param opNames
         * @return
         */
        public Task denyOp(String... opNames) {
            if (hasElement(opNames)) {
                if (denyOpList == null) {
                    denyOpList = new HashSet<>();
                }
                denyOpList.addAll(Arrays.asList(opNames));
            }
            return this;
        }

        /**
         * 拒绝op权限：数值
         *
         * @param opValues
         * @return
         */
        public Task denyOp(int... opValues) {
            return denyOp(intToString(opValues));
        }

        /**
         * 是否允许出现在其它应用之上
         *
         * @param allow
         * @return
         */
        public Task allowDrawOverlays(boolean allow) {
            if (allow) {
                return allowOp(OpValue.OP_SYSTEM_ALERT_WINDOW);
            } else {
                return denyOp(OpValue.OP_SYSTEM_ALERT_WINDOW);
            }
        }

        /**
         * 是否允许修改系统设置
         *
         * @param allow
         * @return
         */
        public Task allowWriteSettings(boolean allow) {
            if (allow) {
                return allowOp(OpValue.OP_WRITE_SETTINGS);
            } else {
                return denyOp(OpValue.OP_WRITE_SETTINGS);
            }
        }

        /**
         * 是否允许投射屏幕（录屏）
         *
         * @param allow
         * @return
         */
        public Task allowProjectMedia(boolean allow) {
            if (allow) {
                return allowOp(OpValue.OP_PROJECT_MEDIA);
            } else {
                return denyOp(OpValue.OP_PROJECT_MEDIA);
            }
        }

        /**
         * 是否允许查看应用使用情况
         *
         * @param allow
         * @return
         */
        public Task allowUsageStats(boolean allow) {
            if (allow) {
                return allowOp(OpValue.OP_GET_USAGE_STATS);
            } else {
                return denyOp(OpValue.OP_GET_USAGE_STATS);
            }
        }

        /**
         * 允许无障碍
         *
         * @param className
         * @param allow
         * @return
         */
        public Task allowAccessibility(String className, boolean allow) {
            if (hasValue(className)) {
                if (className.startsWith(".")) {
                    className = packageName + className;
                }
                accessibilityPermission = new AdvancedPermission(className, allow);
            }
            return this;
        }

        /**
         * 允许无障碍，适用于自己的类
         *
         * @param serviceClass
         * @param allow
         * @return
         */
        public Task allowAccessibility(Class<? extends AccessibilityService> serviceClass, boolean allow) {
            return allowAccessibility(serviceClass.getCanonicalName(), allow);
        }

        /**
         * 允许设备管理器
         *
         * @param className
         * @param allow
         * @return
         */
        public Task allowDeviceAdmin(String className, boolean allow) {
            if (hasValue(className)) {
                deviceAdminPermission = new AdvancedPermission(className, allow);
            }
            return this;
        }

        /**
         * 允许设备管理器，适用于自己的类
         *
         * @param receiverClass
         * @param allow
         * @return
         */
        public Task allowDeviceAdmin(Class<? extends DeviceAdminReceiver> receiverClass, boolean allow) {
            return allowDeviceAdmin(receiverClass.getCanonicalName(), allow);
        }

        /**
         * 允许设备所有者
         *
         * @param className
         * @return
         */
        public Task allowDeviceOwner(String className) {
            if (hasValue(className)) {
                deviceOwnerClassName = className;
            }
            return this;
        }

        public Task allowDeviceOwner(Class<? extends DeviceAdminReceiver> receiverClass) {
            return allowDeviceOwner(receiverClass.getCanonicalName());
        }

        public boolean isAllowOpSuccessful() {
            return allowOpSuccessful;
        }

        public boolean isAllowPermissionSuccessful() {
            return allowPermissionSuccessful;
        }

        public boolean isDenyOpSuccessful() {
            return denyOpSuccessful;
        }

        public boolean isDenyPermissionSuccessful() {
            return denyPermissionSuccessful;
        }

        public boolean isAccessibilitySuccessful() {
            return accessibilitySuccessful;
        }

        public boolean isDeviceAdminSuccessful() {
            return deviceAdminSuccessful;
        }

        public boolean isDeviceOwnerSuccessful() {
            return deviceOwnerSuccessful;
        }

        public boolean isSuccessful() {
            return (!hasElement(allowPermissionList) || isAllowPermissionSuccessful())
                    && (!hasElement(denyPermissionList) || isDenyPermissionSuccessful())
                    && (!hasElement(allowOpList) || isAllowOpSuccessful())
                    && (!hasElement(denyOpList) || isDenyOpSuccessful())
                    && (accessibilityPermission == null || accessibilitySuccessful)
                    && (deviceAdminPermission == null || deviceAdminSuccessful)
                    && (deviceOwnerClassName == null || deviceOwnerSuccessful);
        }

        private String[] intToString(int[] intArray) {
            if (intArray == null || intArray.length == 0) {
                return null;
            }
            String[] strArray = new String[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                strArray[i] = String.valueOf(intArray[i]);
            }
            return strArray;
        }

    }

    /**
     * 同步执行
     *
     * @return
     */
    public synchronized Result execute() {
        boolean isExecuteSuccess = true;
        List<String> commandList = new ArrayList<>();
        if (hasElement(mTaskList)) {
            for (Task task : mTaskList) {
                //运行时权限
                if (needRequestPermission()) {
                    task.allowPermissionList = getDeniedPermissionList(mContext, task.packageName, task.allowPermissionList);
                    if (hasElement(task.allowPermissionList)) {
                        for (String permission : task.allowPermissionList) {
                            commandList.add("pm grant " + task.packageName + " " + permission);
                        }
                    }
                    task.denyPermissionList = getGrantedPermissionList(mContext, task.packageName, task.denyPermissionList);
                    if (hasElement(task.denyPermissionList)) {
                        for (String permission : task.denyPermissionList) {
                            commandList.add("pm revoke " + task.packageName + " " + permission);
                        }
                    }
                }
                //高级权限
                if (isSupportOpsManager()) {
                    if (hasElement(task.allowOpList)) {
                        task.allowOpList = getDeniedOpsList(mContext, task.packageName, task.allowOpList);
                        if (hasElement(task.allowOpList)) {
                            for (String opsName : task.allowOpList) {
                                commandList.add("appops set " + task.packageName + " " + opsName + " " + AppOpsManager.MODE_ALLOWED);
                            }
                        }
                    }
                    if (hasElement(task.denyOpList)) {
                        task.denyOpList = getAllowedOpsList(mContext, task.packageName, task.denyOpList);
                        if (hasElement(task.denyOpList)) {
                            for (String opsName : task.denyOpList) {
                                commandList.add("appops set " + task.packageName + " " + opsName + " " + AppOpsManager.MODE_IGNORED);
                            }
                        }
                    }
                }
                //无障碍
                if (isSupportAccessibility() && task.accessibilityPermission != null) {
                    boolean hasService = isAccessibilityServiceEnabled(mContext, task.packageName, task.accessibilityPermission.className);
                    String srcServiceName = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    String myServiceName = task.packageName + "/" + task.accessibilityPermission.className;
                    if (task.accessibilityPermission.allow) {
                        if (!hasService) {
                            int enabled = 0;
                            try {
                                enabled = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
                            } catch (Settings.SettingNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (enabled == 0) {
                                commandList.add("settings put secure " + Settings.Secure.ACCESSIBILITY_ENABLED + " 1");
                            }
                            if (srcServiceName == null) {
                                srcServiceName = myServiceName;
                            } else {
                                srcServiceName = srcServiceName + ":" + myServiceName;
                            }
                            commandList.add("settings put secure " + Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES + " " + srcServiceName);
                        }
                    } else {
                        if (hasService) {
                            if (srcServiceName.equals(myServiceName)) {
                                srcServiceName = "";
                            } else {
                                if (srcServiceName.endsWith(myServiceName)) {
                                    srcServiceName = srcServiceName.replace(":" + myServiceName, "");
                                } else {
                                    srcServiceName = srcServiceName.replace(myServiceName + ":", "");
                                }
                            }
                            commandList.add("settings put secure " + Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES + " " + srcServiceName);
                        }
                    }
                }
                //设备所有者
                if (isSupportDeviceAdmin() && task.deviceOwnerClassName != null) {
                    if (!isDeviceOwner(mContext, task.packageName)) {
                        //先禁用账号和用户
                        List<String> accountList = getAccountAppList(mContext);
                        if (accountList != null) {
                            for (String pkgName : accountList) {
                                commandList.add("pm disable " + pkgName);
                            }
                        }
                        commandList.add("dpm set-device-owner " + task.packageName + "/" + task.deviceOwnerClassName);
                        if (accountList != null) {
                            for (String pkgName : accountList) {
                                commandList.add("pm enable " + pkgName);
                            }
                        }
                    }
                }
                //设备管理器
                if (isSupportDeviceAdmin() && task.deviceAdminPermission != null) {
                    boolean isDeviceAdmin = isDeviceAdmin(mContext, task.packageName, task.deviceAdminPermission.className);
                    if (task.deviceAdminPermission.allow) {
                        if (!isDeviceAdmin) {
                            commandList.add("dpm set-active-admin " + task.packageName + "/" + task.deviceAdminPermission.className);
                        }
                    } else {
                        if (isDeviceAdmin) {
                            commandList.add("dpm remove-active-admin " + task.packageName + "/" + task.deviceAdminPermission.className);
                        }
                    }
                }
            }
        }
        //安全设置
        if (hasElement(mSets.getSecureSets())) {
            for (Map.Entry<String, String> entry : mSets.getSecureSets().entrySet()) {
                commandList.add("settings put secure " + entry.getKey() + " " + entry.getValue());
            }
        }
        //全局设置
        if (hasElement(mSets.getGlobalSets())) {
            for (Map.Entry<String, String> entry : mSets.getGlobalSets().entrySet()) {
                commandList.add("settings put global " + entry.getKey() + " " + entry.getValue());
            }
        }
        //执行结果
        if (hasElement(commandList)) {
            isExecuteSuccess = execCommand(commandList);
            Log.e(TAG, "command execute result: " + isExecuteSuccess);
        }
        if(hasElement(mTaskList)){
            for (final Task task : mTaskList) {
                if (needRequestPermission()) {
                    if (hasElement(task.allowPermissionList)) {
                        task.allowPermissionSuccessful = isAllowedPermissionAll(mContext, task.packageName, task.allowPermissionList);
                    }
                    if (hasElement(task.denyPermissionList)) {
                        task.denyPermissionSuccessful = isDeniedPermissionAll(mContext, task.packageName, task.denyPermissionList);
                    }
                }
                if (isSupportOpsManager()) {
                    if (hasElement(task.allowOpList)) {
                        task.allowOpSuccessful = isAllowedOpAll(mContext, task.packageName, task.allowOpList);
                    }
                    if (hasElement(task.denyOpList)) {
                        task.denyOpSuccessful = isDeniedOpAll(mContext, task.packageName, task.denyOpList);
                    }
                }
                if (isSupportAccessibility() && task.accessibilityPermission != null) {
                    task.accessibilitySuccessful = task.accessibilityPermission.allow == isAccessibilityServiceEnabled(mContext, task.packageName, task.accessibilityPermission.className);
                }
                if (isSupportDeviceAdmin() && task.deviceAdminPermission != null) {
                    task.deviceAdminSuccessful = task.deviceAdminPermission.allow == isDeviceAdmin(mContext, task.packageName, task.deviceAdminPermission.className);
                }
                if (isSupportDeviceAdmin() && task.deviceOwnerClassName != null) {
                    task.deviceOwnerSuccessful = isDeviceOwner(mContext, task.packageName);
                }
            }
        }
        if (hasElement(mSets.getSecureSets())) {
            for (Map.Entry<String, String> entry : mSets.getSecureSets().entrySet()) {
                String value = Settings.Secure.getString(mContext.getContentResolver(), entry.getKey());
                boolean success = equalsSettingsValue(value, entry.getValue());
                mSets.getSecureSetResult().put(entry.getKey(), success);
                if (!success) mSets.isSuccessful = false;
            }
        }
        if (hasElement(mSets.getGlobalSets())) {
            for (Map.Entry<String, String> entry : mSets.getGlobalSets().entrySet()) {
                String value = Settings.Global.getString(mContext.getContentResolver(), entry.getKey());
                boolean success = equalsSettingsValue(value, entry.getValue());
                mSets.getGlobalSetResult().put(entry.getKey(), success);
                if (!success) mSets.isSuccessful = false;
            }
        }

        //处理结果
        Result result = new Result(isExecuteSuccess, mTaskList, mSets);
        if (result.isExecuteSuccess()) {
            boolean isSuccessful = false;
            if (mSets.isSuccessful()) {
                isSuccessful = true;
                if (hasElement(mTaskList)) {
                    for (Task task : mTaskList) {
                        if (!task.isSuccessful()) {
                            isSuccessful = false;
                            break;
                        }
                    }
                }
            }
            result.isAllSuccessful = isSuccessful;
        }
        return result;
    }

    /**
     * 异步执行
     */
    public void start() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                final Result result = execute();
                if (callbacks != null) {
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                processResult(result);
                            }
                        });
                    } else {
                        processResult(result);
                    }
                }
            }
        }.start();
    }

    /**
     * 清除所有任务
     *
     * @return
     */
    public PermissionManager clearTask() {
        mTaskList.clear();
        return this;
    }

    /**
     * 清除设置
     *
     * @return
     */
    public PermissionManager clearSets() {
        mSets = new Sets();
        return this;
    }

    public PermissionManager putSecure(String key, Object value) {
        mSets.getSecureSets().put(key, String.valueOf(value));
        return this;
    }

    public PermissionManager putGlobal(String key, Object value) {
        mSets.getGlobalSets().put(key, String.valueOf(value));
        return this;
    }

    private static boolean execCommand(Collection<String> commandList) {
        if (!hasElement(commandList)) return true;
        String[] array = new String[commandList.size()];
        commandList.toArray(array);
        return execCommand(array);
    }

    private static boolean execCommand(String... commands) {
        if (!hasElement(commands)) return true;
        ProcessBuilder builder = null;
        java.lang.Process process = null;
        PrintWriter writer = null;
        int result = -1;
        try {
            builder = new ProcessBuilder("su");
            builder.redirectErrorStream(true);
            process = builder.start();
            writer = new PrintWriter(process.getOutputStream());
            for (String command : commands) {
                //Log.e(TAG, "cmd=" + command);
                writer.println(command);
                writer.flush();
            }
            writer.close();
            result = process.waitFor();
            Log.e(TAG, "root success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (process != null) process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result == 0;
    }


    private void processResult(Result result) {
        if (result.isExecuteSuccess()) {
            callbacks.onSuccess(result.tasks, result.sets, result.isAllSuccessful());
        } else {
            callbacks.onFailure(result.tasks, result.sets);
        }
    }

    /**
     * 设置callback和handler，用于回调给主线程，异步调用
     *
     * @param callbacks
     * @param handler
     * @return
     */
    public PermissionManager setCallbacks(Callbacks callbacks, Handler handler) {
        this.callbacks = callbacks;
        this.handler = handler;
        return this;
    }

    public interface Callbacks {
        void onSuccess(List<Task> tasks, Sets sets, boolean isAllSuccessful);

        void onFailure(List<Task> tasks, Sets sets);
    }

    private Set<String> getDeniedPermissionList(Context context, String pkgName, Set<String> permissions) {
        if (hasElement(permissions)) {
            Set<String> mList = new HashSet<>();
            for (String permission : permissions) {
                if (!isAllowedPermission(context, pkgName, permission)) {
                    mList.add(permission);
                }
            }
            return mList;
        } else {
            return null;
        }
    }

    private Set<String> getGrantedPermissionList(Context context, String pkgName, Set<String> permissions) {
        if (hasElement(permissions)) {
            Set<String> mList = new HashSet<>();
            for (String permission : permissions) {
                if (isAllowedPermission(context, pkgName, permission)) {
                    mList.add(permission);
                }
            }
            return mList;
        } else {
            return null;
        }
    }

    private Set<String> getDeniedOpsList(Context context, String pkgName, Set<String> ops) {
        if (hasElement(ops)) {
            Set<String> mList = new HashSet<>();
            for (String op : ops) {
                if (getOpMode(context, pkgName, op) != AppOpsManager.MODE_ALLOWED) {
                    mList.add(op);
                }
            }
            return mList;
        } else {
            return null;
        }
    }

    private Set<String> getAllowedOpsList(Context context, String pkgName, Set<String> ops) {
        if (hasElement(ops)) {
            Set<String> mList = new HashSet<>();
            for (String op : ops) {
                if (getOpMode(context, pkgName, op) != AppOpsManager.MODE_IGNORED) {
                    mList.add(op);
                }
            }
            return mList;
        } else {
            return null;
        }
    }


    private static boolean hasElement(Collection c) {
        return c != null && !c.isEmpty();
    }

    private static boolean hasElement(Object[] array) {
        return array != null && array.length > 0;
    }

    private static boolean hasElement(Map map) {
        return map != null && !map.isEmpty();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }

    private static boolean isEmpty(String value) {
        return !hasValue(value);
    }

    private static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    private static boolean equalsValue(String v1, String v2) {
        return v1 != null && v2 != null && v1.equals(v2);
    }

    private static int getOpPermissionByName(Context context, String packageName, String opName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                if (appOpsManager != null) {
                    int mode = appOpsManager.checkOpNoThrow(opName, Process.myUid(), packageName);
                    return mode;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return OP_MODE_INVALID;
    }


    private static int getOpPermissionByValue(Context context, String packageName, int opValue) {
        try {
            Object object = context.getSystemService(Context.APP_OPS_SERVICE);
            if (object == null) {
                return AppOpsManager.MODE_ALLOWED;
            }
            Class localClass = object.getClass();
            Class[] arrayOfClass = new Class[3];
            arrayOfClass[0] = Integer.TYPE;
            arrayOfClass[1] = Integer.TYPE;
            arrayOfClass[2] = String.class;
            Method method = localClass.getMethod("checkOp", arrayOfClass);

            if (method == null) {
                return OP_MODE_INVALID;
            }
            Object[] arrayOfObject = new Object[3];
            arrayOfObject[0] = Integer.valueOf(opValue);
            arrayOfObject[1] = Integer.valueOf(Binder.getCallingUid());
            arrayOfObject[2] = packageName;
            int m = ((Integer) method.invoke(object, arrayOfObject)).intValue();
            //Log.e(TAG, "m=" + m);
            return m;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return OP_MODE_INVALID;
    }

    private static List<String> getAccountAppList(Context context) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                execCommand("pm grant " + context.getPackageName() + " " + Manifest.permission.GET_ACCOUNTS);
            }
            AccountManager manager = (AccountManager) context.getSystemService(Service.ACCOUNT_SERVICE);
            Account[] accounts = manager.getAccounts();
            AuthenticatorDescription[] descriptions = manager.getAuthenticatorTypes();
            if (accounts != null && accounts.length > 0 && descriptions != null && descriptions.length > 0) {
                List<String> mList = new ArrayList<>();
                for (Account account : accounts) {
                    for (AuthenticatorDescription description : descriptions) {
                        if (account.type.equals(description.type)) {
                            mList.add(description.packageName);
                            break;
                        }
                    }
                }
                return mList.isEmpty() ? null : mList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private boolean equalsSettingsValue(String value, String userValue) {
        //Log.e(TAG, userValue + "=" + value);
        boolean success = false;
        if (userValue != null && value != null) {
            if (userValue.startsWith("+")) {
                success = value.contains(userValue.substring(1));
            } else if (userValue.startsWith("-")) {
                success = !value.contains(userValue.substring(1));
            } else {
                success = equalsValue(value, userValue);
            }
        }
        return success;
    }

    public static class Sets {
        private Map<String, String> secureSets = new HashMap<>();
        private Map<String, String> globalSets = new HashMap<>();
        private Map<String, Boolean> secureSetResult = new HashMap<>();
        private Map<String, Boolean> globalSetResult = new HashMap<>();

        private boolean isSuccessful = true;

        public Map<String, String> getSecureSets() {
            return secureSets;
        }

        public Map<String, String> getGlobalSets() {
            return globalSets;
        }

        public Map<String, Boolean> getSecureSetResult() {
            return secureSetResult;
        }

        public Map<String, Boolean> getGlobalSetResult() {
            return globalSetResult;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }
    }

    public static class Result {
        private boolean executeSuccess = true;
        private List<Task> tasks = null;
        private Sets sets = null;
        private boolean isAllSuccessful;

        public Result(boolean executeSuccess, List<Task> tasks, Sets sets) {
            this.executeSuccess = executeSuccess;
            this.tasks = tasks;
            this.sets = sets;
        }

        public boolean isAllSuccessful() {
            return isAllSuccessful;
        }

        public boolean isExecuteSuccess() {
            return executeSuccess;
        }

        public List<Task> getTasks() {
            return tasks;
        }

    }

    /**
     * op值，参考AppOpsManager类的声明
     */
    public static class OpValue {
        public static final int OP_WRITE_SETTINGS = 23;
        public static final int OP_SYSTEM_ALERT_WINDOW = 24;
        public static final int OP_GET_USAGE_STATS = 43;
        public static final int OP_PROJECT_MEDIA = 46;
    }

    private static class AdvancedPermission {
        private String className;
        private boolean allow;

        public AdvancedPermission(String className, boolean allow) {
            this.className = className;
            this.allow = allow;
        }
    }
}
