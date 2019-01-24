package com.example.fitup;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {
    private EditText mEmail;
    private EditText mPassword;

    private Button mLogin;
    private Button mLoginGuest;

    private FirebaseAuth mAuth;
    private FirebaseFirestore nFirestore;

    private FirebaseAuth.AuthStateListener mAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        nFirestore = FirebaseFirestore.getInstance();
        mEmail = findViewById(R.id.emailField);
        mPassword = findViewById(R.id.passField);

        mLogin = findViewById(R.id.loginBtn);
        mLoginGuest = findViewById(R.id.loginGuestBtn);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (mAuth.getCurrentUser() != null){
                    Toast.makeText(login.this, "USUARIO CORRECTO "+ mAuth.getCurrentUser().getUid(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(login.this,MainActivity.class));
                }
            }
        };

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSingIn();
            }
        });

        mLoginGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuestSignIn();
            }
        });

    }

    private void startGuestSignIn() {
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    final Map<String,String> groupAdd = new HashMap<>();
                    groupAdd.put("group","guest");
                    groupAdd.put("steps","");
                    groupAdd.put("top","0");
                    nFirestore.collection("user").document(mAuth.getCurrentUser().getUid()).set(groupAdd);
                    startActivity(new Intent(login.this,MainActivity.class));
                }else{
                    Toast.makeText(login.this, "Error Al intentar logearte como invitado", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }


    private void startSingIn(){
        String email = mEmail.getText().toString();
        String password = mPassword.getText().toString();

        if (TextUtils.isEmpty(email)|| TextUtils.isEmpty(password)){
            Toast.makeText(login.this, "Campos Vacios", Toast.LENGTH_SHORT).show();
        }else{
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()){
                        Toast.makeText(login.this, "Usuario o Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

}
