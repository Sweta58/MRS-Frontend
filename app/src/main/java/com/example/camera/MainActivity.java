package com.example.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 100;

    Button button_capture, button_copy;
    TextView textview_data;
    Bitmap bitmap;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_capture = findViewById(R.id.button_capture);
        button_copy = findViewById(R.id.button_copy);
        textview_data = findViewById(R.id.text_data);

        requestNeededPermissions();

        button_capture.setOnClickListener(v ->
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this));

        button_copy.setOnClickListener(v ->
                copyToClipboard(textview_data.getText().toString()));
    }

    // Fix #2: request both CAMERA and READ_MEDIA_IMAGES (API 33+) at runtime
    private void requestNeededPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_MEDIA_IMAGES);
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Uri resultUri = result.getUriContent();
                try {
                    Bitmap newBitmap = decodeBitmap(resultUri);
                    // Fix #6: recycle previous bitmap before reassigning
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    bitmap = newBitmap;
                    getTextFromImage(bitmap);
                } catch (IOException e) {
                    // Fix #5: proper logging instead of e.printStackTrace()
                    Log.e(TAG, "Failed to decode bitmap", e);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode != RESULT_CANCELED) {
                // Fix #4: handle crop error (not just OK and silent cancel)
                Toast.makeText(this, "Image crop failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Fix #3: use ImageDecoder on API 28+; fall back to deprecated call on older devices
    @SuppressWarnings("deprecation")
    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }

    @SuppressLint("SetTextI18n")
    private void getTextFromImage(Bitmap bmp) {
        TextRecognizer recognizer = new TextRecognizer.Builder(this).build();
        // Fix #1: always release the recognizer to avoid native resource leak
        try {
            if (!recognizer.isOperational()) {
                Toast.makeText(this, "Text recognizer not available", Toast.LENGTH_SHORT).show();
                return;
            }
            Frame frame = new Frame.Builder().setBitmap(bmp).build();
            SparseArray<TextBlock> blocks = recognizer.detect(frame);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                sb.append(blocks.valueAt(i).getValue());
                sb.append("\n");
            }
            textview_data.setText(sb.toString());
            button_capture.setText("Retake");
            button_copy.setVisibility(View.VISIBLE);
        } finally {
            recognizer.release();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipBoard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied data", text);
        clipBoard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
