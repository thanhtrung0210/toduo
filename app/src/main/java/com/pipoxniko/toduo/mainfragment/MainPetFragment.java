package com.pipoxniko.toduo.mainfragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.pipoxniko.toduo.R;

public class MainPetFragment extends Fragment {

    private View mView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Khởi tạo giao diện từ file XML
        mView = inflater.inflate(R.layout.fragment_main_pet, container, false);

        // Thiết lập ViewPager2 và TabLayout
        ViewPager2 viewPager = mView.findViewById(R.id.pet_view_pager);
        TabLayout tabLayout = mView.findViewById(R.id.tab_layout);

        // Tạo adapter cho ViewPager2
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                // Trả về fragment tương ứng với vị trí
                return position == 0 ? new PetInputFragment() : new PetListFragment();
            }

            @Override
            public int getItemCount() {
                // Số lượng fragment (Nhập dữ liệu và Danh sách)
                return 2;
            }
        });

        // Liên kết TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Nhập dữ liệu" : "Danh sách");
        }).attach();

        return mView;
    }
}