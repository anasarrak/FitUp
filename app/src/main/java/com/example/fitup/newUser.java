package com.example.fitup;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class newUser extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private EditText mEmailCU;
    private EditText mPasswordCu;
    private EditText mPassword2Cu;
    private Spinner sCreateUser;
    private Button btnCu;
    private FirebaseAuth mAuth;
    private FirebaseFirestore nFirestore;

    private FirebaseAuth.AuthStateListener mAuthListener;
    private ArrayList<String> grupos = new ArrayList<>();

    Grupo g;
    User u;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user);
        g = new Grupo();
        u = new User();
        mAuth = FirebaseAuth.getInstance();
        nFirestore = FirebaseFirestore.getInstance();

        mEmailCU = findViewById(R.id.emailFieldCU);
        mPasswordCu = findViewById(R.id.passFieldCU);
        mPassword2Cu = findViewById(R.id.passFieldCU2);
        sCreateUser = findViewById(R.id.sCreateUser);
        btnCu = findViewById(R.id.cuBtn);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mAuth.getCurrentUser() != null){
                    /*startActivity(new Intent(login.this,MainActivity.class));
                    Toast.makeText(login.this, "USUARIO CORRECTO "+ mAuth.getCurrentUser().getUid(), Toast.LENGTH_SHORT).show();*/
                }
            }
        };

        nFirestore.collection("groups").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot document:task.getResult()){
                        grupos.add(document.getId());
                    }
                    fillList();
                }
            }
        });

        btnCu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });



    }

    private void fillList() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, grupos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sCreateUser.setAdapter(adapter);
        sCreateUser.setOnItemSelectedListener(this);
    }

    private void createAccount() {
        String user = mEmailCU.getText().toString();
        String password = mPasswordCu.getText().toString();
        String password2 = mPassword2Cu.getText().toString();

        if (TextUtils.isEmpty(user)|| TextUtils.isEmpty(password) || TextUtils.isEmpty(password2) || !password.equalsIgnoreCase(password2)){
            Toast.makeText(newUser.this, "Campos Vacios o la contraseña no coincide", Toast.LENGTH_SHORT).show();
        }else{
            mAuth.createUserWithEmailAndPassword("anasarrak@acutronic.com","anasarrak")
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                nFirestore.collection("usergroup").document(mAuth.getCurrentUser().getUid()).set(g);
                                Toast.makeText(newUser.this, "Usuario Creado", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(newUser.this, "Authentication fallida.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }
    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String text = parent.getItemAtPosition(position).toString();
        g.setGroup(text);
       /* User u = new User();
        Grupo g = new Grupo(text);
        u.setUid(mAuth.getCurrentUser().getUid());
        nFirestore.collection("usergroup").document(mAuth.getCurrentUser().getUid()).set(g);*/
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
