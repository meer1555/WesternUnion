package com.westernunion.bank.service;

import com.westernunion.bank.dto.AmountRequest;
import com.westernunion.bank.dto.TransferRequest;
import com.westernunion.bank.exception.BankException;
import com.westernunion.bank.model.Account;
import com.westernunion.bank.model.Transaction;
import com.westernunion.bank.model.User;
import com.westernunion.bank.repository.AccountRepository;
import com.westernunion.bank.repository.TransactionRepository;
import com.westernunion.bank.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    // ---------- Helpers ----------

    public Account getAccountForCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BankException("User not found", HttpStatus.NOT_FOUND));
        return accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));
    }

    public List<Transaction> getTransactionHistory(String accountNumber) {
        return transactionRepository.findByFromAccountNumberOrToAccountNumberOrderByTimestampDesc(accountNumber, accountNumber);
    }

    /**
     * Returns just enough public info (masked name) for the sender to confirm
     * who they're transferring money to, without leaking the recipient's balance.
     */
    public java.util.Map<String, Object> lookupPublic(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankException("No account found with that number", HttpStatus.NOT_FOUND));

        String fullName = account.getUser().getFullName();
        String masked = maskName(fullName);

        return java.util.Map.of(
                "accountNumber", account.getAccountNumber(),
                "accountHolder", masked,
                "status", account.getStatus()
        );
    }

    private String maskName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                result.append(part); // keep last name visible
            } else {
                result.append(part.charAt(0)).append(".");
            }
            if (i < parts.length - 1) result.append(" ");
        }
        return result.toString();
    }

    private String newReference() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    // ---------- Deposit ----------

    @Transactional
    public Transaction deposit(String email, AmountRequest request) {
        Account account = getAccountForCurrentUser(email);
        // re-fetch with a pessimistic lock to guard against concurrent updates
        Account locked = accountRepository.findByAccountNumberForUpdate(account.getAccountNumber())
                .orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));

        if (!"ACTIVE".equals(locked.getStatus())) {
            throw new BankException("Account is not active", HttpStatus.FORBIDDEN);
        }

        locked.setBalance(locked.getBalance().add(request.getAmount()));
        accountRepository.save(locked);

        Transaction txn = new Transaction();
        txn.setReferenceId(newReference());
        txn.setType(Transaction.TransactionType.DEPOSIT);
        txn.setFromAccountNumber(locked.getAccountNumber());
        txn.setToAccountNumber(locked.getAccountNumber());
        txn.setAmount(request.getAmount());
        txn.setBalanceAfter(locked.getBalance());
        txn.setDescription(request.getDescription() != null ? request.getDescription() : "Cash deposit");
        txn.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(txn);
    }

    // ---------- Withdraw ----------

    @Transactional
    public Transaction withdraw(String email, AmountRequest request) {
        Account account = getAccountForCurrentUser(email);
        Account locked = accountRepository.findByAccountNumberForUpdate(account.getAccountNumber())
                .orElseThrow(() -> new BankException("Account not found", HttpStatus.NOT_FOUND));

        if (!"ACTIVE".equals(locked.getStatus())) {
            throw new BankException("Account is not active", HttpStatus.FORBIDDEN);
        }

        if (locked.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BankException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        locked.setBalance(locked.getBalance().subtract(request.getAmount()));
        accountRepository.save(locked);

        Transaction txn = new Transaction();
        txn.setReferenceId(newReference());
        txn.setType(Transaction.TransactionType.WITHDRAWAL);
        txn.setFromAccountNumber(locked.getAccountNumber());
        txn.setToAccountNumber(null);
        txn.setAmount(request.getAmount());
        txn.setBalanceAfter(locked.getBalance());
        txn.setDescription(request.getDescription() != null ? request.getDescription() : "Cash withdrawal");
        txn.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(txn);
    }

    // ---------- Transfer ----------

    @Transactional
    public Transaction transfer(String email, TransferRequest request) {
        Account fromAccount = getAccountForCurrentUser(email);

        if (fromAccount.getAccountNumber().equals(request.getToAccountNumber())) {
            throw new BankException("You cannot transfer money to your own account", HttpStatus.BAD_REQUEST);
        }

        // Lock accounts in a deterministic order (by account number) to avoid deadlocks
        String first = fromAccount.getAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? fromAccount.getAccountNumber() : request.getToAccountNumber();
        String second = fromAccount.getAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? request.getToAccountNumber() : fromAccount.getAccountNumber();

        Account lockedFirst = accountRepository.findByAccountNumberForUpdate(first)
                .orElseThrow(() -> new BankException("Destination account not found", HttpStatus.NOT_FOUND));
        Account lockedSecond = accountRepository.findByAccountNumberForUpdate(second)
                .orElseThrow(() -> new BankException("Destination account not found", HttpStatus.NOT_FOUND));

        Account lockedFrom = lockedFirst.getAccountNumber().equals(fromAccount.getAccountNumber()) ? lockedFirst : lockedSecond;
        Account lockedTo = lockedFirst.getAccountNumber().equals(fromAccount.getAccountNumber()) ? lockedSecond : lockedFirst;

        if (!"ACTIVE".equals(lockedFrom.getStatus()) || !"ACTIVE".equals(lockedTo.getStatus())) {
            throw new BankException("One of the accounts is not active", HttpStatus.FORBIDDEN);
        }

        if (lockedFrom.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BankException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }

        lockedFrom.setBalance(lockedFrom.getBalance().subtract(request.getAmount()));
        lockedTo.setBalance(lockedTo.getBalance().add(request.getAmount()));
        accountRepository.save(lockedFrom);
        accountRepository.save(lockedTo);

        String description = request.getDescription() != null ? request.getDescription()
                : "Transfer to " + lockedTo.getAccountNumber();

        Transaction outTxn = new Transaction();
        outTxn.setReferenceId(newReference());
        outTxn.setType(Transaction.TransactionType.TRANSFER_OUT);
        outTxn.setFromAccountNumber(lockedFrom.getAccountNumber());
        outTxn.setToAccountNumber(lockedTo.getAccountNumber());
        outTxn.setAmount(request.getAmount());
        outTxn.setBalanceAfter(lockedFrom.getBalance());
        outTxn.setDescription(description);
        outTxn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(outTxn);

        Transaction inTxn = new Transaction();
        inTxn.setReferenceId(newReference());
        inTxn.setType(Transaction.TransactionType.TRANSFER_IN);
        inTxn.setFromAccountNumber(lockedFrom.getAccountNumber());
        inTxn.setToAccountNumber(lockedTo.getAccountNumber());
        inTxn.setAmount(request.getAmount());
        inTxn.setBalanceAfter(lockedTo.getBalance());
        inTxn.setDescription("Received from " + lockedFrom.getAccountNumber());
        inTxn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(inTxn);

        return outTxn;
    }
}
