package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class RegisterActivity extends AppCompatActivity {

    private Button createAccountButton;
    private TextInputLayout userEmailLayout;
    private TextInputLayout userPasswordLayout;
    private TextInputLayout userConfirmPasswordLayout;
    private TextView login;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference dbReference;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        dbReference = FirebaseDatabase.getInstance().getReference();
        setupUI();

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToLogin();
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }

    private void setupUI(){
        createAccountButton = findViewById(R.id.account_button);
        userEmailLayout = findViewById(R.id.reg_email);
        userPasswordLayout = findViewById(R.id.reg_password);
        userConfirmPasswordLayout = findViewById(R.id.reg_password_confirm);
        login = findViewById(R.id.have_account);

        progressDialog = new ProgressDialog(this);
    }


    private void createNewAccount(){
        String email = userEmailLayout.getEditText().getText().toString();
        String password = userPasswordLayout.getEditText().getText().toString();

        if(confirmInput()){

            progressDialog.setTitle("Creating New Account");
            progressDialog.setMessage("Please wait.");
            progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.show();

            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){

                        String deviceToken = FirebaseInstanceId.getInstance().getToken();

                        String userId = firebaseAuth.getCurrentUser().getUid();
                        dbReference.child("Users").child(userId).setValue("");
                        dbReference.child("Users").child(userId).child("device_token").setValue(deviceToken)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                SendUserToMain();
                                Toast.makeText(RegisterActivity.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });
                    }
                 /*   else {
                        String message = task.getException().toString();
                        Toast.makeText(RegisterActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    } */

                }
            });
        }
    }

    private void SendUserToLogin(){
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }

    private void SendUserToMain(){
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private boolean validateEmail() {
        String emailInput = userEmailLayout.getEditText().getText().toString().trim();

        if(emailInput.isEmpty()){
            userEmailLayout.getEditText().setError("Field can't be empty");
            return false;
        }

        else if(!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            userEmailLayout.getEditText().setError("Please enter a valid email address");
            return false;
        }
        else {
            userEmailLayout.getEditText().setError(null);
            return true;
        }
    }

    private boolean validatePasswords() {
        String passwordInput = userPasswordLayout.getEditText().getText().toString().trim();
        String passwordInputConfirm = userConfirmPasswordLayout.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            userPasswordLayout.getEditText().setError("Field can't be empty");
            return false;
        }
        if(passwordInputConfirm.isEmpty()){
            userConfirmPasswordLayout.getEditText().setError("Field can't be empty");
            return false;
        }
        if(!passwordInput.equals(passwordInputConfirm) && !passwordInput.isEmpty() && !passwordInputConfirm.isEmpty()){
            userConfirmPasswordLayout.getEditText().setError("Passwords not match");
            return false;
        }
        else {
            userPasswordLayout.getEditText().setError(null);
            userConfirmPasswordLayout.getEditText().setError(null);
            return true;
        }
    }

    public boolean confirmInput() {
        if (!validateEmail() | !validatePasswords()){
            return false;
        }
        return true;
    }
}
