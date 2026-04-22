package com.atmbanksimulator;

// ===== 🧠 UIModel (Brain) =====

// The UIModel represents all the actual content and functionality of the app
// For the ATM, it keeps track of the information shown in the display
// (the laMsg and two tfInput boxes), and the interaction with the bank, executes
// commands provided by the controller and tells the view to update when
// something changes
public class UIModel {
    private static final String BLANK_INPUT = "";

    View view; // Reference to the View (part of the MVC setup)
    private Bank bank; // The ATM communicates with this Bank

    // The ATM UIModel can be in one of three states:
    // 1. Waiting for an account number
    // 2. Waiting for a password
    // 3. Logged in (ready to process requests for the logged-in account)
    // We represent each state with a String constant.
    // The 'final' keyword ensures these values cannot be changed.
    private final String STATE_WELCOME = "welcome";
    private final String STATE_ACCOUNT_NO = "account_no";
    private final String STATE_PASSWORD = "password";
    private final String STATE_LOGGED_IN = "logged_in";
    private final String STATE_GOODBYE = "goodbye";
    private final String STATE_CHANGE_PASSWORD_OLD = "change_password_old";
    private final String STATE_CHANGE_PASSWORD_NEW = "change_password_new";
    private final String STATE_TRANSFER_TARGET = "transfer_target";
    private final String STATE_TRANSFER_AMOUNT = "transfer_amount";
    private final String STATE_TRANSFER_CONFIRM = "transfer_confirm";
    private final String STATE_WITHDRAW_CONFIRM = "withdraw_confirm";
    private final String STATE_CREATE_ACCOUNT_NUMBER = "create_account_number";
    private final String STATE_CREATE_PASSWORD = "create_password";
    private final String STATE_CREATE_BALANCE = "create_balance";
    private final String STATE_CREATE_TYPE = "create_type";

    // Variables representing the state and data of the ATM UIModel
    private String state = STATE_ACCOUNT_NO;    // Current state of the ATM
    private String accNumber = "";         // Account number being typed
    private String accPasswd = "";         // Password being typed
    private String pendingOldPassword = "";
    private String transferTargetAccount = "";
    private int pendingTransactionAmount = 0;
    private String newAccountNumber = "";
    private String newAccountPassword = "";
    private int newAccountBalance = 0;

    // Variables shown on the View display
    private String message;                // Message label text
    private String numberPadInput;         // Current number displayed in the TextField (as a string)
    private String result;                 // Contents of the TextArea (may be multiple lines)

    // UIModel constructor: pass a Bank object that the ATM interacts with
    public UIModel(Bank bank) {
        this.bank = bank;
    }

    // Initialize the ATM UIModel: this method is called by Main when starting the app
    // - Set state to STATE_ACCOUNT_NO
    // - Clear the numberPadInput - numbers displayed in the TextField
    // - Display the welcome message and user instructions
    public void initialise() {
        showWelcomePage("Welcome to the ATM");
        update();
    }

    // Reset the ATM UIModel after an invalid action or logout:
    // - Set state to STATE_ACCOUNT_NO
    // - Clear the numberPadInput
    // - Display the provided message and user instructions
    private void reset(String msg) {
        pendingOldPassword = "";
        transferTargetAccount = "";
        pendingTransactionAmount = 0;
        newAccountNumber = "";
        newAccountPassword = "";
        newAccountBalance = 0;
        showWelcomePage(msg);
    }

    // Change the ATM state and print a debug message whenever the state changes
    private void setState(String newState)
    {
        if ( !state.equals(newState) )
        {
            String oldState = state;
            state = newState;
            System.out.println("UIModel::setState: changed state from "+ oldState + " to " + newState);
        }
    }

    // These process**** methods are called by the Controller
    // in response to specific button presses on the GUI.

