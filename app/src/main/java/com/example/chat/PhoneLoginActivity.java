package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chat.util.Util;
import com.goodiebag.pinview.Pinview;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.hbb20.CountryCodePicker;
import com.onesignal.OneSignal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rezwan.pstu.cse12.view.CircularMorphLayout;

public class PhoneLoginActivity extends AppCompatActivity implements View.OnClickListener{

    private TextInputLayout phoneLayout;
    private TextInputLayout codeLayout;
    //private EditText phoneText;
    private Button verifyButton;
    private Button sendCodeButton;
    private LinearLayout layout;

    //private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private String verificationID, notificationKey;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth firebaseAuth;

    private ProgressDialog dialog;
    //private CountryCodePicker ccp;
    private String selected_country_code;

    public ProgressDialog progressDialog;
    private ViewGroup phoneNumberViews;
    private ViewGroup verifyViews;
    private CircularMorphLayout cmLayout,cmlVerifyLayout;
    private CountryCodePicker ccp;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private boolean isVerificationInProgress = false;
    private String mVerificationId,mPhoneNumber,myCCP;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private TextView mDetailText,send,tvPhoneNumber;
    private ImageView editPhoneNumber;
    private EditText mPhoneNumberField;
    //private EditText mVerificationField;
    private RelativeLayout layoutRegistration,layoutVerification;

    private TextView resend, verify;
    //private Button mSignOutButton;
    ProgressBar progressBar, progressBarVerify;
    Pinview otp;
    CountDownTimer countdownTimer;

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
        firebaseAuth = FirebaseAuth.getInstance();

        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBarVerify=(ProgressBar)findViewById(R.id.progressbar2);
        layoutRegistration = (RelativeLayout) findViewById(R.id.reg_layout);
        layoutVerification =(RelativeLayout)findViewById(R.id.verify_layout);
        //mDetailText = (TextView) findViewById(R.id.detail);
        //tvPhoneNumber =(TextView)findViewById(R.id.tv_phone_number);
        mPhoneNumberField = (EditText) findViewById(R.id.phone);
        //mVerificationField = (EditText) findViewById(R.id.field_verification_code);
        otp = findViewById(R.id.pinview);
        ccp = findViewById(R.id.ccp);
        cmLayout = (CircularMorphLayout)findViewById(R.id.circular);
        cmlVerifyLayout=(CircularMorphLayout)findViewById(R.id.circular2);
        send = findViewById(R.id.send);
        verify = findViewById(R.id.verify);
        //editPhoneNumber = (ImageView)findViewById(R.id.ib_edit_number);
        resend =findViewById(R.id.not_receive);

        resend.setOnClickListener(this);
        send.setOnClickListener(this);
        verify.setOnClickListener(this);

     /*   firebaseAuth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);

        setupUI();

        ccp = findViewById(R.id.ccp);
        ccp.setOnCountryChangeListener(new CountryCodePicker.OnCountryChangeListener() {
            @Override
            public void onCountrySelected() {
                //Alert.showMessage(RegistrationActivity.this, ccp.getSelectedCountryCodeWithPlus());
                selected_country_code = ccp.getSelectedCountryCodeWithPlus();
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCodeButton.setVisibility(View.INVISIBLE);
                layout.setVisibility(View.INVISIBLE);
                String verificationCode = codeLayout.getEditText().getText().toString();
                if(validateCode()){
                    dialog.setTitle("Verification Code");
                    dialog.setMessage("Please wait, while we are verifying verification code.");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationID, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });

        sendCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validatePhone()){
                    dialog.setTitle("Phone Verification");
                    dialog.setMessage("Please wait, while we are authenticating your phone...");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                    String phoneNumber = phoneLayout.getEditText().getText().toString().trim();
                    final String fullNumber = selected_country_code+phoneNumber;

                    Log.d("Mydebug", fullNumber);
                    if(validatePhone()){
                        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");
                        userRef.orderByChild("phone").equalTo(fullNumber).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                                        String id = ds.child("uid").getValue().toString();
                                        dialog.dismiss();
                                        Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
                                        startActivity(mainIntent);
                                        finish();
                                    }

                                }
                                else {
                                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                            fullNumber,        // Phone number to verify
                                            60,                 // Timeout duration
                                            TimeUnit.SECONDS,   // Unit of timeout
                                            PhoneLoginActivity.this,               // Activity (for callback binding)
                                            callbacks);        // OnVerificationStateChangedCallbacks
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

                }

            }
        }); */

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d("Mydebug", "in onVerificationCompleted " );

