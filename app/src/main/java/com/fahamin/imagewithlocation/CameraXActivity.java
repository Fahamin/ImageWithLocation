package com.fahamin.imagewithlocation;

import static android.os.Build.VERSION.SDK_INT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraXActivity extends AppCompatActivity {

    private final Executor executor = Executors.newSingleThreadExecutor();


    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    androidx.camera.view.PreviewView mPreviewView;

    ImageButton captureImage;
    public static String imageurl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_xactivity);
        captureImage = findViewById(R.id.captureImg);
        mPreviewView = findViewById(R.id.camera);

        startCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startCamera();
        }
    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll();
                bindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                //MiscUtil.toastIconError(this, this, "" + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();


        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        if (preview != null) {

            cameraProvider.bindToLifecycle(CameraXActivity.this, cameraSelector, preview, imageAnalysis, imageCapture);

        } else {
            cameraProvider.bindToLifecycle(CameraXActivity.this, cameraSelector, imageAnalysis, imageCapture);

        }


        captureImage.setOnClickListener(v -> {


            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            String imageFileName = mDateFormat.format(new Date()) + ".jpg";
            File file = new File(getBatchDirectoryName(), imageFileName);

            Uri uri = Uri.fromFile(file);
            Log.e("uri", String.valueOf(uri));
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
            imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Handler h = new Handler(Looper.getMainLooper());
                    h.post(() -> {
                        //here show dialog

                        Log.e("captureImage", "" + file.getAbsolutePath());

                        imageurl = file.getAbsolutePath();


                        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.getAbsolutePath()},
                                null,
                                (path, uri1) -> {
                                    getCameraPhotoOrientation(file.getAbsolutePath());


                                });


                    });

                }

                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    error.printStackTrace();
                }
            });
        });
    }

    public int getCameraPhotoOrientation(String imagePath) {

        int rotate = 0;
        try {
            File imageFile = new File(imagePath);

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Log.i("RotateImage", "Exif orientation: " + orientation);
            Log.i("RotateImage", "rotate: " + rotate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }


    public void checkOrientation(Bitmap bitmap) {
        if (bitmap.getWidth() > bitmap.getHeight()) {
            Log.i("RotateImage", "checkOrientation: " + "landscape");
        } else {
            Log.i("RotateImage", "checkOrientation: " + "potratite");

        }
    }

    public String getBatchDirectoryName() {

        String app_folder_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/Durbin";
        File dir = new File(app_folder_path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return app_folder_path;

    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }
}