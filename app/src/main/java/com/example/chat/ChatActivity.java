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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chat.adapter.MessagesAdapter;
import com.example.chat.model.Contacts;
import com.example.chat.model.Message;
import com.example.chat.model.User;
import com.example.chat.notifications.Data;
import com.example.chat.notifications.Sender;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatActivity extends AppCompatActivity {

    private static final int IMAGE_INTENT_REQ_CODE = 13;

    private Toolbar toolbar;
    private String receiverId, receiverName, receiverImage;
    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageView sendMessageButton, sendFileButton;

    private TextView userName, userLastSeen;
    private CircleImageView userImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference rootRef;
    private String senderId;
    private String currentDate, currentTime;
    private String checker = "";
    private Uri fileUri;
    private ProgressDialog loading;
    private Uri imageUri = null;

    private RequestQueue requestQueue;
    private boolean isNotify = false;

    private List<Message> messageList = new ArrayList<>();
    private List<User> userPhoneList;
    private MessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firebaseAuth = FirebaseAuth.getInstance();
        senderId = firebaseAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        receiverId = getIntent().getExtras().get("id").toString();
        receiverName = getIntent().getExtras().get("name").toString();
        receiverImage = getIntent().getExtras().get("image").toString();

        userPhoneList = new ArrayList<>();
        getContactsList();

        setupUI();

        updateUserStatus("online");
        displayLastSeen();

        rootRef.child("Messages").child(senderId).child(receiverId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);
                messageList.add(message);
                messagesAdapter.notifyDataSetChanged();

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


        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNotify = true;
                saveMessage();
                inputMessage.setText("");
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
                AlertDialog.Builder dialog = new AlertDialog.Builder(ChatActivity.this);
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

        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() == 0){
                    checkTypingStatus("noOne");
                } else{
                    checkTypingStatus(receiverId);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void setupUI(){

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        userName = findViewById(R.id.user_name);
        userLastSeen = findViewById(R.id.last_seen);
        userImage = findViewById(R.id.user_image);

        userName.setText(receiverName);
        if(receiverImage.equals("default_image") || receiverImage.isEmpty()){
            Picasso.get().load(R.drawable.icon_profile).into(userImage);
        } else{
            Picasso.get().load(receiverImage).into(userImage);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        inputMessage = findViewById(R.id.input_message);
        sendMessageButton = findViewById(R.id.send_button);
        sendFileButton = findViewById(R.id.send_file_button);

        messagesAdapter = new MessagesAdapter(messageList);
        recyclerView.setAdapter(messagesAdapter);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        Calendar calendarDate = Calendar.getInstance();
        SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
        currentDate = currentDateFormat.format(calendarDate.getTime());

        Calendar calendarTime = Calendar.getInstance();
        SimpleDateFormat currentTimeFormat = new SimpleDateFormat("HH:mm");
        currentTime = currentTimeFormat.format(calendarTime.getTime());

        loading = new ProgressDialog(ChatActivity.this);
    }

    private void checkTypingStatus(String typing){
        String currentTime, currentDate;
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat formatCurrentDate = new SimpleDateFormat("MMM dd, yyyy");
        currentDate = formatCurrentDate.format(calendar.getTime());

        SimpleDateFormat formatCurrentTime = new SimpleDateFormat("HH:mm");
        currentTime = formatCurrentTime.format(calendar.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", currentTime);
        onlineStateMap.put("date", currentDate);
        onlineStateMap.put("state", "online");
        onlineStateMap.put("typingTo", typing);

        rootRef.child("Users").child(senderId).child("user_state").updateChildren(onlineStateMap);

    }

    private void updateUserStatus(String state){
            String currentTime, currentDate;
            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat formatCurrentDate = new SimpleDateFormat("MMM dd, yyyy");
            currentDate = formatCurrentDate.format(calendar.getTime());

            SimpleDateFormat formatCurrentTime = new SimpleDateFormat("HH:mm");
            currentTime = formatCurrentTime.format(calendar.getTime());

            HashMap<String, Object> onlineStateMap = new HashMap<>();
            onlineStateMap.put("time", currentTime);
            onlineStateMap.put("date", currentDate);
            onlineStateMap.put("state", state);

            senderId = firebaseAuth.getCurrentUser().getUid();
            rootRef.child("Users").child(senderId).child("user_state").updateChildren(onlineStateMap);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus("online");
        displayLastSeen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkTypingStatus("noOne");
        updateUserStatus("offline");
        displayLastSeen();
    }

    @Override
    protected void onResume() {
        updateUserStatus("online");
        displayLastSeen();
        super.onResume();
    }

    private void getContactsList(){

        String ISOPrefix = Util.getCountryISO();
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()){
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            number = number.replace(" ", "");
            number = number.replace("-", "");
            number = number.replace("(", "");
            number = number.replace(")", "");

            if(!String.valueOf(number.charAt(0)).equals("+")){
                number = ISOPrefix + number;
            }

            User user = new User(name, number);
            userPhoneList.add(user);
        }
    }


    private void displayLastSeen(){

        Query userQuery = rootRef.child("Users").orderByChild("uid").equalTo(receiverId);
        userQuery.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Contacts contact = ds.getValue(Contacts.class);
                    if(contact.getUser_state() != null){
                        String state = contact.getUser_state().getState();
                        String date = contact.getUser_state().getDate();
                        String time = contact.getUser_state().getTime();
                        String typing = contact.getUser_state().getTypingTo();

                        if(typing.equals(senderId)){
                            userLastSeen.setText("typing...");
                        }
                        else {
                            if(state.equals("online")){
                                userLastSeen.setText("online");
                            }


                            else if(state.equals("offline")){
                                userLastSeen.setText("last Seen: " + date + ", " + time);
                            }
                            else{
                                userLastSeen.setText("offline");
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveMessage() {
        final String message = inputMessage.getText().toString();

        if(!message.isEmpty()){

            String messageSenderRef = "Messages/" + senderId + "/" + receiverId;
            String messageReceiverRef = "Messages/" + receiverId + "/" + senderId;

            DatabaseReference userMessageKeyRef = rootRef.child("Messages").child(senderId).child(receiverId).push();
            String messageKeyID = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", message);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", senderId);
            messageTextBody.put("to", receiverId);
            messageTextBody.put("messageID", messageKeyID);
            messageTextBody.put("date", currentDate);
            messageTextBody.put("time", currentTime);

            Map messageBodyDetail = new HashMap();
            messageBodyDetail.put(messageSenderRef + "/" + messageKeyID, messageTextBody);
            messageBodyDetail.put(messageReceiverRef + "/" + messageKeyID, messageTextBody);

            rootRef.updateChildren(messageBodyDetail).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                }
            });

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(senderId);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        Contacts contact = dataSnapshot.getValue(Contacts.class);
                        String name = Util.getUserName(userPhoneList, contact.getPhone());
                        contact.setName(name);

                        if(isNotify){
                            sendNotification(receiverId, contact.getName(), message);
                        }
                        isNotify= false;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    }


    private void sendNotification(final String receiverId, final String name, final String message) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String token = dataSnapshot.child("device_token").getValue().toString();
                    Data data = new Data(senderId, name + ": " + message, "New Message", receiverId, Util.MESSAGE_NOTIFICATION , R.mipmap.ic_launcher_round);

                    Sender sender = new Sender(data, token);
                    try {
                        JSONObject senderObject = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderObject,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {

                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAAQZIy4Tg:APA91bGRV9U9YNjz8CnS0WznHor-qhUO-2qutM68PWZa3UWs6i0wz6sAGsY6h6iMP_pz7bF3Qk7XF6Cd_td1LYjT1dayWvxE3_pGQ0kxL4IdI--Rk1t0eVqeLyfWdVzd4yrlptXrIAeM");
                                return headers;
                            }
                        };
                        requestQueue.add(jsonObjectRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                    if(!Util.permissionsCameraCheck(ChatActivity.this)){
                        Util.permissionsCameraRequest(ChatActivity.this);
                    }
                    else{
                        pickFromCamera();

                    }
                }
                else { //gallery
                    if(!Util.permissionsStorageCheck(ChatActivity.this)){
                        Util.permissionsStorageRequest(ChatActivity.this);
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
                final String messageSenderRef = "Messages/" + senderId + "/" + receiverId;
                final String messageReceiverRef = "Messages/" + receiverId + "/" + senderId;

                DatabaseReference userMessageKeyRef = rootRef.child("Messages").child(senderId).child(receiverId).push();

                final String messageKeyID = userMessageKeyRef.getKey();
                final StorageReference filePath = storageReference.child(messageKeyID + "." + checker);

                filePath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();

                                Map messageDocumentBody = new HashMap();
                                messageDocumentBody.put("message", downloadUrl);
                                messageDocumentBody.put("name", fileUri.getLastPathSegment());
                                messageDocumentBody.put("type", checker);
                                messageDocumentBody.put("from", senderId);
                                messageDocumentBody.put("to", receiverId);
                                messageDocumentBody.put("messageID", messageKeyID);
                                messageDocumentBody.put("date", currentDate);
                                messageDocumentBody.put("time", currentTime);

                                Map documentBodyDetail = new HashMap();
                                documentBodyDetail.put(messageSenderRef + "/" + messageKeyID, messageDocumentBody);
                                documentBodyDetail.put(messageReceiverRef + "/" + messageKeyID, messageDocumentBody);

                                rootRef.updateChildren(documentBodyDetail);
                                loading.dismiss();
                                inputMessage.setText("");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loading.dismiss();
                                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

        else if (resultCode == RESULT_OK && data != null && data.getData() != null && ((requestCode == Util.IMAGE_PICK_GALLERY_CODE) || (requestCode == Util.IMAGE_PICK_CAMERA_CODE))){
            loading.setTitle("Sending Image");
            loading.setMessage("Please wait, while sending the image...");
            loading.setCanceledOnTouchOutside(false);
            loading.show();

            if(requestCode == Util.IMAGE_PICK_GALLERY_CODE){
                imageUri = data.getData();
            }

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");
            final String messageSenderRef = "Messages/" + senderId + "/" + receiverId;
            final String messageReceiverRef = "Messages/" + receiverId + "/" + senderId;

            DatabaseReference userMessageKeyRef = rootRef.child("Messages").child(senderId).child(receiverId).push();

            final String messageKeyID = userMessageKeyRef.getKey();
            final StorageReference filePath = storageReference.child(messageKeyID + ".jpg");

            filePath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> taskUri = taskSnapshot.getStorage().getDownloadUrl();
                    while (!taskUri.isSuccessful());
                    Uri imageDownloadUri = taskUri.getResult();
                    if(taskUri.isSuccessful()){

                        Map messageImageBody = new HashMap();
                        messageImageBody.put("message", String.valueOf(imageDownloadUri));
                        messageImageBody.put("name", imageDownloadUri.getLastPathSegment());
                        messageImageBody.put("type", checker);
                        messageImageBody.put("from", senderId);
                        messageImageBody.put("to", receiverId);
                        messageImageBody.put("messageID", messageKeyID);
                        messageImageBody.put("date", currentDate);
                        messageImageBody.put("time", currentTime);

                        Map imageBodyDetail = new HashMap();
                        imageBodyDetail.put(messageSenderRef + "/" + messageKeyID, messageImageBody);
                        imageBodyDetail.put(messageReceiverRef + "/" + messageKeyID, messageImageBody);

                        rootRef.updateChildren(imageBodyDetail).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                loading.dismiss();
                                //sendNotification("image");

                                inputMessage.setText("");
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    loading.dismiss();
                    Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
