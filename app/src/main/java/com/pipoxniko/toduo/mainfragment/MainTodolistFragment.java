package com.pipoxniko.toduo.mainfragment;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.adapter.TodolistViewPagerAdapter;

public class MainTodolistFragment extends Fragment {

    private View mView;

    private TextView tabTime, tabCategory, tabAssign, tabStatus;
    private ViewPager2 viewPager2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main_todolist, container, false);

        //Ánh xạ view
        viewPager2 = mView.findViewById(R.id.tab_view_pager);

        tabTime = mView.findViewById(R.id.main_tab_time);
        tabCategory = mView.findViewById(R.id.main_tab_category);
        tabAssign = mView.findViewById(R.id.main_tab_assign);
        tabStatus = mView.findViewById(R.id.main_tab_status);

        //Mặc định sẽ là tab Thời gian
        setTabSelected("time");

        //Set adapter cho ViewPager2
        TodolistViewPagerAdapter adapter = new TodolistViewPagerAdapter(getActivity());
        viewPager2.setAdapter(adapter);

        //Xử lý sự kiện khi click vào các tab
        tabTime.setOnClickListener(v -> {
            setTabSelected("time");
            viewPager2.setCurrentItem(0, true);
        });
        tabCategory.setOnClickListener(v -> {
            setTabSelected("category");
            viewPager2.setCurrentItem(1, true);
        });
        tabAssign.setOnClickListener(v -> {
            setTabSelected("assign");
            viewPager2.setCurrentItem(2, true);
        });
        tabStatus.setOnClickListener(v -> {
            setTabSelected("status");
            viewPager2.setCurrentItem(3, true);
        });

        return mView;
    }

    private void setTabSelected(String selected) {
        resetAllTabs();

        TextView selectedTab;
        switch (selected) {
           case "time":     selectedTab = tabTime;     break;
           case "category": selectedTab = tabCategory; break;
           case "assign":   selectedTab = tabAssign;   break;
           case "status":   selectedTab = tabStatus;   break;
           default:         selectedTab = tabTime;     break;
        }

        selectedTab.setBackgroundResource(R.drawable.main_tab_bg_selected_tab);
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_default_secondary));
    }

    private void resetAllTabs() {
        TextView[] tabs = {tabTime, tabCategory, tabAssign, tabStatus};
        for (TextView tab : tabs) {
            tab.setBackgroundColor(Color.TRANSPARENT);
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_default_primary));
        }
    }
}