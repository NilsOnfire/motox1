package com.sena_app.motox1;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserLoginRegisterActivity extends AppCompatActivity {

    private TextView userLoginText;
    private Button userLoginBtn;
    private TextView userLoginLink;
    private Button userRegisterBtn;
    private EditText userEmail;
    private EditText userPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference customerDatabaseRef;
    private String onlineCustomerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login_register);

        mAuth = FirebaseAuth.getInstance();



        userLoginText = (TextView) findViewById(R.id.userLoginText);
        userLoginBtn = (Button) findViewById(R.id.userLoginBtn);
        userLoginLink = (TextView) findViewById(R.id.userRegisterLink);
        userRegisterBtn = (Button) findViewById(R.id.userRegisterBtn);
        userEmail = (EditText) findViewById(R.id.userEmail);
        userPassword = (EditText) findViewById(R.id.userPassword);

        loadingBar = new ProgressDialog(this);


        userLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userLoginLink.setVisibility(View.INVISIBLE);
                userLoginBtn.setVisibility(View.INVISIBLE);
                userLoginText.setText("Registro de usuarios");
                userRegisterBtn.setVisibility(View.VISIBLE);
                userRegisterBtn.setEnabled(true);
            }
        });

        userRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = userEmail.getText().toString();
                String password = userPassword.getText().toString();

                userRegister(email, password);

            }
        });

        userLoginBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                String email = userEmail.getText().toString();
                String password = userPassword.getText().toString();

                signInUser(email, password);
            }
        });

    }


    private void signInUser(String email, String password) {
        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(UserLoginRegisterActivity.this, "Por favor escriba su Email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password))
        {
            Toast.makeText(UserLoginRegisterActivity.this, "Por favor escriba su clave...", Toast.LENGTH_SHORT).show();
        }

        else
        {
            loadingBar.setTitle("Login Usuario");
            loadingBar.setMessage("Por favor espere mientras validamos sus credenciales.");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful())
                    {
                        Intent driverIntent = new Intent(UserLoginRegisterActivity.this, CustomerMapActivity.class);
                        startActivity(driverIntent);

                        Toast.makeText(UserLoginRegisterActivity.this, "Validacion exitosa!!", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }
                    else
                    {
                        Toast.makeText(UserLoginRegisterActivity.this, "Validacion fallida, intente de nuevo por favor!!.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }
                }
            });
        }



    }


    private void userRegister(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(UserLoginRegisterActivity.this, "Por favor escriba su Email...", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(UserLoginRegisterActivity.this, "Por favor digite una clave...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Registrando usuario");
            loadingBar.setMessage("Por favor espere mientras registramos sus datos.");
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {

                        onlineCustomerID = mAuth.getCurrentUser().getUid();
                        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users")
                                .child("Customers").child(onlineCustomerID);

                        customerDatabaseRef.setValue(true);

                        Intent driverIntent = new Intent(UserLoginRegisterActivity.this, CustomerMapActivity.class);
                        startActivity(driverIntent);

                        Toast.makeText(UserLoginRegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();


                    } else {
                        Toast.makeText(UserLoginRegisterActivity.this, "Intente de nuevo, ha ocurrido durante el registro.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }
                }
            });
        }

    }
}