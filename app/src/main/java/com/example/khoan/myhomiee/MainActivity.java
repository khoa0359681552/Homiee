package com.example.khoan.myhomiee;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public String pathRoom ="/Room/1";
    // khi do co duoc cai do :

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseFirestore db= FirebaseFirestore.getInstance();
        CollectionReference usersRef= db.collection("User");

        addData();

        /*
        HashMap<String,Object> user= new HashMap<>();
        user.put("password", "654321"); user.put("phone","0987654321");
        usersRef.add(user).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if(task.isSuccessful()){
                    Log.d("HELLO","vao day");
                    String id= task.getResult().getId();
                    Log.d("PUS", id);
                }
            }
        });
        */
    }
    public void addData(){
        long x= Long.valueOf("1621184400000") , del = 5*60*1000;
        Random rd= new Random();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch  = db.batch();
        CollectionReference tempsRef = db.collection("TemperatureSensor");
        DocumentReference room1Ref = db.document("/Room/1");

        for(int i=0;i<286;i++){
            x+= del;
            Timestamp timestamp = new Timestamp(new Date(x));
            int t = 26 + (rd.nextInt(Integer.MAX_VALUE)%8);
            DocumentReference tempDoc = tempsRef.document(String.valueOf(x));
            HashMap<String,Object> val =new HashMap<>();
            val.put("FkSensor_RoomId",room1Ref); val.put("time",timestamp); val.put("temperature",t);
            batch.set(tempDoc,val);
        }
        batch.commit();
        Log.d("FIN","commit finish");
    }
}
