package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.adapters.RecentConversionsAdapter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.listeners.ConversionListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilites.Constants;
import com.example.chatapp.utilites.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversionsAdapter conversionsAdapter;
    private FirebaseFirestore database;
    private Boolean stillInApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversionsAdapter = new RecentConversionsAdapter(conversations, this);
        binding.conversionsRecyclerView.setAdapter(conversionsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stillInApp = false;
                signOut();
            }
        });
        binding.addUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stillInApp = true;
                startActivity(new Intent(getApplicationContext(),UserActivity.class));
            }
        });

    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        if (preferenceManager.getString(Constants.KEY_IMAGE) != null) {
            byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),
                    Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.profileImage.setImageBitmap(bitmap);
        }

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,
                        preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,
                        preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(
                            Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(
                            Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversionImage =
                                documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName =
                                documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId =
                                documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    } else {
                        chatMessage.conversionImage =
                                documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName =
                                documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId =
                                documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(
                            Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(
                            Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(
                                Constants.KEY_SENDER_ID
                        );
                        String receiverId = documentChange.getDocument().getString(
                                Constants.KEY_RECEIVER_ID
                        );
                        if (conversations.get(i).senderId.equals(senderId) &&
                                conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message =
                                    documentChange.getDocument().getString(
                                            Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject =
                                    documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations,
                    (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversionsAdapter.notifyDataSetChanged();
            binding.conversionsRecyclerView.smoothScrollToPosition(0);
            binding.conversionsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressbar.setVisibility(View.GONE);
        }
    };

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void signOut() {
        showToast("Signing out");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        updates.put(Constants.KEY_AVAILABLE, 0);
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SigninActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to sign out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        stillInApp = true;
        startActivity(intent);
    }


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
    }
}