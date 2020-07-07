package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity {

    private TextInputLayout phoneLayout, codeLayout;
    private Button sendVerificationButton;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_login);

        FirebaseApp.initializeApp(this);

        userIsLoggedIn();

        phoneLayout = findViewById(R.id.login_phone);
        codeLayout = findViewById(R.id.login_code);
        sendVerificationButton = findViewById(R.id.send_code);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

            }

            @Override
            public void onCodeSent(String verification, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verification, forceResendingToken);
                verificationId = verification;
                sendVerificationButton.setText("Verify Code");
            }
        };

        sendVerificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(verificationId != null){
                    verifyPhoneWithCode();
                }
                else{
                    startPhoneNumberVerification();
                }
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(user != null){
                        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()){
                                    String deviceToken = FirebaseInstanceId.getInstance().getToken();

                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("phone", user.getPhoneNumber());
                                    //userMap.put("name", user.getPhoneNumber());
                                    userMap.put("uid", user.getUid());
                                    userMap.put("device_token", deviceToken);

                                    userRef.updateChildren(userMap);
                                }
                                userIsLoggedIn();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
        });
    }

    private void userIsLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startPhoneNumberVerification() {
        String number = phoneLayout.getEditText().getText().toString();
        PhoneAuthProvider.getInstance().verifyPhoneNumber(number, 60, TimeUnit.SECONDS, this, mCallbacks);
    }

    private void verifyPhoneWithCode(){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, codeLayout.getEditText().getText().toString());
        signInWithPhoneAuthCredential(credential);
    }

}
