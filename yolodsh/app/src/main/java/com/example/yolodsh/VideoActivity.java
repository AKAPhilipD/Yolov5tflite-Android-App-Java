package com.example.yolodsh;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.yolodsh.CameraProcess;
import com.example.yolodsh.Yolov5TFLiteDetector;
import com.google.common.util.concurrent.ListenableFuture;

public class VideoActivity extends AppCompatActivity {

    private boolean IS_FULL_SCREEN = false;

    private PreviewView cameraPreviewMatch;
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabelCanvas;
    private Spinner modelSpinner;
    private Switch immersive;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    private CameraProcess cameraProcess = new CameraProcess();

    // 获取屏幕旋转角度, 0 表示拍照出来的图片是横屏
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video);

        // 为视图设置窗口适配
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 打开应用时隐藏顶部状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // 初始化视图
        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);
        boxLabelCanvas = findViewById(R.id.box_label_canvas);
        modelSpinner = findViewById(R.id.model);
        immersive = findViewById(R.id.immersive);
        inferenceTimeTextView = findViewById(R.id.inference_time);
        frameSizeTextView = findViewById(R.id.frame_size);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // 初始化 Yolov5TFLiteDetector 以及初始化模型。初始化为yolo5s
        yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("yolov5s-fp16.tflite");
        yolov5TFLiteDetector.setYOLO(1,6300,85,320,320,
                    "coco_label.txt");
        yolov5TFLiteDetector.initialModel(this);


        // 申请摄像头权限
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);
        }

        // 获取屏幕旋转角度
        int rotation = getScreenOrientation();
        Log.i("image", "rotation: " + rotation);

        // 显示摄像头支持的尺寸
        cameraProcess.showCameraSupportSize(this);

        // 监听模型选择变化
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //切换模型的函数。
                String model = (String) adapterView.getItemAtPosition(i);
                Toast.makeText(VideoActivity.this, "loading model: " + model, Toast.LENGTH_LONG).show();
                change_model(model);


                // 切换全屏和全图模式
                if (IS_FULL_SCREEN) {
                    cameraPreviewWrap.removeAllViews();
                    FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(VideoActivity.this,
                            cameraPreviewMatch,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov5TFLiteDetector);
                    cameraProcess.startCamera(VideoActivity.this, fullScreenAnalyse, cameraPreviewMatch);
                } else {
                    cameraPreviewMatch.removeAllViews();
                    FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                            VideoActivity.this,
                            cameraPreviewWrap,
                            boxLabelCanvas,
                            rotation,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            yolov5TFLiteDetector);
                    cameraProcess.startCamera(VideoActivity.this, fullImageAnalyse, cameraPreviewWrap);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // 监听沉浸式体验切换
        immersive.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            IS_FULL_SCREEN = isChecked;
            if (isChecked) {
                // 进入全屏模式
                cameraPreviewWrap.removeAllViews();
                FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(VideoActivity.this,
                        cameraPreviewMatch,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov5TFLiteDetector);
                cameraProcess.startCamera(VideoActivity.this, fullScreenAnalyse, cameraPreviewMatch);
            } else {
                // 进入全图模式
                cameraPreviewMatch.removeAllViews();
                FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                        VideoActivity.this,
                        cameraPreviewWrap,
                        boxLabelCanvas,
                        rotation,
                        inferenceTimeTextView,
                        frameSizeTextView,
                        yolov5TFLiteDetector);
                cameraProcess.startCamera(VideoActivity.this, fullImageAnalyse, cameraPreviewWrap);
            }
        });

        /**
         * 加载模型
         *
         * @param modelName
         */

    }

    private void change_model(String modelName) {
        //此函数进行modelName切换代码。
        if(modelName=="yolov5s"){
            try{
               this.yolov5TFLiteDetector=new Yolov5TFLiteDetector();
               this.yolov5TFLiteDetector.setModelFile("yolov5s-fp16.tflite");
               this.yolov5TFLiteDetector.addGPUDelegate();
               this.yolov5TFLiteDetector.initialModel(this);
               this.yolov5TFLiteDetector.setYOLO(1,6300,85,320,320,
                       "coco_label.txt");
                Log.i("model", "Success loading model" + this.yolov5TFLiteDetector.getModelFile());

            } catch (Exception e) {
                Log.e("image", "load model error: " + e.getMessage() + e.toString());
            }

        }
        else if(modelName=="ig_model"){
            try{
                this.yolov5TFLiteDetector=new Yolov5TFLiteDetector();
                this.yolov5TFLiteDetector.setModelFile("best_float16.tflite");
                this.yolov5TFLiteDetector.addGPUDelegate();
                this.yolov5TFLiteDetector.initialModel(this);
                this.yolov5TFLiteDetector.setYOLO(1,7,2100,320,320,
                        "ig_label.txt");
                Log.i("model", "Success loading model" + this.yolov5TFLiteDetector.getModelFile());

            } catch (Exception e) {
                Log.e("image", "load model error: " + e.getMessage() + e.toString());
            }
        }
    }
}
