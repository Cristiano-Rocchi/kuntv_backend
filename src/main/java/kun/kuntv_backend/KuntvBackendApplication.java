package kun.kuntv_backend;

import kun.kuntv_backend.config.BackblazeB2Config;
import kun.kuntv_backend.entities.Sezione;
import kun.kuntv_backend.entities.Stagione;
import kun.kuntv_backend.entities.Video;
import kun.kuntv_backend.exceptions.InternalServerErrorException;
import kun.kuntv_backend.exceptions.NotFoundException;
import kun.kuntv_backend.payloads.NewVideoDTO;
import kun.kuntv_backend.payloads.VideoRespDTO;
import kun.kuntv_backend.repositories.StagioneRepository;
import kun.kuntv_backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SpringBootApplication
public class KuntvBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KuntvBackendApplication.class, args);
	}

}


