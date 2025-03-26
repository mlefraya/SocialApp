package com.example.socialapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.IOException;

public class newPostFragment extends Fragment {

    private Button publishButton;
    private ImageView profileImageView; // Vista para mostrar la foto de perfil
    private NavController navController;
    private Uri mediaUri;
    private String mediaTipo;
    private AppViewModel appViewModel;

    // Launcher para seleccionar imagen desde la galería (devuelve Uri)
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            mediaTipo = "image";
                            handleMediaSelection(uri);
                        }
                    });

    // Launcher para tomar foto con la cámara (utiliza TakePicture(), que devuelve boolean)
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    isSuccess -> {
                        if (isSuccess && mediaUri != null) {
                            mediaTipo = "image";
                            handleMediaSelection(mediaUri);
                        }
                    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        profileImageView = view.findViewById(R.id.previsualizacion);
        publishButton = view.findViewById(R.id.publishButton);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Configura click para seleccionar imagen desde la galería
        view.findViewById(R.id.imagen_galeria).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        // Configura click para tomar foto con la cámara
        view.findViewById(R.id.camara_fotos).setOnClickListener(v -> tomarFoto());

        // Observa cambios en la imagen seleccionada para actualizar la vista
        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media -> {
            if (media != null) {
                updateMediaPreview(media);
            }
        });
    }

    // Método para tomar foto con la cámara
    private void tomarFoto() {
        try {
            File tempDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (tempDir == null) {
                showError("No se pudo acceder al almacenamiento");
                return;
            }
            File tempFile = File.createTempFile("IMG_" + System.currentTimeMillis(), ".jpg", tempDir);
            mediaUri = FileProvider.getUriForFile(requireContext(), "com.example.socialapp.fileprovider", tempFile);
            cameraLauncher.launch(mediaUri);
        } catch (IOException e) {
            showError("Error creando archivo temporal: " + e.getMessage());
        }
    }

    // Método que guarda la imagen seleccionada en el ViewModel
    private void handleMediaSelection(Uri uri) {
        if (uri != null) {
            appViewModel.setMediaSeleccionado(uri, mediaTipo);
        }
    }

    // Actualiza la vista de la foto de perfil con Glide
    private void updateMediaPreview(AppViewModel.Media media) {
        mediaUri = media.uri;
        mediaTipo = media.tipo;
        Glide.with(this)
                .load(media.uri)
                .into(profileImageView);
    }

    private void showError(String mensaje) {
        Snackbar.make(requireView(), mensaje, Snackbar.LENGTH_LONG).show();
    }
}
