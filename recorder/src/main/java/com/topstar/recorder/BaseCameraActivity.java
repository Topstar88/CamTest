package com.topstar.recorder;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daasuu.camerarecorder.CameraRecordListener;
import com.daasuu.camerarecorder.CameraRecorder;
import com.daasuu.camerarecorder.CameraRecorderBuilder;
import com.daasuu.camerarecorder.LensFacing;
import com.topstar.recorder.widget.Filters;
import com.topstar.recorder.widget.GLView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by topstar on 2020/02/06.
 */

public class BaseCameraActivity extends AppCompatActivity {
    private GLView GLView_back, GLView_front;
    protected CameraRecorder cameraRecorder_back, cameraRecorder_front;
    private String filepath_back, filepath_front;
    protected int cameraWidth = 1280;
    protected int cameraHeight = 720;
    protected int videoWidth = 720;
    protected int videoHeight = 720;
    private AlertDialog filterDialog;
    private boolean toggleClick = false;
    private Handler handler = new Handler();
    private TextView recordBtn;
    private int timeInterval = 3*60*1000;

    protected void onCreateActivity() {
        getSupportActionBar().hide();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();



        recordBtn = findViewById(R.id.btn_record);
        recordBtn.setOnClickListener(v -> {
            if (recordBtn.getText().equals(getString(R.string.app_record))) {
                filepath_back = getVideoFilePath(true);
                filepath_front = getVideoFilePath(false);
                cameraRecorder_back.start(filepath_back);
                cameraRecorder_front.start(filepath_front);
                recordBtn.setText(getResources().getString(R.string.str_stop));
            } else {
                cameraRecorder_back.stop();
                cameraRecorder_front.stop();
                recordBtn.setText(getString(R.string.app_record));
            }

        });


        findViewById(R.id.btn_filter).setOnClickListener(v -> {
            if (filterDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.str_choose_filter);
                builder.setOnDismissListener(dialog -> {
                    filterDialog = null;
                });
                final Filters[] filters = Filters.values();
                CharSequence[] charList = new CharSequence[filters.length];
                for (int i = 0, n = filters.length; i < n; i++) {
                    charList[i] = filters[i].name();
                }
                builder.setItems(charList, (dialog, item) -> {
                    changeFilter(filters[item]);
                });
                filterDialog = builder.show();
            } else {
                filterDialog.dismiss();
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        releaseCamera();
        setUpCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    private void releaseCamera() {
        if (GLView_back != null) {
            GLView_back.onPause();
        }
        if (GLView_front != null) {
            GLView_front.onPause();
        }

        if (cameraRecorder_back != null) {
            cameraRecorder_back.stop();
            cameraRecorder_back.release();
            cameraRecorder_back = null;
        }
        if (cameraRecorder_front != null) {
            cameraRecorder_front.stop();
            cameraRecorder_front.release();
            cameraRecorder_front = null;
        }

        if (GLView_back != null) {
            ((FrameLayout) findViewById(R.id.wrap_view)).removeView(GLView_back);
            GLView_back = null;
        }
        if (GLView_front != null) {
            ((FrameLayout) findViewById(R.id.wrap_view1)).removeView(GLView_front);
            GLView_front = null;
        }
    }


    private void setUpCameraView() {
        runOnUiThread(() -> {
            FrameLayout frameLayout = findViewById(R.id.wrap_view);
            frameLayout.removeAllViews();
            GLView_back = null;
            GLView_back = new GLView(getApplicationContext());
            GLView_back.setTouchListener((event, width, height) -> {
                if (cameraRecorder_back == null) return;
                cameraRecorder_back.changeManualFocusPoint(event.getX(), event.getY(), width, height);
            });
            frameLayout.addView(GLView_back);


            FrameLayout frameLayout1 = findViewById(R.id.wrap_view1);
            frameLayout1.removeAllViews();
            GLView_front = null;
            GLView_front = new GLView(getApplicationContext());
            GLView_front.setTouchListener((event, width, height) -> {
                if (cameraRecorder_front == null) return;
                cameraRecorder_front.changeManualFocusPoint(event.getX(), event.getY(), width, height);
            });
            frameLayout1.addView(GLView_front);
        });
    }


    private void setUpCamera() {
        setUpCameraView();
        cameraRecorder_back = new CameraRecorderBuilder(this, GLView_back)
                .cameraRecordListener(new CameraRecordListener() {
                    @Override
                    public void onGetFlashSupport(boolean flashSupport) {
                        runOnUiThread(() -> {
                            findViewById(R.id.btn_flash).setEnabled(flashSupport);
                        });
                    }

                    @Override
                    public void onRecordComplete() {
                        exportMp4ToGallery(getApplicationContext(), filepath_back, true);
                    }

                    @Override
                    public void onRecordStart() {

                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e("CameraRecorder", exception.toString());
                    }

                    @Override
                    public void onCameraThreadFinish() {
                        if (toggleClick) {
                            runOnUiThread(() -> {
                                setUpCamera();
                            });
                        }
                        toggleClick = false;
                    }
                })
                .videoSize(videoWidth, videoHeight)
                .cameraSize(cameraWidth, cameraHeight)
                .lensFacing(LensFacing.BACK)
                .build();

        cameraRecorder_front = new CameraRecorderBuilder(this, GLView_front)
                .cameraRecordListener(new CameraRecordListener() {
                    @Override
                    public void onGetFlashSupport(boolean flashSupport) {
                        runOnUiThread(() -> {
                            findViewById(R.id.btn_flash).setEnabled(flashSupport);
                        });
                    }

                    @Override
                    public void onRecordComplete() {
                        exportMp4ToGallery(getApplicationContext(), filepath_front,false);
                    }

                    @Override
                    public void onRecordStart() {

                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e("CameraRecorder", exception.toString());
                    }

                    @Override
                    public void onCameraThreadFinish() {
                        if (toggleClick) {
                            runOnUiThread(() -> {
                                setUpCamera();
                            });
                        }
                        toggleClick = false;
                    }
                })
                .videoSize(videoWidth, videoHeight)
                .cameraSize(cameraWidth, cameraHeight)
                .lensFacing(LensFacing.FRONT)

                .build();
    }

    private void changeFilter(Filters filters) {
        cameraRecorder_back.setFilter(Filters.getFilterInstance(filters, getApplicationContext()));
        cameraRecorder_front.setFilter(Filters.getFilterInstance(filters, getApplicationContext()));
    }
    public void exportMp4ToGallery(Context context, String filePath, boolean save) {
        final ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filePath)));
    }

    public String getVideoFilePath(boolean type) {
        if (type)
            return getExternalDirectory().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "cameraRecorder_back.mp4";
        else
            return getExternalDirectory().getAbsolutePath() + "/" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + "cameraRecorder_front.mp4";
    }
    public File getExternalDirectory() {
        final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "topstar");
        if (!f.exists()) {
            f.mkdir();
        }
        return f;
    }

}
