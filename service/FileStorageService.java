package com.syncpoint.archive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;


@Service
public class FileStorageService {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

   
    public record StoredFile(String storageKey, long fileSizeBytes, String checksumSha256) {
    }

    private final Path storageDirectory;

    
    public FileStorageService(
            @Value("${hospitalarchive.storage.location}") String location) {

        this.storageDirectory = Path.of(location)
                .toAbsolutePath()
                .normalize();
    }

    
    public StoredFile storePdf(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must not exceed 10 MB.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported.");
        }

        byte[] content;

        
        try (InputStream input = file.getInputStream()) {
            content = input.readNBytes(MAX_FILE_SIZE + 1);
        }

        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must not exceed 10 MB.");
        }

        
        if (content.length < 5
                || content[0] != '%'
                || content[1] != 'P'
                || content[2] != 'D'
                || content[3] != 'F'
                || content[4] != '-') {
            throw new IllegalArgumentException(
                    "File does not have a PDF header."
            );
        }

        Files.createDirectories(storageDirectory);

        
        Path destination = Files.createTempFile(
                storageDirectory,
                "document-",
                ".pdf"
        );

        try {
            Files.write(destination, content);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }

        return new StoredFile(
                destination.getFileName().toString(),
                content.length,
                sha256Hex(content)
        );
    }

    public void deleteStoredFile(String storedFilename) throws IOException {
        Files.deleteIfExists(resolveInsideStorage(storedFilename));
    }

  
    public Resource loadAsResource(String storedFilename) throws IOException {
        Path file = resolveInsideStorage(storedFilename);

        if (!Files.isRegularFile(file)) {
            throw new FileNotFoundException("Stored file is missing.");
        }

        return new FileSystemResource(file);
    }

    private Path resolveInsideStorage(String storedFilename) {

        if (storedFilename == null
                || !storedFilename.matches("document-[0-9]+\\.pdf")) {
            throw new IllegalArgumentException("Invalid stored filename.");
        }

        Path destination = storageDirectory.resolve(storedFilename).normalize();

        if (!storageDirectory.equals(destination.getParent())) {
            throw new IllegalArgumentException(
                    "File must be inside the storage directory."
            );
        }

        return destination;
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
