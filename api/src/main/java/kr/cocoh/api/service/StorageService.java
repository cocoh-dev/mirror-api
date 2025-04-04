package kr.cocoh.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AmazonS3 amazonS3Client;
    private final Environment environment;

    @Value("${ncloud.bucket-name}")
    private String bucketName;

    /**
     * 파일 업로드 함수
     * @param file 업로드할 파일
     * @param folder 상위 폴더 경로
     * @param subFolders 하위 폴더들 (가변 인자)
     * @return 업로드된 파일 URL
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public String uploadFile(MultipartFile file, String folder, String... subFolders) throws IOException {
        // 개발 환경에 따라 기본 경로 설정
        String baseFolder = environment.getActiveProfiles().length > 0 && 
                            "production".equals(environment.getActiveProfiles()[0]) ? "" : "test";
        
        // 하위 폴더 경로 생성
        StringBuilder subFolderPath = new StringBuilder();
        for (String subFolder : subFolders) {
            if (subFolder != null && !subFolder.isEmpty()) {
                subFolderPath.append(subFolder).append("/");
            }
        }
        
        // 최종 폴더 경로 생성
        String folderPath = baseFolder.isEmpty() ? 
                folder + "/" + subFolderPath : 
                baseFolder + "/" + folder + "/" + subFolderPath;
        
        // 파일명 생성 (UUID-원본파일명)
        String originalFileName = file.getOriginalFilename();
        String fileName = folderPath + UUID.randomUUID() + "-" + 
                         (originalFileName != null ? originalFileName : "unknown");
        
        // 메타데이터 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        // S3에 업로드
        try {
            amazonS3Client.putObject(new PutObjectRequest(
                    bucketName, fileName, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
            
            // 파일 URL 반환
            URL url = amazonS3Client.getUrl(bucketName, fileName);
            return url.toString();
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생: {}", e.getMessage());
            throw new IOException("파일 업로드 실패: " + e.getMessage());
        }
    }

    /**
     * 여러 파일 업로드 함수
     * @param files 업로드할 파일 목록
     * @param folder 상위 폴더 경로
     * @param subFolders 하위 폴더들 (가변 인자)
     * @return 업로드된 파일 URL 목록
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public List<String> uploadMultipleFiles(List<MultipartFile> files, String folder, String... subFolders) throws IOException {
        List<String> urls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String url = uploadFile(file, folder, subFolders);
                urls.add(url);
            }
        }
        
        return urls;
    }

    /**
     * 파일 삭제 함수
     * @param fileUrl 삭제할 파일 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // URL에서 키 추출
            URL url = new URL(fileUrl);
            String key = url.getPath().substring(1); // 첫 번째 '/' 제거
            
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Invalid file URL: Unable to extract key");
            }
            
            log.debug("Deleting file with key: {}", key);
            
            // S3에서 파일 삭제
            amazonS3Client.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", e.getMessage());
        }
    }
}