package com.example.chat.adapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.example.chat.fragments.ChatsFragment;
import com.example.chat.fragments.GroupsFragment;
import com.example.chat.fragments.PostsFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }


    public Fragment getItem(int position) {

        switch (position){

            case 0:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;

            case 1:
                GroupsFragment groupsFragment = new GroupsFragment();
                return groupsFragment;

            case 2:
                PostsFragment postsFragment = new PostsFragment();
                return postsFragment;

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {

        switch (position){

            case 0:
                return "Chats";

            case 1:
                return "Groups";

            case 2:
                return "Posts";

            default:
                return null;
        }
    }
}
