package com.securetask.backend.document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
class DocumentStorageService {

    private final Path storageRoot;

    DocumentStorageService(
            @Value("${securetask.documents.storage-path}") String storagePath) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize document storage", exception);
        }
    }

    void store(String storedFilename, InputStream inputStream) {
        Path target = resolve(storedFilename);
        try (inputStream) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not store document",
                    exception);
        }
    }

    Resource load(String storedFilename) {
        Path path = resolve(storedFilename);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Stored document is unavailable");
        }
        return new FileSystemResource(path);
    }

    void deleteQuietly(String storedFilename) {
        try {
            Files.deleteIfExists(resolve(storedFilename));
        } catch (IOException ignored) {
            // Database failure remains the primary error.
        }
    }

    private Path resolve(String storedFilename) {
        Path resolved = storageRoot.resolve(storedFilename).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid stored filename");
        }
        return resolved;
    }
}
