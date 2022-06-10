package com.sena_app.motox1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {
    private Button userWelcomeBtn;
    private Button driverWelcomeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

         userWelcomeBtn= findViewById(R.id.userWelcomeBtn);
        driverWelcomeBtn = findViewById(R.id.driverWelcomeBtn);

        userWelcomeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent loginRegIntent = new Intent(WelcomeActivity.this,UserLoginRegisterActivity.class);
                startActivity(loginRegIntent);
            }

        });
        driverWelcomeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent loginRegIntent = new Intent(WelcomeActivity.this,DriverLoginRegisterActivity.class);
                startActivity(loginRegIntent);
            }

        });

    }
}