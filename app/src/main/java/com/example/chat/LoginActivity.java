package com.example.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient googleSignInClient;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private ProgressDialog progressDialog;

    private Button loginButton;
    private Button phoneLoginButton;
    private SignInButton googleLoginButton;
    private TextInputLayout userEmailLayout;
    private TextInputLayout userPasswordLayout;
    private TextView forgetPassword;
    private TextView newAccount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this,gso);

        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        setupUI();

        newAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegister();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUserValid();
            }
        });

        phoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToPhoneLogin();
            }
        });

        forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Configure Google Sign In
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void setupUI(){
        loginButton = findViewById(R.id.login_button);
        phoneLoginButton = findViewById(R.id.login_phone);
        userEmailLayout = findViewById(R.id.login_email);
        userPasswordLayout = findViewById(R.id.login_password);
        forgetPassword = findViewById(R.id.forget_password);
        newAccount = findViewById(R.id.register);
        googleLoginButton = findViewById(R.id.login_google);

        progressDialog = new ProgressDialog(this);
    }

    private void showRecoverPasswordDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");
        LinearLayout linearLayout = new LinearLayout(this);
        final EditText emailEt = new EditText(this);
        emailEt.setHint("Enter your Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);
        builder.setView(linearLayout);

        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = emailEt.getText().toString().trim();
                progressDialog.setTitle("Sending Email...");
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.show();

                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if(task.isSuccessful()){
                            Toast.makeText(LoginActivity.this, "Email Recovery Password Sent.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void checkUserValid() {
        String email = userEmailLayout.getEditText().getText().toString();
        String password = userPasswordLayout.getEditText().getText().toString();

        if(confirmInput()){
            progressDialog.setTitle("Sing In");
            progressDialog.setMessage("Please wait...");
            progressDialog.setCanceledOnTouchOutside(true);
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    if(task.isSuccessful()){

                        String currentUserId = firebaseAuth.getCurrentUser().getUid();
                        String deviceToken = FirebaseInstanceId.getInstance().getToken();
                        userRef.child(currentUserId).child("device_token").setValue(deviceToken)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            SendUserToMain();
                                            Toast.makeText(LoginActivity.this, "Login successfully", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    }
                                });

                    } else{
                        String message = task.getException().toString();
                        Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }
            });
        }

    }


    private void SendUserToMain(){
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToPhoneLogin(){
        Intent phoneLoginIntent = new Intent(LoginActivity.this, PhoneLoginActivity.class);
        startActivity(phoneLoginIntent);
    }

    private void SendUserToRegister(){
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    private boolean validateEmail() {
        String emailInput = userEmailLayout.getEditText().getText().toString().trim();

        if (emailInput.isEmpty()) {
            userEmailLayout.getEditText().setError("");
            userEmailLayout.setError("Field can't be empty");
            return false;
        }
        else {
            userEmailLayout.getEditText().setError(null);
            userEmailLayout.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordInput = userPasswordLayout.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            userPasswordLayout.getEditText().setError("");
            userPasswordLayout.setError("Field can't be empty");
            return false;
        }
        else {
            userPasswordLayout.getEditText().setError(null);
            userPasswordLayout.setError(null);
            return true;
        }
    }

    public boolean confirmInput() {
        if (!validateEmail() | !validatePassword()){
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                            finish();
                            //updateUI(user);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }
}
