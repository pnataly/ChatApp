package com.example.chat.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.model.Contacts;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder>{

    private List<Contacts> contactsList;
    private Context context;
    private String currentUserRole, groupId;

    public ParticipantsAdapter(List<Contacts> contactsList, Context context, String currentUserRole, String groupId) {
        this.contactsList = contactsList;
        this.context = context;
        this.currentUserRole = currentUserRole;
        this.groupId = groupId;
    }

    @NonNull
    @Override
    public ParticipantsAdapter.ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.friends_row, viewGroup, false);
        return new ParticipantsAdapter.ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantsAdapter.ParticipantViewHolder holder, int position) {
        final Contacts contact = contactsList.get(position);
        holder.userName.setText(contact.getName());

        if(contact.getImage().isEmpty()){
            Picasso.get().load(R.drawable.icon_profile).into(holder.userImage);
        }
        else{
            Picasso.get().load(contact.getImage()).into(holder.userImage);
        }

        checkIfAlreadyExists(contact, holder);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
                dbRef.child(groupId).child("Participants").child(contact.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){      //user exists - is participants
                                    String contactPrevRole = dataSnapshot.child("role").getValue().toString();
                                    String[] options;

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Choose Option");
                                    if(currentUserRole.equals("creator")){  //im creator his admin
                                        if(contactPrevRole.equals("admin")){
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int position) {
                                                    if(position == 0){ //remove admin
                                                        removeAdmin(contact);
                                                    }
                                                    else { //remove user
                                                        removeParticipant(contact);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if(contactPrevRole.equals("participant")){ //im creator his participant
                                            options = new String[]{"Make Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int position) {
                                                    if(position == 0){ //remove admin
                                                        makeAdmin(contact);
                                                    }
                                                    else { //remove user
                                                        removeParticipant(contact);
                                                    }
                                                }
                                            }).show();
                                        }

                                    }
                                    else if(currentUserRole.equals("admin")){
                                        if(contactPrevRole.equals("creator")){ //im admin his creator
                                            Toast.makeText(context, "Creator of Group...", Toast.LENGTH_SHORT).show();
                                        }
                                        else if(contactPrevRole.equals("admin")) { //im admin his admin
                                            options = new String[]{"Remove Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int position) {
                                                    if(position == 0){ //remove admin
                                                        removeAdmin(contact);
                                                    }
                                                    else { //remove user
                                                        removeParticipant(contact);
                                                    }
                                                }
                                            }).show();
                                        }
                                        else if(contactPrevRole.equals("participant")){ //im admin his participant
                                            options = new String[]{"Make Admin", "Remove User"};
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int position) {
                                                    if(position == 0){ //remove admin
                                                        makeAdmin(contact);
                                                    }
                                                    else { //remove user
                                                        removeParticipant(contact);
                                                    }
                                                }
                                            }).show();
                                        }
                                    }
                                }
                                else{ //user not exists - not participants - add to group participants
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setTitle("Add Participants")
                                            .setMessage("Add this contact to this group?")
                                            .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    addParticipants(contact);
                                                }
                                            }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    private void addParticipants(Contacts contact) {
        String time = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> participantsBodyMap = new HashMap<>();
        participantsBodyMap.put("uid", contact.getUid());
        participantsBodyMap.put("role", "participant");
        participantsBodyMap.put("time", time);
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Participants").child(contact.getUid()).setValue(participantsBodyMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Added successfully to the group...", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeAdmin(Contacts contact) {
        HashMap<String, Object> participantsBodyMap = new HashMap<>();
        participantsBodyMap.put("role", "admin");

        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Participants").child(contact.getUid()).updateChildren(participantsBodyMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "The user is now admin...", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeParticipant(Contacts contact) {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Participants").child(contact.getUid()).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
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

    private void removeAdmin(Contacts contact) {
        HashMap<String, Object> participantsBodyMap = new HashMap<>();
        participantsBodyMap.put("role", "participant");

        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Participants").child(contact.getUid()).updateChildren(participantsBodyMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "The user is no longer admin...", Toast.LENGTH_SHORT).show();

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfAlreadyExists(Contacts contact, final ParticipantsAdapter.ParticipantViewHolder holder){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        dbRef.child(groupId).child("Participants").child(contact.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //the contact exists in the group
                        if(dataSnapshot.exists()){
                            String contactRole = ""+dataSnapshot.child("role").getValue();
                            holder.userStatus.setText(contactRole);
                        }else{
                            holder.userStatus.setText("");

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

    public static class ParticipantViewHolder extends RecyclerView.ViewHolder {

        public TextView userName;
        public TextView userStatus;
        public CircleImageView userImage;


        public ParticipantViewHolder(@NonNull View view) {
            super(view);

            userName = view.findViewById(R.id.username);
            userStatus = view.findViewById(R.id.status);
            userImage = view.findViewById(R.id.user_image);
        }

    }
}
