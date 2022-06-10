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

public class DriverLoginRegisterActivity extends AppCompatActivity {
    private TextView driverLoginText;
    private Button driverLoginBtn;
    private TextView driverLoginLink;
    private Button driverRegisterBtn;
    private EditText driverEmail;
    private EditText driverPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);
        mAuth = FirebaseAuth.getInstance();

        driverLoginText= (TextView) findViewById(R.id.driverLoginText);
        driverLoginBtn = (Button) findViewById(R.id.driverLoginBtn);
        driverLoginLink = (TextView) findViewById(R.id.driverRegisterLink);
        driverRegisterBtn = (Button) findViewById(R.id.driverRegisterBtn);
        driverEmail = (EditText) findViewById(R.id.driverEmail);
        driverPassword = (EditText) findViewById(R.id.driverPassword);
        loadingBar = new ProgressDialog(this);

        driverLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                driverLoginLink.setVisibility(View.INVISIBLE);
                driverLoginBtn.setVisibility(View.INVISIBLE);
                driverLoginText.setText("Registro de conductor");

                driverRegisterBtn.setVisibility(View.VISIBLE);
                driverRegisterBtn.setEnabled(true);
            }
        });

        driverRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();

                driverRegister(email, password);

            }
        });

        driverLoginBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                String email = driverEmail.getText().toString();
                String password = driverPassword.getText().toString();

                signInDriver(email, password);
            }
        });

    }

    private void signInDriver(String email, String password) {
        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(DriverLoginRegisterActivity.this, "Por favor escriba su Email...", Toast.LENGTH_SHORT).show();


        }

        if(TextUtils.isEmpty(password))
        {
            Toast.makeText(DriverLoginRegisterActivity.this, "Por favor escriba su clave...", Toast.LENGTH_SHORT).show();
        }

        else
        {
            loadingBar.setTitle("Login conductor");
            loadingBar.setMessage("Por favor espere mientras validamos sus credenciales.");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful())
                    {
                        Toast.makeText(DriverLoginRegisterActivity.this, "Validacion exitosa!!", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                        Intent driverIntent = new Intent(DriverLoginRegisterActivity.this,DriversMapActivity.class);
                       startActivity(driverIntent);
                    }
                    else
                    {
                        Toast.makeText(DriverLoginRegisterActivity.this, "Validacion fallida, intente de nuevo por favor!!.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }
                }
            });
        }

    }

    private void driverRegister(String email, String password) {
        if(TextUtils.isEmpty(email))
        {
            Toast.makeText(DriverLoginRegisterActivity.this, "Por favor escriba su Email...", Toast.LENGTH_SHORT).show();


        }

        if(TextUtils.isEmpty(password))
        {
            Toast.makeText(DriverLoginRegisterActivity.this, "Por favor escriba su clave...", Toast.LENGTH_SHORT).show();
        }

        else
        {
            loadingBar.setTitle("Registrando conductor");
            loadingBar.setMessage("Por favor espere mientras registramos sus datos.");
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful())
                    {
                        Toast.makeText(DriverLoginRegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                       Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriversMapActivity.class);
                        startActivity(driverIntent);
                    }
                    else
                    {
                        Toast.makeText(DriverLoginRegisterActivity.this, "Intemte de nuevo , error ocurrido durante el registro.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                    }
                }
            });
        }

    }
}