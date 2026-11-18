package pv.domain.files.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.files.entity.StoredFile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByObjectKey(String objectKey);

    List<StoredFile> findAllByObjectKeyIn(Collection<String> objectKeys);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from StoredFile f where f.objectKey in :objectKeys order by f.id")
    List<StoredFile> findAllByObjectKeyInForUpdate(@Param("objectKeys") Collection<String> objectKeys);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from StoredFile f where f.id in :ids order by f.id")
    List<StoredFile> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true)
    @Query("delete from StoredFile f where f.objectKey in :objectKeys")
    int deleteAllByObjectKeyIn(@Param("objectKeys") Collection<String> objectKeys);

    @Query(value = """
            select object_key from files
            where attached_at is null and created_at < :threshold
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<String> lockUnattachedKeysCreatedBefore(@Param("threshold") Instant threshold,
                                                 @Param("limit") int limit);
}
