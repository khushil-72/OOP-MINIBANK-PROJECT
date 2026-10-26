import static util.Validator.isValidMobile;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.BankInfo;
import model.Command;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.MenuOption;
import model.Premium;
import model.SavingsAccount;
import model.TransactionType;
import model.WithdrawRule;
import service.AccountStore;
import service.AccountWorker;
import service.Repository;
import service.ReportGenerator;
import service.StatePersister;
import service.TransactionBuffer;
import service.TransactionLog;
import service.TransactionProcessor;
import service.TransferTask;
import util.AnnotationValidator;
import util.CommandParser;
import util.SampleResource;
import util.StatementFormatter;
import util.Validator;

/**
 * MiniBank - the console banking application built across Practicals 1 to 13.
 *
 * main() prints the header (Practical 1), runs one demonstration section for
 * every later practical (2 to 13), and finally shows the menu loop (Practical 1).
 */
public class MiniBank {

    /** Practical 9: each of the 10 threads deposits 1 rupee this many times. */
    private static final int DEPOSITS_PER_THREAD = 1000;

    public static void main(String[] args) {
        // ---------- Practical 1: header ----------
        BankInfo info = new BankInfo("MiniBank", "CHARUSAT Branch");
        System.out.println("=====================================");
        System.out.println(info);
        System.out.println("=====================================");

        try {
            practical2();
            practical3();
            practical4();
            practical5();
            practical6();
            practical7();
            practical8();
            practical9();
            practical10();
            practical11();
            practical12();
            practical13();
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e);
        }

