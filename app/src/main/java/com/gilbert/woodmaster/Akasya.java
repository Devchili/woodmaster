package com.gilbert.woodmaster;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class Akasya extends AppCompatActivity {
    ImageView imageView;
    TextView woodSpeciesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_akasya);

        imageView = findViewById(R.id.akasyaImageView);
        woodSpeciesTextView = findViewById(R.id.akasyaWoodSpeciesTextView);

        // Retrieve wood species and image file path from intent extras
        String woodSpecies = getIntent().getStringExtra("woodSpecies");
        String woodImagePath = getIntent().getStringExtra("woodImagePath");

        // Set wood species
        woodSpeciesTextView.setText(woodSpecies);

        // Load image from file path and set it to the ImageView
        Bitmap woodImage = BitmapFactory.decodeFile(woodImagePath);
        imageView.setImageBitmap(woodImage);
    }
}
