package com.pipoxniko.toduo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.pipoxniko.toduo.mainfragment.MainDiaryFragment;
import com.pipoxniko.toduo.mainfragment.MainPetFragment;
import com.pipoxniko.toduo.mainfragment.MainProfileFragment;
import com.pipoxniko.toduo.mainfragment.MainSavingsFragment;
import com.pipoxniko.toduo.mainfragment.MainTodolistFragment;

public class MainViewPagerAdapter extends FragmentStateAdapter {
    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MainTodolistFragment();
            case 1:
                return new MainDiaryFragment();
            case 2:
                return new MainSavingsFragment();
            case 3:
                return new MainPetFragment();
            case 4:
                return new MainProfileFragment();
            default:
                return new MainTodolistFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
