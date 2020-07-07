package com.example.chat.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.AddPostActivity;
import com.example.chat.ChatActivity;
import com.example.chat.PostDetailActivity;
import com.example.chat.PostLikesActivity;
import com.example.chat.R;
import com.example.chat.UserProfileActivity;
import com.example.chat.model.Post;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private String activity;
    private Boolean isLiked = false;
    private String currentUserId;

    private DatabaseReference postRef;

    public PostAdapter(Context context, List<Post> postList, String activity) {
        this.context = context;
        this.postList = postList;
        this.activity = activity;

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.post_row, viewGroup, false);
        return new PostViewHolder(view, context);

    }

    @Override
    public void onBindViewHolder(@NonNull final PostViewHolder holder, final int position) {

        final Post post = postList.get(position);

        Calendar calendar = Calendar.getInstance();
        String postTime = post.getTime();
        calendar.setTimeInMillis(Long.parseLong(postTime));
        String time = DateFormat.format("MMM dd, yyyy HH:mm", calendar).toString();

        holder.userName.setText(post.getUser_name());
        holder.postTime.setText(time);
        holder.postTitle.setText(post.getPost_title());
        holder.likes.setText(post.getLikes() + " Likes");
        holder.comments.setText(post.getComments() + " Comments");

        setLikes(holder, post);

        if(!post.getPost_description().isEmpty()){
            holder.postDescription.setText(post.getPost_description());
        }
        else{
            holder.postDescription.setVisibility(View.GONE);
        }

        try {
            Picasso.get().load(post.getUser_image()).placeholder(R.drawable.icon_profile).into(holder.userImage);
        }catch (Exception e){

        }
        if(!post.getPost_image().isEmpty()){
            Picasso.get().load(post.getPost_image()).into(holder.postImage);
        }
        else{
            holder.postImage.setVisibility(View.GONE);
        }

        if(activity.equals("MyProfile")){
            holder.moreButton.setVisibility(View.VISIBLE);
            holder.userName.setVisibility(View.GONE);
        }
        else if(activity.equals("userProfile")){
            holder.userName.setVisibility(View.GONE);
        }

        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final int numOfLikes = Integer.parseInt(postList.get(position).getLikes());
                isLiked = true;

                final String postId = postList.get(position).getTime();
                final String userPostId = postList.get(position).getUser_id();

                final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                        .child(userPostId).child(postId);
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(isLiked){

                            if(dataSnapshot.child("Likes").hasChild(currentUserId)){ //already liked, so remove like.
                                postRef.child(userPostId).child(postId).child("Post_Info").child("likes").setValue(String.valueOf(numOfLikes-1));
                                likesRef.child("Likes").child(currentUserId).removeValue();  //remove from likes list for this post
                                isLiked = false;
                            }
                            else{ //not like - so like it
                                postRef.child(userPostId).child(postId).child("Post_Info").child("likes").setValue(String.valueOf(numOfLikes+1));
                                isLiked = false;
                                likesRef.child("Likes").child(currentUserId).setValue("");
                                addToHisNotifications(userPostId, postId, " Liked your post");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.postImage.getDrawable();
                if(bitmapDrawable == null){  //post without image
                    shareText(post.getPost_title(), post.getPost_description());
                }
                else{ //post with image
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImage(post.getPost_title(), post.getPost_description(), bitmap);
                }
            }
        });
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
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLikes(final PostViewHolder holder, Post post) {

        String userPostId =post.getUser_id();
        String postId = post.getTime();

        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                .child(userPostId).child(postId);
        likesRef.child("Likes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild(currentUserId)){   //user liked this post = change color of the like button.
                    holder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_color, 0, 0, 0);
                    holder.likeButton.setText("Liked");
                    holder.likeButton.setTextColor(Color.parseColor("#2196F3"));
                }
                else {
                    holder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);
                    holder.likeButton.setText("Like");
                    holder.likeButton.setTextColor(Color.WHITE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deletePost(Post post){

        if(!post.getPost_image().isEmpty()){
            deleteWithImage(post);
        }
        else{
            deleteWithoutImage(post);
        }
    }

    private void deleteWithImage(final Post post){
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Deleting...");


        StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl(post.getPost_image());
        storage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                Query query = FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getUser_id())
                        .orderByChild("time").equalTo(post.getTime());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds : dataSnapshot.getChildren()){
                            ds.getRef().removeValue();
                        }
                        dialog.dismiss();
                        Toast.makeText(context, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWithoutImage(Post post){
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Deleting...");

        Query query = FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getUser_id())
                .orderByChild("time").equalTo(post.getTime());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    ds.getRef().removeValue();
                }
                dialog.dismiss();
                Toast.makeText(context, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();
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
        context.startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try{
            imageFolder.mkdir();
            File file = new File(imageFolder, "shred_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "com.example.chat.fileprovider", file);

        }catch (Exception e){
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void shareText(String title, String description) {
        String shareBody = title + "\n" + description;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        context.startActivity(Intent.createChooser(intent, "Share Via"));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public CircleImageView userImage;
        public TextView userName;
        public TextView postTime, postTitle, postDescription, likes, comments;
        private ImageView postImage;
        public ImageButton moreButton;
        private TextView likeButton, commentButton, shareButton;
        private LinearLayout profileLayout;


        public PostViewHolder(@NonNull View view, Context ctx) {
            super(view);
            context = ctx;

            userImage = view.findViewById(R.id.user_image);
            userName = view.findViewById(R.id.user_name);
            postTime = view.findViewById(R.id.post_time);
            postTitle = view.findViewById(R.id.post_title);
            postDescription = view.findViewById(R.id.post_description);
            postImage = view.findViewById(R.id.post_image);
            likes = view.findViewById(R.id.likes);
            comments = view.findViewById(R.id.comments);
            moreButton = view.findViewById(R.id.more_button);
            likeButton = view.findViewById(R.id.like_button);
            commentButton = view.findViewById(R.id.comment_button);
            shareButton = view.findViewById(R.id.share_button);
            profileLayout = view.findViewById(R.id.profile_layout);

            moreButton.setOnClickListener(this);
            likes.setOnClickListener(this);
            commentButton.setOnClickListener(this);
            shareButton.setOnClickListener(this);
            profileLayout.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            final Post post = postList.get(position);
            switch (v.getId()){
                case R.id.more_button:

                    PopupMenu popupMenu = new PopupMenu(context, moreButton,  Gravity.END);
                    if(activity.equals("MyProfile")){
                        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
                        popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
                    }

                    popupMenu.getMenu().add(Menu.NONE, 2, 0, "View Details");

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case 0:
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Delete")
                                            .setMessage("Add you sure you want to delete this post?")
                                            .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    deletePost(post);
                                                    postList.remove(position);
                                                    notifyItemRemoved(position);
                                                }
                                            }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                                    return true;

                                case 1:
                                    Intent intent = new Intent(context, AddPostActivity.class);
                                    intent.putExtra("key", "edit");
                                    intent.putExtra("post_id", post.getTime());
                                    context.startActivity(intent);
                                    return true;

                                case 2:
                                    Intent detailIntent = new Intent(context, PostDetailActivity.class);
                                    detailIntent.putExtra("post_id", post.getTime());
                                    detailIntent.putExtra("post_user_id", post.getUser_id());
                                    context.startActivity(detailIntent);
                                    return true;

                                default:
                                    return true;
                            }
                        }
                    });
                    popupMenu.show();
                    break;

                case R.id.likes:
                    Intent likeIntent = new Intent(context, PostLikesActivity.class);
                    likeIntent.putExtra("post_id", post.getTime());
                    likeIntent.putExtra("post_user_id", post.getUser_id());
                    context.startActivity(likeIntent);
                    break;

                case R.id.comment_button:
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("post_id", post.getTime());
                    intent.putExtra("post_user_id", post.getUser_id());
                    context.startActivity(intent);
                    break;

                case R.id.profile_layout:
                    if(!activity.equals("MyProfile")){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                        dialog.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int position) {

                                if(position == 0){
                                    Intent intent = new Intent(context, UserProfileActivity.class);
                                    intent.putExtra("user_id", post.getUser_id());
                                    intent.putExtra("name", post.getUser_name());
                                    context.startActivity(intent);
                                }

                                if(position == 1){
                                    Intent intent = new Intent(context, ChatActivity.class);
                                    intent.putExtra("id", post.getUser_id());
                                    intent.putExtra("name", post.getUser_name());
                                    if(!post.getPost_image().isEmpty()){
                                        intent.putExtra("image", post.getUser_image());
                                    }
                                    else{
                                        intent.putExtra("image", "");
                                    }
                                    context.startActivity(intent);
                                }
                            }
                        });
                        dialog.create().show();
                    }
                    break;
            }
        }
    }

}
