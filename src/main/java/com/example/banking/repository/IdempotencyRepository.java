package com.example.banking.repository;
import com.example.banking.entity.IdempotencyRecord; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,Long>{Optional<IdempotencyRecord> findByOwnerIdAndKey(Long ownerId,String key);}
