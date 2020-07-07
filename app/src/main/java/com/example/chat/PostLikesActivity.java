package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.example.chat.adapter.ContactAdapter;
import com.example.chat.model.Contacts;
import com.example.chat.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostLikesActivity extends AppCompatActivity {

    private String postId, postUserId;
    private RecyclerView recyclerView;
    private Toolbar toolbar;

    private List<Contacts> likedList;
    private ContactAdapter contactAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_likes);

        postId = getIntent().getStringExtra("post_id");
        postUserId = getIntent().getStringExtra("post_user_id");

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Post Liked By...");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        loadPostLikes();
    }

    private void loadPostLikes() {
        likedList = new ArrayList<>();
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.child(postUserId).child(postId).child("Likes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                likedList.clear();
                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        String uid = ds.getKey();

                        getUser(uid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getUser(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        userRef.orderByChild("uid").equalTo(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Contacts contact = ds.getValue(Contacts.class);
                    if(!Util.containUser(likedList, contact.getUid())){
                        likedList.add(contact);
                    }
                }
                contactAdapter = new ContactAdapter(likedList, PostLikesActivity.this, "contacts");
                recyclerView.setAdapter(contactAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
