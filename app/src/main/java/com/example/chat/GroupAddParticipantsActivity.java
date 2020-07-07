package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;


import com.example.chat.adapter.ParticipantsAdapter;
import com.example.chat.model.Contacts;
import com.example.chat.model.User;
import com.example.chat.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupAddParticipantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ActionBar actionBar;

    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private String groupId, groupName;
    private String currentUserRole;

    private List<Contacts> contactsList;
    private List<User> userPhoneList;

    private ParticipantsAdapter participantsAdapter;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_add_participants);

        groupId = getIntent().getStringExtra("group_id");
        groupName = getIntent().getStringExtra("group_name");

        contactsList = new ArrayList<>();
        userPhoneList = new ArrayList<>();
        getContactsList();

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle("Add Participants");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantsAdapter = new ParticipantsAdapter(contactsList, GroupAddParticipantsActivity.this, currentUserRole, groupId);
        recyclerView.setAdapter(participantsAdapter);

        loadGroupInfo();
    }

    private void loadGroupInfo(){
        final DatabaseReference userRoleRef = FirebaseDatabase.getInstance().getReference().child("Groups");

        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");
        groupRef.child(groupId).child("Group_Info").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    actionBar.setTitle("Add Participants");

                    userRoleRef.child(groupId).child("Participants").child(currentUserId)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()){
                                        currentUserRole = dataSnapshot.child("role").getValue().toString();
                                        actionBar.setTitle(groupName + " ("+currentUserRole+")");

                                        getUserDetails();
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

    private void getUserDetails() {
        contactsList.clear();
        for(final User user: userPhoneList){
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
            Query query = userRef.orderByChild("phone").equalTo(user.getPhone());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        for(DataSnapshot ds: dataSnapshot.getChildren()){

                            Contacts contact = ds.getValue(Contacts.class);
                            contact.setName(user.getName());

                            if(!Util.containUser(contactsList, contact.getUid())){
                                contactsList.add(contact);
                                participantsAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

}
