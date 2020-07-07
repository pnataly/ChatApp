package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.example.chat.adapter.NotificationAdapter;
import com.example.chat.model.Notification;
import com.example.chat.model.User;
import com.example.chat.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Toolbar toolbar;

    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private List<Notification> notificationList;
    private List<Notification> searchList;
    private List<User> userPhoneList;
    private NotificationAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Notification");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        userPhoneList = new ArrayList<>();
        getContactsList();

        notificationList = new ArrayList<>();
        searchList = new ArrayList<>();
        getAllNotifications();
    }

    private void getAllNotifications() {
        final DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");
        notificationRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationList.clear();

                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        Notification notification = ds.getValue(Notification.class);
                        notificationList.add(notification);
                    }
                    notificationAdapter = new NotificationAdapter(NotificationActivity.this, notificationList, userPhoneList);
                    recyclerView.setAdapter(notificationAdapter);
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

    private void search(final String query){

        searchList.clear();
        for(final Notification notification: notificationList) {
            if (!notification.getPostUserId().equals(currentUserId)) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
                userRef.orderByChild("uid").equalTo(notification.getSenderId()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            String phone = ds.child("phone").getValue().toString();
                            String name = Util.getUserName(userPhoneList, phone);
                            notification.setSenderName(name);

                            if(notification.getSenderName().toLowerCase().contains(query) ||
                                    notification.getNotification().toLowerCase().contains(query)){
                                searchList.add(notification);
                            }

                        }
                        notificationAdapter = new NotificationAdapter(NotificationActivity.this, searchList, userPhoneList);
                        recyclerView.setAdapter(notificationAdapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        MenuItem notificationItem = menu.findItem(R.id.action_notification);
        notificationItem.setVisible(false);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String input) {
                //called when user press on search button
                if(!TextUtils.isEmpty(input.trim())){
                    search(input.toLowerCase());
                } else{
                    getAllNotifications();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String input) {
                //called when user type any letter
                if(!TextUtils.isEmpty(input.trim())){
                    search(input.toLowerCase());
                } else{
                    getAllNotifications();
                }
                return false;
            }
        });
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

            default:
                return true;
        }
    }

    private void sendUserToLogin(){
        Intent loginIntent = new Intent(NotificationActivity.this, PhoneLoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void sendUserToProfile(){
        Intent profileIntent = new Intent(NotificationActivity.this, MyProfileActivity.class);
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        profileIntent.putExtra("user_id", currentUserId);
        startActivity(profileIntent);
    }

    private void sendUserToContacts(){
        Intent findIntent = new Intent(NotificationActivity.this, ContactsActivity.class);
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

        currentUserId = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference dbReference = FirebaseDatabase.getInstance().getReference();
        dbReference.child("Users").child(currentUserId).child("user_state").updateChildren(onlineStateMap);
    }
}
