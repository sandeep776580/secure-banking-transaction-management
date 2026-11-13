package com.example.banking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="transactions",indexes={@Index(name="idx_tx_account_created",columnList="account_id,created_at"),@Index(name="idx_tx_reference",columnList="reference_id")})
public class BankTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="reference_id",nullable=false,length=36,updatable=false) private String referenceId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false,updatable=false) private BankAccount account;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20,updatable=false) private TransactionType type;
    @Column(nullable=false,precision=19,scale=2,updatable=false) private BigDecimal amount;
    @Column(name="balance_after",nullable=false,precision=19,scale=2,updatable=false) private BigDecimal balanceAfter;
    @Column(length=255,updatable=false) private String description;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected BankTransaction() {}
    public BankTransaction(String referenceId,BankAccount account,TransactionType type,BigDecimal amount,String description){this.referenceId=referenceId;this.account=account;this.type=type;this.amount=amount;this.description=description;this.balanceAfter=account.getBalance();}
    public Long getId(){return id;} public String getReferenceId(){return referenceId;} public BankAccount getAccount(){return account;} public TransactionType getType(){return type;} public BigDecimal getAmount(){return amount;} public BigDecimal getBalanceAfter(){return balanceAfter;} public String getDescription(){return description;} public Instant getCreatedAt(){return createdAt;}
}
