package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.adapters.UserAdapters;
import com.example.chatapp.databinding.ActivityUserBinding;
import com.example.chatapp.listeners.UserListeners;
import com.example.chatapp.models.User;
import com.example.chatapp.utilites.Constants;
import com.example.chatapp.utilites.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserActivity extends AppCompatActivity implements UserListeners {

    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;
    private int clicked = 0;
    private Boolean stillInApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.imageSearch.setOnClickListener(v -> {
            if (clicked == 0) {
                binding.headerTitle.setVisibility(View.INVISIBLE);
                binding.SearchBar.setVisibility(View.VISIBLE);
                clicked++;
            } else {
                binding.headerTitle.setVisibility(View.VISIBLE);
                binding.SearchBar.setVisibility(View.INVISIBLE);
                clicked = 0;
            }

        });
        binding.SearchBar.setOnClickListener(v -> {
            if (!binding.SearchBar.getText().toString().isEmpty()) {
                getUsersOnSearch();
            }
        });
    }

    private void getUsersOnSearch() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())
                                || !binding.SearchBar.getText().toString().equals(
                                        queryDocumentSnapshot.getString(Constants.KEY_NAME))) {
                                continue;
                            }

                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                            break;
                        }
                        if (users.size() > 0) {
                            UserAdapters userAdapters = new UserAdapters(users, this);
                            binding.usersRecyclerView.setAdapter(userAdapters);
                            binding.errorText.setVisibility(View.INVISIBLE);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            binding.usersRecyclerView.setVisibility(View.INVISIBLE);
                            errorText();
                        }
                    } else {
                        binding.usersRecyclerView.setVisibility(View.INVISIBLE);
                        errorText();
                    }
                });
    }

    private void getUsers() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            if(Objects.equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL),
                                    "robsod94@gmail.com")) {
                                User user = new User();
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                            }
                        }
                        if (users.size() > 0) {
                            UserAdapters userAdapters = new UserAdapters(users, this);
                            binding.usersRecyclerView.setAdapter(userAdapters);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            errorText();
                        }
                    } else {
                        errorText();
                    }
                });
    }

    private void errorText() {
        binding.errorText.setText(String.format("%s", "No users available"));
        binding.errorText.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean loading) {
        if (loading) {
            binding.progressbar.setVisibility(View.VISIBLE);
        } else {
            binding.progressbar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        //stillInApp = true;
        startActivity(intent);
        finish();
    }
/*
    @Override
    protected void onPause() {
        super.onPause();
        if (!stillInApp) {
            DocumentReference documentReference;
            PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID));
            documentReference.update(Constants.KEY_AVAILABLE, 0);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        DocumentReference documentReference;
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_AVAILABLE, 1);
        stillInApp = false;
    }*/
}