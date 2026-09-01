package com.westernunion.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, length = 20)
    private String fromAccountNumber;

    @Column(length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.SUCCESS;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN
    }

    public enum TransactionStatus {
        SUCCESS, FAILED, PENDING
    }
}
