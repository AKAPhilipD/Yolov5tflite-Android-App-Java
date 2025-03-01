package com.example.yolodsh;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import android.Manifest;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoActivity extends AppCompatActivity {
    private static final int REQUEST_TAKE_PHOTO = 1001;
    //定义权限数组。
    String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //请求码

    private static final int REQUEST_CODE = 10001;

    private ImageButton open_button;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private OrtEnvironment env;
    private OrtSession session;
    private ImageView imageView;
    //接收照相机的代码。
    private ImageButton camera_button;

    //定义显示答案的answer。
    private TextView answer;

    //定义YOLOTFLite工具
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    //定义画矩形边框的对象。一个画方框，一个显示文字。
    Paint boxPaint=new Paint();
    Paint textPaint=new Paint();
    //照相机文件路径。
    private String currentPhotoPath;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        // 初始化按钮和 ImageView
        open_button = findViewById(R.id.imageButton);
        imageView = findViewById(R.id.imageView);
        //引入yolov5TFLiteDetector
        yolov5TFLiteDetector=new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("ig_best_fp16.tflite");
        yolov5TFLiteDetector.setYOLO(1,2100,7,320,320,"ig_label.txt");
        yolov5TFLiteDetector.initialModel(this);

        //初始化camera button。
        camera_button=findViewById(R.id.camera);

        //设置BoxPaint
        boxPaint.setStrokeWidth(10);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.GREEN);
        //设置文字
        textPaint.setTextSize(50);//设置文字大小。
        textPaint.setColor(Color.RED);
        textPaint.setStyle(Paint.Style.FILL);

        //获取answer.
        answer = findViewById(R.id.answer);




        // 定义一个选择图片的操作
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 获取选择的图片的 URI
                        Intent data = result.getData();
                        if (data != null) {
                            Uri selectedImageUri = data.getData();
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                                // 处理图像
                                get_photo_predicted(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // 点击按钮时启动选择图片
        open_button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });


        //定义一个访问拍照功能的函数。
        // 定义一个访问拍照功能的函数。
        camera_button.setOnClickListener(v -> {
            // 定义拍照动作。
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // 检查设备是否有照相机。
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                // 创建文件用于保存拍照的照片
                File photoFile = null;
                try {
                    // 申请存储权限。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // 检查该权限是否已经获取
                        for (String permission : permissions) {
                            // GRANTED---授权  DENIED---拒绝
                            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED) {
                                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
                                return; // 如果没有权限，则请求权限后返回
                            }
                        }
                    }

                    // 创建一个保存照片的文件
                    photoFile = createImageFile();
                    if (photoFile != null) {
                        // 获取文件 URI
                        Uri photoURI = FileProvider.getUriForFile(this, "com.example.yolodsh.fileprovider", photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // 指定输出路径
                        startActivityForResult(cameraIntent, REQUEST_TAKE_PHOTO); // 使用 startActivityForResult
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });




    }

    //设置保存图片的路径。
    private File createImageFile() throws IOException {
        // 创建一个唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* 文件名 */
                ".jpg",         /* 后缀 */
                storageDir      /* 存储目录 */
        );

        // 保存照片的路径
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 处理权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            // 检查权限请求是否被授予
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予，继续拍照
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQUEST_TAKE_PHOTO);
            } else {
                // 权限被拒绝，给出提示
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 处理拍照结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // 拍照完成后，图片已保存到指定路径
            File file = new File(currentPhotoPath);
            Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            get_photo_predicted(imageBitmap);
        }
    }

    private void get_photo_predicted(Bitmap bitmap) {
        //进行我们的预测
        ArrayList<Recognition> recognitions=yolov5TFLiteDetector.detect(bitmap);
        //定义可变画布。
        Bitmap mutableBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true);
        //创建画布Canvas.
        Canvas canvas=new Canvas(mutableBitmap);

        //遍历图片识别。
        for(Recognition recognition:recognitions){
            //如果置信度大于0.4，则显示。
            if(recognition.getConfidence()>0.4){
                //定义矩形位置。
                RectF location=recognition.getLocation();
                //绘画矩形和输出置信度。
                canvas.drawRect(location,boxPaint);
                canvas.drawText(recognition.getLabelName() + ":" + recognition.getConfidence(), location.centerX(), location.centerY(), textPaint);
                answer.setText(recognition.getLabelName());
            }
        }
        imageView.setImageBitmap(mutableBitmap);
    }
}



