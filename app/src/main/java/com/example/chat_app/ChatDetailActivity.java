package com.example.chat_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chat_app.Adapter.ChatAdapter;
import com.example.chat_app.Models.MessageModel;
import com.example.chat_app.databinding.ActivityChatDetailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class ChatDetailActivity extends AppCompatActivity {

    ActivityChatDetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;

    // check the extension of the file to upload:
    private String checker = "";
    private String myUrl = "";
    private Uri fileUri;

    private StorageTask uploadTask;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        final String senderId = auth.getUid();
        String receiverId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");
        String profilePic = getIntent().getStringExtra("profilePic");

        binding.userName.setText(userName);
        Picasso.get().load(profilePic).placeholder(R.drawable.avatar).into(binding.profileImage);

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatDetailActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });


        final ArrayList<MessageModel> messageModels = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageModels, this, receiverId);

        binding.chatRecyclerView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecyclerView.setLayoutManager(layoutManager);

        // identifies which user is the sender and which is the receiver
        final String senderRoom = senderId + receiverId;
        final String receiverRoom = receiverId + senderId;

        // fetch the messages to display them
        database.getReference().child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        messageModels.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()){
                            MessageModel model = snapshot1.getValue(MessageModel.class);
                            model.setMessageID(snapshot1.getKey());
                            messageModels.add(model);
                        }
                        // inform the adapter for the change:
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });





        binding.uploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence options[] = new CharSequence[]{
                  "Изображение",
                  "PDF файл",
                  "MS Word файл"
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatDetailActivity.this);
                builder.setTitle("ИЗБЕРИ ФАЙЛ");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0){
                            // images
                            checker = "image";
                            // an Intent to send the user to their device gallery:
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select Image"), 438);

                        }
                        if (i == 1){
                            // pdf
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent, "Select PDF File"), 438);

                        }
                        if (i == 2){
                            // word
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent, "Select Word File"), 438);

                        }
                    }
                }).show();
            }
        });




        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = binding.enterMessage.getText().toString();
                final MessageModel model = new MessageModel(senderId, message);
                model.setTimestamp(new Date().getTime());
                binding.enterMessage.setText("");

                // store the message into the database
                database.getReference().child("chats")
                        .child(senderRoom)
                        .push()
                        .setValue(model)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {

                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .push()
                                        .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // message sent successfully
                                            }
                                        });
                            }
                        });
            }
        });

    }


    // override method for uploading file:


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // selected to upload an image
        if(requestCode == 438  &&  resultCode == RESULT_OK  && data != null && data.getData() != null){
            fileUri = data.getData();

            if (!checker.equals("image")){
                // it means that the user has not selected an image, but rather a different kind of file:

                StorageReference reference = FirebaseStorage.getInstance().getReference().child("Document_Files_Chat").child(fileUri.getLastPathSegment().toString());

                // initialize sender and receiver
                final String senderId = auth.getUid();
                String receiverId = getIntent().getStringExtra("userId");
                final String senderRoom = senderId + receiverId;
                final String receiverRoom = receiverId + senderId;


                reference.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final MessageModel model = new MessageModel(senderId, uri.toString(), checker);
                                model.setTimestamp(new Date().getTime());

                                database.getReference().child("chats")
                                        .child(senderRoom)
                                        .push()
                                        .setValue(model)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // message sent
                                            }
                                        });
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .push()
                                        .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // message sent successfully
                                            }
                                        });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(ChatDetailActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });









            }
            else if (checker.equals("image")){
                //final StorageReference reference = storage.getReference().child("profile_pic").child(FirebaseAuth.getInstance().getUid());
                // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! first upload to storage, than get the id and push  it as id, than to database

                StorageReference reference = FirebaseStorage.getInstance().getReference().child("Image_Files_Chat").child(fileUri.getLastPathSegment().toString());

                // initialize sender and receiver
                final String senderId = auth.getUid();
                String receiverId = getIntent().getStringExtra("userId");
                final String senderRoom = senderId + receiverId;
                final String receiverRoom = receiverId + senderId;


                // initialize the image model

                reference.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final MessageModel model = new MessageModel(senderId, uri.toString(), "image");
                                model.setTimestamp(new Date().getTime());

                                database.getReference().child("chats")
                                        .child(senderRoom)
                                        .push()
                                        .setValue(model)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // message sent
                                            }
                                        });
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .push()
                                        .setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // message sent successfully
                                            }
                                        });

                            }
                        });
                    }
                });

                // messageID, message = uri; uID = user(sender)

            }
            else{
                Toast.makeText(this, "Nothing Selected!", Toast.LENGTH_SHORT).show();
            }

        }



    }
}