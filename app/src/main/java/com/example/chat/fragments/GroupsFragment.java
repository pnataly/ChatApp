package com.example.chat.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.chat.CreateGroupActivity;
import com.example.chat.R;
import com.example.chat.adapter.GroupsAdapter;
import com.example.chat.model.Group;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class GroupsFragment extends Fragment {

    private View groupView;
    private RecyclerView recyclerView;
    private GroupsAdapter groupsAdapter;
    private List<Group> groupList = new ArrayList<>();

    private DatabaseReference dbReference;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;

    private List<Group> searchList;

    public GroupsFragment() {
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
        groupView = inflater.inflate(R.layout.fragment_groups, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUserId = firebaseAuth.getCurrentUser().getUid();
        dbReference = FirebaseDatabase.getInstance().getReference().child("Groups");

        recyclerView = groupView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        displayGroups();
        return groupView;
    }

    private void displayGroups() {
        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                groupList.clear();

                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(ds.child("Participants").hasChild(currentUserId)){
                        Group group = ds.child("Group_Info").getValue(Group.class);
                        groupList.add(group);
                    }
                }

                groupsAdapter = new GroupsAdapter(groupList, getContext(), currentUserId);
                groupsAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(groupsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchGroup(final String query){
        searchList = new ArrayList<>();

        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                searchList.clear();

                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if(ds.child("Participants").hasChild(currentUserId)){
                        Group group = ds.child("Group_Info").getValue(Group.class);
                        if(group.getTitle().toLowerCase().contains(query)){
                            searchList.add(group);
                        }
                    }
                    groupsAdapter = new GroupsAdapter(searchList, getContext(), currentUserId);
                    groupsAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(groupsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {

        MenuItem addGroupItem = menu.findItem(R.id.action_add_group);
        addGroupItem.setVisible(true);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String input) {
                //called when user press on search button
                if(!TextUtils.isEmpty(input.trim())){
                    searchGroup(input.toLowerCase());
                } else{
                    displayGroups();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String input) {
                //called when user type any letter
                if(!TextUtils.isEmpty(input.trim())){
                    searchGroup(input.toLowerCase());
                } else{
                    displayGroups();
                }
                return false;
            }
        });

        addGroupItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_add_group){
                    Intent intent = new Intent(getContext(), CreateGroupActivity.class);
                    getContext().startActivity(intent);
                }
                return true;
            }

        });
        super.onCreateOptionsMenu(menu, inflater);
    }
}
