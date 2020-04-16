package com.example.socialmediaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socialmediaapp.adapters.AdapterChat;
import com.example.socialmediaapp.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profile;
    TextView name,userstatus;
    EditText msg;
    ImageButton send;
    FirebaseAuth firebaseAuth;
    String uid,myuid,image;
    ValueEventListener valueEventListener;
    DatabaseReference userforseen;
    List<ModelChat> chatList;
    AdapterChat adapterChat;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        firebaseAuth=FirebaseAuth.getInstance();
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView=findViewById(R.id.chatrecycle);
        profile=findViewById(R.id.profiletv);
        name=findViewById(R.id.nameptv);
        userstatus=findViewById(R.id.onlinetv);
        msg=findViewById(R.id.messaget);
        send=findViewById(R.id.sendmsg);

        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        uid=getIntent().getStringExtra("uid");
        firebaseDatabase=FirebaseDatabase.getInstance();
        users=firebaseDatabase.getReference("Users");
        Query userquery=users.orderByChild("uid").equalTo(uid);
        userquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1:dataSnapshot.getChildren()) {
                    String nameh = "" + dataSnapshot1.child("name").getValue();
                    image = "" + dataSnapshot1.child("image").getValue();
                    String onlinestatus = "" + dataSnapshot1.child("onlineStatus").getValue();
                    if (onlinestatus.equals("online")) {
                        userstatus.setText(onlinestatus);
                    } else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(Long.parseLong(onlinestatus));
                        String timedate = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                        userstatus.setText("Last Seen:" + timedate);
                    }
                    name.setText(nameh);
                    try {
                        Picasso.with(ChatActivity.this).load(image).placeholder(R.drawable.profile_image).into(profile);
                    }
                    catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=msg.getText().toString().trim();
                if (TextUtils.isEmpty(message)){
                    Toast.makeText(ChatActivity.this,"Please Write Something Here",Toast.LENGTH_LONG).show();
                }
                else {

                    sendmessage(message);
                }
            }

        });
        readMessages();
        seenMessgae();
    }

    private void seenMessgae() {
        userforseen=FirebaseDatabase.getInstance().getReference("Chats");
        valueEventListener=userforseen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                    ModelChat chat=dataSnapshot1.getValue(ModelChat.class);
                    if(chat.getReceiver().equals(myuid)&&chat.getSender().equals(uid)){
                        HashMap<String ,Object> hashMap=new HashMap<>();
                        hashMap.put("dilihat",true);
                        dataSnapshot1.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        String timestamp= String.valueOf(System.currentTimeMillis());
        checkOnlineStatus(timestamp);
        userforseen.removeEventListener(valueEventListener);
    }

    @Override
    protected void onResume() {
        checkOnlineStatus("online");
        super.onResume();
    }

    private void checkOnlineStatus(String status){

        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference("Users").child(myuid);
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("onlineStatus", status);
        dbref.updateChildren(hashMap);
    }
    @Override
    protected void onStart() {
        checkUserStatus();
        checkOnlineStatus("online");
        super.onStart();
    }

    private void sendmessage(String message) {

        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();
        String timestamp=String.valueOf(System.currentTimeMillis());
        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("sender",myuid);
        hashMap.put("receiver",uid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp);
        hashMap.put("dilihat",false);
        databaseReference.child("Chats").push().setValue(hashMap);
        msg.setText("");
    }
    private void readMessages() {

        chatList=new ArrayList<>();
        DatabaseReference dbref=FirebaseDatabase.getInstance().getReference().child("Chats");
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                chatList.clear();
                for (DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                    ModelChat modelChat=dataSnapshot1.getValue(ModelChat.class);
                    if(modelChat.getReceiver().equals(myuid)&& modelChat.getSender().equals(uid)||
                            modelChat.getReceiver().equals(uid)&&modelChat.getSender().equals(myuid)){
                        chatList.add(modelChat);
                    }
                    adapterChat=new AdapterChat(ChatActivity.this,chatList,image);
                    adapterChat.notifyDataSetChanged();
                    recyclerView.setAdapter(adapterChat);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if(user!=null){
            myuid=user.getUid();
        }
        else {
            startActivity(new Intent(ChatActivity.this,MainActivity.class));
           finish();
        }
    }


}
