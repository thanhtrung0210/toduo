package com.pipoxniko.toduo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.pipoxniko.toduo.todolistfragment.TodolistAssignFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistCategoryFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistStatusFragment;
import com.pipoxniko.toduo.todolistfragment.TodolistTimeFragment;

public class TodolistViewPagerAdapter extends FragmentStateAdapter {
    public TodolistViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:  return new TodolistTimeFragment();
            case 1:  return new TodolistCategoryFragment();
            case 2:  return new TodolistAssignFragment();
            case 3:  return new TodolistStatusFragment();
            default: return new TodolistTimeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
