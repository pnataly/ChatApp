package com.example.chat.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.GroupActivity;
import com.example.chat.R;
import com.example.chat.model.Group;
import com.example.chat.model.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupsViewHolder> {

    private List<Group> groupsList;
    private Context context;
    private String currentUserId;

    public GroupsAdapter(List<Group> groupsList, Context context, String currentUserId) {
        this.groupsList = groupsList;
        this.context = context;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public GroupsAdapter.GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.group_row, viewGroup, false);
        return new GroupsAdapter.GroupsViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupsAdapter.GroupsViewHolder holder, int position) {
        Group group = groupsList.get(position);

        holder.groupTitle.setText(group.getTitle());
        holder.lastMessage.setText("");

        //load last message and time
        loadLastMessage(group, holder);

        if(!group.getImage().isEmpty()){
            Picasso.get().load(group.getImage()).placeholder(R.mipmap.ic_launcher_round).into(holder.groupImage);
        }

    }

    private void loadLastMessage(Group group, final GroupsViewHolder holder){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(group.getTime()).child("Messages").limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       for(DataSnapshot ds : dataSnapshot.getChildren()){
                           final Message message = ds.getValue(Message.class);

                           DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
                           userRef.orderByChild("uid").equalTo(message.getFrom())
                                   .addValueEventListener(new ValueEventListener() {
                                       @Override
                                       public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                           for(DataSnapshot ds : dataSnapshot.getChildren()){
                                               String userName = ds.child("name").getValue().toString();
                                                String lastMessage = "";

                                               if(message.getFrom().equals(currentUserId)){
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
                                               holder.lastMessage.setText(lastMessage);
                                           }
                                       }

                                       @Override
                                       public void onCancelled(@NonNull DatabaseError databaseError) {

                                       }
                                   });
                       }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return groupsList.size();
    }

    public class GroupsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public TextView groupTitle;
        public TextView lastMessage;
        public CircleImageView groupImage;
        public LinearLayout groupLayout;


        public GroupsViewHolder(@NonNull View view, Context ctx) {
            super(view);
            context = ctx;
            groupTitle = view.findViewById(R.id.group_name);
            lastMessage = view.findViewById(R.id.last_message);
            groupImage = view.findViewById(R.id.group_image);
            groupLayout = view.findViewById(R.id.group);

            groupLayout.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            Group group = groupsList.get(position);
            if(v.getId() == R.id.group){
                Intent groupChatIntent = new Intent(context, GroupActivity.class);
                groupChatIntent.putExtra("group_id", group.getTime());

                groupChatIntent.putExtra("group_name", group.getTitle());
                groupChatIntent.putExtra("group_image", group.getImage());

                context.startActivity(groupChatIntent);
            }
        }
    }
}
