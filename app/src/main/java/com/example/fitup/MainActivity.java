package com.example.fitup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Goal;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.GoalsReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.FirebaseAuthCredentialsProvider;


public class MainActivity extends AppCompatActivity implements  SensorEventListener {
    private FirebaseFirestore nFirestore;
    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = System.identityHashCode(this) & 0xFFFF;
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private Button ajustes;
    static final String LOG_TAG = "FitUP!";
    boolean running = false;

    private Button btnLogOut;
    TextView steps;
    SensorManager sensorManager;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String group;
    int top;
    private static final String TAGFit = "FitActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        nFirestore = FirebaseFirestore.getInstance();

        setContentView(R.layout.activity_main);

        btnLogOut = findViewById(R.id.logOutBtn);

        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .build();
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            subscribe();
        }


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(mAuth.getCurrentUser().isAnonymous()){
                    mAuth.signOut();
                    startActivity(new Intent(MainActivity.this,login.class));
            }
        });

    }

    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAGFit, "Successfully subscribed!");
                                } else {
                                    Log.w(TAGFit, "There was a problem subscribing.", task.getException());
                                }
                            }
                        });
        }




    @Override
    protected void onResume() {
        super.onResume();
        //connectFitness();
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (countSensor!= null){
            sensorManager.registerListener(this,countSensor,sensorManager.SENSOR_DELAY_UI);
        }else{
            Toast.makeText(this,"SENSOR NOT FOUND",Toast.LENGTH_LONG).show();
        }
    }

        @Override
    protected void onPause() {
        super.onPause();
        running = false;

    }
    //Calculate the current steps using the sensor and add it to Firebase
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running){
            steps = findViewById(R.id.steps);
            final Map<String,String> userMap = new HashMap<>();

            Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(
                            new OnSuccessListener<DataSet>() {
                                @Override
                                public void onSuccess(DataSet dataSet) {
                                    long total =
                                            dataSet.isEmpty()
                                                    ? 0
                                                    : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                    //Toast.makeText(MainActivity.this, String.valueOf(total), Toast.LENGTH_SHORT).show();
                                    group = getGroup();
                                    userMap.put("group",group);
                                    userMap.put("steps",String.valueOf(total));
                                    calculateTop();
                                    top = calculateTop();
                                    userMap.put("top",String.valueOf(top));
                                    nFirestore.collection("user").document(mAuth.getCurrentUser().getUid()).set(userMap);
                                    steps.setText(String.valueOf(total));

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAGFit, "There was a problem getting the step count.", e);
                                }
                            });
        }
    }
    private int actual = 1;
    private int position = 0;
    private int calculateTop() {

        CollectionReference dbref = nFirestore.collection("user");

        dbref.orderBy("steps",Query.Direction.DESCENDING).limit(10).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                actual = 0;
               if (task.isSuccessful()){
                   for (QueryDocumentSnapshot document : task.getResult()){
                       actual++;
                       if (document.getId().equalsIgnoreCase(mAuth.getUid())){
                           position = actual;
                           Log.d("Mostrando datos", String.valueOf(document.getData().get("steps")));
                       }

                   }
               }
            }

        });
        return actual;
    }

    private String getGroup() {
       String group="";
       /* DocumentReference docRef = nFirestore.collection("user").document(mAuth.getCurrentUser().getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        Log.d("MOSTRANDO ", "DocumentSnapshot data: " + document.getData().get("group"));
                        grupo[0] = document.getData().get("group").toString();
                    }else{
                        grupo[0] = "nogroup";
                    }
                }
            }
        });*/
       if (mAuth.getCurrentUser().isAnonymous()){
           group = "guest";
       }
        return group;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
