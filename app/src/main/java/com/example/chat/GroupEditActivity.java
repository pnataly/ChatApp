package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.chat.model.Group;
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

import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupEditActivity extends AppCompatActivity {

    private ActionBar actionBar;
    private Toolbar toolbar;

    private String groupId;

    private Uri imageUri = null;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private DatabaseReference groupRef;
    private StorageReference groupImageReference;

    private FloatingActionButton updateGroupButton;
    private TextInputLayout groupTitleLayout;
    private TextInputLayout groupDescriptionLayout;
    private CircleImageView groupImage;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_edit);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle("Edit Group");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        groupId = getIntent().getStringExtra("group_id");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        groupRef =  FirebaseDatabase.getInstance().getReference().child("Groups");
        groupImageReference = FirebaseStorage.getInstance().getReference().child("Groups images");

        updateGroupButton = findViewById(R.id.update_group_button);
        groupTitleLayout = findViewById(R.id.group_title);
        groupDescriptionLayout = findViewById(R.id.group_description);
        groupImage = findViewById(R.id.group_image);

        Util.cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        Util.storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        loadGroupInfo();

        groupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
            }
        });

        updateGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGroup();
            }
        });
    }

    private void loadGroupInfo(){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){
                    Group group = dataSnapshot.child("Group_Info").getValue(Group.class);

                    Calendar calendar = Calendar.getInstance();
                    String groupTime = group.getTime();
                    calendar.setTimeInMillis(Long.parseLong(groupTime));

                    groupTitleLayout.getEditText().setText(group.getTitle());
                    groupDescriptionLayout.getEditText().setText(group.getDescription());

                    if(group.getImage().isEmpty()){
                        groupImage.setImageResource(R.mipmap.ic_launcher_round);
                    }
                    else{
                        Picasso.get().load(group.getImage()).placeholder(R.mipmap.ic_launcher_round).into(groupImage);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void updateGroup(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating Group");

        final String title = groupTitleLayout.getEditText().getText().toString().trim();
        final String description = groupDescriptionLayout.getEditText().getText().toString().trim();

        if(title.isEmpty()){
            groupTitleLayout.setError("Field can't be Empty");
        }
        else{
            groupTitleLayout.setError(null);
            progressDialog.show();

            if(imageUri == null){

                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
                HashMap<String, Object> groupBodyMap = new HashMap<>();
                groupBodyMap.put("title", title);
                groupBodyMap.put("description", description);

                dbRef.child(groupId).child("Group_Info").updateChildren(groupBodyMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Toast.makeText(GroupEditActivity.this, "Group info updated successfully... ", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(GroupEditActivity.this, GroupActivity.class);
                                intent.putExtra("group_id", groupId);
                                intent.putExtra("group_name", title);
                                intent.putExtra("group_image", "");
                                startActivity(intent);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(GroupEditActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
            else{
                final StorageReference filePath = groupImageReference.child(title + ".jpg");
                filePath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> taskUri = taskSnapshot.getStorage().getDownloadUrl();
                        while (!taskUri.isSuccessful());
                        final Uri imageDownloadUri = taskUri.getResult();

                        if(taskUri.isSuccessful()){

                            HashMap<String, Object> groupBodyMap = new HashMap<>();
                            groupBodyMap.put("title", title);
                            groupBodyMap.put("description", description);
                            groupBodyMap.put("image", String.valueOf(imageDownloadUri));

                            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
                            dbRef.child(groupId).child("Group_Info").updateChildren(groupBodyMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            progressDialog.dismiss();
                                            Toast.makeText(GroupEditActivity.this, "Group info updated successfully... ", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(GroupEditActivity.this, GroupActivity.class);
                                            intent.putExtra("group_name", title);
                                            intent.putExtra("group_image", String.valueOf(imageDownloadUri));
                                            startActivity(intent);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(GroupEditActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(GroupEditActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


    private void showImageDialog(){
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if(position == 0){ //camera
                    if(!Util.permissionsCameraCheck(GroupEditActivity.this)){
                        Util.permissionsCameraRequest(GroupEditActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(GroupEditActivity.this)){
                        Util.permissionsStorageRequest(GroupEditActivity.this);
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
