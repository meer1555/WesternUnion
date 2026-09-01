package com.westernunion.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private int status;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
