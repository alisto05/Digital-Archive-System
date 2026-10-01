package com.syncpoint.archive.service;

import com.syncpoint.archive.dao.DocumentDao;
import com.syncpoint.archive.dto.DocumentUploadRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Service
public class DocumentUploadService {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final FileStorageService fileStorageService;
    private final DocumentDao documentDao;

    public DocumentUploadService(FileStorageService fileStorageService, DocumentDao documentDao) {
        this.fileStorageService = fileStorageService;
        this.documentDao = documentDao;
    }

    public long upload(MultipartFile file, long patientId, int documentTypeId, long uploadedByUserId)
            throws IOException {

        
        FileStorageService.StoredFile stored = fileStorageService.storePdf(file);

        try {
            return documentDao.uploadDocument(new DocumentUploadRequest(
                    patientId,
                    uploadedByUserId,
                    documentTypeId,
                    displayName(file.getOriginalFilename()),
                    stored.storageKey(),
                    PDF_MIME_TYPE,
                    stored.fileSizeBytes(),
                    stored.checksumSha256()
            ));
        } catch (RuntimeException exception) {
           
            try {
                fileStorageService.deleteStoredFile(stored.storageKey());
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

   
    static String displayName(String clientFilename) {
        String name = clientFilename == null ? "" : clientFilename;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        return name.isEmpty() ? "document.pdf" : name;
    }
}
