package com.pipoxniko.toduo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.pipoxniko.toduo.todolistfragment.TodolistAssignmentFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistCategoryFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistStatusFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistTimeFragment;

public class TodolistViewPagerAdapter extends FragmentStateAdapter {

    private final String coupleId;

    public TodolistViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String coupleId) {
        super(fragmentActivity);
        this.coupleId = coupleId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return TodolistTimeFragment.newInstance(coupleId);
            case 1:
                return TodolistCategoryFragment.newInstance(coupleId);
            case 2:
                return TodolistAssignmentFragment.newInstance(coupleId);
            case 3:
                return TodolistStatusFragment.newInstance(coupleId);
            default:
                return TodolistTimeFragment.newInstance(coupleId);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}