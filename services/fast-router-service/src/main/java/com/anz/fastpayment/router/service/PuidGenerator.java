package com.anz.fastpayment.router.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PuidGenerator {

    private static final String CHANNEL_PREFIX = "G3I"; // first 3 characters
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private final SecureRandom random = new SecureRandom();

    public PuidGenerator() {}

    public String nextPuid() {
        String date = LocalDate.now().format(DATE_FMT); // yymmdd -> 6 digits
        long rand = Math.abs(random.nextLong());
        String randDigits = String.format("%013d", rand).substring(0, 7); // 7 digits to get total 13
        String thirteenDigits = date + randDigits; // 6 + 7 = 13
        return CHANNEL_PREFIX + thirteenDigits;
    }
}


