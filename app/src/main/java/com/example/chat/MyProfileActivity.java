package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chat.adapter.PostAdapter;
import com.example.chat.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyProfileActivity extends AppCompatActivity {

    private TextView userName, userStatus, userPhone;
    private CircleImageView userImage;
    private RecyclerView recyclerView;

    private Toolbar toolbar;

    private DatabaseReference userRef;
    private FirebaseAuth firebaseAuth;

    private List<Post> postList;
    private PostAdapter postAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = getIntent().getStringExtra("user_id");

        setupUI();

        postList = new ArrayList<>();

        getMyInfo();
        loadMyPosts();
    }

    private void setupUI(){

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Profile");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);

        userName = findViewById(R.id.username);
        userStatus = findViewById(R.id.status);
        userImage = findViewById(R.id.profile_image);
        userPhone = findViewById(R.id.user_phone);

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void loadMyPosts(){

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        postRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Post post = ds.child("Post_Info").getValue(Post.class);

                    postList.add(post);

                    postAdapter = new PostAdapter(MyProfileActivity.this, postList, "MyProfile");
                    recyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MyProfileActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getMyInfo(){
        userRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("image")){
                    String image = dataSnapshot.child("image").getValue().toString();
                    String name = dataSnapshot.child("name").getValue().toString();
                    String status = dataSnapshot.child("status").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    if(!image.isEmpty()){
                        Picasso.get().load(image).placeholder(R.drawable.icon_profile).into(userImage);
                    }

                    userName.setText(name);
                    userStatus.setText(status);
                    userPhone.setText(phone);
                }
                else {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String status = dataSnapshot.child("status").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    userName.setText(name);
                    userStatus.setText(status);
                    userPhone.setText(phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchMyPost(final String query){
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        postRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Post post = ds.child("Post_Info").getValue(Post.class);

                    if(post.getPost_title().toLowerCase().contains(query) || post.getPost_description().toLowerCase().contains(query)){
                        postList.add(post);
                    }

                    postAdapter = new PostAdapter(MyProfileActivity.this, postList, "MyProfile");
                    recyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MyProfileActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem addPostItem = menu.findItem(R.id.action_add_post);
        MenuItem editItem = menu.findItem(R.id.action_edit);
        MenuItem profileItem = menu.findItem(R.id.action_profile);

        searchItem.setVisible(true);
        addPostItem.setVisible(true);
        editItem.setVisible(true);
        profileItem.setVisible(false);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String input) {
                //called when user press on search button
                if(!TextUtils.isEmpty(input.trim())){
                    searchMyPost(input.toLowerCase());
                } else{
                    loadMyPosts();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String input) {
                //called when user type any letter
                if(!TextUtils.isEmpty(input.trim())){
                    searchMyPost(input.toLowerCase());
                } else{
                    loadMyPosts();
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.action_logout:
                updateUserStatus("offline");
                firebaseAuth.signOut();
                OneSignal.setSubscription(false);
                sendUserToLogin();
                return true;

            case R.id.action_add_post:
                Intent intent = new Intent(MyProfileActivity.this, AddPostActivity.class);
                intent.putExtra("key", "new");
                startActivity(intent);
                return true;

            case R.id.action_edit:
                sendUserToEditProfile();
                return true;

            case R.id.action_contacts:
                sendUserToContacts();
                return  true;

            default:
                return true;

        }
    }


    private void sendUserToContacts(){
        Intent findIntent = new Intent(MyProfileActivity.this, ContactsActivity.class);
        startActivity(findIntent);
    }

    private void updateUserStatus(String state){
        String currentTime, currentDate;
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat formatCurrentDate = new SimpleDateFormat("MMM dd, yyyy");
        currentDate = formatCurrentDate.format(calendar.getTime());

        SimpleDateFormat formatCurrentTime = new SimpleDateFormat("HH:mm");
        currentTime = formatCurrentTime.format(calendar.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", currentTime);
        onlineStateMap.put("date", currentDate);
        onlineStateMap.put("state", state);
        onlineStateMap.put("typingTo", "noOne");

        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference();
        dbReference.child("Users").child(currentUserId).child("user_state").updateChildren(onlineStateMap);
    }

    private void sendUserToLogin(){
        Intent loginIntent = new Intent(MyProfileActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void sendUserToEditProfile(){
        Intent intent = new Intent(MyProfileActivity.this, EditProfileActivity.class);
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
    }
}
