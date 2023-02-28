package com.example.agepredict;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button camaraBtn, importBtn, predictBtn;
    TextView result, time;
    ImageView imageView;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        camaraBtn = findViewById(R.id.camaraButton);
        importBtn = findViewById(R.id.importButton);
        imageView = findViewById(R.id.image_View);
        predictBtn = findViewById(R.id.predictButton);
        result = findViewById(R.id.result);
        time = findViewById(R.id.infTime);

        ImageProcessor processor = new ImageProcessor.Builder()
                .add(new ResizeOp(200,200, ResizeOp.ResizeMethod.BILINEAR))
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

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    AgeModel1 model = AgeModel1.newInstance(MainActivity.this);
                    long start = System.currentTimeMillis();
                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 200, 200, 3}, DataType.FLOAT32);
                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                    tensorImage.load(bitmap);
                    tensorImage = processor.process(tensorImage);
                    //bitmap = Bitmap.createScaledBitmap(bitmap,400,400,true);
                    //TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
                    TensorBuffer tensorBuffer = tensorImage.getTensorBuffer();
                    Log.d("tensorImage shape", Arrays.toString(tensorBuffer.getShape()));
                    Log.d("tensorImage data", Arrays.toString(tensorBuffer.getFloatArray()));
                    inputFeature0.loadBuffer(tensorImage.getBuffer());
                    //inputFeature0.loadBuffer(TensorImage.fromBitmap(bitmap).getBuffer());

                    Log.d("inputFeature0 shape", Arrays.toString(inputFeature0.getShape()));
                    Log.d("inputFeature0 data", Arrays.toString(inputFeature0.getFloatArray()));

                    float[] inputArray = inputFeature0.getFloatArray();
                    for (int i = 0; i < inputArray.length; i++) {
                        if (Float.isNaN(inputArray[i])) {
                            inputArray[i] = 0;
                        }
                    }

                    inputFeature0.loadArray(inputArray);
                    Log.d("inputFeature0 processed data", Arrays.toString(inputFeature0.getFloatArray()));
                    // Runs model inference and gets result.
                    AgeModel1.Outputs outputs = model.process(inputFeature0);

                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                    long inferenceTime = System.currentTimeMillis() - start;
                    Log.d("outputs shape", Arrays.toString(outputFeature0.getShape()));
                    Log.d("outputs data", Arrays.toString(outputFeature0.getFloatArray()));
                    result.setText(outputFeature0.getFloatArray()[0]+"");
                    time.setText(inferenceTime + "");
                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }

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


}