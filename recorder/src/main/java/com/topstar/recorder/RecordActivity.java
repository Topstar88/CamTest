package com.topstar.recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by topstar on 2020/02/06.
 */

public class RecordActivity extends BaseCameraActivity {
    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, RecordActivity.class);
        activity.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portrate);
        onCreateActivity();
        videoWidth = 720;
        videoHeight = 1280;
        cameraWidth = 1280;
        cameraHeight = 720;
    }
}
