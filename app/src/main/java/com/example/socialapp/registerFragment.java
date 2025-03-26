package com.example.socialapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.snackbar.Snackbar;
import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Session;
import io.appwrite.services.Account;

public class registerFragment extends Fragment {

    private Client client;
    private NavController navController;
    private Button registerButton;
    private EditText usernameEditText, emailEditText, passwordEditText;

    public registerFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registrer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID));

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        registerButton = view.findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> crearCuenta());
    }

    private void crearCuenta() {
        if (!validarFormulario()) return;

        registerButton.setEnabled(false);
        Account account = new Account(client);

        try {
            account.create(
                    "unique()",
                    emailEditText.getText().toString(),
                    passwordEditText.getText().toString(),
                    usernameEditText.getText().toString(),
                    new CoroutineCallback<>((user, error) -> {
                        if (error != null) {
                            mostrarError("Error en registro: " + error.getMessage());
                            return;
                        }
                        crearSesion(account);
                    })
            );
        } catch (AppwriteException e) {
            mostrarError(e.getMessage());
        }
    }

    private void crearSesion(Account account) {
        account.createEmailPasswordSession(
                emailEditText.getText().toString(),
                passwordEditText.getText().toString(),
                new CoroutineCallback<>((session, error) -> {
                    registerButton.setEnabled(true);

                    if (error != null) {
                        mostrarError("Error iniciando sesión: " + error.getMessage());
                        return;
                    }

                    new Handler(Looper.getMainLooper()).post(() ->
                            navController.navigate(R.id.homeFragment)
                    );
                })
        );
    }

    private boolean validarFormulario() {
        boolean valid = true;

        if (TextUtils.isEmpty(usernameEditText.getText().toString())) {
            usernameEditText.setError("Requerido");
            valid = false;
        }

        if (TextUtils.isEmpty(emailEditText.getText().toString())) {
            emailEditText.setError("Requerido");
            valid = false;
        }

        if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
            passwordEditText.setError("Requerido");
            valid = false;
        }

        return valid;
    }

    private void mostrarError(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() -> {
            registerButton.setEnabled(true);
            Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
        });
    }
}