package com.example.chat.fragments;

import android.content.Intent;
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
import android.widget.Toast;

import com.example.chat.AddPostActivity;
import com.example.chat.R;
import com.example.chat.adapter.PostAdapter;
import com.example.chat.model.Contacts;
import com.example.chat.model.Post;
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


public class PostsFragment extends Fragment {

    private View PostsView;

    private RecyclerView recyclerView;
    private List<Post> postList;
    private PostAdapter postAdapter;

    private FirebaseAuth firebaseAuth;
    private String currentUserId;
    private DatabaseReference contactRef;
    private List<Contacts> contactsList;
    private List<User> userPhoneList;


    public PostsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        PostsView =  inflater.inflate(R.layout.fragment_posts, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        contactRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId);

        contactsList = new ArrayList<>();
        userPhoneList = new ArrayList<>();
        getContactsList();

        recyclerView = PostsView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();

        loadPosts();

        return PostsView;
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

                        Contacts contact = ds.getValue(Contacts.class);
                        contact.setName(user.getName());

                        if(!Util.containUser(contactsList, contact.getUid())){
                            contact.setName(user.getName());
                            contactsList.add(contact);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadPosts(){

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    if(Util.containUser(contactsList, ds.getKey())){
                        for(DataSnapshot d: ds.getChildren()){
                            Post post = d.child("Post_Info").getValue(Post.class);
                            post.setUser_name(Util.getName(contactsList, post.getUser_id()));
                            postList.add(post);

                            postAdapter = new PostAdapter(getContext(), postList, "fragment");
                            recyclerView.setAdapter(postAdapter);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void searchPost(final String query){
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        postRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    if(Util.containUser(contactsList, ds.getKey())){
                        for(DataSnapshot d: ds.getChildren()){
                            Post post = d.child("Post_Info").getValue(Post.class);
                            post.setUser_name(Util.getName(contactsList, post.getUser_id()));

                            if(post.getPost_title().toLowerCase().contains(query) || post.getPost_description().toLowerCase().contains(query)){
                                postList.add(post);
                            }

                            postAdapter = new PostAdapter(getContext(), postList, "fragment");
                            recyclerView.setAdapter(postAdapter);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem addPostItem = menu.findItem(R.id.action_add_post);
        addPostItem.setVisible(true);
        searchItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String input) {
                //called when user press on search button
                if(!TextUtils.isEmpty(input.trim())){
                    searchPost(input.toLowerCase());
                } else{
                    loadPosts();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String input) {
                //called when user type any letter
                if(!TextUtils.isEmpty(input.trim())){
                    searchPost(input.toLowerCase());
                } else{
                    loadPosts();
                }
                return false;
            }
        });

        addPostItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_add_post){
                    Intent intent = new Intent(getContext(), AddPostActivity.class);
                    intent.putExtra("key", "new");
                    getContext().startActivity(intent);
                }
                return true;
            }

        });

        super.onCreateOptionsMenu(menu, inflater);
    }

}
