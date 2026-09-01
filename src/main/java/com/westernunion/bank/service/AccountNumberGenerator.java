package com.westernunion.bank.service;

import com.westernunion.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.bank.account-number-prefix:WU}")
    private String prefix;

    /**
     * Generates a unique, 12-digit style bank account number, e.g. WU384920175610
     */
    public String generate() {
        String candidate;
        do {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                digits.append(random.nextInt(10));
            }
            candidate = prefix + digits;
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
