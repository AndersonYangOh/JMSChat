package com.nilskuijpers.jmschatter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RegistrationActivity extends AppCompatActivity {

    private EditText chosenName;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        chosenName = (EditText) findViewById(R.id.chosenName);
        registerButton = (Button) findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosenName.getText().toString() != null)
                {
                    Intent mainActivity = new Intent(RegistrationActivity.this,MainActivity.class);

                    mainActivity.putExtra("local_user", chosenName.getText().toString());

                    startActivity(mainActivity);
                }
            }
        });
    }
}
