package com.harithavastra.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

  private final Path directory;

  public UploadService(@Value("${app.upload-dir}") String path)
    throws IOException {
    directory = Path.of(path).toAbsolutePath().normalize();
    Files.createDirectories(directory);
  }

  public String upload(MultipartFile file) throws IOException {
    if (file.isEmpty() || file.getSize() > 8 * 1024 * 1024) throw Errors.bad(
      "Choose an image below 8 MB"
    );
    try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw Errors.bad("Use JPEG or PNG images");
      var reader = readers.next();
      try {
        reader.setInput(stream);
        String format = reader.getFormatName();
        if (
          !List.of("JPEG", "JPG", "PNG").contains(format.toUpperCase())
        ) throw Errors.bad("Use JPEG or PNG images");
        if (
          (long) reader.getWidth(0) * reader.getHeight(0) > 25000000
        ) throw Errors.bad("Image exceeds 25 megapixels");
        BufferedImage image = reader.read(0);
        String name = UUID.randomUUID() + ".png";
        ImageIO.write(image, "png", directory.resolve(name).toFile());
        return "/uploads/" + name;
      } finally {
        reader.dispose();
      }
    }
  }
}
