package com.example.objectdetectionproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonLoadImage = (Button) findViewById(R.id.button);
        Button detectButton = (Button) findViewById(R.id.detect);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TextView textView = findViewById(R.id.result_text);
                textView.setText("");
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);


            }
        });

        detectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Bitmap bitmap = null;
                Module module = null;

                ImageView imageView = (ImageView) findViewById(R.id.image);

                try {
                    bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

                    bitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

                    module = Module.load(fetchModelFile(MainActivity.this, "resnet18_traced.pt"));
                } catch (IOException e) {
                    finish();
                }

                final Tensor input = TensorImageUtils.bitmapToFloat32Tensor(
                        bitmap,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                        TensorImageUtils.TORCHVISION_NORM_STD_RGB
                );

                final Tensor output = module.forward(IValue.from(input)).toTensor();


                final float[] score_arr = output.getDataAsFloatArray();

                float max_score = -Float.MAX_VALUE;

                TextView textView = findViewById(R.id.result_text);
                textView.setMovementMethod(new ScrollingMovementMethod());
                int ms_ix = -1;
                for (int i = 0; i < score_arr.length; i++) {
                    if (score_arr[i] > 5) {
                        max_score = score_arr[i];
                        ms_ix = i;
                        String detected_class = ModelClasses.MODEL_CLASSES[ms_ix];
                        textView.setText(textView.getText()+" "+max_score+":"+detected_class+";\n");
                    }
                }





            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.image);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            imageView.setImageURI(null);
            imageView.setImageURI(selectedImage);


        }


    }

    public static String fetchModelFile(Context context, String modelName) throws IOException {
        File file = new File(context.getFilesDir(), modelName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(modelName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

}
