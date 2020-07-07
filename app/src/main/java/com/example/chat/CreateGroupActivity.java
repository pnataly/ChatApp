package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chat.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroupActivity extends AppCompatActivity {


    private Uri imageUri = null;

    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private DatabaseReference groupRef;
    private StorageReference groupImageReference;

    private FloatingActionButton createGroupButton;
    private TextInputLayout groupTitleLayout;
    private TextInputLayout groupDescriptionLayout;
    private CircleImageView groupImage;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        groupRef =  FirebaseDatabase.getInstance().getReference().child("Groups");
        groupImageReference = FirebaseStorage.getInstance().getReference().child("Groups images");


        createGroupButton = findViewById(R.id.create_group_button);
        groupTitleLayout = findViewById(R.id.group_title);
        groupDescriptionLayout = findViewById(R.id.group_description);
        groupImage = findViewById(R.id.group_image);

        Util.cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Util.storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        groupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
            }
        });

        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGroupInfo();
            }
        });
    }

    private void getGroupInfo(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating Group");

        final String groupTitle = groupTitleLayout.getEditText().getText().toString().trim();
        final String groupDescription = groupDescriptionLayout.getEditText().getText().toString().trim();
        Log.d("Mydebug", groupTitle);

        final String time = String.valueOf(System.currentTimeMillis());
        if(groupTitle.isEmpty()){
            groupTitleLayout.setError("Field can't be Empty");
        } else{
            groupTitleLayout.setError(null);
            progressDialog.show();

            if(imageUri == null){
                createGroup(time, groupTitle, groupDescription, "");
            }
            else{
                //upload image to firebase storage
                final StorageReference filePath = groupImageReference.child(groupTitle + ".jpg");
                filePath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> taskUri = taskSnapshot.getStorage().getDownloadUrl();
                        while (!taskUri.isSuccessful());
                        Uri imageDownloadUri = taskUri.getResult();
                        if(taskUri.isSuccessful()){
                            createGroup(time, groupTitle, groupDescription, String.valueOf(imageDownloadUri));
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateGroupActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void createGroup(final String time, final String title, String description, final String image){

        HashMap<String, Object> groupBodyMap = new HashMap<>();
        Log.d("Mydebug", title);
        groupBodyMap.put("title", title);
        groupBodyMap.put("description", description);
        groupBodyMap.put("image",image);
        groupBodyMap.put("time", time);
        Log.d("Mydebug", time);

        groupBodyMap.put("createdBy", currentUserId);

        groupRef.child(time).child("Group_Info").setValue(groupBodyMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                HashMap<String, Object> participantGroupMap = new HashMap<>();
                participantGroupMap.put("uid", currentUserId);
                participantGroupMap.put("role", "creator");
                participantGroupMap.put("time", time);

                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
                dbRef.child(time).child("Participants").child(currentUserId).setValue(participantGroupMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(CreateGroupActivity.this, "Group Created Successfully.", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(CreateGroupActivity.this, GroupActivity.class);
                                intent.putExtra("group_id", time);
                                intent.putExtra("group_name", title);
                                intent.putExtra("group_image", image);
                                startActivity(intent);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(CreateGroupActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(CreateGroupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void showImageDialog(){
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if(position == 0){ //camera
                    if(!Util.permissionsCameraCheck(CreateGroupActivity.this)){
                        Util.permissionsCameraRequest(CreateGroupActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(CreateGroupActivity.this)){
                        Util.permissionsStorageRequest(CreateGroupActivity.this);
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
                Picasso.get().load(imageUri).placeholder(R.mipmap.ic_launcher_round).into(groupImage);
            }
            else if(requestCode == Util.IMAGE_PICK_CAMERA_CODE){
                Picasso.get().load(imageUri).placeholder(R.mipmap.ic_launcher_round).into(groupImage);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
