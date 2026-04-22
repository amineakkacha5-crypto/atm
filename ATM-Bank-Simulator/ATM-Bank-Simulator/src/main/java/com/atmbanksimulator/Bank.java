package com.atmbanksimulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

// ===== 📚🌐Bank (Domain / Service / Business Logic) =====

// Bank class: a simple implementation of a bank, containing a list of bank accounts
// and has a currently logged-in account (loggedInAccount).
public class Bank {
    public static final int TYPE_STUDENT = 1;
    public static final int TYPE_PRIME = 2;
    public static final int TYPE_SAVING = 3;
    public static final int TYPE_STANDARD = 4;
    private static final int MIN_ACCOUNT_NUMBER_LENGTH = 3;
    private static final int MAX_ACCOUNT_NUMBER_LENGTH = 8;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 8;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOW_BALANCE_THRESHOLD = 50;
    private static final int MINI_STATEMENT_SIZE = 5;
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path ACCOUNTS_FILE = DATA_DIRECTORY.resolve("accounts.txt");
    private static final Path TRANSACTIONS_FILE = DATA_DIRECTORY.resolve("transactions.txt");

    // ToDO: Optional extension:
    // Improve account management in the Bank class:
    // Replace Array with ArrayList for managing BankAccount objects.
    // Refactor addBankAccount and login methods to leverage ArrayList.

    // Instance variables storing bank information
    private int maxAccounts = 10;                       // Maximum number of accounts the bank can hold
    private int numAccounts = 0;                        // Current number of accounts in the bank
    private BankAccount[] accounts = new BankAccount[maxAccounts];  // Array to hold BankAccount objects
    private BankAccount loggedInAccount = null;         // Currently logged-in account ('null' if no one is logged in)
    private int failedLoginAttempts = 0;

    public Bank() {
        loadData();
    }

