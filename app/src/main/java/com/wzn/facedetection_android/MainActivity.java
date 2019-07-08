package com.wzn.facedetection_android;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.wzn.dlibtool.DlibUtils;
import com.wzn.dlibtool.RectInfo;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        test();
    }
    private void test(){

        //工具类
        DlibUtils dlibUtils;
        //标准照片
        Bitmap standardBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.wzn);
        //实时照片
        Bitmap actualTimeBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.actualtime);
//        Toast.makeText(MainActivity.this,standardBitmap.getHeight()+"",Toast.LENGTH_LONG).show();

        //标准照片数据文件夹路径
        String filePath =Environment.getExternalStorageDirectory().getAbsolutePath()+"/";

        //初始化
        dlibUtils = new DlibUtils();
        dlibUtils.checkExist(this, Environment.getExternalStorageDirectory().getAbsolutePath());
        dlibUtils.load();

        //对标准图片进行检测，分析数据并写入文件
        ArrayList<Float> Info = dlibUtils.analyseData(standardBitmap);
        dlibUtils.writeTxtToFile(Info, filePath);

        ArrayList<Float> actualInfo = dlibUtils.analyseData(actualTimeBitmap);
        System.out.println("actualInfo"+actualInfo.get(0)+actualInfo.get(1));
        //获取实时照片和标准照片比对后的提示信息
        String hintInfo = dlibUtils.compareImg(actualTimeBitmap, filePath);

        Toast.makeText(MainActivity.this,hintInfo,Toast.LENGTH_LONG).show();
    }
}
