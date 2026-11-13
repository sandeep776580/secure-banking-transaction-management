package com.example.banking.service;

import com.example.banking.entity.*;import com.example.banking.exception.ApiException;import com.example.banking.repository.*;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.util.Optional;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class TransactionServiceTest {
 @Test void insufficientWithdrawalLeavesBalanceAndCreatesNoTransaction(){
  AccountRepository accounts=mock(AccountRepository.class);UserRepository users=mock(UserRepository.class);TransactionRepository txs=mock(TransactionRepository.class);IdempotencyRepository keys=mock(IdempotencyRepository.class);AuditRepository audits=mock(AuditRepository.class);FailedTransactionAuditService failureAudit=mock(FailedTransactionAuditService.class);
  AppUser owner=new AppUser("owner@example.com","hash","Owner",Role.CUSTOMER);BankAccount account=new BankAccount("100000000000000001",owner);setId(account,41L);
  when(accounts.lockById(41L)).thenReturn(Optional.of(account));TransactionService service=new TransactionService(accounts,users,txs,keys,audits,failureAudit);
  assertThrows(ApiException.class,()->service.withdraw(new com.example.banking.dto.ApiDtos.MoneyRequest(new BigDecimal("1.00"),41L,"test"),"owner@example.com"));
  assertEquals(0,new BigDecimal("0.00").compareTo(account.getBalance()));verify(txs,never()).save(any());
 }
 private static void setId(BankAccount a,Long id){try{var f=BankAccount.class.getDeclaredField("id");f.setAccessible(true);f.set(a,id);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
