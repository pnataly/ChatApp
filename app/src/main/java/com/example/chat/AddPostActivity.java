package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chat.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ActionBar actionBar;
    private TextInputLayout postTitleLayout, postDescriptionLayout;
    private ImageView postImage;
    private FloatingActionButton uploadButton;

    private FirebaseAuth firebaseAuth;
    private String currentUserId, currentUserName, currentUserImage;
    private DatabaseReference userRef;

    private Uri imageUri = null;
    private ProgressDialog progressDialog;

    private String editPostTitle, editPostDescription, editPostImage, editPostId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        setupUI();

        final String key = getIntent().getStringExtra("key");
        if(key.equals("edit")){ //update post
            editPostId = getIntent().getStringExtra("post_id");
            actionBar.setTitle("Edit Post");
            loadPostInfo(editPostId);
        }
        else{  //add new post
            actionBar.setTitle("Add New Post");

        }

        //get current user info
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    currentUserName = dataSnapshot.child("name").getValue().toString();
                    if(dataSnapshot.hasChild("image")){
                        currentUserImage = dataSnapshot.child("image").getValue().toString();
                    }
                    else{
                        currentUserImage = "";
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        postImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = postTitleLayout.getEditText().getText().toString().trim();
                String description = postDescriptionLayout.getEditText().getText().toString().trim();

                if(title.isEmpty()){
                    postTitleLayout.setError("Field can't be Empty");
                } else {
                    postTitleLayout.setError(null);

                    if(key.equals("edit")){
                        updatePost(title, description);
                    }
                    else{
                        if(imageUri == null){
                            uploadData(title, description, "");
                        }
                        else{
                            uploadData(title, description, String.valueOf(imageUri));
                        }
                    }

                }
            }
        });

    }

    private void setupUI(){
        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);

        postTitleLayout = findViewById(R.id.post_title);
        postDescriptionLayout = findViewById(R.id.post_description);
        postImage = findViewById(R.id.post_image);
        uploadButton = findViewById(R.id.upload_button);
    }

    private void loadPostInfo(String editPostId){
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        //Query query = postRef.child(currentUserId).child("Post_Info").orderByChild("time").equalTo(editPostId);
        //query.addValueEventListener(new ValueEventListener() {
        postRef.child(currentUserId).child(editPostId).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                  //  for(DataSnapshot ds : dataSnapshot.getChildren()){

                        //get the info of the post
                        editPostTitle = dataSnapshot.child("Post_Info").child("post_title").getValue().toString();
                        editPostDescription = dataSnapshot.child("Post_Info").child("post_description").getValue().toString();
                        editPostImage = dataSnapshot.child("Post_Info").child("post_image").getValue().toString();

                        //set data to views
                        postTitleLayout.getEditText().setText(editPostTitle);
                        postDescriptionLayout.getEditText().setText(editPostDescription);
                        if(!editPostImage.isEmpty()){
                            Picasso.get().load(editPostImage).into(postImage);
                        }
                   // }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void updatePost(String title, String description){
        progressDialog.setMessage("Updating Post...");
        progressDialog.show();

        if(editPostImage.isEmpty()){
            updateWasWithoutImage(title, description);
        }
        else{
            updateWasWithImage(title, description);
        }
    }

    private void updateWasWithImage(final String title, final String description) {
        StorageReference postStorage = FirebaseStorage.getInstance().getReferenceFromUrl(editPostImage);
        postStorage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //delete the prev image - upload new image
                if(imageUri != null){
                    final String time = String.valueOf(System.currentTimeMillis());
                    StorageReference storage = FirebaseStorage.getInstance().getReference().child("Posts").child("post" + time);
                    storage.putFile(Uri.parse( String.valueOf(imageUri))).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                            while (!task.isSuccessful());
                            String downloadUri = task.getResult().toString();

                            if(task.isSuccessful()){
                                HashMap<String, Object> postBodyMap = new HashMap<>();
                                postBodyMap.put("user_id", currentUserId);
                                postBodyMap.put("user_name", currentUserName);
                                postBodyMap.put("user_image", currentUserImage);
                                //postBodyMap.put("time", time);
                                postBodyMap.put("post_title", title);
                                if(description.isEmpty()){
                                    postBodyMap.put("post_description", "");

                                }
                                else{
                                    postBodyMap.put("post_description", description);
                                }
                                postBodyMap.put("post_image", downloadUri);

                                DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
                                postRef.child(currentUserId).child(editPostId).child("Post_Info").updateChildren(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddPostActivity.this, "Post Updated Successfully... " , Toast.LENGTH_SHORT).show();
                                        //reset views
                                        postTitleLayout.getEditText().setText("");
                                        postDescriptionLayout.getEditText().setText("");
                                        postImage.setImageURI(null);
                                        imageUri = null;

                                        sendUserToProfile();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{

                    HashMap<String, Object> postBodyMap = new HashMap<>();
                    postBodyMap.put("user_id", currentUserId);
                    postBodyMap.put("user_name", currentUserName);
                    postBodyMap.put("user_image", currentUserImage);
                    //postBodyMap.put("time", time);
                    postBodyMap.put("post_title", title);
                    if(description.isEmpty()){
                        postBodyMap.put("post_description", "");

                    }
                    else{
                        postBodyMap.put("post_description", description);
                    }
                    postBodyMap.put("post_image", "");

                    DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
                    postRef.child(currentUserId).child(editPostId).child("Post_Info").updateChildren(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "Post Updated Successfully... " , Toast.LENGTH_SHORT).show();
                            //reset views
                            postTitleLayout.getEditText().setText("");
                            postDescriptionLayout.getEditText().setText("");
                            postImage.setImageURI(null);
                            imageUri = null;

                            sendUserToProfile();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage() , Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWasWithoutImage(final String title, final String description) {
        if(imageUri != null){
            final String time = String.valueOf(System.currentTimeMillis());
            StorageReference storage = FirebaseStorage.getInstance().getReference().child("Posts").child("post" + time);
            storage.putFile(Uri.parse( String.valueOf(imageUri))).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                    while (!task.isSuccessful());
                    String downloadUri = task.getResult().toString();

                    if(task.isSuccessful()){
                        HashMap<String, Object> postBodyMap = new HashMap<>();
                        postBodyMap.put("user_id", currentUserId);
                        postBodyMap.put("user_name", currentUserName);
                        postBodyMap.put("user_image", currentUserImage);
                        //postBodyMap.put("time", time);
                        postBodyMap.put("post_title", title);
                        if(description.isEmpty()){
                            postBodyMap.put("post_description", "");

                        }
                        else{
                            postBodyMap.put("post_description", description);
                        }
                        postBodyMap.put("post_image", downloadUri);

                        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
                        postRef.child(currentUserId).child(editPostId).child("Post_Info").updateChildren(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(AddPostActivity.this, "Post Updated Successfully... " , Toast.LENGTH_SHORT).show();
                                //reset views
                                postTitleLayout.getEditText().setText("");
                                postDescriptionLayout.getEditText().setText("");
                                postImage.setImageURI(null);
                                imageUri = null;

                                sendUserToProfile();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{

            HashMap<String, Object> postBodyMap = new HashMap<>();
            postBodyMap.put("user_id", currentUserId);
            postBodyMap.put("user_name", currentUserName);
            postBodyMap.put("user_image", currentUserImage);
            postBodyMap.put("post_title", title);
            if(description.isEmpty()){
                postBodyMap.put("post_description", "");

            }
            else{
                postBodyMap.put("post_description", description);
            }
            postBodyMap.put("post_image", "");

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
            postRef.child(currentUserId).child(editPostId).child("Post_Info").updateChildren(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Post Updated Successfully... " , Toast.LENGTH_SHORT).show();
                    //reset views
                    postTitleLayout.getEditText().setText("");
                    postDescriptionLayout.getEditText().setText("");
                    postImage.setImageURI(null);
                    imageUri = null;

                    sendUserToProfile();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

    }

    private void uploadData(final String title, final String description, final String image){
        progressDialog.setMessage("Publishing post...");
        progressDialog.show();

        final String time = String.valueOf(System.currentTimeMillis());

        if(image.isEmpty()){ //post without image

            HashMap<String, Object> postBodyMap = new HashMap<>();
            postBodyMap.put("user_id", currentUserId);
            postBodyMap.put("user_name", currentUserName);
            postBodyMap.put("user_image", currentUserImage);
            postBodyMap.put("time", time);
            postBodyMap.put("post_title", title);
            if(description.isEmpty()){
                postBodyMap.put("post_description", "");

            }
            else{
                postBodyMap.put("post_description", description);
            }
            postBodyMap.put("post_image", "");
            postBodyMap.put("likes", "0");
            postBodyMap.put("comments", "0");

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
            postRef.child(currentUserId).child(time).child("Post_Info").setValue(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Post Published Successfully... " , Toast.LENGTH_SHORT).show();

                    //reset views
                    postTitleLayout.getEditText().setText("");
                    postDescriptionLayout.getEditText().setText("");
                    postImage.setImageURI(null);
                    imageUri = null;


                    sendUserToProfile();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });


        }
        else{   //post with image
            StorageReference storage = FirebaseStorage.getInstance().getReference().child("Posts").child("post" + time);
            storage.putFile(Uri.parse(image)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                    while (!task.isSuccessful());
                    String downloadUri = task.getResult().toString();

                    if(task.isSuccessful()){
                        HashMap<String, Object> postBodyMap = new HashMap<>();
                        postBodyMap.put("user_id", currentUserId);
                        postBodyMap.put("user_name", currentUserName);
                        postBodyMap.put("user_image", currentUserImage);
                        postBodyMap.put("time", time);
                        postBodyMap.put("post_title", title);
                        if(description.isEmpty()){
                            postBodyMap.put("post_description", "");

                        }
                        else{
                            postBodyMap.put("post_description", description);
                        }
                        postBodyMap.put("post_image", downloadUri);
                        postBodyMap.put("likes", "0");
                        postBodyMap.put("comments", "0");

                        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
                        postRef.child(currentUserId).child(time).setValue(postBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(AddPostActivity.this, "Post Published Successfully... " , Toast.LENGTH_SHORT).show();
                                //reset views
                                postTitleLayout.getEditText().setText("");
                                postDescriptionLayout.getEditText().setText("");
                                postImage.setImageURI(null);
                                imageUri = null;


                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showImageDialog(){
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if(position == 0){ //camera
                    if(!Util.permissionsCameraCheck(AddPostActivity.this)){
                        Util.permissionsCameraRequest(AddPostActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(AddPostActivity.this)){
                        Util.permissionsStorageRequest(AddPostActivity.this);
                    }
                    else{
                        pickImageFromGallery();
                    }
                }
            }
        }).show();
    }

    private void pickImageFromGallery(){
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Util.IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera(){
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Group Image Title");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Group Image Description");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, Util.IMAGE_PICK_CAMERA_CODE);
    }

    private void sendUserToProfile(){
        Intent intent = new Intent(AddPostActivity.this, MyProfileActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Util.CAMERA_REQUEST_CODE:
                if(grantResults.length >0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        pickFromCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & Storage permissions required", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case Util.STORAGE_REQUEST_CODE:
                if(grantResults.length >0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        pickImageFromGallery();
                    }
                }else{
                    Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //image pick result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(resultCode == RESULT_OK){

            if(requestCode == Util.IMAGE_PICK_GALLERY_CODE){
                imageUri = data.getData();
                postImage.setImageURI(imageUri);
            }
            else if(requestCode == Util.IMAGE_PICK_CAMERA_CODE){
                postImage.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
