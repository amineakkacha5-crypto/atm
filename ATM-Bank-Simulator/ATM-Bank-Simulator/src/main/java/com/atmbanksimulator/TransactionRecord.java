package com.atmbanksimulator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionRecord {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final LocalDateTime timestamp;
    private final String type;
    private final int amount;
    private final String details;
    private final int resultingBalance;

    public TransactionRecord(LocalDateTime timestamp, String type, int amount, String details, int resultingBalance) {
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.details = details;
        this.resultingBalance = resultingBalance;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String toStorageLine(String accountNumber) {
        return String.join("|",
                escape(accountNumber),
                timestamp.toString(),
                escape(type),
                Integer.toString(amount),
                escape(details),
                Integer.toString(resultingBalance));
    }

    public static TransactionRecord fromStorageLine(String[] parts) {
        if (parts.length < 6) {
            return null;
        }

        try {
            return new TransactionRecord(
                    LocalDateTime.parse(unescape(parts[1])),
                    unescape(parts[2]),
                    Integer.parseInt(parts[3]),
                    unescape(parts[4]),
                    Integer.parseInt(parts[5]));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public String formatForStatement() {
        String amountPrefix = amount >= 0 ? "+" : "-";
        return timestamp.format(DISPLAY_FORMAT) + " | " + type + " | " + amountPrefix + "£" + Math.abs(amount)
                + " | Bal £" + resultingBalance + (details.isBlank() ? "" : " | " + details);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaping) {
                if (current == 'p') {
                    builder.append('|');
                } else {
                    builder.append(current);
                }
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                builder.append(current);
            }
        }

        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
    }
}
