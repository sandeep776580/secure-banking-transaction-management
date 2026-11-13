package com.example.banking.repository;
import com.example.banking.entity.*; import java.time.Instant; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface TransactionRepository extends JpaRepository<BankTransaction,Long>{
 @Query("select t from BankTransaction t where t.account.id=:accountId and t.createdAt between :from and :to and (:type is null or t.type=:type)") Page<BankTransaction> searchOwnerHistory(@Param("accountId") Long accountId,@Param("from") Instant from,@Param("to") Instant to,@Param("type") TransactionType type,Pageable pageable);
 @Query("select t from BankTransaction t where (:accountId is null or t.account.id=:accountId) and (:type is null or t.type=:type) and (:from is null or t.createdAt>=:from) and (:to is null or t.createdAt<=:to)") Page<BankTransaction> adminSearch(@Param("accountId") Long accountId,@Param("type") TransactionType type,@Param("from") Instant from,@Param("to") Instant to,Pageable pageable);
 long countByType(TransactionType type);
 java.util.Optional<BankTransaction> findFirstByReferenceIdAndType(String referenceId, TransactionType type);
 java.util.Optional<BankTransaction> findFirstByAccountIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(Long accountId,Instant before);
 java.util.Optional<BankTransaction> findFirstByAccountIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(Long accountId,Instant from,Instant to);
 Page<BankTransaction> findByTypeAndAmountGreaterThanEqualAndCreatedAtAfter(TransactionType type,java.math.BigDecimal amount,Instant after,Pageable pageable);
}