                isVerificationInProgress = false;
                signInWithPhoneAuthCredential(credential);
                updateUI(STATE_VERIFY_SUCCESS, credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
              /*  dialog.dismiss();
                sendCodeButton.setVisibility(View.VISIBLE);
                layout.setVisibility(View.VISIBLE);
                codeLayout.setVisibility(View.INVISIBLE);
                verifyButton.setVisibility(View.INVISIBLE);*/
                isVerificationInProgress = false;
                setStartProgressVisibility(false);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumberField.setError("Invalid phone number.");
                    layoutVerification.setVisibility(View.GONE);
                    layoutRegistration.setVisibility(View.VISIBLE);
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    showSnackBar(R.string.msg_sms_verification_limit_exceeded);
                    layoutVerification.setVisibility(View.GONE);
                    layoutRegistration.setVisibility(View.VISIBLE);
                }
                else {
                    showSnackBar(R.string.msg_encountered_an_unexpected_error);
                }
                updateUI(STATE_VERIFY_FAILED);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d("Mydebug", "in onCodeSent " );
                mVerificationId = verificationId;
                mResendToken = token;
                showSnackBar("Code has been sent, Please check and verify");
                updateUI(STATE_CODE_SENT);
             /*   sendCodeButton.setVisibility(View.INVISIBLE);
                layout.setVisibility(View.INVISIBLE);
                codeLayout.setVisibility(View.VISIBLE);
                verifyButton.setVisibility(View.VISIBLE);

                dialog.dismiss();

                // Save verification ID and resending token so we can use them later
                verificationID = verificationId;
                resendToken = token;
                Toast.makeText(PhoneLoginActivity.this, "Code has been sent, Please check and verify", Toast.LENGTH_SHORT).show(); */
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, isVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        Log.d("Mydebug", "in startPhoneNumberVerification " + isVerificationInProgress);

        if (!isVerificationInProgress){
            Log.d("Mydebug", "in " );
            setStartProgressVisibility(true);

            mPhoneNumber = phoneNumber;
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    callbacks);        // OnVerificationStateChangedCallbacks