        // ---------- Practical 1: menu loop ----------
        runMenu();
    }

    // =====================================================================
    // Practical 1 - menu: enum + switch expression + Scanner loop.
    // The options use the Account classes, Repository (P13) and saving (P11).
    // =====================================================================
    private static final Path MENU_DATA_FILE = Paths.get("data", "bank.dat");

    private static void runMenu() {
        Scanner scanner = new Scanner(System.in);
        MenuOption[] options = MenuOption.values();
        Repository<Account> repository = new Repository<>();
        loadAccounts(repository);

        while (true) {
            System.out.println();
            System.out.println("------ MENU ------");
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.print("Enter your choice: ");

            if (!scanner.hasNext()) {
                break; // input ended
            }
            if (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Please enter a number from the menu.");
                continue;
            }
            int choice = scanner.nextInt();
            if (choice < 1 || choice > options.length) {
                System.out.println("Invalid choice. Please enter 1 to " + options.length + ".");
                continue;
            }

            MenuOption option = options[choice - 1];
            String message;
            try {
                message = switch (option) {
                    case OPEN_ACCOUNT -> openAccount(scanner, repository);
                    case DEPOSIT -> deposit(scanner, repository);
                    case WITHDRAW -> withdraw(scanner, repository);
                    case TRANSFER -> transfer(scanner, repository);
                    case EXIT -> "Thank you for using MiniBank. Goodbye!";
                };
            } catch (NoSuchElementException e) {
                break; // input ended in the middle of an option
            }
            System.out.println(message);

            if (option == MenuOption.EXIT) {
                break;
            }
        }
        saveAccounts(repository);
    }

    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.next();
    }

    private static Account find(Repository<Account> repository, String accountNumber) throws BankException {
        Account account = repository.findById(accountNumber.toUpperCase());
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    private static String openAccount(Scanner scanner, Repository<Account> repository) {
        String owner = ask(scanner, "Owner name: ");
        try {
            long opening = Long.parseLong(ask(scanner, "Opening balance: "));
            if (opening < 0) {
                return "Opening balance cannot be negative.";
            }
            Account account = repository.save(new SavingsAccount(owner, opening, 0));
            return "Account opened: " + account;
        } catch (NumberFormatException e) {
            return "Please enter the balance as a whole number.";
        }
    }

    private static String deposit(Scanner scanner, Repository<Account> repository) {
        String number = ask(scanner, "Account number: ");
        try {
            long amount = Long.parseLong(ask(scanner, "Amount: "));
            Account account = find(repository, number);
            account.deposit(amount);
            return "Deposited " + amount + ". New balance: " + account.getBalance();
        } catch (NumberFormatException e) {
            return "Please enter the amount as a whole number.";
        } catch (BankException e) {
            return e.getMessage();
        }
    }

    private static String withdraw(Scanner scanner, Repository<Account> repository) {
        String number = ask(scanner, "Account number: ");
        try {
            long amount = Long.parseLong(ask(scanner, "Amount: "));
            Account account = find(repository, number);
            account.withdraw(amount);
            return "Withdrew " + amount + ". New balance: " + account.getBalance();
        } catch (NumberFormatException e) {
            return "Please enter the amount as a whole number.";
        } catch (BankException e) {
            return e.getMessage();
        }
    }

    private static String transfer(Scanner scanner, Repository<Account> repository) {
        String fromNumber = ask(scanner, "From account number: ");
        String toNumber = ask(scanner, "To account number: ");
        try {
            long amount = Long.parseLong(ask(scanner, "Amount: "));
            Account from = find(repository, fromNumber);
            Account to = find(repository, toNumber);
            from.transfer(to, amount);
            return "Transferred " + amount + ". " + from.getAccountNumber() + " balance: " + from.getBalance()
                    + ", " + to.getAccountNumber() + " balance: " + to.getBalance();
        } catch (NumberFormatException e) {
            return "Please enter the amount as a whole number.";
        } catch (BankException e) {
            return "Transfer not done: " + e.getMessage();
        }
    }

    /** Practical 11 code used on start-up: load the accounts saved by the last run. */
    private static void loadAccounts(Repository<Account> repository) {
        if (!Files.exists(MENU_DATA_FILE)) {
            return;
        }
        try {
            Account[] saved = StatePersister.load(MENU_DATA_FILE);
            for (Account account : saved) {
                repository.save(account);
            }
            System.out.println();
            System.out.println("Loaded " + saved.length + " saved account(s) from " + MENU_DATA_FILE);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Could not load saved accounts: " + e.getMessage());
        }
    }

    /** Practical 11 code used on exit: save all accounts. */
    private static void saveAccounts(Repository<Account> repository) {
        try {
            StatePersister.save(repository.findAll().toArray(new Account[0]), MENU_DATA_FILE);
            System.out.println("Accounts saved to " + MENU_DATA_FILE);
        } catch (IOException e) {
            System.out.println("Could not save accounts: " + e.getMessage());
        }
    }

    private static void heading(String text) {
        System.out.println();
        System.out.println("===== " + text + " =====");
    }

    // =====================================================================
    // Practical 2 - Customer and Account (encapsulation, static ids, constructors)
    // =====================================================================
    private static void practical2() throws BankException {
        heading("Practical 2: Customer and Account");

        Customer customer1 = new Customer("Riya Patel", "riya@example.com", "9876543210");
        Customer customer2 = new Customer("Aman Shah", "aman@example.com", "9123456780");
        System.out.println(customer1.getCustomerId() + " -> " + customer1.getName());
        System.out.println(customer2.getCustomerId() + " -> " + customer2.getName());

        Account[] accounts = {
                new SavingsAccount("Riya", 5000, 0),
                new CurrentAccount("Aman", 1000, 2000),
                new FixedDepositAccount("Meera", 20000)
        };

        accounts[0].deposit(2000);
        System.out.println("After deposit(2000)   : " + accounts[0].getBalance());
        accounts[0].withdraw(3000);
        System.out.println("After withdraw(3000)  : " + accounts[0].getBalance());
        try {
            accounts[0].withdraw(10000);
        } catch (InsufficientFundsException e) {
            System.out.println("withdraw(10000) rejected: " + e.getMessage());
        }
        System.out.println("Balance still         : " + accounts[0].getBalance());

        accounts[1].deposit(500);
        accounts[1].withdraw(2500);
        accounts[2].deposit(1000);

        for (Account account : accounts) {
            System.out.println(account.getAccountNumber() + " (" + account.getOwnerName() + ") balance = "
                    + account.getBalance());
        }
    }

    // =====================================================================
    // Practical 3 - toString, equals/hashCode, nested Address, clone, instanceof
    // =====================================================================
    private static void practical3() {
        heading("Practical 3: toString, equals, nested class, clone");

        Account a1 = new SavingsAccount("Riya", 4000, 0);
        Account a2 = a1.clone(); // same accountNumber as a1
        System.out.println("a1 = " + a1);
        System.out.println("a2 = " + a2);
        System.out.println("a1.equals(a2)  : " + a1.equals(a2));
        System.out.println("same hashCode  : " + (a1.hashCode() == a2.hashCode()));
        System.out.println("a1 == a2       : " + (a1 == a2));

        Object obj = a1;
        System.out.println("obj instanceof Account : " + (obj instanceof Account));
        System.out.println("obj instanceof Customer: " + (obj instanceof Customer));

        Customer.Address address = new Customer.Address("12 MG Road", "Anand", "388421");
        Customer customer = new Customer("Riya Patel", "riya@example.com", "9876543210", address);
        Customer copy = customer.clone();
        System.out.println("customer = " + customer);
        System.out.println("address  = " + customer.getAddress());
        System.out.println("clone    = " + copy + " | " + copy.getAddress());
        System.out.println("clone is a different object: " + (customer != copy));
    }

    // =====================================================================
    // Practical 4 - validation, command parsing, StringBuilder statement
    // =====================================================================
    private static void practical4() {
        heading("Practical 4: Validation and command parsing");

        System.out.println("isValidMobile(9876543210): " + isValidMobile("9876543210")); // static import
        System.out.println("isValidMobile(12345)     : " + isValidMobile("12345"));
        System.out.println("isValidEmail(abc@xyz.com): " + Validator.isValidEmail("abc@xyz.com"));
        System.out.println("isValidEmail(abc@)       : " + Validator.isValidEmail("abc@"));
        System.out.println("isValidPan(ABCDE1234F)   : " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("isValidPan(abcde1234)    : " + Validator.isValidPan("abcde1234"));
        System.out.println("isValidIfsc(SBIN0001234) : " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("isValidIfsc(SBIN1001234) : " + Validator.isValidIfsc("SBIN1001234"));

        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("type=" + command.type() + ", accountNumber=" + command.accountNumber() + ", amount="
                + command.amount());

        Account account = new SavingsAccount("Riya", 4000, 0);
        System.out.println(StatementFormatter.buildStatement(account));
    }

    // =====================================================================
    // Practical 5 - abstract Account, subclasses, polymorphism, pattern instanceof
    // =====================================================================
    private static void practical5() {
        heading("Practical 5: Inheritance and polymorphism");

        Account[] accounts = {
                new SavingsAccount("Riya", 5000, 500),
                new CurrentAccount("Aman", 1000, 2000),
                new FixedDepositAccount("Meera", 20000)
        };
        for (Account account : accounts) {
            System.out.println(account.getClass().getSimpleName() + " interestRate() = " + account.interestRate());
            if (account instanceof SavingsAccount savings) {
                System.out.println("   (savings account, minimum balance " + savings.getMinBalance() + ")");
            }
        }
    }

    // =====================================================================
    // Practical 6 - interfaces, functional interface, anonymous class, lambda, marker, static import
    // =====================================================================
    private static void practical6() {
        heading("Practical 6: Interfaces, lambda, packages");

        Account account = new SavingsAccount("Riya", 9000, 1000);
        System.out.println("yearlyInterest() = " + account.yearlyInterest());

        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account acc, long amount) {
                return amount <= 10000 && acc.canWithdraw(amount);
            }
        };
        WithdrawRule lambdaRule = (acc, amount) -> amount <= 10000 && acc.canWithdraw(amount);

        long[] amounts = {500, 8000, 8500, 20000};
        for (long amount : amounts) {
            System.out.println("amount " + amount + " -> anonymous: " + anonymousRule.allow(account, amount)
                    + ", lambda: " + lambdaRule.allow(account, amount));
        }

        Account fixed = new FixedDepositAccount("Meera", 20000);
        System.out.println("FixedDepositAccount is Premium: " + (fixed instanceof Premium));
        System.out.println("SavingsAccount is Premium     : " + (account instanceof Premium));
    }

    // =====================================================================
    // Practical 7 - annotations + reflection
    // =====================================================================
    private static void practical7() {
        heading("Practical 7: Annotations and reflection");

        Account bad = new SavingsAccount("Riya", -100, 0);
        System.out.println("Account with balance -100: " + Arrays.toString(AnnotationValidator.validate(bad)));

        Account good = new SavingsAccount("Riya", 100, 0);
        System.out.println("Valid account           : " + Arrays.toString(AnnotationValidator.validate(good)));

        Account longName = new SavingsAccount("A very very long owner name that is too long", 100, 0);
        System.out.println("Owner name too long     : " + Arrays.toString(AnnotationValidator.validate(longName)));
    }

    // =====================================================================
    // Practical 8 - custom exceptions, try/catch/finally, try-with-resources
    // =====================================================================
    private static void practical8() {
        heading("Practical 8: Exception handling");

        Account account = new SavingsAccount("Riya", 1000, 0);
        try {
            account.withdraw(5000);
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage() + " (shortfall = " + e.getShortfall() + ")");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Balance is still " + account.getBalance());
        }

        try {
            account.deposit(-50);
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }

        Account other = new CurrentAccount("Aman", 0, 0);
        try {
            account.transfer(other, 400);
            System.out.println("Transfer done: " + account.getBalance() + " / " + other.getBalance());
            account.transfer(other, 5000);
        } catch (BankException e) {
            System.out.println("Transfer rethrown and caught in main: " + e.getMessage());
        }

        try (SampleResource resource = new SampleResource("Statement printer")) {
            resource.use();
            throw new IllegalStateException("something went wrong inside the block");
        } catch (IllegalStateException e) {
            System.out.println("Caught after the resource was closed: " + e.getMessage());
        }
    }

    // =====================================================================
    // Practical 9 - threads, race condition, synchronization
    // =====================================================================
    private static void practical9() throws InterruptedException {
        heading("Practical 9: Threads and synchronization");

        Account.setThreadSafe(false);
        System.out.println("--- Without synchronization ---");
        long unsafeBalance = runDepositThreads();
        Account.setThreadSafe(true);
        System.out.println("--- With synchronization ---");
        long safeBalance = runDepositThreads();

        System.out.println("Expected balance            : " + (10L * DEPOSITS_PER_THREAD));
        System.out.println("Balance without synchronized: " + unsafeBalance);
        System.out.println("Balance with synchronized   : " + safeBalance);
    }

    private static long runDepositThreads() throws InterruptedException {
        Account account = new SavingsAccount("Shared", 0, 0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            threads.add(new Thread(new AccountWorker(account, DEPOSITS_PER_THREAD, 1), "Worker-" + i));
        }
        System.out.println("States before start: " + states(threads));
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("States after join  : " + states(threads));
        return account.getBalance();
    }

    private static String states(List<Thread> threads) {
        StringBuilder sb = new StringBuilder("[");
        for (Thread thread : threads) {
            sb.append(thread.getName()).append('=').append(thread.getState()).append(' ');
        }
        return sb.toString().trim() + "]";
    }

    // =====================================================================
    // Practical 10 - thread pool, wait/notify producer-consumer, deadlock and fix
    // =====================================================================
    private static void practical10() throws InterruptedException {
        heading("Practical 10: Thread pool, wait/notify, deadlock");

        // --- thread pool ---
        System.out.println("--- Thread pool (4 threads) ---");
        TransactionProcessor processor = new TransactionProcessor();
        Account pooled = new SavingsAccount("Pool", 1000, 0);
        processor.register(pooled);
        for (int i = 0; i < 5; i++) {
            processor.submit(processor.taskFor(new Command(TransactionType.DEPOSIT, pooled.getAccountNumber(), 100)));
            processor.submit(processor.taskFor(new Command(TransactionType.WITHDRAW, pooled.getAccountNumber(), 50)));
        }
        processor.stop();
        System.out.println("Balance after 5 deposits of 100 and 5 withdrawals of 50: " + pooled.getBalance()
                + " (expected 1250)");

        // --- producer / consumer ---
        System.out.println("--- Producer-consumer (wait/notify) ---");
        TransactionBuffer buffer = new TransactionBuffer(3);
        Account target = new SavingsAccount("Buffer", 0, 0);
        int items = 6;
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= items; i++) {
                    Command command = new Command(TransactionType.DEPOSIT, target.getAccountNumber(), i * 10L);
                    buffer.put(command);
                    System.out.println("Produced: " + command);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= items; i++) {
                    Command command = buffer.take();
                    target.deposit(command.amount());
                    System.out.println("Consumed: " + command);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BankException e) {
                System.out.println("Consumer error: " + e.getMessage());
            }
        }, "Consumer");
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Balance after consuming all items: " + target.getBalance() + " (expected 210)");

        // --- deadlock ---
        System.out.println("--- Deadlock: opposite lock order ---");
        Account a = new SavingsAccount("A", 1000, 0);
        Account b = new SavingsAccount("B", 1000, 0);
        Thread t1 = new Thread(new TransferTask(a, b, 100, false), "Transfer-A-to-B");
        Thread t2 = new Thread(new TransferTask(b, a, 50, false), "Transfer-B-to-A");
        t1.setDaemon(true); // daemon so the JVM can still exit while these two are stuck
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join(1500);
        t2.join(1500);
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlocked = bean.findDeadlockedThreads();
        if (deadlocked != null) {
            System.out.println("DEADLOCK detected: " + deadlocked.length + " threads are waiting for each other forever");
        } else {
            System.out.println("No deadlock detected");
        }

        // --- fix: always lock the smaller accountNumber first ---
        System.out.println("--- Fixed: lock the smaller accountNumber first ---");
        Account c = new SavingsAccount("C", 1000, 0);
        Account d = new SavingsAccount("D", 1000, 0);
        Thread t3 = new Thread(new TransferTask(c, d, 100, true), "Transfer-C-to-D");
        Thread t4 = new Thread(new TransferTask(d, c, 50, true), "Transfer-D-to-C");
        t3.start();
        t4.start();
        t3.join();
        t4.join();
        System.out.println("Balances: C=" + c.getBalance() + " (expected 950), D=" + d.getBalance()
                + " (expected 1050)");
    }

    // =====================================================================
    // Practical 11 - serialization, transaction log, NIO report
    // =====================================================================
    private static void practical11() throws IOException, ClassNotFoundException {
        heading("Practical 11: Persistence and report");

        Path dataDir = Paths.get("data");
        Path accountsFile = dataDir.resolve("accounts.dat");
        Path logsDir = dataDir.resolve("logs");
        Path reportFile = dataDir.resolve("report.txt");

        // --- save and reload accounts ---
        Account[] saved = {
                new SavingsAccount("Riya", 5000, 500),
                new CurrentAccount("Aman", 2000, 1000),
                new FixedDepositAccount("Meera", 9000)
        };
        StatePersister.save(saved, accountsFile);
        Account[] loaded = StatePersister.load(accountsFile);
        System.out.println("Saved accounts:");
        for (Account account : saved) {
            System.out.println("   " + account);
        }
        System.out.println("Reloaded accounts:");
        for (int i = 0; i < loaded.length; i++) {
            System.out.println("   " + loaded[i] + "   (same as saved: " + loaded[i].equals(saved[i]) + ")");
        }

        // --- transaction log (start from empty logs so the totals are predictable) ---
        Files.createDirectories(logsDir);
        try (DirectoryStream<Path> old = Files.newDirectoryStream(logsDir)) {
            for (Path path : old) {
                Files.deleteIfExists(path);
            }
        }
        String ac1 = saved[0].getAccountNumber();
        String ac2 = saved[1].getAccountNumber();
        String ac3 = saved[2].getAccountNumber();
        TransactionLog.append(logsDir.resolve("day1.log"), "DEPOSIT " + ac1 + " 500");
        TransactionLog.append(logsDir.resolve("day1.log"), "WITHDRAW " + ac2 + " 200");
        TransactionLog.append(logsDir.resolve("day1.log"), "DEPOSIT " + ac3 + " 1500");
        TransactionLog.append(logsDir.resolve("day2.log"), "WITHDRAW " + ac1 + " 300");
        TransactionLog.append(logsDir.resolve("day2.log"), "DEPOSIT " + ac2 + " 700");

        // --- report ---
        List<String> report = ReportGenerator.generate(logsDir, reportFile);
        System.out.println("Report written to " + reportFile + ":");
        for (String line : report) {
            System.out.println("   " + line);
        }
        System.out.println("(expected: deposits 2700, withdrawals 500, net 2200)");
    }

    // =====================================================================
    // Practical 12 - collections: HashMap store, ArrayList history, sorting
    // =====================================================================
    private static void practical12() throws BankException {
        heading("Practical 12: Collections");

        AccountStore store = new AccountStore();
        Account first = new SavingsAccount("Riya", 5000, 0);
        Account second = new SavingsAccount("Aman", 2000, 0);
        Account third = new SavingsAccount("Meera", 9000, 0);
        store.add(first);
        store.add(second);
        store.add(third);

        System.out.println("get(" + second.getAccountNumber() + ") = " + store.get(second.getAccountNumber()));
        System.out.println("Accounts sorted by balance (highest first):");
        for (Account account : store.sortedByBalance()) {
            System.out.println("   " + account);
        }
        System.out.println("History: " + store.getHistory());

        List<Account> byNumber = store.getAll();
        java.util.Collections.sort(byNumber); // uses Comparable (compareTo by account number)
        System.out.println("Sorted by account number (Comparable): " + byNumber);

        try {
            store.get("AC9999");
        } catch (BankException e) {
            System.out.println(e.getMessage());
        }
    }

    // =====================================================================
    // Practical 13 - generics: Repository<T> with @Id found by reflection
    // =====================================================================
    private static void practical13() {
        heading("Practical 13: Generic Repository");

        Repository<Account> accountRepo = new Repository<>();
        Account x = new SavingsAccount("Riya", 5000, 0);
        Account y = new CurrentAccount("Aman", 2000, 500);
        Account z = new FixedDepositAccount("Meera", 9000);
        accountRepo.saveAll(x, y, z);

        System.out.println("findById(" + y.getAccountNumber() + ") = " + accountRepo.findById(y.getAccountNumber()));
        System.out.println("findAll():");
        for (Account account : accountRepo.findAll()) {
            System.out.println("   " + account);
        }
        System.out.println("delete(" + z.getAccountNumber() + ") = " + accountRepo.delete(z.getAccountNumber()));
        System.out.println("findAll().size() after delete = " + accountRepo.findAll().size());

        Repository<Customer> customerRepo = new Repository<>();
        Customer customer = customerRepo.save(new Customer("Riya Patel", "riya@example.com", "9876543210"));
        System.out.println("Same Repository class for Customer: " + customerRepo.findById(customer.getCustomerId()));

        Long[] balances = {5000L, 2000L, 9000L};
        Integer[] numbers = {1, 2, 3, 4};
        System.out.println("average(Long[])    = " + Repository.average(balances));
        System.out.println("average(Integer[]) = " + Repository.average(numbers));
    }
}
