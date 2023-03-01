package com.example.agepredict;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.agepredict.ml.AgeModel1;
import com.example.agepredict.ml.AgeModelcnn;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button camaraBtn, importBtn, predictBtn;
    TextView result, time;
    ImageView imageView;
    Bitmap bitmap;
    Mat mat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
        getPermission();
        camaraBtn = findViewById(R.id.camaraButton);
        importBtn = findViewById(R.id.importButton);
        imageView = findViewById(R.id.image_View);
        predictBtn = findViewById(R.id.predictButton);
        result = findViewById(R.id.result);
        time = findViewById(R.id.infTime);

        ImageProcessor processor = new ImageProcessor.Builder()
                .add(new ResizeOp(200, 200, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camaraIntent = new Intent();
                camaraIntent.setAction(Intent.ACTION_GET_CONTENT);
                camaraIntent.setType("image/*");
                startActivityForResult(camaraIntent, 10);
            }
        });

        camaraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });
        byte[] buffer;
        String prototxt;
        byte[] caffeModel;
        try {
            // Load the prototxt file from the assets directory
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("age.prototxt");
            prototxt = readStream(inputStream);

            // Load the caffemodel file from the assets directory
            inputStream = assetManager.open("dex_chalearn_iccv2015.caffemodel");
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            caffeModel = outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        net = Dnn.readNetFromCaffe(prototxt, String.valueOf(caffeModel));
        Log.i(TAG, "Network loaded successfully");

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long start = System.currentTimeMillis();

                Mat blob = Dnn.blobFromImage(mat);
                net.setInput(blob);
                Mat prediction = net.forward();
                result.setText(prediction + "");
                    /*AgeModel1 model = AgeModel1.newInstance(MainActivity.this);
                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 200, 200, 3}, DataType.FLOAT32);
                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                    tensorImage.load(bitmap);
                    tensorImage = processor.process(tensorImage);
                    TensorBuffer tensorBuffer = tensorImage.getTensorBuffer();
                    inputFeature0.loadBuffer(tensorImage.getBuffer());

                    float[] inputArray = inputFeature0.getFloatArray();
                    for (int i = 0; i < inputArray.length; i++) {
                        if (Float.isNaN(inputArray[i])) {
                            inputArray[i] = 0;
                        }
                    }
                    inputFeature0.loadArray(inputArray);
                    // Runs model inference and gets result.
                    AgeModel1.Outputs outputs = model.process(inputFeature0);

                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    result.setText(outputFeature0.getFloatArray()[0]+"");*/
                long inferenceTime = System.currentTimeMillis() - start;
                time.setText(inferenceTime + "");
                // Releases model resources if no longer used.
                //model.close();

            }
        });
    }

    void getPermission(){
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==11){
            if(grantResults.length>0){
                if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==10){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    mat = Imgcodecs.imread(uri.getPath());
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if(requestCode==12){
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toString("UTF-8");
    }
    private Net net;
}