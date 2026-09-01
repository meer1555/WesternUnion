package com.westernunion.bank.service;

import com.westernunion.bank.dto.AuthResponse;
import com.westernunion.bank.dto.LoginRequest;
import com.westernunion.bank.dto.SignupRequest;
import com.westernunion.bank.exception.BankException;
import com.westernunion.bank.model.Account;
import com.westernunion.bank.model.User;
import com.westernunion.bank.repository.AccountRepository;
import com.westernunion.bank.repository.UserRepository;
import com.westernunion.bank.security.CustomUserDetailsService;
import com.westernunion.bank.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator accountNumberGenerator;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BankException("An account with this email already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BankException("An account with this phone number already exists", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        Account account = new Account();
        account.setAccountNumber(accountNumberGenerator.generate());
        account.setUser(user);
        account.setBalance(java.math.BigDecimal.ZERO);
        account = accountRepository.save(account);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getFullName(), user.getEmail(), account.getAccountNumber(), account.getBalance());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception ex) {
            throw new BankException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BankException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BankException("No account found for this user", HttpStatus.NOT_FOUND));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getFullName(), user.getEmail(), account.getAccountNumber(), account.getBalance());
    }
}
