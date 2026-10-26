package pv.domain.files.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.files.entity.StoredFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByObjectKey(String objectKey);

    List<StoredFile> findAllByObjectKeyIn(Collection<String> objectKeys);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StoredFile f where f.objectKey in :objectKeys")
    int deleteAllByObjectKeyIn(@Param("objectKeys") Collection<String> objectKeys);
}
