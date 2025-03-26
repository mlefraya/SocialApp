package com.example.socialapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Storage;
import io.appwrite.models.InputFile;

public class profileFragment extends Fragment {

    private ImageView photoImageView;
    private Button btnCambiarFoto;
    private Client client;
    private Account account;
    private Storage storage;
    private Uri nuevaFotoUri;
    private ActivityResultLauncher<String> galeriaLauncher;

    public profileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = new Client(requireContext())
                .setProject(getString(R.string.APPWRITE_PROJECT_ID));

        account = new Account(client);
        storage = new Storage(client);

        photoImageView = view.findViewById(R.id.photoImageView);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);
        TextView displayNameTextView = view.findViewById(R.id.displayNameTextView);
        TextView emailTextView = view.findViewById(R.id.emailTextView);

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        nuevaFotoUri = uri;
                        actualizarFotoPerfil();
                    }
                });

        cargarDatosUsuario(displayNameTextView, emailTextView);

        btnCambiarFoto.setOnClickListener(v -> galeriaLauncher.launch("image/*"));
    }

    private void cargarDatosUsuario(TextView nameView, TextView emailView) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        try {
            account.get(new CoroutineCallback<>((user, error) -> {
                if (error != null) {
                    mostrarError("Error cargando datos: " + error.getMessage());
                    return;
                }

                mainHandler.post(() -> {
                    nameView.setText(user.getName());
                    emailView.setText(user.getEmail());

                    Map<String, Object> prefs = user.getPrefs().getData();
                    if (prefs != null && prefs.containsKey("profilePhoto")) {
                        String fotoUrl = prefs.get("profilePhoto").toString();
                        Glide.with(requireContext())
                                .load(fotoUrl)
                                .circleCrop()
                                .error(R.drawable.user)
                                .into(photoImageView);
                    }
                });
            }));
        } catch (AppwriteException e) {
            mostrarError(e.getMessage());
        }
    }

    private void actualizarFotoPerfil() {
        try {
            File tempFile = getFileFromUri(requireContext(), nuevaFotoUri);

            storage.createFile(
                    getString(R.string.APPWRITE_STORAGE_BUCKET_ID),
                    "unique()",
                    InputFile.Companion.fromFile(tempFile),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mostrarError("Error subiendo foto: " + error.getMessage());
                            return;
                        }

                        String nuevaUrl = "https://cloud.appwrite.io/v1/storage/buckets/" +
                                getString(R.string.APPWRITE_STORAGE_BUCKET_ID) +
                                "/files/" + result.getId() +
                                "/view?project=" + getString(R.string.APPWRITE_PROJECT_ID);

                        actualizarPrefsUsuario(nuevaUrl);
                    })
            );
        } catch (Exception e) {
            mostrarError("Error procesando imagen: " + e.getMessage());
        }
    }

    private void actualizarPrefsUsuario(String fotoUrl) {
        try {
            Map<String, Object> newPrefs = new HashMap<>();
            newPrefs.put("profilePhoto", fotoUrl);

            account.updatePrefs(
                    newPrefs,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            mostrarError("Error actualizando perfil: " + error.getMessage());
                            return;
                        }

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Glide.with(requireContext())
                                    .load(fotoUrl)
                                    .circleCrop()
                                    .into(photoImageView);
                            Snackbar.make(requireView(), "¡Foto actualizada!", Snackbar.LENGTH_SHORT).show();
                        });
                    })
            );
        } catch (AppwriteException e) {
            mostrarError(e.getMessage());
        }
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

    private void mostrarError(String mensaje) {
        new Handler(Looper.getMainLooper()).post(() ->
                Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show()
        );
    }
}