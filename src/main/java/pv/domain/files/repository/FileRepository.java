package pv.domain.files.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.files.entity.StoredFile;

import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByObjectKey(String objectKey);
}
