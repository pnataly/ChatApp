package com.example.chat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.model.Comment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>{

    private Context context;
    private List<Comment> commentList;
    private String currentUserId, currentPostId, postUserId;

    public CommentsAdapter(Context context, List<Comment> commentList, String currentUserId, String currentPostId, String postUserId) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = currentUserId;
        this.currentPostId = currentPostId;
        this.postUserId = postUserId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_comment, viewGroup, false);
        return new CommentViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        final Comment comment = commentList.get(position);

        Calendar calendar = Calendar.getInstance();
        String commentTime = comment.getTime();
        calendar.setTimeInMillis(Long.parseLong(commentTime));
        String time = DateFormat.format("MMM dd, yyyy HH:mm", calendar).toString();

        holder.userComment.setText(comment.getComment());
        holder.userName.setText(comment.getUser_name());
        holder.commentTime.setText(time);

        try {
            Picasso.get().load(comment.getUser_image()).placeholder(R.drawable.icon_profile).into(holder.userImage);
        }catch (Exception e){ }


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentUserId.equals(comment.getUser_id())){ //its my comment - can delete.
                    final AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure you want to delete this comment?");

                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteComment(comment.getComment_id());
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
                else{  //not my comment
                    Toast.makeText(context, "Can't delete other's comments...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteComment(String commentId) {
        final DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference().child("Posts")
                .child(postUserId).child(currentPostId);
        commentRef.child("Comments").child(commentId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                commentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String numOfComments = dataSnapshot.child("Post_Info").child("comments").getValue().toString();
                            int newComments = Integer.parseInt(numOfComments)-1;
                            commentRef.child("Post_Info").child("comments").setValue(String.valueOf(newComments));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }


    public class CommentViewHolder extends RecyclerView.ViewHolder{

        public CircleImageView userImage;
        private TextView userName, userComment, commentTime;

        public CommentViewHolder(@NonNull View view, Context ctx) {
            super(view);
            context = ctx;

            userImage = view.findViewById(R.id.user_image);
            userName = view.findViewById(R.id.user_name);
            userComment = view.findViewById(R.id.comment);
            commentTime = view.findViewById(R.id.time);
        }
    }

}
