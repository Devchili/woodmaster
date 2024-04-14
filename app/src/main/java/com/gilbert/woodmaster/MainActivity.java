package com.gilbert.woodmaster;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.gilbert.woodmaster.ml.Model;

public class MainActivity extends AppCompatActivity {

    TextView result, confidence, classifiedText, confidencesText;
    ImageView imageView;
    Button picture, uploadFromGalleryButton, seeMoreButton, captureAgainButton;
    int imageSize = 224;

    // Define a global variable to store the temporary image file path
    private String tempImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        uploadFromGalleryButton = findViewById(R.id.uploadFromGalleryButton);
        seeMoreButton = findViewById(R.id.seeMoreButton);
        captureAgainButton = findViewById(R.id.CaptureAgain);
        classifiedText = findViewById(R.id.classified);
        confidencesText = findViewById(R.id.confidencesText);

        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    // Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        uploadFromGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        seeMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open activity based on the result
                String woodSpecies = result.getText().toString();
                Bitmap woodImage = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                if (woodSpecies.equals("Akasya")) {
                    Intent intent = new Intent(MainActivity.this, Akasya.class);
                    intent.putExtra("woodSpecies", woodSpecies);
                    intent.putExtra("woodImagePath", tempImagePath); // Pass the image file path
                    startActivity(intent);
                } else if (woodSpecies.equals("Melina")) {
                    Intent intent = new Intent(MainActivity.this, Melina.class);
                    intent.putExtra("woodSpecies", woodSpecies);
                    intent.putExtra("woodImagePath", tempImagePath);
                    startActivity(intent);
                } else if (woodSpecies.equals("Mahogany")) {
                    Intent intent = new Intent(MainActivity.this, Mahogany.class);
                    intent.putExtra("woodSpecies", woodSpecies);
                    intent.putExtra("woodImagePath", tempImagePath);
                    startActivity(intent);
                } else {
                    // Show toast message for unclassified wood species
                    Toast.makeText(MainActivity.this, "No information available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        captureAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Hide "See More" and "Capture Again" buttons
                seeMoreButton.setVisibility(View.GONE);
                captureAgainButton.setVisibility(View.GONE);

                // Show "Camera" and "Gallery" buttons
                picture.setVisibility(View.VISIBLE);
                uploadFromGalleryButton.setVisibility(View.VISIBLE);

                // Clear the result and confidence
                result.setText("");
                confidence.setText("");

                // Reset the image view to display no image
                imageView.setImageDrawable(null);
            }
        });

    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, 2);
    }

    public void classifyImage(Bitmap image) {
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // Iterate over pixels and extract RGB values. Add to bytebuffer.
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int pixel = image.getPixel(j, i); // Get pixel value at (i, j)
                    byteBuffer.putFloat(Color.red(pixel) * (1.f / 255.f)); // Red component
                    byteBuffer.putFloat(Color.green(pixel) * (1.f / 255.f)); // Green component
                    byteBuffer.putFloat(Color.blue(pixel) * (1.f / 255.f)); // Blue component
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // Find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"Akasya", "Mahogany", "Melina", "Unclassified"};
            result.setText(classes[maxPos]);

            String s = "";
            for (int i = 0; i < classes.length; i++) {
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }
            confidence.setText(s);

            // Show the buttons
            seeMoreButton.setVisibility(View.VISIBLE);
            captureAgainButton.setVisibility(View.VISIBLE);

            // Hide "Camera" and "Gallery" buttons
            picture.setVisibility(View.GONE);
            uploadFromGalleryButton.setVisibility(View.GONE);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            // Save the image to temporary storage
            tempImagePath = saveImageToTempStorage(image);
            // Perform classification
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                imageView.setImageBitmap(bitmap);
                // Save the image to temporary storage
                tempImagePath = saveImageToTempStorage(bitmap);
                // Perform classification
                bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
                classifyImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Method to save image to temporary storage and return the file path
    private String saveImageToTempStorage(Bitmap bitmap) {
        try {
            // Create a temporary file in the cache directory
            File tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
            // Write the bitmap to the file
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            // Return the file path
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
