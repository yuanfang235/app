package com.zwxuf.permissiondemo;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laiqu on 2017/12/25.
 */

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "fly";

    private static final String NAME_PACKAGE_SETTINGS = "com.android.settings"; //设置
    private static final String NAME_PACKAGE_CAMERA = "com.mediatek.camera";

    private static List<String> blackList = new ArrayList<>();
    private static List<String> whiteList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = this.getServiceInfo();
        info.packageNames = new String[]{NAME_PACKAGE_SETTINGS};
        this.setServiceInfo(info);
        initList();
    }

    private void initList() {

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {


    }


    @Override
    public void onInterrupt() {

    }


}
