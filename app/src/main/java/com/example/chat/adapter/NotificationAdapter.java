package com.example.chat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.PostDetailActivity;
import com.example.chat.R;
import com.example.chat.model.Notification;
import com.example.chat.model.User;
import com.example.chat.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;
    private List<User> userPhoneList;
    private FirebaseAuth firebaseAuth;

    public NotificationAdapter(Context context, List<Notification> notificationList, List<User> userPhoneList) {

        this.context = context;
        this.notificationList = notificationList;

        this.userPhoneList = userPhoneList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_notification, viewGroup, false);
        return new NotificationViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull final NotificationViewHolder holder, final int position) {

        final Notification notification = notificationList.get(position);

        Calendar calendar = Calendar.getInstance();
        final String time = notification.getTime();
        calendar.setTimeInMillis(Long.parseLong(time));
        final String timeFormatted = DateFormat.format("MMM dd, yyyy HH:mm", calendar).toString();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.orderByChild("uid").equalTo(notification.getSenderId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String phone = ds.child("phone").getValue().toString();
                    String image = ds.child("image").getValue().toString();

                    notification.setSenderImage(image);
                    String name = Util.getUserName(userPhoneList, phone);
                    notification.setSenderName(name);
                    holder.userName.setText(name);

                    if(image.isEmpty()){
                        Picasso.get().load(R.drawable.icon_profile).into(holder.userImage);
                    }
                    else{
                        Picasso.get().load(image).into(holder.userImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.notification.setText(notification.getNotification());
        holder.time.setText(timeFormatted);

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent detailIntent = new Intent(context, PostDetailActivity.class);
                detailIntent.putExtra("post_id", notification.getPostId());
                detailIntent.putExtra("post_user_id", notification.getPostUserId());
                context.startActivity(detailIntent);
            }
        });

        holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this notification?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseReference notRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
                        notRef.child(firebaseAuth.getUid()).child(notification.getTime()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, "Notification deleted successfully... ", Toast.LENGTH_SHORT).show();
                                notificationList.remove(position);
                                notifyItemRemoved(position);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return false;
            }
        });

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }


    public class NotificationViewHolder extends RecyclerView.ViewHolder{

        public TextView userName, notification, time;
        public CircleImageView userImage;
        public LinearLayout layout;


        public NotificationViewHolder(@NonNull View view, Context ctx) {
            super(view);
            context = ctx;

            userName = view.findViewById(R.id.user_name);
            notification = view.findViewById(R.id.notification);
            userImage = view.findViewById(R.id.user_image);
            time = view.findViewById(R.id.time);
            layout = view.findViewById(R.id.layout);

        }
    }
}
