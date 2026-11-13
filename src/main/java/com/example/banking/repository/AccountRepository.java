package com.example.banking.repository;
import com.example.banking.entity.*; import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AccountRepository extends JpaRepository<BankAccount,Long>{List<BankAccount> findByOwnerId(Long ownerId); Optional<BankAccount> findByAccountNumber(String number); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from BankAccount a where a.id=:id") Optional<BankAccount> lockById(@Param("id") Long id);}
