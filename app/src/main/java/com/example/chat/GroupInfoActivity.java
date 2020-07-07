package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chat.adapter.ParticipantsAdapter;
import com.example.chat.model.Contacts;
import com.example.chat.model.Group;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GroupInfoActivity extends AppCompatActivity {

    private String currentUserRole;
    private String currentUserId;
    private FirebaseAuth firebaseAuth;

    private String groupId, groupName;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ImageView groupImage;
    private TextView groupDescription;
    private TextView createdBy;

    private TextView editGroup;
    private TextView addParticipant;
    private TextView leaveGroupTv;
    private TextView numberOfParticipants;
    private ActionBar actionBar;

    private List<Contacts> participantsList;
    private List<User> userPhoneList;
    private ParticipantsAdapter participantsAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        groupId = getIntent().getStringExtra("group_id");
        groupName = getIntent().getStringExtra("group_name");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        userPhoneList = new ArrayList<>();
        getContactsList();

        setupUI();

        loadGroupInfo();
        loadCurrentUserRole();

        addParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupInfoActivity.this, GroupAddParticipantsActivity.class);
                intent.putExtra("group_id", groupId);
                intent.putExtra("group_name", groupName);
                startActivity(intent);
            }
        });

        leaveGroupTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dialogTitle = "";
                String dialogDescription = "";
                String positiveButton = "";
                if(currentUserRole.equals("creator")){
                    dialogTitle = "Delete Group";
                    dialogDescription = "Are you sure you want to Delete the group?";
                    positiveButton = "DELETE";

                } else{
                    dialogTitle = "Leave Group";
                    dialogDescription = "Are you sure you want to Leave the group?";
                    positiveButton = "LEAVE";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupInfoActivity.this);
                builder.setTitle(dialogTitle)
                        .setMessage(dialogDescription)
                        .setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(currentUserRole.equals("creator")){
                                    deleteGroup();
                                }
                                else{
                                    leaveGroup();
                                }

                            }
                        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

            }
        });

        editGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupInfoActivity.this, GroupEditActivity.class);
                intent.putExtra("group_id", groupId);
                startActivity(intent);
            }
        });

    }

    private void setupUI(){

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        recyclerView = findViewById(R.id.recyclerView);
        groupImage = findViewById(R.id.group_image);
        groupDescription = findViewById(R.id.group_description);
        createdBy = findViewById(R.id.createdBy);

        editGroup = findViewById(R.id.edit_group);
        addParticipant = findViewById(R.id.add_participant);
        leaveGroupTv = findViewById(R.id.leave_group);
        numberOfParticipants = findViewById(R.id.number_participants);
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

            String str = number;

            if(!String.valueOf(number.charAt(0)).equals("+")){
                str = number.substring(1);
                str = ISOPrefix + str;
            }

            User user = new User(name, str);
            userPhoneList.add(user);
        }
    }


    private void deleteGroup(){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(GroupInfoActivity.this, "Group deleted successfully..." , Toast.LENGTH_SHORT).show();
                startActivity(new Intent(GroupInfoActivity.this, MainActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void leaveGroup(){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Participants").child(currentUserId).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(GroupInfoActivity.this, "You left the group..." , Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(GroupInfoActivity.this, MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(GroupInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroupInfo(){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){
                    Group group = dataSnapshot.child("Group_Info").getValue(Group.class);

                    Calendar calendar = Calendar.getInstance();
                    String groupTime = group.getTime();
                    calendar.setTimeInMillis(Long.parseLong(groupTime));
                    String time = DateFormat.format("MMM dd, yyyy HH:mm", calendar).toString();

                    actionBar.setTitle(group.getTitle());
                    if(group.getDescription().isEmpty()){
                        groupDescription.setVisibility(View.GONE);
                    }
                    else{
                        groupDescription.setText(group.getDescription());
                    }
                    loadCreatorInfo(time, group.getCreatedBy());

                    try{
                        Picasso.get().load(group.getImage()).placeholder(R.mipmap.ic_launcher_round).into(groupImage);

                    } catch (Exception e){
                        Picasso.get().load(R.mipmap.ic_launcher_round).into(groupImage);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadCreatorInfo(final String time, final String creator){

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.child(creator).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){
                    String name = dataSnapshot.child("name").getValue().toString();
                    String uid = dataSnapshot.child("uid").getValue().toString();
                    if(uid.equals(currentUserId)){
                        createdBy.setText("Created by You on " + time);
                    }
                    else{
                        createdBy.setText("Created by " + name + " on " + time);

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void loadCurrentUserRole(){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        dbRef.child(groupId).child("Participants").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    currentUserRole = dataSnapshot.child("role").getValue().toString();
                    actionBar.setSubtitle("You " + "("+currentUserRole+")");

                    if(currentUserRole.equals("creator")){
                        editGroup.setVisibility(View.VISIBLE);
                        addParticipant.setVisibility(View.VISIBLE);
                        leaveGroupTv.setText("Delete Group");

                    }else if(currentUserRole.equals("admin")){
                        editGroup.setVisibility(View.GONE);
                        addParticipant.setVisibility(View.VISIBLE);

                    }else if(currentUserRole.equals("participant")){
                        editGroup.setVisibility(View.GONE);
                        addParticipant.setVisibility(View.GONE);
                    }
                }

                loadParticipants();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadParticipants(){
        participantsList = new ArrayList<>();
        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        DatabaseReference groupParticipants = FirebaseDatabase.getInstance().getReference().child("Groups");

        groupParticipants.child(groupId).child("Participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                participantsList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    final String userId = ds.child("uid").getValue().toString();

                    userRef.child(userId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {

                                Contacts contact = dataSnapshot.getValue(Contacts.class);
                                if(contact.getUid().equals(currentUserId)){
                                    contact.setName("You");
                                }
                                else{
                                    String name = Util.getUserName(userPhoneList, contact.getPhone());
                                    contact.setName(name);
                                }

                                participantsList.add(contact);
                            }

                            participantsAdapter = new ParticipantsAdapter(participantsList, GroupInfoActivity.this, currentUserRole, groupId);
                            participantsAdapter.notifyDataSetChanged();
                            recyclerView.setAdapter(participantsAdapter);
                            numberOfParticipants.setText("Participants (" + participantsList.size()+")");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }

                    });
                }
            }

            @Override
            public void onCancelled (@NonNull DatabaseError databaseError){

            }

        });

    }


}
