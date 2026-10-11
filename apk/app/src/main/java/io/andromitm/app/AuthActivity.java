package io.andromitm.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

/** Écran de déclaration d'usage autorisé — obligatoire au premier lancement. */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs prefs = new Prefs(this);
        if (prefs.isAuthorized()) { goMain(); return; }
        setContentView(R.layout.activity_auth);

        CheckBox cb = findViewById(R.id.auth_check);
        Button accept = findViewById(R.id.auth_accept);
        Button decline = findViewById(R.id.auth_decline);
        accept.setEnabled(false);
        cb.setOnCheckedChangeListener((b, checked) -> accept.setEnabled(checked));
        accept.setOnClickListener(v -> {
            prefs.setAuthorized(true);
            goMain();
        });
        decline.setOnClickListener(v -> finishAffinity());
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
