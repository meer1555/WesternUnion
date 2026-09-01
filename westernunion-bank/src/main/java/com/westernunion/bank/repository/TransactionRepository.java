package com.westernunion.bank.repository;

import com.westernunion.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountNumberOrToAccountNumberOrderByTimestampDesc(String fromAcc, String toAcc);
}
