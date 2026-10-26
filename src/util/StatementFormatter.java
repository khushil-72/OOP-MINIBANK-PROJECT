package util;

import model.Account;

/** Practical 4: builds a multi-line account statement with a StringBuilder. */
public class StatementFormatter {
    private StatementFormatter() {
    }

    public static String buildStatement(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append("------ ACCOUNT STATEMENT ------\n");
        sb.append("Account No : ").append(account.getAccountNumber()).append('\n');
        sb.append("Owner      : ").append(account.getOwnerName()).append('\n');
        sb.append("Type       : ").append(account.getClass().getSimpleName()).append('\n');
        sb.append("Balance    : ").append(account.getBalance()).append('\n');
        sb.append("Active     : ").append(account.isActive()).append('\n');
        sb.append("-------------------------------");
        return sb.toString();
    }
}
