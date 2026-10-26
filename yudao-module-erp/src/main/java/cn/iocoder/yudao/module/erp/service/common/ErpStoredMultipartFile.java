package cn.iocoder.yudao.module.erp.service.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ErpStoredMultipartFile implements MultipartFile {
    private final Path path;
    private final String originalFilename;

    public ErpStoredMultipartFile(Path path, String originalFilename) {
        this.path = path;
        this.originalFilename = originalFilename;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() {
        try { return Files.probeContentType(path); } catch (IOException ex) { return null; }
    }
    @Override public boolean isEmpty() { return getSize() <= 0; }
    @Override public long getSize() {
        try { return Files.size(path); } catch (IOException ex) { return 0L; }
    }
    @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
    @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
    @Override public void transferTo(File dest) throws IOException {
        Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
