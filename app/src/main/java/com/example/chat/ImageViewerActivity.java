package com.example.chat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends AppCompatActivity {

    private ImageView image;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        image = findViewById(R.id.imageView);
        imageUrl = getIntent().getStringExtra("url");
        Picasso.get().load(imageUrl).into(image);
    }
}
