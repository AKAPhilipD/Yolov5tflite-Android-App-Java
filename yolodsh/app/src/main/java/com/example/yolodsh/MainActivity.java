package com.example.yolodsh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    //声明控件。
    private Button button_picture;
    private Button button_video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.button_first), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //找到控件
        button_picture=findViewById(R.id.open);
        button_video=findViewById(R.id.button2);

        //实现跳转
        button_picture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //定义一个动作。该动作使程序跳转到插入图片的界面。
                Intent to_get_photo=null;
                to_get_photo=new Intent(MainActivity.this, PhotoActivity.class);
                startActivity(to_get_photo);
            }
        });

        button_video.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent to_get_video=null;
                to_get_video=new Intent(MainActivity.this, VideoActivity.class);
                startActivity(to_get_video);
            }
        });
    }

}