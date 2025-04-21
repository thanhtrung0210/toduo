package com.pipoxniko.toduo.todolistfragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.adapter.ItemTaskGroupAdapter;
import com.pipoxniko.toduo.model.ItemTask;
import com.pipoxniko.toduo.model.ItemTaskGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TodolistTimeFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private List<ItemTaskGroup> groupList;
    private ItemTaskGroupAdapter groupAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_time, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_time_recycler_group);
        recyclerGroup.setLayoutManager(new LinearLayoutManager(getContext()));

        //Tạo danh sách mẫu
        groupList = new ArrayList<>();

        List<ItemTask> todayTasks = Arrays.asList(
                new ItemTask("Mua đồ ăn", false),
                new ItemTask("Lau nhà", true),
                new ItemTask("Tập thể dục", false)
        );
        groupList.add(new ItemTaskGroup("Hôm nay", todayTasks));

        List<ItemTask> weekTasks = Arrays.asList(
                new ItemTask("Dọn phòng", false),
                new ItemTask("Chạy bộ", false)
        );
        groupList.add(new ItemTaskGroup("Tuần này", weekTasks));

        List<ItemTask> futureTasks = Arrays.asList(
                new ItemTask("Học Unity", false),
                new ItemTask("Viết nhạc cho game", false)
        );
        groupList.add(new ItemTaskGroup("Tương lai", futureTasks));

        // Gắn adapter
        groupAdapter = new ItemTaskGroupAdapter(groupList);
        recyclerGroup.setAdapter(groupAdapter);

        return mView;
    }
}