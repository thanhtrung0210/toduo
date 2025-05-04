package com.pipoxniko.toduo.mainfragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.FoodItem;

import java.util.ArrayList;
import java.util.List;

public class FoodListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FoodAdapter adapter;
    private List<FoodItem> foodList;
    private DatabaseReference databaseReference;
    private View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_food_list, container, false);
        initViews();
        setupFirebase();
        setupRecyclerView();
        loadFoodItems();
        return mView;
    }

    private void initViews() {
        recyclerView = mView.findViewById(R.id.recyclerViewFoodList);
    }

    private void setupFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Thi");
    }

    private void setupRecyclerView() {
        foodList = new ArrayList<>();
        adapter = new FoodAdapter(foodList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // Lấy dữ liệu từ Firebase và cập nhật lên RecyclerView
    private void loadFoodItems() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                foodList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    FoodItem foodItem = dataSnapshot.getValue(FoodItem.class);
                    foodList.add(foodItem);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

        private List<FoodItem> foodList;

        public FoodAdapter(List<FoodItem> foodList) {
            this.foodList = foodList;
        }

        @NonNull
        @Override
        public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
            return new FoodViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
            FoodItem foodItem = foodList.get(position);
            holder.textViewName.setText(foodItem.getName());
            holder.textViewDescription.setText(foodItem.getDescription());
            holder.textViewDate.setText(foodItem.getDate());

            if (foodItem.getImageBase64() != null && !foodItem.getImageBase64().isEmpty()) {
                Bitmap bitmap = base64ToBitmap(foodItem.getImageBase64());
                holder.imageViewFood.setImageBitmap(bitmap);
            }
        }

        @Override
        public int getItemCount() {
            return foodList.size();
        }

        private Bitmap base64ToBitmap(String base64Str) {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        class FoodViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewFood;
            TextView textViewName, textViewDescription, textViewDate;

            public FoodViewHolder(@NonNull View itemView) {
                super(itemView);
                imageViewFood = itemView.findViewById(R.id.imageViewFood);
                textViewName = itemView.findViewById(R.id.textViewName);
                textViewDescription = itemView.findViewById(R.id.textViewDescription);
                textViewDate = itemView.findViewById(R.id.textViewDate);
            }
        }
    }
}