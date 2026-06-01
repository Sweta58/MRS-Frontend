package com.example.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 100;

    Button button_capture, button_copy;
    TextView textview_data;
    LinearLayout layout_empty;
    Bitmap bitmap;

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri resultUri = result.getUriContent();
                    if (resultUri == null) {
                        Toast.makeText(this, "Could not retrieve cropped image", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Bitmap newBitmap = decodeBitmap(resultUri);
                        if (bitmap != null) bitmap.recycle();
                        bitmap = newBitmap;
                        getTextFromImage(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to decode bitmap", e);
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.getError() != null) {
                    Log.e(TAG, "Crop error", result.getError());
                    Toast.makeText(this, "Image crop failed", Toast.LENGTH_SHORT).show();
                }
                // else: user cancelled — do nothing
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_capture = findViewById(R.id.button_capture);
        button_copy = findViewById(R.id.button_copy);
        textview_data = findViewById(R.id.text_data);
        layout_empty = findViewById(R.id.layout_empty);

        requestNeededPermissions();

        button_capture.setOnClickListener(v -> {
            CropImageOptions options = new CropImageOptions();
            options.guidelines = CropImageView.Guidelines.ON;
            cropImageLauncher.launch(new CropImageContractOptions(null, options));
        });

        button_copy.setOnClickListener(v ->
                copyToClipboard(textview_data.getText().toString()));
    }

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

    @SuppressWarnings("deprecation")
    private Bitmap decodeBitmap(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }

    private void getTextFromImage(Bitmap bmp) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage image = InputImage.fromBitmap(bmp, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String text = visionText.getText().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "No text found in image", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    layout_empty.setVisibility(View.GONE);
                    textview_data.setVisibility(View.VISIBLE);
                    textview_data.setText(text);
                    button_capture.setText(R.string.btn_retake);
                    button_copy.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> recognizer.close());
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipBoard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied data", text);
        clipBoard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
