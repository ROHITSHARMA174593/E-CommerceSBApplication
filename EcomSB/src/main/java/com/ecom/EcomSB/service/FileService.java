package com.ecom.EcomSB.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.NonNull;

import java.io.IOException;

public interface FileService {
    String uploadImage(String path, @NonNull MultipartFile file) throws IOException;
}
