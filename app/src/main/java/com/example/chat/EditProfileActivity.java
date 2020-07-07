package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView coverImage;
    private TextInputLayout userNameLayout;
    private TextInputLayout userStatusLayout;
    private CircleImageView userProfileImage;
    private FloatingActionButton updateButton;

    private ProgressDialog dialog;
    private String currentUserId;

    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        currentUserId = getIntent().getStringExtra("user_id");

        setupUI();
        getUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
            }
        });
    }

    private void setupUI(){
        updateButton = findViewById(R.id.update);
        userNameLayout = findViewById(R.id.user_name);
        userStatusLayout = findViewById(R.id.user_status);
        userProfileImage = findViewById(R.id.profile_image);
        coverImage = findViewById(R.id.background);
        dialog = new ProgressDialog(this);
    }

    private void update(){
        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Profile");

        final String username = userNameLayout.getEditText().getText().toString();
        final String status = userStatusLayout.getEditText().getText().toString();

        if(confirmInput()){
            dialog.show();

            if(imageUri == null){

                updateUserInfo(username, status, "");
            }

            else{
                StorageReference storage = FirebaseStorage.getInstance().getReference().child("Profile images");

                final StorageReference filePath = storage.child(currentUserId + ".jpg");
                filePath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> taskUri = taskSnapshot.getStorage().getDownloadUrl();
                        while (!taskUri.isSuccessful());
                        Uri imageDownloadUri = taskUri.getResult();
                        if(taskUri.isSuccessful()){
                            updateUserInfo(username, status, String.valueOf(imageDownloadUri));

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }


        }
    }

    private void updateUserInfo(final String username, String status, final String image){

        HashMap<String, Object> userBodyMap = new HashMap<>();
        userBodyMap.put("name", username);
        userBodyMap.put("status", status);
        userBodyMap.put("image",image);
        userBodyMap.put("uid",currentUserId);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(currentUserId).updateChildren(userBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dialog.dismiss();
                sendUserToProfile();
                Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getUserInfo(){
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.hasChild("name") && dataSnapshot.hasChild("image")){

                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();
                    String retrieveImage = dataSnapshot.child("image").getValue().toString();

                    if (!retrieveImage.isEmpty()){
                        Picasso.get().load(retrieveImage).into(userProfileImage);
                    }

                    userNameLayout.getEditText().setText(retrieveUsername);
                    userStatusLayout.getEditText().setText(retrieveStatus);
                }
                else if(dataSnapshot.exists() && dataSnapshot.hasChild("name")){

                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                    userNameLayout.getEditText().setText(retrieveUsername);
                    userStatusLayout.getEditText().setText(retrieveStatus);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean validateUsername() {
        String username = userNameLayout.getEditText().getText().toString();

        if(username.isEmpty()){
            userNameLayout.setError("Field can't be empty");
            return false;
        }

        else {
            userNameLayout.setError(null);
            return true;
        }
    }

    private boolean validateStatus() {
        String status = userStatusLayout.getEditText().getText().toString();

        if(status.isEmpty()){
            userStatusLayout.setError("write your status");
            return false;
        }

        else {
            userStatusLayout.setError(null);
            return true;
        }
    }

    public boolean confirmInput() {
        if (!validateUsername() | !validateStatus()){
            return false;
        }
        return true;
    }

    private void sendUserToProfile(){
        Intent intent = new Intent(EditProfileActivity.this, MyProfileActivity.class);
        intent.putExtra("user_id", currentUserId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showImageDialog(){
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if(position == 0){ //camera
                    if(!Util.permissionsCameraCheck(EditProfileActivity.this)){
                        Util.permissionsCameraRequest(EditProfileActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(EditProfileActivity.this)){
                        Util.permissionsStorageRequest(EditProfileActivity.this);
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
                userProfileImage.setImageURI(imageUri);
            }
            else if(requestCode == Util.IMAGE_PICK_CAMERA_CODE){
                userProfileImage.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