    // a method to create new BankAccount - this is known as a 'factory method' and is a more
    // flexible way to do it than just using the 'new' keyword directly.
    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance) {
        return new BankAccount(accNumber, accPasswd, balance);
    }

    public BankAccount makeBankAccount(String accNumber, String accPasswd, int balance, int accountType) {
        return switch (accountType) {
            case TYPE_STUDENT -> new StudentAccount(accNumber, accPasswd, balance);
            case TYPE_PRIME -> new PrimeAccount(accNumber, accPasswd, balance);
            case TYPE_SAVING -> new SavingAccount(accNumber, accPasswd, balance);
            case TYPE_STANDARD -> new BankAccount(accNumber, accPasswd, balance);
            default -> null;
        };
    }

    // a method to add a new bank account to the bank - it returns true if it succeeds
    // or false if it fails (because the bank is 'full')
    public boolean addBankAccount(BankAccount a) {
        return addBankAccount(a, true, true);
    }

    private boolean addBankAccount(BankAccount a, boolean persist, boolean recordOpeningBalance) {
        if (numAccounts < maxAccounts) {
            accounts[numAccounts] = a;
            numAccounts++ ;
            if (recordOpeningBalance) {
                a.recordTransaction("ACCOUNT", a.getBalance(), "Opening balance", a.getBalance());
            }
            if (persist) {
                saveData();
            }
            return true;
        } else {
            return false;
        }
    }

    // Variant of addBankAccount: creates a BankAccount and adds it in one step.
    // This is an example of method overloading: two methods can share the same name
    // if they have different parameter lists.
    public boolean addBankAccount(String accNumber, String accPasswd, int balance) {
        return addBankAccount(makeBankAccount(accNumber, accPasswd, balance));
    }

    public boolean createBankAccount(String accNumber, String accPasswd, int balance, int accountType) {
        if (validateAccountNumber(accNumber) != null ||
                validatePassword(accPasswd) != null ||
                balance < 0 ||
                accountExists(accNumber)) {
            return false;
        }

        BankAccount account = makeBankAccount(accNumber, accPasswd, balance, accountType);
        if (account == null) {
            return false;
        }

        return addBankAccount(account);
    }

    // Check whether the given accountNumber and password match an existing BankAccount.
    // If successful, set 'loggedInAccount' to that account and return true.
    // Otherwise, set 'loggedInAccount' to null and return false.
    public boolean login(String accountNumber, String password) {
        logout(); // logout of any previous loggedInAccount

        if (loginLocked()) {
            return false;
        }

        // Search the accounts array to find a BankAccount with a matching accountNumber and password.
        // - If found, set 'loggedInAccount' to that account and return true.
        // - If not found, reset 'loggedInAccount' to null and return false.
        for (BankAccount b: accounts) {
            if (b == null) {
                continue;
            }
            if (b.getAccNumber().equals(accountNumber) && b.passwordMatches(password)) {
                // found the right account
                loggedInAccount = b;
                failedLoginAttempts = 0;
                return true;
            }
        }
        // not found - return false
        loggedInAccount = null;
        failedLoginAttempts++;
        return false;
    }

    // Log out of the currently logged-in account, if any
    public void logout() {
        if (loggedIn()) {
            loggedInAccount = null;
        }
    }

    // Check whether the bank currently has a logged-in account
    public boolean loggedIn() {
        if (loggedInAccount == null) {
            return false;
        } else {
            return true;
        }
    }

    // Attempt to deposit money into the currently logged-in account
    // by calling the deposit method of the BankAccount object
    public boolean deposit(int amount)
    {
        if (loggedIn()) {
            int balanceBefore = loggedInAccount.getBalance();
            boolean deposited = loggedInAccount.deposit(amount);
            if (deposited) {
                int creditedAmount = loggedInAccount.getBalance() - balanceBefore;
                String details = creditedAmount == amount
                        ? "Cash deposit"
                        : "Cash deposit including bonus/interest of £" + (creditedAmount - amount);
                loggedInAccount.recordTransaction("DEPOSIT", creditedAmount, details, loggedInAccount.getBalance());
                saveData();
            }
            return deposited;
        } else {
            return false;
        }
    }


    // Attempt to withdraw money from the currently logged-in account
    // by calling the withdraw method of the BankAccount object
    public boolean withdraw(int amount)
    {
        if (loggedIn()) {
            boolean withdrawn = loggedInAccount.withdraw(amount);
            if (withdrawn) {
                loggedInAccount.recordTransaction("WITHDRAW", -amount, "Cash withdrawal", loggedInAccount.getBalance());
                saveData();
            }
            return withdrawn;
        } else {
            return false;
        }
    }

    // get the currently logged-in account balance
    // by calling the getBalance method of the BankAccount object
    public int getBalance()
    {
        if (loggedIn()) {
            return loggedInAccount.getBalance();
        } else {
            return -1; // use -1 as an indicator of an error
        }
    }

    public String getLoggedInAccountType() {
        if (loggedIn()) {
            return loggedInAccount.getAccountType();
        }
        return "Unknown Account";
    }

    public String getLoggedInAccountFeatures() {
        if (loggedIn()) {
            return loggedInAccount.getAccountFeatures();
        }
        return "";
    }

    public boolean accountExists(String accountNumber) {
        for (BankAccount account : accounts) {
            if (account != null && account.getAccNumber().equals(accountNumber)) {
                return true;
            }
        }
        return false;
    }

    public String validateAccountNumber(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d+")) {
            return "Account number must contain digits only";
        }
        if (accountNumber.length() < MIN_ACCOUNT_NUMBER_LENGTH || accountNumber.length() > MAX_ACCOUNT_NUMBER_LENGTH) {
            return "Account number must be " + MIN_ACCOUNT_NUMBER_LENGTH + "-" + MAX_ACCOUNT_NUMBER_LENGTH + " digits";
        }
        return null;
    }

    public String validatePassword(String password) {
        if (password == null || !password.matches("\\d+")) {
            return "Password must contain digits only";
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " digits";
        }
        return null;
    }

    public boolean changeLoggedInPassword(String oldPassword, String newPassword) {
        if (!loggedIn()) {
            return false;
        }
        if (!loggedInAccount.passwordMatches(oldPassword)) {
            return false;
        }
        if (validatePassword(newPassword) != null || oldPassword.equals(newPassword)) {
            return false;
        }

        loggedInAccount.setPassword(newPassword);
        saveData();
        return true;
    }

    public boolean verifyLoggedInPassword(String password) {
        return loggedIn() && loggedInAccount.passwordMatches(password);
    }

    public String getPasswordRules() {
        return "Password rules: " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " digits";
    }

    public String getAccountNumberRules() {
        return "Account number rules: " + MIN_ACCOUNT_NUMBER_LENGTH + "-" + MAX_ACCOUNT_NUMBER_LENGTH + " digits";
    }

    public String getAccountTypeMenu() {
        return "Choose type: 1=Student 2=Prime 3=Saving 4=Standard";
    }

    public int getRemainingLoginAttempts() {
        return Math.max(0, MAX_LOGIN_ATTEMPTS - failedLoginAttempts);
    }

    public boolean loginLocked() {
        return failedLoginAttempts >= MAX_LOGIN_ATTEMPTS;
    }

    public void resetLoginAttempts() {
        failedLoginAttempts = 0;
    }

    public BankAccount findAccount(String accountNumber) {
        for (BankAccount account : accounts) {
            if (account != null && account.getAccNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    public String transfer(int amount, String targetAccountNumber) {
        if (!loggedIn()) {
            return "You are not logged in";
        }
        if (amount <= 0) {
            return "Transfer amount must be greater than zero";
        }

        BankAccount targetAccount = findAccount(targetAccountNumber);
        if (targetAccount == null) {
            return "Target account was not found";
        }
        if (targetAccount.getAccNumber().equals(loggedInAccount.getAccNumber())) {
            return "Cannot transfer to the same account";
        }
        if (!loggedInAccount.withdraw(amount)) {
            return "Insufficient funds for transfer";
        }
        int targetBalanceBefore = targetAccount.getBalance();
        if (!targetAccount.deposit(amount)) {
            loggedInAccount.deposit(amount);
            return "Transfer could not be completed";
        }
        int creditedAmount = targetAccount.getBalance() - targetBalanceBefore;
        loggedInAccount.recordTransaction("TRANSFER OUT", -amount,
                "To account " + targetAccountNumber, loggedInAccount.getBalance());
        String transferInDetails = creditedAmount == amount
                ? "From account " + loggedInAccount.getAccNumber()
                : "From account " + loggedInAccount.getAccNumber() + " including bonus/interest of £" + (creditedAmount - amount);
        targetAccount.recordTransaction("TRANSFER IN", creditedAmount, transferInDetails, targetAccount.getBalance());
        saveData();
        return null;
    }

    public boolean hasAccounts() {
        return numAccounts > 0;
    }

    public String getMiniStatement() {
        if (!loggedIn()) {
            return "No account selected.";
        }
        return loggedInAccount.getMiniStatement(MINI_STATEMENT_SIZE);
    }

    public String getLowBalanceWarning() {
        if (!loggedIn()) {
            return "";
        }
        if (loggedInAccount.getBalance() <= LOW_BALANCE_THRESHOLD) {
            return "Low balance warning: your balance is at or below £" + LOW_BALANCE_THRESHOLD + ".";
        }
        return "";
    }

    private void loadData() {
        if (!Files.exists(ACCOUNTS_FILE)) {
            return;
        }

        try {
            List<String> accountLines = Files.readAllLines(ACCOUNTS_FILE);
            for (String line : accountLines) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }

                try {
                    String accNumber = parts[0];
                    String password = parts[1];
                    int balance = Integer.parseInt(parts[2]);
                    int accountType = Integer.parseInt(parts[3]);
                    BankAccount account = makeBankAccount(accNumber, password, balance, accountType);
                    if (account != null) {
                        addBankAccount(account, false, false);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed account rows and continue loading the rest.
                }
            }

            if (Files.exists(TRANSACTIONS_FILE)) {
                List<String> transactionLines = Files.readAllLines(TRANSACTIONS_FILE);
                for (String line : transactionLines) {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 6) {
                        continue;
                    }

                    BankAccount account = findAccount(parts[0]);
                    TransactionRecord record = TransactionRecord.fromStorageLine(parts);
                    if (account != null && record != null) {
                        account.addLoadedTransaction(record);
                    }
                }
            }
        } catch (IOException ignored) {
            // Start with in-memory data only if storage cannot be read.
        }
    }

    private void saveData() {
        try {
            Files.createDirectories(DATA_DIRECTORY);

            List<String> accountLines = new ArrayList<>();
            List<String> transactionLines = new ArrayList<>();
            for (BankAccount account : accounts) {
                if (account == null) {
                    continue;
                }
                accountLines.add(String.join("|",
                        account.getAccNumber(),
                        account.getPassword(),
                        Integer.toString(account.getBalance()),
                        Integer.toString(account.getAccountTypeCode())));

                for (TransactionRecord transaction : account.getTransactions()) {
                    transactionLines.add(transaction.toStorageLine(account.getAccNumber()));
                }
            }

            Files.write(ACCOUNTS_FILE, accountLines,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.write(TRANSACTIONS_FILE, transactionLines,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ignored) {
            // Keep the ATM usable even if storage cannot be written.
        }
    }
}
