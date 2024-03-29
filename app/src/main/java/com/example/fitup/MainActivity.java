package com.example.fitup;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;


public class MainActivity extends AppCompatActivity implements  SensorEventListener {
    private FirebaseFirestore nFirestore;
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    boolean running = false;
    WriteBatch batch ;
    private Button btnLogOut;
    private Button settings;
    TextView steps;
    TextView posIni;
    TextView posFin;
    SensorManager sensorManager;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    User u;
    Grupo g ;
    private static final String TAGFit = "FitActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        nFirestore = FirebaseFirestore.getInstance();
        u = new User();
        g = new Grupo();
        setContentView(R.layout.activity_main);

        btnLogOut = findViewById(R.id.logOutBtn);
        settings = findViewById(R.id.ajustesBtn);

        posIni = findViewById(R.id.tfIni);
        posFin = findViewById(R.id.tfFin);

        batch = nFirestore.batch();
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
        if (mAuth.getCurrentUser().isAnonymous()){
            settings.setEnabled(false);
        }
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Settings.class));
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
        /*final DocumentReference dr = nFirestore.collection("User").document(mAuth.getCurrentUser().getUid());*/
        if (running){
            steps = findViewById(R.id.steps);
            final Map<String,Object> userMap = new HashMap<>();
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
                                    getGroup();
                                    u.setSteps(String.valueOf(total));
                                    u.setTop("10");
                                    u.setUid(mAuth.getCurrentUser().getUid());
                                    getTopPos();

                                    //u.setGroup("group");
                                    /*userMap.get("group") = getGroup().get("group");*/
                                    //userMap.put("group",getGroup().get("group"));
                                    //userMap.put("steps",String.valueOf(total));
                                    /*batch.update(dr,"steps",String.valueOf(total));*/
                                    //calculateTop();
                                    //top = calculateTop();
                                    //userMap.put("top",String.valueOf(top));
                                    nFirestore.collection("user").document(mAuth.getCurrentUser().getUid()).set(u);
                                    //nFirestore.collection("User").document(mAuth.getCurrentUser().getUid()).set(getGroup().get("group"));
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

    private void getTopPos() {
        CollectionReference cUser = nFirestore.collection("user");
        //W/Firestore: (0.6.6-dev) [Firestore]: Listen for Query(user where group == guest order by -steps, -__name__)
        // failed: Status{code=FAILED_PRECONDITION, description=The query requires an index. You can create it here:
        // https://console.firebase.google.com/project/fitup-5768f/database/firestore/indexes?create_index=EgR1c2VyGgkKBWdyb3VwEAIaCQoFc3RlcHMQAxoMCghfX25hbWVfXxAD, cause=null}
        // Añadir esto para determinar una consulta compuesta
        cUser.whereEqualTo("group",u.getGroup()).orderBy("steps",Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            QuerySnapshot document = task.getResult();
                            posFin.setText(String.valueOf(document.size()));
                            int x = 0;
                            for (QueryDocumentSnapshot d : task.getResult()){
                                User us =  d.toObject(User.class);
                                //System.out.println(us.getUid());
                                x++;
                                if (us.getUid() != null && us.getUid().equalsIgnoreCase(mAuth.getUid())){
                                    u.setTop(String.valueOf(x));
                                    posIni.setText(String.valueOf(x));
                                }

                            }
                        }else{
                            System.out.println("SUCCEDED");
                        }
                    }
                });



    }

    private int actual = 1;
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
                          // Log.d("Mostrando datos", String.valueOf(document.getData().get("steps")));
                       }

                   }
               }
            }

        });
        return actual;
    }

    private void getGroup() {
    //final Map<String, Object> datos = new HashMap<String,Object>();

       if (mAuth.getCurrentUser().isAnonymous()){
           g.setGroup("guest");
           u.setGroup(g.getGroup());
           //nFirestore.collection("user").document(mAuth.getCurrentUser().getUid()).set(g);
       }else{

           DocumentReference docRef = nFirestore.collection("usergroup").document(mAuth.getCurrentUser().getUid());
           docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

               @Override
               public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                   if (task.isSuccessful()){
                       DocumentSnapshot document = task.getResult();
                       g = document.toObject(Grupo.class);
                       u.setGroup(g.getGroup());
                       nFirestore.collection("user").document(mAuth.getCurrentUser().getUid()).update("group",g.getGroup()).addOnSuccessListener(new OnSuccessListener<Void>() {
                           @Override
                           public void onSuccess(Void aVoid) {
                               //Toast.makeText(MainActivity.this, "Updateado", Toast.LENGTH_SHORT).show();
                           }
                       });
                   }
               }
           });
       }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
