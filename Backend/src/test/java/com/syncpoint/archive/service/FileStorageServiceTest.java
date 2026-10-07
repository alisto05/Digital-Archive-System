package com.syncpoint.archive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(tempDir.toString());
    }

    private static MockMultipartFile pdf(String name, String content) {
        return new MockMultipartFile("file", name, "application/pdf", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void storesAValidPdfAndReportsSizeAndChecksum() throws Exception {
        FileStorageService.StoredFile stored = service.storePdf(pdf("report.pdf", "%PDF-1.7 hello"));

        assertTrue(stored.storageKey().matches("document-[0-9]+\\.pdf"));
        assertEquals(14, stored.fileSizeBytes());
        assertEquals(64, stored.checksumSha256().length());
        assertTrue(Files.exists(tempDir.resolve(stored.storageKey())));
    }

    @Test
    void rejectsFilesWithoutAPdfHeader() {
        assertThrows(IllegalArgumentException.class,
                () -> service.storePdf(pdf("fake.pdf", "<html>not a pdf</html>")));
    }

    @Test
    void rejectsOtherExtensionsAndEmptyFiles() {
        assertThrows(IllegalArgumentException.class, () -> service.storePdf(pdf("notes.txt", "%PDF-1.7")));
        assertThrows(IllegalArgumentException.class, () -> service.storePdf(pdf("empty.pdf", "")));
        assertThrows(IllegalArgumentException.class, () -> service.storePdf(null));
    }

    @Test
    void neverTouchesPathsOutsideTheStorageFolder() {
        assertThrows(IllegalArgumentException.class, () -> service.loadAsResource("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteStoredFile("document-1.pdf/../x.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.loadAsResource(null));
    }

    @Test
    void loadsAndDeletesStoredFiles() throws Exception {
        String key = service.storePdf(pdf("a.pdf", "%PDF-1.4 body")).storageKey();

        assertTrue(service.loadAsResource(key).exists());

        service.deleteStoredFile(key);
        assertThrows(FileNotFoundException.class, () -> service.loadAsResource(key));
    }
}
