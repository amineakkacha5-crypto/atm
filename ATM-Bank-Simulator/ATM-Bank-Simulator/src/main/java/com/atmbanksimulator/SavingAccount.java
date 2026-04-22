package com.atmbanksimulator;

public class SavingAccount extends BankAccount {
    private final double interestRate;

    public SavingAccount(String accNumber, String accPasswd, int balance) {
        this(accNumber, accPasswd, balance, 0.02);
    }

    public SavingAccount(String accNumber, String accPasswd, int balance, double interestRate) {
        super(accNumber, accPasswd, balance);
        this.interestRate = interestRate;
    }

    @Override
    public boolean deposit(int amount) {
        if (amount < 0) {
            return false;
        }

        int interest = (int) Math.round(amount * interestRate);
        changeBalance(amount + interest);
        return true;
    }

    @Override
    public String getAccountType() {
        return "Saving Account";
    }

    @Override
    public String getAccountFeatures() {
        return "Deposit interest rate: " + Math.round(interestRate * 100) + "%";
    }

    @Override
    public int getAccountTypeCode() {
        return Bank.TYPE_SAVING;
    }
}
