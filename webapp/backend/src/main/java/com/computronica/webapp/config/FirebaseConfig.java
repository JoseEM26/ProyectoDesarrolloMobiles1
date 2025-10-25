package com.computronica.webapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        ClassPathResource resource = new ClassPathResource("computronica-2206d-firebase-adminsdk-fbsvc-b2bc910755.json");

        if (!resource.exists()) {
            throw new IllegalStateException("Archivo Firebase JSON no encontrado en src/main/resources/");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("computronica-2206d")
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp inicializado: {}", app.getName());
            return app;
        } else {
            FirebaseApp app = FirebaseApp.getInstance();
            log.info("FirebaseApp ya existe: {}", app.getName());
            return app;
        }
    }

    // ESTE ES EL BEAN QUE FALTABA
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}