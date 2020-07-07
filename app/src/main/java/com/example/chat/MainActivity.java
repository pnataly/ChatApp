package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import com.example.chat.adapter.ViewPagerAdapter;
import com.example.chat.util.Util;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter viewPagerAdapter;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference dbReference;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        dbReference = FirebaseDatabase.getInstance().getReference();

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chat");

        //Util.permissionsContacts(this);


        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tab_layout);

        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.setupWithViewPager(viewPager);


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        Log.d("Mydebug", "user in onStart: " + currentUser);

        //if the user not logged in
        if(currentUser == null){
            Log.d("Mydebug", "user null");
            firebaseAuth.signOut();
            sendUserToLogin();
        }
        else {
            FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        if(!dataSnapshot.hasChild("notificationKey")){
                            OneSignal.startInit(MainActivity.this).init();
                            OneSignal.setSubscription(true);
                            OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                                @Override
                                public void idsAvailable(String userId, String registrationId) {
                                    FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseAuth.getUid()).child("notificationKey").setValue(userId);
                                }
                            });
                            OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            updateUserStatus("online");
            VerifyUser();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if(currentUser != null){
            updateUserStatus("offline");

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if(currentUser != null){
            updateUserStatus("offline");

        }
    }

    private void VerifyUser(){
        String userId = firebaseAuth.getCurrentUser().getUid();
        dbReference.child("Users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("name").exists()){
                }
                else { //new user - need to set username
                    sendUserToEditProfile();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.action_profile:
                sendUserToProfile();
                return true;

            case R.id.action_logout:
                updateUserStatus("offline");
                firebaseAuth.signOut();
                OneSignal.setSubscription(false);
                sendUserToLogin();
                return true;

            case R.id.action_contacts:
                sendUserToContacts();
                return  true;

            case R.id.action_notification:
                sendUserToNotification();
                return true;

            default:
                return true;

        }
    }

    private void sendUserToLogin(){
        Intent loginIntent = new Intent(MainActivity.this, PhoneLoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void sendUserToProfile(){
        Intent profileIntent = new Intent(MainActivity.this, MyProfileActivity.class);
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        profileIntent.putExtra("user_id", currentUserId);
        //loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(profileIntent);
        //finish();
    }

    private void sendUserToEditProfile(){
        Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        intent.putExtra("user_id", currentUserId);
        startActivity(intent);
    }

    private void sendUserToContacts(){
        Intent findIntent = new Intent(MainActivity.this, ContactsActivity.class);
        startActivity(findIntent);
    }

    private void sendUserToNotification(){
        Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
        startActivity(intent);
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

        currentUserId = firebaseAuth.getCurrentUser().getUid();
        dbReference.child("Users").child(currentUserId).child("user_state").updateChildren(onlineStateMap);
    }

}
