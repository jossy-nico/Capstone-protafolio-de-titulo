package com.example.producto.services;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {
private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadImage(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image"
                )
        );
    }

    public Map deleteByPublicId(String publicId) throws Exception {
        if (publicId == null || publicId.isBlank()) return null;

        return cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "image")
        );
    }
}
