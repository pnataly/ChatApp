package com.example.chat.fragments;

import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.example.chat.R;
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

import java.util.ArrayList;
import java.util.List;


public class ChatsFragment extends Fragment {

    private View ChatView;
    private RecyclerView recyclerView;
    private DatabaseReference userRef;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private List<Contacts> usersList;
    private ContactAdapter contactAdapter;
    private List<Contacts> contactsList;
    private List<String> uidList;
    private List<User> userPhoneList;


    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Util.permissionsContacts(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ChatView = inflater.inflate(R.layout.fragment_chats, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        userPhoneList = new ArrayList<>();
        usersList = new ArrayList<>();
        contactsList = new ArrayList<>();

        uidList = new ArrayList<>();

        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        recyclerView = ChatView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactAdapter = new ContactAdapter(contactsList, getContext(), "chat");
        recyclerView.setAdapter(contactAdapter);



        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(currentUserId);
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        String uid = ds.getKey();
                        uidList.add(uid);
                    }
                    if(Util.permissionsContactsCheck(getActivity())){
                        getContactsList();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return ChatView;
    }


    private void getContactsList(){

        String ISOPrefix = Util.getCountryISO();
        Cursor phones = getActivity().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
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
                        String uid = ds.getKey();
                        if(uidList.contains(uid)){
                            Contacts contact = ds.getValue(Contacts.class);
                            contact.setName(user.getName());
                            if(!Util.containUser(contactsList, contact.getUid())){
                                contactsList.add(contact);
                                contactAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

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

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchUser(final String query){

        usersList.clear();
        for(Contacts contact: contactsList) {
            if (!contact.getUid().equals(currentUserId)) {
                if (contact.getName().toLowerCase().contains(query)) {
                    if (!Util.containUser(usersList, contact.getUid())) {
                        usersList.add(contact);
                    }
                }
            }
            contactAdapter = new ContactAdapter(usersList, getContext(), "chat");
            recyclerView.setAdapter(contactAdapter);
        }
    }
}
