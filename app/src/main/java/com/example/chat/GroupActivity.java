package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.adapter.GroupMessageAdapter;
import com.example.chat.model.Message;
import com.example.chat.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupActivity extends AppCompatActivity {

    private static final int IMAGE_INTENT_REQ_CODE = 11;

    private TextView groupActionbarName;
    private CircleImageView groupActionBarImage;

    private EditText inputMessage;
    private ImageView sendButton;
    private Toolbar toolbar;

    private RecyclerView recyclerView;
    private ImageView sendMessageButton, sendFileButton;
    private TextView displayMessage;
    private GroupMessageAdapter groupMessageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private ProgressDialog loading;

    private String groupId, groupName, groupImage;
    private String currentUserId, currentUserName, currentUserRole;
    private String currentTime, currentDate;
    private String checker = "";
    private Uri fileUri;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference dbReference;
    private DatabaseReference dbGroup;
    private DatabaseReference dbGroupMessage;

    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        groupId = getIntent().getStringExtra("group_id");
        groupName = getIntent().getStringExtra("group_name");
        groupImage = getIntent().getStringExtra("group_image");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        dbReference = FirebaseDatabase.getInstance().getReference().child("Users");
        dbGroup = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId);

        setupUI();

        getUserInfo();
        loadUserRole();

        dbGroup.child("Messages").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);
                messageList.add(message);
                groupMessageAdapter.notifyDataSetChanged();

                recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMessage();
            }
        });

        sendFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[] = new CharSequence[]
                        {
                                "Images",
                                "PDF Files",
                                "Ms Word Files"
                        };
                AlertDialog.Builder dialog = new AlertDialog.Builder(GroupActivity.this);
                dialog.setTitle("Select File");
                dialog.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {

                        if(position == 0){
                            checker = "image";
                            showImageDialog();

                        }
                        if(position == 1){
                            checker = "pdf";
                            Intent pdfIntent = new Intent();
                            pdfIntent.setAction(Intent.ACTION_GET_CONTENT);
                            pdfIntent.setType("application/pdf");
                            startActivityForResult(pdfIntent.createChooser(pdfIntent, "Select PDF File"), IMAGE_INTENT_REQ_CODE);
                        }
                        if(position == 2){
                            checker = "docx";
                            Intent docxIntent = new Intent();
                            docxIntent.setAction(Intent.ACTION_GET_CONTENT);
                            docxIntent.setType("application/msword");
                            startActivityForResult(docxIntent.createChooser(docxIntent, "Select DOCX File"), IMAGE_INTENT_REQ_CODE);
                        }
                    }

                });
                dialog.show();
            }
        });
    }

    private void setupUI(){
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_group_bar, null);
        actionBar.setCustomView(actionBarView);

        groupActionbarName = findViewById(R.id.group_name);
        groupActionBarImage = findViewById(R.id.group_image);

        groupActionbarName.setText(groupName);
        if(groupImage.isEmpty()){
            groupActionBarImage.setImageResource(R.mipmap.ic_launcher_round);
        } else{
            Picasso.get().load(groupImage).into(groupActionBarImage);
        }

        sendButton = findViewById(R.id.send_button);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        inputMessage = findViewById(R.id.input_message);
        sendMessageButton = findViewById(R.id.send_button);
        sendFileButton = findViewById(R.id.send_file_button);

        groupMessageAdapter = new GroupMessageAdapter(messageList, groupId, currentUserId);
        recyclerView.setAdapter(groupMessageAdapter);

        Calendar calendarDate = Calendar.getInstance();
        SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
        currentDate = currentDateFormat.format(calendarDate.getTime());

        Calendar calendarTime = Calendar.getInstance();
        SimpleDateFormat currentTimeFormat = new SimpleDateFormat("HH:mm");
        currentTime = currentTimeFormat.format(calendarTime.getTime());

        loading = new ProgressDialog(GroupActivity.this);

    }

    private void showImageDialog(){
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image:").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                if(position == 0){ //camera
                    if(!Util.permissionsCameraCheck(GroupActivity.this)){
                        Util.permissionsCameraRequest(GroupActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(GroupActivity.this)){
                        Util.permissionsStorageRequest(GroupActivity.this);
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

    private void loadUserRole(){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        dbRef.child(groupId).child("Participants").orderByChild("uid").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                            currentUserRole = ds.child("role").getValue().toString();

                            //refresh menu items
                            invalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void getUserInfo(){
        dbReference.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){

                    currentUserName = dataSnapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveMessage() {
        String message = inputMessage.getText().toString();
        String messageKey = dbGroup.push().getKey();

        if(!message.isEmpty()){
            Calendar calendarDate = Calendar.getInstance();
            SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
            currentDate = currentDateFormat.format(calendarDate.getTime());

            Calendar calendarTime = Calendar.getInstance();
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("HH:mm");
            currentTime = currentTimeFormat.format(calendarTime.getTime());

            HashMap<String, Object> groupMessageKey = new HashMap<>();
            dbGroup.child("Messages").updateChildren(groupMessageKey);

            dbGroupMessage = dbGroup.child("Messages").child(messageKey);

            HashMap<String, Object> messageInfo = new HashMap<>();
            messageInfo.put("name", currentUserName);
            messageInfo.put("from", currentUserId);
            messageInfo.put("type", "text");
            messageInfo.put("message", message);
            messageInfo.put("date", currentDate);
            messageInfo.put("time", currentTime);
            messageInfo.put("messageID", messageKey);

            dbGroupMessage.updateChildren(messageInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    inputMessage.setText("");
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == IMAGE_INTENT_REQ_CODE && resultCode == RESULT_OK && data != null && data.getData() != null){
            loading.setTitle("Sending File");
            loading.setMessage("Please wait, while sending the file...");
            loading.setCanceledOnTouchOutside(false);
            loading.show();

            fileUri = data.getData();

            if(!checker.equals("image")){

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                final String messageKey = dbGroup.push().getKey();

                HashMap<String, Object> groupMessageKey = new HashMap<>();
                dbGroup.child("Messages").updateChildren(groupMessageKey);

                dbGroupMessage = dbGroup.child("Messages").child(messageKey);
                final StorageReference filePath = storageReference.child(messageKey + "." + checker);

                filePath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();

                                Map messageDocumentBody = new HashMap<>();
                                messageDocumentBody.put("message", downloadUrl);
                                messageDocumentBody.put("name", fileUri.getLastPathSegment());
                                messageDocumentBody.put("type", checker);
                                messageDocumentBody.put("from", currentUserId);
                                //messageDocumentBody.put("to", receiverId);
                                messageDocumentBody.put("messageID", messageKey);
                                messageDocumentBody.put("date", currentDate);
                                messageDocumentBody.put("time", currentTime);


                                dbGroupMessage.updateChildren(messageDocumentBody).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        loading.dismiss();
                                        inputMessage.setText("");
                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loading.dismiss();
                                Toast.makeText(GroupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double p = (100.0* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        loading.setMessage((int) p + " % Uploading...");
                    }
                });
            }

            else {
                loading.dismiss();
            }
        }

        else if(resultCode == RESULT_OK && data != null && data.getData() != null && ((requestCode == Util.IMAGE_PICK_GALLERY_CODE) || (requestCode == Util.IMAGE_PICK_CAMERA_CODE))) {
            loading.setTitle("Sending Image");
            loading.setMessage("Please wait, while sending the image...");
            loading.setCanceledOnTouchOutside(false);
            loading.show();

            if((requestCode == Util.IMAGE_PICK_GALLERY_CODE && resultCode == RESULT_OK)){
                imageUri = data.getData();

            }
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

            final String messageKey = dbGroup.push().getKey();
            HashMap<String, Object> groupMessageKey = new HashMap<>();
            dbGroup.child("Messages").updateChildren(groupMessageKey);

            dbGroupMessage = dbGroup.child("Messages").child(messageKey);

            final StorageReference filePath = storageReference.child(messageKey + ".jpg");

            filePath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> taskUri = taskSnapshot.getStorage().getDownloadUrl();
                    while (!taskUri.isSuccessful());
                    Uri imageDownloadUri = taskUri.getResult();
                    if(taskUri.isSuccessful()){
                        //createGroup(time, groupTitle, groupDescription, String.valueOf(imageDownloadUri));

                        Map messageImageBody = new HashMap();
                        messageImageBody.put("message", String.valueOf(imageDownloadUri));
                        messageImageBody.put("name", imageDownloadUri.getLastPathSegment());
                        messageImageBody.put("type", checker);
                        messageImageBody.put("from", currentUserId);
                        messageImageBody.put("messageID", messageKey);
                        messageImageBody.put("date", currentDate);
                        messageImageBody.put("time", currentTime);

                        dbGroupMessage.updateChildren(messageImageBody).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                loading.dismiss();
                                inputMessage.setText("");
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    loading.dismiss();
                    Toast.makeText(GroupActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        if(currentUserRole.equals("creator") || currentUserRole.equals("admin")){
            menu.findItem(R.id.action_add_user).setVisible(true);
        }
        menu.findItem(R.id.action_group_info).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_add_user:
                Intent intent = new Intent(this, GroupAddParticipantsActivity.class);
                intent.putExtra("group_name", groupName);
                intent.putExtra("group_id", groupId);
                startActivity(intent);
                return true;

            case R.id.action_group_info:
                Intent infoIntent = new Intent(this, GroupInfoActivity.class);
                infoIntent.putExtra("group_id", groupId);
                infoIntent.putExtra("group_name", groupName);
                startActivity(infoIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
