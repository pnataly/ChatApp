package com.example.chat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.ChatActivity;
import com.example.chat.R;
import com.example.chat.UserProfileActivity;
import com.example.chat.model.Contacts;
import com.example.chat.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactsViewHolder> {

    private List<Contacts> contactsList;
    private Context context;
    private String activity;
    private FirebaseAuth firebaseAuth;

    public ContactAdapter(List<Contacts> contactsList, Context context, String activity) {
        this.context = context;
        this.contactsList = contactsList;
        this.activity = activity;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.friends_row, viewGroup, false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsViewHolder holder, int position) {
        final Contacts contact = contactsList.get(position);
        holder.userName.setText(contact.getName());

        if(contact.getImage() != null && !contact.getImage().isEmpty()){
            Picasso.get().load(contact.getImage()).into(holder.userImage);
        }
        else{
            Picasso.get().load(R.drawable.icon_profile).into(holder.userImage);
        }

        if(contact.getUser_state() != null){
            if(contact.getUser_state().getState().equals("online")){
                holder.onlineIcon.setVisibility(View.VISIBLE);
            }
            else if(contact.getUser_state().getState().equals("offline")){
                holder.onlineIcon.setVisibility(View.INVISIBLE);
            }
        }

        if(activity.equals("contacts")){
            holder.userStatus.setText(contact.getStatus());

            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {

                            if(position == 0){
                                Intent intent = new Intent(context, UserProfileActivity.class);
                                intent.putExtra("user_id", contact.getUid());
                                intent.putExtra("name", contact.getName());
                                context.startActivity(intent);
                            }

                            if(position == 1){
                                Intent intent = new Intent(context, ChatActivity.class);
                                intent.putExtra("id", contact.getUid());
                                intent.putExtra("name", contact.getName());
                                if(!contact.getImage().isEmpty()){
                                    intent.putExtra("image", contact.getImage());
                                }
                                else{
                                    intent.putExtra("image", "default_image");
                                }
                                context.startActivity(intent);
                            }
                        }
                    });
                    dialog.create().show();
                }
            });

        }
        else if(activity.equals("chat")){
            loadLastMessage(contact, holder);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent chatIntent = new Intent(context, ChatActivity.class);
                    chatIntent.putExtra("name", contact.getName());
                    chatIntent.putExtra("id", contact.getUid());
                    if(contact.getImage() != null){
                        chatIntent.putExtra("image", contact.getImage());
                    }
                    else {
                        chatIntent.putExtra("image", "default_image");
                    }
                    context.startActivity(chatIntent);
                }
            });
        }
        else{
            if(contact.getUser_state() != null){
                String state =  contact.getUser_state().getState();
                String date =  contact.getUser_state().getDate();
                String time =  contact.getUser_state().getTime();

                if(state.equals("online")){
                    holder.userStatus.setText("online");

                }
                else if(state.equals("offline")){
                    holder.userStatus.setText("last Seen: " + date + ", " + time);
                }
            }
            else{
                holder.userStatus.setText("offline");
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent chatIntent = new Intent(context, ChatActivity.class);
                    chatIntent.putExtra("name", contact.getName());
                    chatIntent.putExtra("id", contact.getUid());
                    if(contact.getImage() != null){
                        chatIntent.putExtra("image", contact.getImage());
                    }
                    else {
                        chatIntent.putExtra("image", "default_image");
                    }
                    context.startActivity(chatIntent);
                }
            });

        }
    }

    private void loadLastMessage(final Contacts contacts, final ContactsViewHolder holder){

        DatabaseReference mesRef = FirebaseDatabase.getInstance().getReference().child("Messages");
        mesRef.child(firebaseAuth.getUid()).child(contacts.getUid()).limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                final Message message = ds.getValue(Message.class);
                                String userName = contacts.getName();
                                String lastMessage = "";

                                if(message.getFrom().equals(firebaseAuth.getUid())){
                                    if(message.getType().equals("text")){
                                        lastMessage = message.getMessage();
                                    }
                                    else if(message.getType().equals("image")){
                                        lastMessage= "You send image";
                                    }
                                    else{
                                        lastMessage= "You send file";
                                    }


                                }else{
                                    if(message.getType().equals("text")){
                                        lastMessage = userName +": " + message.getMessage();
                                    }
                                    else if(message.getType().equals("image")){
                                        lastMessage= userName +": send image";
                                    }
                                    else{
                                        lastMessage= userName +": send file";
                                    }
                                }
                                holder.userStatus.setText(lastMessage);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }

    public static class ContactsViewHolder extends RecyclerView.ViewHolder {

        public TextView userName;
        public TextView userStatus;
        public CircleImageView userImage;
        public ImageView onlineIcon;
        public RelativeLayout layout;

        public ContactsViewHolder(@NonNull View view) {
            super(view);

            userName = view.findViewById(R.id.username);
            userStatus = view.findViewById(R.id.status);
            userImage = view.findViewById(R.id.user_image);
            onlineIcon = view.findViewById(R.id.icon);
            layout = view.findViewById(R.id.layout);
        }

    }
}
