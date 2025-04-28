package com.pipoxniko.toduo.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.adapter.MainViewPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 mViewPager2;

    private LinearLayout tabTodolist,  tabDiary,  tabSavings,  tabPet,  tabProfile;
    private ImageView    iconTodolist, iconDiary, iconSavings, iconPet, iconProfile;
    private TextView     textTodolist, textDiary, textSavings, textPet, textProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Ánh xạ View
        mViewPager2 = findViewById(R.id.main_view_pager);

        tabTodolist = findViewById(R.id.main_bn_button_todolist);
        tabDiary    = findViewById(R.id.main_bn_button_diary);
        tabSavings  = findViewById(R.id.main_bn_button_savings);
        tabPet      = findViewById(R.id.main_bn_button_pet);
        tabProfile  = findViewById(R.id.main_bn_button_profile);

        iconTodolist = findViewById(R.id.main_bn_icon_todolist);
        iconDiary    = findViewById(R.id.main_bn_icon_diary);
        iconSavings  = findViewById(R.id.main_bn_icon_savings);
        iconPet      = findViewById(R.id.main_bn_icon_pet);
        iconProfile  = findViewById(R.id.main_bn_icon_profile);

        textTodolist = findViewById(R.id.main_bn_text_todolist);
        textDiary    = findViewById(R.id.main_bn_text_diary);
        textSavings  = findViewById(R.id.main_bn_text_savings);
        textPet      = findViewById(R.id.main_bn_text_pet);
        textProfile  = findViewById(R.id.main_bn_text_profile);

        // Tab mặc định sẽ là tab Mục tiêu
        setTabSelected("todolist");

        //Set adapter cho ViewPager2
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(this);
        mViewPager2.setAdapter(adapter);

        //Xử lý sự kiện khi click vào các tab ở bottom navigation
        tabTodolist.setOnClickListener(v -> {
            setTabSelected("todolist");
            mViewPager2.setCurrentItem(0, true);
        });
        tabDiary.setOnClickListener(v -> {
            setTabSelected("diary");
            mViewPager2.setCurrentItem(1, true);
        });
        tabSavings.setOnClickListener(v -> {
            setTabSelected("savings");
            mViewPager2.setCurrentItem(2, true);
        });
        tabPet.setOnClickListener(v -> {
            setTabSelected("pet");
            mViewPager2.setCurrentItem(3, true);
        });
        tabProfile.setOnClickListener(v -> {
            setTabSelected("profile");
            mViewPager2.setCurrentItem(4, true);
        });

        //Xử lý sự kiện vuốt màn hình để chuyển tab
        mViewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: setTabSelected("todolist"); break;
                    case 1: setTabSelected("diary");    break;
                    case 2: setTabSelected("savings");  break;
                    case 3: setTabSelected("pet");      break;
                    case 4: setTabSelected("profile");  break;
                }
            }
        });
    }

    private void setTabSelected(String selected) {

        resetAllTabs();

        switch (selected) {
            case "todolist":
                activateTab(tabTodolist, iconTodolist, textTodolist);
                break;
            case "diary":
                activateTab(tabDiary, iconDiary, textDiary);
                break;
            case "savings":
                activateTab(tabSavings, iconSavings, textSavings);
                break;
            case "pet":
                activateTab(tabPet, iconPet, textPet);
                break;
            case "profile":
                activateTab(tabProfile, iconProfile, textProfile);
                break;
        }
    }

    private void resetAllTabs() {
        deactivateTab(tabTodolist, iconTodolist, textTodolist);
        deactivateTab(tabDiary, iconDiary, textDiary);
        deactivateTab(tabSavings, iconSavings, textSavings);
        deactivateTab(tabPet, iconPet, textPet);
        deactivateTab(tabProfile, iconProfile, textProfile);
    }

    private void activateTab(LinearLayout layout, ImageView icon, TextView label) {
        // Đặt màu cho tab được chọn
        layout.setBackgroundResource(R.drawable.main_bn_bg_slected_tab);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.color_default_secondary));
        label.setVisibility(TextView.VISIBLE);

        //Chỉnh lại tỉ lệ của tab được chọn
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        params.weight = 2.5f;
        layout.setLayoutParams(params);
    }

    private void deactivateTab(LinearLayout layout, ImageView icon, TextView label) {
        // Đặt màu cho tab không được chọn
        layout.setBackgroundColor(Color.TRANSPARENT);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.color_default_primary));
        label.setVisibility(TextView.GONE);

        //Chỉnh lại tỉ lệ của tab không được chọn
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
        params.weight = 1f;
        layout.setLayoutParams(params);
    }

}