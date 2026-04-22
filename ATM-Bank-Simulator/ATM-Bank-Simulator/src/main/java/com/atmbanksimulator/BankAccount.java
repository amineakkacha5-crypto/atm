package com.atmbanksimulator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ===== 📚🌐BankAccount (Domain / Service / Business Logic) =====

// BankAccount class:
// - Stores instance variables for account number, password, and balance
// - Provides methods to withdraw, deposit, check balance, etc.
public class BankAccount {
    private String accNumber = "";
    private String accPasswd ="";
    private int balance = 0;
    private final ArrayList<TransactionRecord> transactions = new ArrayList<>();

    public BankAccount() {}
    public BankAccount(String a, String p, int b) {
        accNumber = a;
        accPasswd = p;
        balance = b;
    }

    // Withdraw money from this account.
    // Returns true if successful, or false if the amount is negative or exceeds the current balance.
    public boolean withdraw( int amount ) {
        if (amount < 0 || balance < amount) {
            return false;
        } else {
            changeBalance(-amount);  // subtract amount from balance
            return true;
        }
    }

    // deposit the amount of money into this account.
    // Return true if successful,or false if the amount is negative
    public boolean deposit( int amount ) {
        if (amount < 0) {
            return false;
        } else {
            changeBalance(amount);  // add amount to balance
            return true;
        }
    }

    public boolean passwordMatches(String password) {
        return accPasswd.equals(password);
    }

    public void setPassword(String newPassword) {
        accPasswd = newPassword;
    }

    public String getPassword() {
        return accPasswd;
    }

    protected void changeBalance(int amount) {
        balance += amount;
    }

    public String getAccountType() {
        return "Standard Account";
    }

    public String getAccountFeatures() {
        return "Standard balance rules";
    }

    public int getAccountTypeCode() {
        return Bank.TYPE_STANDARD;
    }

    // Getter for the account balance
    // Returns the current balance of this account
    public int getBalance() {
        return balance;
    }

    // Getter for the account number
    public String getAccNumber() {
        return accNumber;
    }

    public void recordTransaction(String type, int amount, String details, int resultingBalance) {
        transactions.add(new TransactionRecord(LocalDateTime.now(), type, amount, details, resultingBalance));
    }

    public void addLoadedTransaction(TransactionRecord transaction) {
        transactions.add(transaction);
    }

    public List<TransactionRecord> getTransactions() {
        return List.copyOf(transactions);
    }

    public String getMiniStatement(int maxEntries) {
        if (transactions.isEmpty()) {
            return "No recent transactions.";
        }

        StringBuilder builder = new StringBuilder();
        int startIndex = Math.max(0, transactions.size() - maxEntries);
        for (int i = transactions.size() - 1; i >= startIndex; i--) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(transactions.get(i).formatForStatement());
        }
        return builder.toString();
    }
}
