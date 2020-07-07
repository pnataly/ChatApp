package com.example.chat.adapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.ImageViewerActivity;
import com.example.chat.MainActivity;
import com.example.chat.R;
import com.example.chat.model.Message;
import com.example.chat.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.GroupMessageViewHolder> {

        private List<Message> messagesList;
        private FirebaseAuth firebaseAuth;
        private DatabaseReference usersRef;
        private String groupId, currentUserId, currentUserRole;

        public GroupMessageAdapter(List<Message> messagesList, String groupId, String currentUserId){
            this.messagesList = messagesList;
            this.groupId = groupId;
            this.currentUserId = currentUserId;
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
            dbRef.child(groupId).child("Participants").child(currentUserId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                currentUserRole = dataSnapshot.child("role").getValue().toString();
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }

        @NonNull
        @Override
        public GroupMessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.custom_message_layout, viewGroup, false);
            firebaseAuth = FirebaseAuth.getInstance();
            return new GroupMessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final GroupMessageViewHolder holder, final int position) {
            String senderID = firebaseAuth.getCurrentUser().getUid();


            final Message message = messagesList.get(position);
            String fromUserId = message.getFrom();
            String messageType = message.getType();

            usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);
            usersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild("image")){
                        String image = dataSnapshot.child("image").getValue().toString();
                        if(image.isEmpty()){
                            Picasso.get().load(R.drawable.icon_profile).into(holder.receiverImage);
                        }
                        else{
                            Picasso.get().load(image).into(holder.receiverImage);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            holder.receiverMessage.setVisibility(View.GONE);
            holder.receiverImage.setVisibility(View.GONE);
            holder.senderMessage.setVisibility(View.GONE);
            holder.imageMessageReceiver.setVisibility(View.GONE);
            holder.imageMessageSender.setVisibility(View.GONE);
            holder.timeReceiver.setVisibility(View.GONE);
            holder.timeSender.setVisibility(View.GONE);
            holder.receiverName.setVisibility(View.GONE);

            if(messageType.equals("text")){

                if(fromUserId.equals(senderID)){
                    holder.senderMessage.setVisibility(View.VISIBLE);
                    holder.timeSender.setVisibility(View.VISIBLE);
                    holder.senderMessage.setText(message.getMessage());
                    holder.timeSender.setText(message.getDate() +", " + message.getTime());
                }

                else {
                    holder.timeReceiver.setVisibility(View.VISIBLE);
                    holder.receiverImage.setVisibility(View.VISIBLE);
                    holder.receiverMessage.setVisibility(View.VISIBLE);
                    holder.receiverName.setVisibility(View.VISIBLE);
                    Picasso.get().load(message.getMessage()).into(holder.imageMessageReceiver);
                    holder.receiverName.setText(message.getName() + ":");
                    holder.receiverMessage.setText(message.getMessage());
                    holder.timeReceiver.setText(message.getDate() +", " + message.getTime());
                }
            }
            else if(messageType.equals("image")){
                if(fromUserId.equals(senderID)){
                    holder.imageMessageSender.setVisibility(View.VISIBLE);
                    holder.timeSender.setVisibility(View.VISIBLE);
                    Picasso.get().load(message.getMessage()).into(holder.imageMessageSender);
                    holder.timeSender.setText(message.getDate() +", " + message.getTime());
                }
                else{
                    holder.receiverImage.setVisibility(View.VISIBLE);
                    holder.timeReceiver.setVisibility(View.VISIBLE);
                    holder.receiverName.setVisibility(View.VISIBLE);
                    holder.imageMessageReceiver.setVisibility(View.VISIBLE);
                    Picasso.get().load(message.getMessage()).into(holder.imageMessageReceiver);
                    holder.timeReceiver.setText(message.getDate() +", " + message.getTime());
                    usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(message.getFrom());
                    usersRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                String userName = dataSnapshot.child("name").getValue().toString();
                                holder.receiverName.setText(userName + ":");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
            else{

                if(fromUserId.equals(senderID)){

                    holder.imageMessageSender.setVisibility(View.VISIBLE);
                    holder.timeSender.setVisibility(View.VISIBLE);
                    holder.timeSender.setText(message.getDate() +", " + message.getTime());
                    Picasso.get().load(Util.FILE_PATH).into(holder.imageMessageSender);
                }
                else if(messageType.equals("pdf") || messageType.equals("docx")){
                    holder.receiverImage.setVisibility(View.VISIBLE);
                    holder.imageMessageReceiver.setVisibility(View.VISIBLE);
                    holder.timeReceiver.setVisibility(View.VISIBLE);
                    holder.receiverName.setVisibility(View.VISIBLE);
                    holder.timeReceiver.setText(message.getDate() +", " + message.getTime());
                    Picasso.get().load(Util.FILE_PATH).into(holder.imageMessageReceiver);
                    usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(message.getFrom());
                    usersRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                String userName = dataSnapshot.child("name").getValue().toString();
                                holder.receiverName.setText(userName + ":");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            if(currentUserRole.equals("creator") || currentUserRole.equals("admin")){
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(messagesList.get(position).getType().equals("pdf") || messagesList.get(position).getType().equals("docx")){
                            CharSequence options[] = new CharSequence[]{
                                    "Delete message",
                                    "Download and View The Document",
                                    "Cancel"
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                            builder.setTitle("Delete Message from the group?");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {

                                    switch (i){
                                        case 0:
                                            deleteMessage(position, holder);
                                            Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                            holder.itemView.getContext().startActivity(intent);
                                            break;

                                        case 1:
                                            Intent docIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMessage()));
                                            holder.itemView.getContext().startActivity(docIntent);
                                            break;

                                        case 2:
                                            break;
                                    }
                                }
                            });
                            builder.show();
                        }

                        else if(messagesList.get(position).getType().equals("text")){
                            CharSequence options[] = new CharSequence[]{
                                    "Delete message",
                                    "Cancel"
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                            builder.setTitle("Delete Message from the group?");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {

                                    switch (i){
                                        case 0:
                                            deleteMessage(position, holder);
                                            Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                            holder.itemView.getContext().startActivity(intent);
                                            break;

                                        case 1:
                                            break;
                                    }
                                }
                            });
                            builder.show();
                        }

                        else if(messagesList.get(position).getType().equals("image")){
                            CharSequence options[] = new CharSequence[]{
                                    "Delete message",
                                    "View Image",
                                    "Cancel"
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                            builder.setTitle("Delete Message from the group?");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {

                                    switch (i){
                                        case 0:
                                            deleteMessage(position, holder);
                                            Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                            holder.itemView.getContext().startActivity(intent);
                                            break;

                                        case 1:
                                            Intent imageIntent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                            imageIntent.putExtra("url", message.getMessage());
                                            holder.itemView.getContext().startActivity(imageIntent);
                                            break;

                                        case 2:
                                            break;
                                    }
                                }
                            });
                            builder.show();
                        }
                    }
                });
            } else if(currentUserRole.equals("participant")){
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(messagesList.get(position).getType().equals("pdf") || messagesList.get(position).getType().equals("docx")){
                            CharSequence options[] = new CharSequence[]{
                                    "Download and View The Document",
                                    "Cancel"
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                            builder.setTitle("View Document?");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {

                                    switch (i){
                                        case 0:
                                            Intent docIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMessage()));
                                            holder.itemView.getContext().startActivity(docIntent);
                                            break;

                                        case 1:
                                            break;
                                    }
                                }
                            });
                            builder.show();
                        }


                        else if(messagesList.get(position).getType().equals("image")){
                            CharSequence options[] = new CharSequence[]{
                                    "View Image",
                                    "Cancel"
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                            builder.setTitle("View Image?");
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {

                                    switch (i){

                                        case 0:
                                            Intent imageIntent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                            imageIntent.putExtra("url", message.getMessage());
                                            holder.itemView.getContext().startActivity(imageIntent);
                                            break;

                                        case 1:
                                            break;
                                    }
                                }
                            });
                            builder.show();
                        }
                        }
                    });
                }

        }

        @Override
        public int getItemCount() {
            return messagesList.size();
        }

    private void deleteMessage(final int position, final GroupMessageViewHolder holder){
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Groups").child(groupId).child("Messages")
                .child(messagesList.get(position).getMessageId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(), "Message Deleted Successfully", Toast.LENGTH_SHORT).show();
                }
                else{
                    String message = task.getException().toString();
                    Toast.makeText(holder.itemView.getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();

                }
            }
        });
    }



    public class GroupMessageViewHolder extends RecyclerView.ViewHolder{

        public TextView senderMessage;
        public TextView receiverMessage;
        public CircleImageView receiverImage;
        public ImageView imageMessageSender, imageMessageReceiver;
        public TextView timeSender, timeReceiver;
        private TextView receiverName;

        public GroupMessageViewHolder(@NonNull View view) {
            super(view);

            senderMessage = view.findViewById(R.id.sender_message_text);
            receiverMessage = view.findViewById(R.id.receiver_message_text);
            receiverImage = view.findViewById(R.id.image);
            imageMessageSender = view.findViewById(R.id.image_sender);
            imageMessageReceiver = view.findViewById(R.id.image_receiver);
            timeSender = view.findViewById(R.id.sender_time);
            timeReceiver = view.findViewById(R.id.receiver_time);
            receiverName = view.findViewById(R.id.receiver_name);
        }
    }
}