    // Handle a number button press: append the digit to numberPadInput
    public void processNumber(String numberOnButton) {
        if (acceptsNumericInput()) {
            numberPadInput += numberOnButton;
            message = getEntryMessage();
        } else if (state.equals(STATE_WELCOME)) {
            message = "Press Ent to start login or New to create an account";
        } else if (state.equals(STATE_GOODBYE)) {
            message = "Press Ent to return to the welcome page";
        } else {
            message = "Select one of the available actions";
        }
        update();
    }

    // Handle the Clear button: reset the current number stored in numberPadInput
    public void processClear() {
        if (acceptsNumericInput() && !numberPadInput.isEmpty()) {
            numberPadInput = "";
            message = getEntryMessage() + " - input cleared";
            update();
        } else if (!acceptsNumericInput()) {
            message = "Nothing to clear on this page";
            update();
        }
    }

    // Handle the Enter button.
    // This is a more complex method: pressing Enter causes the ATM to change state,
    // progressing from STATE_ACCOUNT_NO → STATE_PASSWORD → STATE_LOGGED_IN,
    // and back to STATE_ACCOUNT_NO when logging out.
    public void processEnter()
    {
        // The action depends on the current ATM state
        switch ( state )
        {
            case STATE_WELCOME:
                showAccountNumberPage("Login started");
                break;

            case STATE_ACCOUNT_NO:
                // Waiting for a complete account number
                // If nothing was entered, reset with "Invalid Account Number"
                if (numberPadInput.equals("")) {
                    showAccountNumberPage("Enter an account number first");
                }
                else{
                    accNumber = numberPadInput;
                    showPasswordPage("Account Number Accepted");
                }
                break;

            case STATE_PASSWORD:
                    // Waiting for a password
                    // Save the typed number as accPasswd, clear numberPadInput,
                    // then contact the bank to attempt login
                accPasswd = numberPadInput;
                if ( bank.login(accNumber, accPasswd) )
                {
                    showLoggedInPage("Logged In: " + bank.getLoggedInAccountType());
                    view.playSuccessSound();
                } else {
                    if (bank.loginLocked()) {
                        reset("Too many failed login attempts");
                        result = "Login has been reset.\nPress Ent to begin again.";
                        view.playErrorSound();
                        bank.resetLoginAttempts();
                    } else {
                        showPasswordPage("Login failed: " + bank.getRemainingLoginAttempts() + " attempt(s) left");
                        view.playErrorSound();
                    }
                }
                break;

            case STATE_CHANGE_PASSWORD_OLD:
                pendingOldPassword = numberPadInput;
                numberPadInput = "";
                if (!bank.loggedIn()) {
                    reset("You are not logged in");
                    view.playErrorSound();
                } else if (!bank.verifyLoggedInPassword(pendingOldPassword)) {
                    showChangePasswordOldPage("Old Password Incorrect");
                    view.playErrorSound();
                } else {
                    showChangePasswordNewPage("Old Password Accepted");
                }
                break;

            case STATE_CHANGE_PASSWORD_NEW:
                handlePasswordChange();
                break;

            case STATE_TRANSFER_TARGET:
                handleTransferTarget();
                break;

            case STATE_TRANSFER_AMOUNT:
                handleTransferAmount();
                break;

            case STATE_TRANSFER_CONFIRM:
                handleTransferConfirmation();
                break;

            case STATE_CREATE_ACCOUNT_NUMBER:
                handleCreateAccountNumber();
                break;

            case STATE_CREATE_PASSWORD:
                handleCreatePassword();
                break;

            case STATE_CREATE_BALANCE:
                handleCreateBalance();
                break;

            case STATE_CREATE_TYPE:
                handleCreateAccountType();
                break;

            case STATE_GOODBYE:
                showWelcomePage("Welcome back");
                break;

            case STATE_LOGGED_IN:
            default:
                message = "Choose a service: Dep, W/D, Bal, Trf, Pwd, or Fin";
        }

        update(); // Refresh the GUI to show messages and input
    }

