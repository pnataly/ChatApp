package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chat.adapter.CommentsAdapter;
import com.example.chat.model.Comment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity {

    private String postId, currentUserId, currentUserName, curUserImage, postUserId, postUserName,  postLikes, image;

    public CircleImageView userPostImage, currentUserImage;
    public TextView userName;
    public TextView postTime, postTitle, postDescription, likes, comments;
    private ImageView postImage, sendCommentButton;
    public ImageButton moreButton;
    private TextView likeButton, shareButton;
    private LinearLayout profileLayout;
    private EditText inputComment;

    private ProgressDialog dialog;
    private boolean isComment = false;
    private boolean isLiked = false;

    private RecyclerView recyclerView;
    private List<Comment> commentList;
    private CommentsAdapter commentsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postId = getIntent().getStringExtra("post_id");
        postUserId = getIntent().getStringExtra("post_user_id");

        setupUI();

        loadPostInfo();

        loadCurrentUserInfo();

        setLikes();

        commentList = new ArrayList<>();
        loadComments();

        sendCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) postImage.getDrawable();
                if(bitmapDrawable == null){  //post without image
                    shareText(postTitle.getText().toString(), postDescription.getText().toString());
                }
                else{ //post with image
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImage(postTitle.getText().toString(), postDescription.getText().toString(), bitmap);
                }
            }
        });

        likes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent likeIntent = new Intent(PostDetailActivity.this, PostLikesActivity.class);
                likeIntent.putExtra("post_id", postId);
                likeIntent.putExtra("post_user_id", postUserId);
                startActivity(likeIntent);
            }
        });

    }


    private void setupUI(){
        userPostImage = findViewById(R.id.user_image);
        userName = findViewById(R.id.user_name);
        postTime = findViewById(R.id.post_time);
        postTitle = findViewById(R.id.post_title);
        postDescription = findViewById(R.id.post_description);
        postImage = findViewById(R.id.post_image);
        likes = findViewById(R.id.likes);
        comments = findViewById(R.id.comments);
        likeButton = findViewById(R.id.like_button);
        shareButton = findViewById(R.id.share_button);
        profileLayout = findViewById(R.id.profile_layout);
        moreButton = findViewById(R.id.more_button);

        currentUserImage = findViewById(R.id.profile_image);
        sendCommentButton = findViewById(R.id.send_button);
        inputComment = findViewById(R.id.input_comment);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        dialog = new ProgressDialog(this);
    }

    private void loadPostInfo(){
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        postRef.child(postUserId).child(postId).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    //get the info of the post
                    String title = dataSnapshot.child("Post_Info").child("post_title").getValue().toString();
                    String description = dataSnapshot.child("Post_Info").child("post_description").getValue().toString();
                    image = dataSnapshot.child("Post_Info").child("post_image").getValue().toString();
                    String time = dataSnapshot.child("Post_Info").child("time").getValue().toString();
                    postLikes = dataSnapshot.child("Post_Info").child("likes").getValue().toString();
                    postUserName = dataSnapshot.child("Post_Info").child("user_name").getValue().toString();
                    String userImage = dataSnapshot.child("Post_Info").child("user_image").getValue().toString();
                    String commentsCount = dataSnapshot.child("Post_Info").child("comments").getValue().toString() + " Comments";
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(Long.parseLong(time));
                    String timeFormatted = DateFormat.format("MMM dd, yyyy HH:mm", calendar).toString();

                    //set data to views
                    postTitle.setText(title);

                    if(description.isEmpty()){
                        postDescription.setText(description);
                    }
                    else{
                        postDescription.setVisibility(View.GONE);
                    }

                    String numberOfLikes = postLikes + " Likes";
                    likes.setText(numberOfLikes);
                    comments.setText(commentsCount);
                    userName.setText(postUserName);
                    postTime.setText(timeFormatted);

                    if(!image.isEmpty()){
                        Picasso.get().load(image).into(postImage);
                    }
                    else{
                        postImage.setVisibility(View.GONE);
                    }

                    try {
                        Picasso.get().load(userImage).placeholder(R.drawable.icon_profile).into(userPostImage);
                    }catch (Exception e){
                        Picasso.get().load(R.drawable.icon_profile).into(userPostImage);

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void loadCurrentUserInfo(){
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    currentUserName = dataSnapshot.child("name").getValue().toString();
                    curUserImage = dataSnapshot.child("image").getValue().toString();
                    if(!curUserImage.isEmpty()){
                        Picasso.get().load(curUserImage).into(currentUserImage);
                    }
                    else{
                        Picasso.get().load(R.drawable.icon_profile).into(currentUserImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment(){
        dialog.setMessage("Adding Comment...");
        String comment = inputComment.getText().toString().trim();

        if(comment.isEmpty()){
            return;
        }
        dialog.show();
        String time = String.valueOf(System.currentTimeMillis());

        DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                .child(postUserId).child(postId).child("Comments");

        HashMap<String, Object> bodyCommentMap = new HashMap<>();
        bodyCommentMap.put("comment_id", time);
        bodyCommentMap.put("time", time);
        bodyCommentMap.put("comment", comment);
        bodyCommentMap.put("user_id", currentUserId);
        bodyCommentMap.put("user_name", currentUserName);
        bodyCommentMap.put("user_image", curUserImage);

        commentRef.child(time).setValue(bodyCommentMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dialog.dismiss();
                Toast.makeText(PostDetailActivity.this, "Comment Added... " , Toast.LENGTH_SHORT).show();
                inputComment.setText("");
                updateCommentCount();

                addToHisNotifications(postUserId, postId, " Comment on your post");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(PostDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void updateCommentCount() {
        isComment = true;
        final DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference().child("Posts").child(postUserId);
        commentRef.child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if(isComment){
                        String numOfComments = dataSnapshot.child("Post_Info").child("comments").getValue().toString();
                        int newComments = Integer.parseInt(numOfComments) + 1;
                        commentRef.child(postId).child("Post_Info").child("comments").setValue(String.valueOf(newComments));
                        isComment = false;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void likePost() {

        isLiked = true;
        final DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                .child(postUserId).child(postId);
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(isLiked){

                    if(dataSnapshot.child("Likes").hasChild(currentUserId)){ //already liked, so remove like.
                        postRef.child(postUserId).child(postId).child("Post_Info").child("likes").setValue(Integer.parseInt(postLikes)-1);
                        likesRef.child("Likes").child(currentUserId).removeValue();  //remove from likes list for this post
                        isLiked = false;

                    }
                    else{ //not like - so like it
                        postRef.child(postUserId).child(postId).child("Post_Info").child("likes").setValue(Integer.parseInt(postLikes)+1);
                        isLiked = false;

                        likesRef.child("Likes").child(currentUserId).setValue("");
                        addToHisNotifications(postUserId, postId, " Liked your post");

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikes() {

        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                .child(postUserId).child(postId);
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild("Likes")){   //user liked this post = change color of the like button.
                    if(dataSnapshot.child("Likes").hasChild(currentUserId)){

                        likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_color, 0, 0, 0);
                        likeButton.setText("Liked");
                        likeButton.setTextColor(Color.parseColor("#2196F3"));
                    }
                    else {
                        likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
                        likeButton.setText("Like");
                        likeButton.setTextColor(Color.WHITE);
                    }

                }
                else {
                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
                    likeButton.setText("Like");
                    likeButton.setTextColor(Color.WHITE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions(){

        PopupMenu popupMenu = new PopupMenu(this, moreButton,  Gravity.END);

        if(postUserId.equals(currentUserId)){
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case 0:
                        AlertDialog.Builder builder = new AlertDialog.Builder(PostDetailActivity.this);
                        builder.setTitle("Delete")
                                .setMessage("Add you sure you want to delete this post?")
                                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deletePost();
                                    }
                                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                        return true;

                    case 1:
                        Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);
                        intent.putExtra("key", "edit");
                        intent.putExtra("post_id", postId);
                        startActivity(intent);
                        return true;

                    default:
                        return true;
                }
            }
        });
        popupMenu.show();
    }

    private void deletePost(){

        if(image.isEmpty()){
            deleteWithImage();
        }
        else{
            deleteWithoutImage();
        }
    }

    private void deleteWithImage(){
        dialog.setMessage("Deleting...");


        StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl(image);
        storage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts").child(postUserId);
                postRef.child(postId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                ds.getRef().removeValue();
                            }
                            dialog.dismiss();
                            Toast.makeText(PostDetailActivity.this, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(PostDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWithoutImage(){
        dialog.setMessage("Deleting...");

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts").child(postUserId);
        postRef.child(postId).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        ds.getRef().removeValue();
                    }
                    dialog.dismiss();
                    Toast.makeText(PostDetailActivity.this, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadComments(){
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        postRef.child(postUserId).child(postId).child("Comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Comment comment = ds.getValue(Comment.class);

                    commentList.add(comment);
                    commentsAdapter = new CommentsAdapter(PostDetailActivity.this, commentList, currentUserId, postId, postUserId);
                    recyclerView.setAdapter(commentsAdapter);
                    recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void shareImage(String title, String description, Bitmap bitmap) {
        String shareBody = title + "\n" + description;

        Uri uri = saveImageToShare(bitmap);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        intent.setType("images/png");
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try{
            imageFolder.mkdir();
            File file = new File(imageFolder, "shred_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(PostDetailActivity.this, "com.example.chat.fileprovider", file);

        }catch (Exception e){
            Toast.makeText(PostDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void shareText(String title, String description) {
        String shareBody = title + "\n" + description;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private void addToHisNotifications(String hisUid, String postId, String notification){
        String time = String.valueOf(System.currentTimeMillis());

        HashMap<Object, String > notificationMap = new HashMap<>();
        notificationMap.put("postId", postId);
        notificationMap.put("time", time);
        notificationMap.put("postUserId", hisUid);
        notificationMap.put("senderId", currentUserId);
        notificationMap.put("notification", notification);

        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        notRef.child(hisUid).child(time).setValue(notificationMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PostDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
