package com.example.socialapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.InputFile;
import io.appwrite.models.Session;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Storage;

public class registerFragment extends Fragment {

    private Client client;
    private NavController navController;
    private Button registerButton, btnSeleccionarFoto;
    private EditText usernameEditText, emailEditText, passwordEditText;
    private ImageView previewFoto;
    private Uri fotoUri;
    private Storage storage;
    private final ActivityResultLauncher<String> galeria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    fotoUri = uri;
                    Glide.with(this).load(uri).into(previewFoto);
                }
            });

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
        storage = new Storage(client);

        usernameEditText = view.findViewById(R.id.usernameEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        registerButton = view.findViewById(R.id.registerButton);
        btnSeleccionarFoto = view.findViewById(R.id.btnSeleccionarFoto);
        previewFoto = view.findViewById(R.id.previewFoto);

        btnSeleccionarFoto.setOnClickListener(v -> galeria.launch("image/*"));
        registerButton.setOnClickListener(v -> crearCuenta());
    }

    private void crearCuenta() {
        if (!validarFormulario()) return;

        registerButton.setEnabled(false);
        Account account = new Account(client);

        if (fotoUri != null) {
            subirFotoYRegistrar(account);
        } else {
            registrarUsuario(account, null);
        }
    }

    private void subirFotoYRegistrar(Account account) {
        try {
            File tempFile = getFileFromUri(requireContext(), fotoUri);
            storage.createFile(
                    "perfiles", // Bucket ID para fotos de perfil
                    "unique()",
                    InputFile.Companion.fromFile(tempFile),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mostrarError("Error subiendo foto: " + error.getMessage());
                            return;
                        }

                        String fotoUrl = "https://cloud.appwrite.io/v1/storage/buckets/perfiles/files/"
                                + result.getId() + "/view?project=" + getString(R.string.APPWRITE_PROJECT_ID);
                        registrarUsuario(account, fotoUrl);
                    })
            );
        } catch (Exception e) {
            mostrarError("Error procesando la imagen: " + e.getMessage());
        }
    }

    private void registrarUsuario(Account account, String fotoUrl) {
        try {
            account.create(
                    "unique()",
                    emailEditText.getText().toString(),
                    passwordEditText.getText().toString(),
                    usernameEditText.getText().toString(),
                    new CoroutineCallback<>((user, error) -> {
                        if (error != null) {
                            mostrarError("Error registro: " + error.getMessage());
                            return;
                        }

                        if (fotoUrl != null) {
                            actualizarFotoPerfil(account, user.getId(), fotoUrl);
                        }

                        crearSesion(account);
                    })
            );
        } catch (AppwriteException e) {
            mostrarError(e.getMessage());
        }
    }

    private void actualizarFotoPerfil(Account account, String userId, String fotoUrl) {
        try {
            account.updatePrefs(
                    new HashMap<String, Object>() {{
                        put("profilePhoto", fotoUrl);
                    }},
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            System.err.println("Error guardando foto de perfil: " + error.getMessage());
                        }
                    })
            );
        } catch (AppwriteException e) {
            e.printStackTrace();
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

                    new Handler(Looper.getMainLooper()).post(() -> {
                        navController.navigate(R.id.homeFragment);
                    });
                })
        );
    }

    private File getFileFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = new File(context.getCacheDir(), getFileName(context, uri));
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        outputStream.close();
        inputStream.close();
        return tempFile;
    }

    private String getFileName(Context context, Uri uri) {
        String fileName = "temp_image";
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
        }
        return fileName;
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