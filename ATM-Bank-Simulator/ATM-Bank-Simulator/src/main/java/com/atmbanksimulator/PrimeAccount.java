package com.atmbanksimulator;

public class PrimeAccount extends BankAccount {
    private final int overdraftLimit;

    public PrimeAccount(String accNumber, String accPasswd, int balance) {
        this(accNumber, accPasswd, balance, 500);
    }

    public PrimeAccount(String accNumber, String accPasswd, int balance, int overdraftLimit) {
        super(accNumber, accPasswd, balance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public boolean withdraw(int amount) {
        if (amount < 0 || getBalance() - amount < -overdraftLimit) {
            return false;
        }

        changeBalance(-amount);
        return true;
    }

    @Override
    public String getAccountType() {
        return "Prime Account";
    }

    @Override
    public String getAccountFeatures() {
        return "Overdraft available up to: " + overdraftLimit;
    }

    @Override
    public int getAccountTypeCode() {
        return Bank.TYPE_PRIME;
    }
}
