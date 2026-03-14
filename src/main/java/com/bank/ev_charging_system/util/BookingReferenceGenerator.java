package com.bank.ev_charging_system.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class BookingReferenceGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    // Generates reference like: EVC-202403151200-A3F2B1
    public String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String uniquePart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "EVC-" + timestamp + "-" + uniquePart;
    }
}
