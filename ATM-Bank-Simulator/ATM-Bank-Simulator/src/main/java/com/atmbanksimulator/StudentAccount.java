package com.atmbanksimulator;

public class StudentAccount extends BankAccount {
    private final int dailyWithdrawalLimit;
    private int withdrawnToday;

    public StudentAccount(String accNumber, String accPasswd, int balance) {
        this(accNumber, accPasswd, balance, 100);
    }

    public StudentAccount(String accNumber, String accPasswd, int balance, int dailyWithdrawalLimit) {
        super(accNumber, accPasswd, balance);
        this.dailyWithdrawalLimit = dailyWithdrawalLimit;
        this.withdrawnToday = 0;
    }

    @Override
    public boolean withdraw(int amount) {
        if (amount < 0 || withdrawnToday + amount > dailyWithdrawalLimit) {
            return false;
        }

        if (super.withdraw(amount)) {
            withdrawnToday += amount;
            return true;
        }

        return false;
    }

    @Override
    public String getAccountType() {
        return "Student Account";
    }

    @Override
    public String getAccountFeatures() {
        return "Daily withdrawal cap: " + dailyWithdrawalLimit;
    }

    @Override
    public int getAccountTypeCode() {
        return Bank.TYPE_STUDENT;
    }
}