    /**
     * Parses a string into a valid transaction amount.
     * - If the string is empty, invalid, or consists only of zeros, returns 0.
     * - Otherwise, returns the integer value.
     *
     * Purpose:
     * Helper method for validating user-entered amounts in transactions (Deposit, Withdraw, etc.).
     *
     * Note: If you later add features like Transfer, this method can be reused.
     */
    private int parseValidAmount(String number) {
        if (number.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0; // Invalid input -> treated as 0
        }
    }

    // Handle the Balance button:
    // - If the user is logged in, retrieve the current balance and update messages/results accordingly
    // - Otherwise, reset the ATM and display an error message
    public void processBalance() {
        if (state.equals(STATE_LOGGED_IN) ) {
            showLoggedInPage("Balance Available",
                    buildBalanceAndStatementSummary());
        } else {
            reset("You are not logged in");
            view.playErrorSound();
        }
        update();
    }

    public void processMiniStatement() {
        if (state.equals(STATE_LOGGED_IN)) {
            view.showMiniStatementWindow("Mini Statement", buildMiniStatementWindowSummary());
            message = "Mini statement opened";
        } else {
            reset("You are not logged in");
            view.playErrorSound();
        }
        update();
    }

    // Handle the Withdraw button:
    // If the user is logged in, attempt to withdraw the amount entered;
    // otherwise, reset the ATM and display an error message.
    // Reads the amount from numberPadInput, validates it, and updates messages/results accordingly.
    public void processWithdraw() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            if (amount > 0) {
                pendingTransactionAmount = amount;
                showWithdrawConfirmationPage("Confirm Withdrawal");
            }
            else{
                showLoggedInPage("Invalid Amount",
                        "Enter digits for the amount, then press W/D");
                view.playErrorSound();
            }
        }
        else if (state.equals(STATE_WITHDRAW_CONFIRM)) {
            if(bank.withdraw(pendingTransactionAmount)){
                showLoggedInPage("Withdraw Successful",
                        buildReceiptSummary("Withdrawn: " + formatCurrency(pendingTransactionAmount)));
                view.playSuccessSound();
            }
            else{
                showLoggedInPage("Withdraw Failed",
                        bank.getLoggedInAccountType() + "\nBalance remains: " + formatCurrency(bank.getBalance()));
                view.playErrorSound();
            }
            pendingTransactionAmount = 0;
        }
        else {
            reset("You are not logged in");
            view.playErrorSound();
        }
        update();
    }

    // Handle the Deposit button:
    // - If the user is logged in, deposit the amount entered into the bank
    // - Reads the amount from numberPadInput, validates it, and updates messages/results accordingly
    // - Otherwise, reset the ATM and display an error message
    public void processDeposit() {
        if (state.equals(STATE_LOGGED_IN)) {
            int amount = parseValidAmount(numberPadInput);
            if (amount > 0) {
                if (bank.deposit(amount)) {
                    showLoggedInPage("Deposit Successful",
                            buildReceiptSummary("Deposited: " + formatCurrency(amount)));
                    view.playSuccessSound();
                } else {
                    showLoggedInPage("Deposit Failed",
                            buildBalanceAndStatementSummary());
                    view.playErrorSound();
                }
            }
            else {
                showLoggedInPage("Invalid Amount",
                        "Enter digits for the amount, then press Dep");
                view.playErrorSound();
            }
        }
        else {
            reset("You are not logged in");
            view.playErrorSound();
        }
        update();
    }

    // Handle the Finish button:
    // - If the user is logged in, log out
    // - Otherwise, reset the ATM and display an error message
    public void processFinish() {
        if (state.equals(STATE_LOGGED_IN) ) {
            bank.logout();
            showGoodbyePage("Thank you for using the Bank ATM");
        } else if (state.equals(STATE_CHANGE_PASSWORD_OLD) || state.equals(STATE_CHANGE_PASSWORD_NEW)) {
            pendingOldPassword = "";
            showLoggedInPage("Password Change Cancelled");
        } else if (state.equals(STATE_TRANSFER_TARGET) || state.equals(STATE_TRANSFER_AMOUNT) || state.equals(STATE_TRANSFER_CONFIRM)) {
            transferTargetAccount = "";
            pendingTransactionAmount = 0;
            showLoggedInPage("Transfer Cancelled");
        } else if (state.equals(STATE_WITHDRAW_CONFIRM)) {
            pendingTransactionAmount = 0;
            showLoggedInPage("Withdrawal Cancelled");
        } else if (state.equals(STATE_CREATE_ACCOUNT_NUMBER) || state.equals(STATE_CREATE_PASSWORD)
                || state.equals(STATE_CREATE_BALANCE) || state.equals(STATE_CREATE_TYPE)) {
            reset("Account Creation Cancelled");
        } else if (state.equals(STATE_GOODBYE)) {
            showWelcomePage("Welcome back");
        } else {
            showGoodbyePage("No active session to finish");
        }
        update();
    }

    public void processTransfer() {
        if (state.equals(STATE_LOGGED_IN)) {
            transferTargetAccount = "";
            pendingTransactionAmount = 0;
            showTransferTargetPage("Transfer Between Accounts");
        } else if (state.equals(STATE_TRANSFER_CONFIRM)) {
            handleTransferConfirmation();
        } else {
            reset("You are not logged in");
            view.playErrorSound();
        }
        update();
    }

    public void processCreateAccount() {
        if (bank.loggedIn()) {
            showLoggedInPage("Finish current session first",
                    "Press Fin to log out before creating a new account");
        } else {
            newAccountNumber = "";
            newAccountPassword = "";
            newAccountBalance = 0;
            showCreateAccountNumberPage("Create New Account");
        }
        update();
    }

    public void processChangePassword() {
        if (state.equals(STATE_LOGGED_IN)) {
            pendingOldPassword = "";
            showChangePasswordOldPage("Change Password");
        } else {
            reset("Log in before changing password");
            view.playErrorSound();
        }
        update();
    }

    public void processBackToWelcome() {
        if (bank.loggedIn()) {
            bank.logout();
        }

        pendingOldPassword = "";
        transferTargetAccount = "";
        pendingTransactionAmount = 0;
        newAccountNumber = "";
        newAccountPassword = "";
        newAccountBalance = 0;
        showWelcomePage("Welcome to the ATM");
        update();
    }

    // Handle unknown or invalid buttons for the current state:
    // - Reset the ATM and display an "Invalid Command" message
    public void processUnknownKey(String action) {
        reset("Invalid Command");
        view.playErrorSound();
        update();
    }

    // Notify the View of changes by calling its update method
    private void update() {
        view.update(message, numberPadInput, result, state);
    }

    private String getWelcomeInstructions() {
        return "";
    }

    private String getLoggedInInstructions() {
        return bank.getLoggedInAccountFeatures()
                + "\n\nWhat to do next:\nEnter an amount, then press Dep or W/D.\nPress Bal for balance and mini statement.\nPress Mini for a popup receipt window.\nPress Trf to transfer money.\nPress Pwd to change password.\nPress Fin to log out.";
    }

    private String formatCurrency(int amount) {
        return "£" + amount;
    }

    private void handlePasswordChange() {
        String newPassword = numberPadInput;
        numberPadInput = "";

        if (!bank.loggedIn()) {
            reset("You are not logged in");
            return;
        }

        String passwordValidation = bank.validatePassword(newPassword);
        if (passwordValidation != null) {
            showChangePasswordNewPage("New Password Invalid",
                    passwordValidation + "\n" + bank.getPasswordRules());
            view.playErrorSound();
            return;
        }
        if (newPassword.equals(pendingOldPassword)) {
            showChangePasswordNewPage("New password must be different");
            view.playErrorSound();
            return;
        }
        if (bank.changeLoggedInPassword(pendingOldPassword, newPassword)) {
            pendingOldPassword = "";
            showLoggedInPage("Password Changed");
            view.playSuccessSound();
        } else {
            pendingOldPassword = "";
            showLoggedInPage("Password Change Failed");
            view.playErrorSound();
        }
    }

    private void handleTransferTarget() {
        String targetAccount = numberPadInput;
        numberPadInput = "";

        String accountValidation = bank.validateAccountNumber(targetAccount);
        if (accountValidation != null) {
            showTransferTargetPage("Target Account Invalid", accountValidation);
            view.playErrorSound();
            return;
        }
        if (!bank.accountExists(targetAccount)) {
            showTransferTargetPage("Target Account Invalid", "Target account was not found");
            view.playErrorSound();
            return;
        }
        if (accNumber.equals(targetAccount)) {
            showTransferTargetPage("Target Account Invalid", "You cannot transfer to the same account");
            view.playErrorSound();
            return;
        }

        transferTargetAccount = targetAccount;
        showTransferAmountPage("Target Account Accepted");
    }

    private void handleTransferAmount() {
        int amount = parseValidAmount(numberPadInput);
        String enteredAmount = numberPadInput;
        numberPadInput = "";

        if (amount <= 0) {
            showTransferAmountPage("Invalid Transfer Amount");
            view.playErrorSound();
            return;
        }
        pendingTransactionAmount = amount;
        showTransferConfirmationPage("Confirm Transfer",
                "Transfer " + formatCurrency(amount) + " to account " + transferTargetAccount + "?");
    }

    private void handleTransferConfirmation() {
        String transferError = bank.transfer(pendingTransactionAmount, transferTargetAccount);
        if (transferError == null) {
            showLoggedInPage("Transfer Successful",
                    buildReceiptSummary("Transferred: " + formatCurrency(pendingTransactionAmount)
                            + "\nTo account: " + transferTargetAccount));
            transferTargetAccount = "";
            pendingTransactionAmount = 0;
            view.playSuccessSound();
        } else {
            showTransferAmountPage("Transfer Failed", transferError);
            pendingTransactionAmount = 0;
            view.playErrorSound();
        }
    }

    private void handleCreateAccountNumber() {
        String candidateAccountNumber = numberPadInput;
        numberPadInput = "";

        String accountValidation = bank.validateAccountNumber(candidateAccountNumber);
        if (accountValidation != null) {
            showCreateAccountNumberPage("Account Number Invalid",
                    accountValidation + "\n" + bank.getAccountNumberRules());
            view.playErrorSound();
            return;
        }
        if (bank.accountExists(candidateAccountNumber)) {
            showCreateAccountNumberPage("Account Number Already Exists",
                    "Enter a different account number");
            view.playErrorSound();
            return;
        }

        newAccountNumber = candidateAccountNumber;
        showCreatePasswordPage("Account Number Accepted");
    }

    private void handleCreatePassword() {
        String candidatePassword = numberPadInput;
        numberPadInput = "";

        String passwordValidation = bank.validatePassword(candidatePassword);
        if (passwordValidation != null) {
            showCreatePasswordPage("Password Invalid",
                    passwordValidation + "\n" + bank.getPasswordRules());
            view.playErrorSound();
            return;
        }

        newAccountPassword = candidatePassword;
        showCreateBalancePage("Password Accepted");
    }

    private void handleCreateBalance() {
        String balanceInput = numberPadInput;
        int amount = parseValidAmount(balanceInput);
        numberPadInput = "";

        if (!balanceInput.matches("\\d+")) {
            showCreateBalancePage("Invalid Balance",
                    "Enter digits only for the starting balance");
            view.playErrorSound();
            return;
        }

        newAccountBalance = amount;
        showCreateTypePage("Balance Accepted");
    }

    private void handleCreateAccountType() {
        int accountType = parseValidAmount(numberPadInput);
        numberPadInput = "";

        if (accountType < Bank.TYPE_STUDENT || accountType > Bank.TYPE_STANDARD) {
            showCreateTypePage("Invalid Account Type");
            view.playErrorSound();
            return;
        }

        if (bank.createBankAccount(newAccountNumber, newAccountPassword, newAccountBalance, accountType)) {
            reset("New Account Created");
            result = "Welcome Page\n\nAccount " + newAccountNumber + " created successfully with opening balance " + formatCurrency(newAccountBalance) + ".\nPress Ent to log in, or New to create another account.";
            view.playSuccessSound();
        } else {
            reset("Account Creation Failed");
            view.playErrorSound();
        }
    }

    private boolean acceptsNumericInput() {
        return state.equals(STATE_ACCOUNT_NO)
                || state.equals(STATE_PASSWORD)
                || state.equals(STATE_CHANGE_PASSWORD_OLD)
                || state.equals(STATE_CHANGE_PASSWORD_NEW)
                || state.equals(STATE_TRANSFER_TARGET)
                || state.equals(STATE_TRANSFER_AMOUNT)
                || state.equals(STATE_TRANSFER_CONFIRM)
                || state.equals(STATE_CREATE_ACCOUNT_NUMBER)
                || state.equals(STATE_CREATE_PASSWORD)
                || state.equals(STATE_CREATE_BALANCE)
                || state.equals(STATE_CREATE_TYPE)
                || state.equals(STATE_LOGGED_IN);
    }

    private String getEntryMessage() {
        return switch (state) {
            case STATE_ACCOUNT_NO -> "Enter account number";
            case STATE_PASSWORD -> "Enter password";
            case STATE_CHANGE_PASSWORD_OLD -> "Enter current password";
            case STATE_CHANGE_PASSWORD_NEW -> "Enter new password";
            case STATE_TRANSFER_TARGET -> "Enter target account number";
            case STATE_TRANSFER_AMOUNT -> "Enter transfer amount";
            case STATE_TRANSFER_CONFIRM -> "Press Trf to confirm transfer";
            case STATE_CREATE_ACCOUNT_NUMBER -> "Enter new account number";
            case STATE_CREATE_PASSWORD -> "Enter new account password";
            case STATE_CREATE_BALANCE -> "Enter starting balance";
            case STATE_CREATE_TYPE -> "Enter account type number";
            case STATE_LOGGED_IN -> "Enter amount, then choose Dep or W/D";
            default -> "Enter digits";
        };
    }

    private void showWelcomePage(String msg) {
        setState(STATE_WELCOME);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = getWelcomeInstructions();
    }

    private void showAccountNumberPage(String msg) {
        setState(STATE_ACCOUNT_NO);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Login Page\n\nStep 1 of 2\nEnter your account number, then press Ent.\nPress New to create an account instead.";
    }

    private void showPasswordPage(String msg) {
        setState(STATE_PASSWORD);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Login Page\n\nStep 2 of 2\nEnter your password, then press Ent.\n" + bank.getPasswordRules();
    }

    private void showLoggedInPage(String msg) {
        showLoggedInPage(msg, buildBalanceAndStatementSummary());
    }

    private void showLoggedInPage(String msg, String summary) {
        setState(STATE_LOGGED_IN);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = summary + "\n\n" + getLoggedInInstructions();
    }

    private void showGoodbyePage(String msg) {
        setState(STATE_GOODBYE);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Press Ent to return to the welcome screen.";
    }

    private void showChangePasswordOldPage(String msg) {
        setState(STATE_CHANGE_PASSWORD_OLD);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Change Password\n\nStep 1 of 2\nEnter your current password, then press Ent.\nPress Fin to cancel.";
    }

    private void showChangePasswordNewPage(String msg) {
        showChangePasswordNewPage(msg, bank.getPasswordRules());
    }

    private void showChangePasswordNewPage(String msg, String extraInfo) {
        setState(STATE_CHANGE_PASSWORD_NEW);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Change Password\n\nStep 2 of 2\nEnter your new password, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showTransferTargetPage(String msg) {
        showTransferTargetPage(msg, "Enter the target account number, then press Ent.");
    }

    private void showTransferTargetPage(String msg, String extraInfo) {
        setState(STATE_TRANSFER_TARGET);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Transfer Money\n\nStep 1 of 2\nEnter the target account number, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showTransferAmountPage(String msg) {
        showTransferAmountPage(msg, "Enter the amount to transfer, then press Ent.");
    }

    private void showTransferAmountPage(String msg, String extraInfo) {
        setState(STATE_TRANSFER_AMOUNT);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Transfer Money\n\nStep 2 of 2\nTarget account: " + transferTargetAccount
                + "\nEnter the amount to transfer, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showTransferConfirmationPage(String msg, String extraInfo) {
        setState(STATE_TRANSFER_CONFIRM);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Transfer Money\n\nReview transfer\n"
                + extraInfo + "\nPress Trf to confirm or Fin to cancel.";
    }

    private void showWithdrawConfirmationPage(String msg) {
        setState(STATE_WITHDRAW_CONFIRM);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Withdraw Money\n\nReview withdrawal\nWithdraw " + formatCurrency(pendingTransactionAmount)
                + " from your account?\nPress W/D to confirm or Fin to cancel.";
    }

    private String buildBalanceAndStatementSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append(bank.getLoggedInAccountType())
                .append("\nBalance: ").append(formatCurrency(bank.getBalance()));

        String warning = bank.getLowBalanceWarning();
        if (!warning.isBlank()) {
            builder.append("\n").append(warning);
        }

        builder.append("\n\nMini Statement\n").append(bank.getMiniStatement());
        return builder.toString();
    }

    private String buildReceiptSummary(String headline) {
        return headline + "\n\n" + buildBalanceAndStatementSummary();
    }

    private String buildMiniStatementWindowSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append(bank.getLoggedInAccountType())
                .append("\nAccount: ").append(accNumber)
                .append("\nBalance: ").append(formatCurrency(bank.getBalance()));

        String warning = bank.getLowBalanceWarning();
        if (!warning.isBlank()) {
            builder.append("\n").append(warning);
        }

        builder.append("\n\nRecent Transactions\n").append(bank.getMiniStatement());
        return builder.toString();
    }

    private void showCreateAccountNumberPage(String msg) {
        showCreateAccountNumberPage(msg, bank.getAccountNumberRules());
    }

    private void showCreateAccountNumberPage(String msg, String extraInfo) {
        setState(STATE_CREATE_ACCOUNT_NUMBER);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Create Account\n\nStep 1 of 4\nEnter a new account number, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showCreatePasswordPage(String msg) {
        showCreatePasswordPage(msg, bank.getPasswordRules());
    }

    private void showCreatePasswordPage(String msg, String extraInfo) {
        setState(STATE_CREATE_PASSWORD);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Create Account\n\nStep 2 of 4\nEnter a password for the new account, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showCreateBalancePage(String msg) {
        showCreateBalancePage(msg, "Starting balance can be 0 or more.");
    }

    private void showCreateBalancePage(String msg, String extraInfo) {
        setState(STATE_CREATE_BALANCE);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Create Account\n\nStep 3 of 4\nEnter the starting balance, then press Ent.\n"
                + extraInfo + "\nPress Fin to cancel.";
    }

    private void showCreateTypePage(String msg) {
        setState(STATE_CREATE_TYPE);
        numberPadInput = BLANK_INPUT;
        message = msg;
        result = "Create Account\n\nStep 4 of 4\n" + bank.getAccountTypeMenu()
                + "\nEnter the account type number, then press Ent.\nPress Fin to cancel.";
    }
}

