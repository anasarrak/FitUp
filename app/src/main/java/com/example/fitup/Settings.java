package com.example.fitup;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;

public class Settings extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Spinner sp;
    private FirebaseFirestore nFirestore;
    private ArrayList<String> grupos = new ArrayList<>();
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //FirebaseApp.initializeApp(this);
        nFirestore = FirebaseFirestore.getInstance();
        sp = findViewById(R.id.sGroup);


        nFirestore.collection("groups").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot document:task.getResult()){
                        grupos.add(document.getId());
                    }
                    fillSpinner();
                }
            }
        });

    }

    private void fillSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, grupos);
        //ArrayAdapter<String> ad = ArrayAdapter.createFromResource(this,grupos,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setOnItemSelectedListener(this);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String text = parent.getItemAtPosition(position).toString();
        User u = new User();
        Grupo g = new Grupo(text);
        u.setUid(mAuth.getCurrentUser().getUid());
        nFirestore.collection("usergroup").document(mAuth.getCurrentUser().getUid()).set(g);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
