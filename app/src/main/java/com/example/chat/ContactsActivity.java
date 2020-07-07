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

import com.example.chat.adapter.ContactAdapter;
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
import com.onesignal.OneSignal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference userRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private List<User> userPhoneList;
    private List<Contacts> usersList;
    private ContactAdapter contactAdapter;

    private List<Contacts> contactsList;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        setupUI();
        getContactsList();
    }

    private void setupUI(){

        toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Contacts");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        userPhoneList = new ArrayList<>();
        usersList = new ArrayList<>();
        contactsList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new ContactAdapter(contactsList, this, "contacts");
        recyclerView.setAdapter(contactAdapter);
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

            getUserDetails(user);
        }
    }

    private void getUserDetails(final User user) {

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
                            contactAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void searchUser(final String query){
        usersList.clear();
        for(Contacts contact: contactsList){
            if(!contact.getUid().equals(currentUserId)){
                if(contact.getName().toLowerCase().contains(query)){
                    if(!Util.containUser(usersList, contact.getUid())){
                        usersList.add(contact);
                    }
                }
            }
            contactAdapter = new ContactAdapter(usersList, ContactsActivity.this, "contacts");
            recyclerView.setAdapter(contactAdapter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);

        MenuItem contactItem = menu.findItem(R.id.action_contacts);
        contactItem.setVisible(false);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String input) {
                //called when user press on search button
                if(!TextUtils.isEmpty(input.trim())){
                    searchUser(input.toLowerCase());
                } else{
                    getContactsList();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String input) {
                //called when user type any letter
                if(!TextUtils.isEmpty(input.trim())){
                    searchUser(input.toLowerCase());
                } else{
                    getContactsList();
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

            default:
                return true;

        }
    }

    private void sendUserToLogin(){
        Intent loginIntent = new Intent(ContactsActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void sendUserToProfile(){
        Intent profileIntent = new Intent(ContactsActivity.this, MyProfileActivity.class);
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        profileIntent.putExtra("user_id", currentUserId);
        startActivity(profileIntent);
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
