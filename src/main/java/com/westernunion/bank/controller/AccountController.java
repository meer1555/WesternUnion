package com.westernunion.bank.controller;

import com.westernunion.bank.dto.AmountRequest;
import com.westernunion.bank.dto.TransferRequest;
import com.westernunion.bank.model.Account;
import com.westernunion.bank.model.Transaction;
import com.westernunion.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<Account> me(Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountForCurrentUser(authentication.getName()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(Authentication authentication, @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.deposit(authentication.getName(), request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(Authentication authentication, @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(accountService.withdraw(authentication.getName(), request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(Authentication authentication, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(accountService.transfer(authentication.getName(), request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> transactions(Authentication authentication) {
        Account account = accountService.getAccountForCurrentUser(authentication.getName());
        return ResponseEntity.ok(accountService.getTransactionHistory(account.getAccountNumber()));
    }

    @GetMapping("/lookup/{accountNumber}")
    public ResponseEntity<Map<String, Object>> lookup(@PathVariable String accountNumber) {
        // used by the "transfer" screen to confirm a recipient's name before sending money
        return ResponseEntity.ok(accountService.lookupPublic(accountNumber));
    }
}
