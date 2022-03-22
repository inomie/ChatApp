package com.example.chatapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ContainerUserBinding;
import com.example.chatapp.models.User;

import java.util.List;


public class UserAdapters extends RecyclerView.Adapter<UserAdapters.UserViewHolder>{

    private final List<User> users;

    public UserAdapters(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContainerUserBinding containerUserBinding = ContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new UserViewHolder(containerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ContainerUserBinding binding;

        UserViewHolder(ContainerUserBinding containerUserBinding){
            super(containerUserBinding.getRoot());
            binding = containerUserBinding;
        }

        void setUserData(User user) {
            binding.textName.setText(user.name);
            binding.profileImage.setImageBitmap(getUserImage(user.image));
        }
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }


}