package com.example.banking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="accounts", indexes=@Index(name="idx_accounts_owner",columnList="owner_id"))
public class BankAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="account_number",nullable=false,unique=true,length=24) private String accountNumber;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private AppUser owner;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal balance=BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private AccountStatus status=AccountStatus.ACTIVE;
    @Version @Column(nullable=false) private long version;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected BankAccount() {}
    public BankAccount(String accountNumber,AppUser owner){this.accountNumber=accountNumber;this.owner=owner;}
    public Long getId(){return id;} public String getAccountNumber(){return accountNumber;} public AppUser getOwner(){return owner;} public BigDecimal getBalance(){return balance;} public AccountStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public void credit(BigDecimal amount){balance=balance.add(amount);} public void debit(BigDecimal amount){balance=balance.subtract(amount);} public void setStatus(AccountStatus status){this.status=status;}
}