            isVerificationInProgress = true;
        }else {
            showSnackBar("Please wait! Verification already in progress....");
        }
    }


    public void showSnackBar(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void showSnackBar(int messageId) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                messageId, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void setStartProgressVisibility(boolean isVisible) {
        if (isVisible) {
            cmLayout.revealFrom(send.getWidth() / 2f,
                    send.getHeight() / 2f,
                    send.getWidth() / 2f,
                    send.getHeight() / 2f).setListener(
                    () -> {
                        send.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                    }).start();
        } else {
            send.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            cmLayout.reverse();
        }
    }

    private void setVerifyProgressVisibility(boolean isVisible) {
        if (isVisible) {
            cmlVerifyLayout.revealFrom(verify.getWidth() / 2f,
                    verify.getHeight() / 2f,
                    verify.getWidth() / 2f,
                    verify.getHeight() / 2f).setListener(
                    () -> {
                        verify.setVisibility(View.GONE);
                        progressBarVerify.setVisibility(View.VISIBLE);
                    }).start();
        } else {
            verify.setVisibility(View.VISIBLE);
            progressBarVerify.setVisibility(View.GONE);
            cmlVerifyLayout.reverse();
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d("Mydebug", "in signInWithPhoneAuthCredential " );

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    //updateUI(STATE_SIGNIN_SUCCESS);
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    Log.d("mydebug", "user: " + user);
                    if (user != null) {
                        final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                setVerifyProgressVisibility(false);
                                if (!dataSnapshot.exists()) {
                                    String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                    //OneSignal.startInit(PhoneLoginActivity.this).init();
                                    //OneSignal.setSubscription(true);
                                    /*OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                                        @Override
                                        public void idsAvailable(String userId, String registrationId) {
                                            notificationKey = userId;
                                        }
                                    });*/

                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("phone", user.getPhoneNumber());
                                    userMap.put("uid", user.getUid());
                                    userMap.put("device_token", deviceToken);
                                    userMap.put("notificationKey", notificationKey);

                                    userRef.updateChildren(userMap);

                                    //OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);

                                    //dialog.dismiss();
                                    Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
                                    startActivity(mainIntent);
                                    finish();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                setVerifyProgressVisibility(false);
                                //setTitle(getString(R.string.verification));
                                layoutVerification.setVisibility(View.VISIBLE);
                                layoutRegistration.setVisibility(View.GONE);

                                showSnackBar(R.string.msg_encountered_an_unexpected_error);
                            }
                        });
                    }


                } else {
                    updateUI(STATE_SIGNIN_FAILED);
                    String message = task.getException().toString();
                    Toast.makeText(PhoneLoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startCountdown() {
        tvPhoneNumber.setText(mPhoneNumber);
        setResendButtonEnabled(false);
        countdownTimer = new CountDownTimer(60 * 1000, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                setResendButtonTimerCount(millisUntilFinished / 1000);
            }

            @Override public void onFinish() {
                setResendButtonEnabled(true);
            }
        }.start();
    }

    public void setResendButtonEnabled(boolean isEnabled) {
        if (isEnabled) {
            resend.setEnabled(true);
            resend.setText("Resend");
        } else {
            resend.setEnabled(false);
        }
    }

    private void setResendButtonTimerCount(long secondsRemaining) {
        resend.setText(
                String.format(Locale.ENGLISH, "Resend code timer", secondsRemaining));

    }

    private boolean validatePhone() {
        String phoneNumber = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Invalid phone number.");
            //mPhoneNumberField.setTextColor(Color.parseColor("#ff1744"));
            return false;
        }
        if(!Patterns.PHONE.matcher(phoneNumber).matches()){
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }

        return true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void updateUI(int uiState) {
        updateUI(uiState, firebaseAuth.getCurrentUser(), null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState,null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {

            case STATE_INITIALIZED:
                mDetailText.setText("Please verify your phone first!");
                break;

            case STATE_CODE_SENT:
                //progressBar.setVisibility(View.INVISIBLE);
                setStartProgressVisibility(false);

                setTitle(getString(R.string.verification));
                layoutVerification.setVisibility(View.VISIBLE);
                layoutRegistration.setVisibility(View.GONE);
                startCountdown();

                mDetailText.setText("Code Sent");
                mDetailText.setTextColor(Color.parseColor("#43a047"));
                break;

            case STATE_VERIFY_FAILED:

                if (countdownTimer != null) {
                    countdownTimer.cancel();
                }

                setVerifyProgressVisibility(false);
                //mDetailText.setText("Verification failed");
                //mDetailText.setTextColor(Color.parseColor("#dd2c00"));
                progressBar.setVisibility(View.INVISIBLE);
                break;

            case STATE_VERIFY_SUCCESS:
                //mDetailText.setText("Verfication Sucessfull");
                //mDetailText.setTextColor(Color.parseColor("#43a047"));
                progressBar.setVisibility(View.INVISIBLE);
                Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        // mVerificationField.setText(cred.getSmsCode());
                        otp.setValue(cred.getSmsCode());
                    } else {
                        showSnackBar("Instant Validation");
                        //mVerificationField.setTextColor(Color.parseColor("#4bacb8"));
                    }
                }
                break;

            case STATE_SIGNIN_FAILED:
                // No-op, handled by sign-in check
                mDetailText.setText("Sign In Failed !");
                mDetailText.setTextColor(Color.parseColor("#dd2c00"));
                progressBar.setVisibility(View.INVISIBLE);
                setVerifyProgressVisibility(false);
                break;

            case STATE_SIGNIN_SUCCESS:
                // Np-op, handled by sign-in check
                //mStatusText.setText(R.string.signed_in);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send:
                if (hasInternetConnection()) {
                    String phone = mPhoneNumberField.getText().toString();
                    selected_country_code = ccp.getSelectedCountryCodeWithPlus();
                    if(validatePhone()){
                        Log.d("Mydebug", "in send click");
                        progressBar.setVisibility(View.VISIBLE);
                        String fullNumber = selected_country_code + phone;
                        Log.d("Mydebug", "in send- full number: " + fullNumber);
                        startPhoneNumberVerification(fullNumber);
                    }
                }
                else {
                    showSnackBar(R.string.msg_no_internet_connection);
                }
                break;

            case R.id.verify:
                String code = otp.getValue().toString();
                if(TextUtils.isEmpty(code)){
                    showSnackBar("Please enter the phone code");
                    return;
                }
                setVerifyProgressVisibility(true);
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;

            case R.id.not_receive:
                showSnackBar("The code resend.");
                startPhoneNumberVerification(mPhoneNumber);
                break;
        }

    }

  /*  private void setupUI(){
        phoneLayout = findViewById(R.id.login_phone);
        codeLayout = findViewById(R.id.verification_code);
        verifyButton = findViewById(R.id.login_button);
        sendCodeButton = findViewById(R.id.send_verification);
        //phoneText = findViewById(R.id.phone);
        layout = findViewById(R.id.layout);
    }


    private boolean validatePhone() {
        String phoneInput = phoneLayout.getEditText().getText().toString().trim();
        //String phoneInput = phoneText.getText().toString().trim();

        if (phoneInput.isEmpty()) {
            phoneLayout.getEditText().setError("Enter your Phone number");
            return false;
        }
        else {
            phoneLayout.getEditText().setError(null);
            return true;
        }
    }

    private boolean validateCode() {
        String codeInput = codeLayout.getEditText().getText().toString().trim();

        if (codeInput.isEmpty()) {
            codeLayout.getEditText().setError("Enter your verification code");
            return false;
        }
        else {
            codeLayout.getEditText().setError(null);
            return true;
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if(user != null){
                                final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUid());
                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(!dataSnapshot.exists()){
                                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                            OneSignal.startInit(PhoneLoginActivity.this).init();
                                            OneSignal.setSubscription(true);
                                            OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                                                @Override
                                                public void idsAvailable(String userId, String registrationId) {
                                                    notificationKey = userId;
                                                }
                                            });

                                            Map<String, Object> userMap = new HashMap<>();
                                            userMap.put("phone", user.getPhoneNumber());
                                            userMap.put("uid", user.getUid());
                                            userMap.put("device_token", deviceToken);
                                            userMap.put("notificationKey", notificationKey);

                                            userRef.updateChildren(userMap);

                                            OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);

                                            dialog.dismiss();
                                            Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
                                            startActivity(mainIntent);
                                            finish();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }


                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    } */
}
