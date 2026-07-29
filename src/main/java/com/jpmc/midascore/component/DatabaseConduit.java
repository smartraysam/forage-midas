package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DatabaseConduit {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConduit.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    public DatabaseConduit(UserRepository userRepository,
                            TransactionRepository transactionRepository,
                            RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    /**
     * Validates and, if valid, records the given transaction:
     *  - senderId must correspond to an existing UserRecord
     *  - recipientId must correspond to an existing UserRecord
     *  - sender's balance must be >= transaction amount
     *
     * On success: posts the transaction to the incentive API, then persists
     * a new TransactionRecord (amount + incentive) and adjusts balances --
     * sender is debited by the transaction amount only; recipient is
     * credited with the transaction amount PLUS the incentive.
     *
     * On failure: no database changes are made and the incentive API is
     * never called.
     *
     * @return true if the transaction was valid and recorded, false otherwise
     */
    public boolean processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            log.info("Transaction rejected: unknown sender ({}) or recipient ({})",
                    transaction.getSenderId(), transaction.getRecipientId());
            return false;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            log.info("Transaction rejected: {} has insufficient balance ({} < {})",
                    sender.getName(), sender.getBalance(), transaction.getAmount());
            return false;
        }

        float incentiveAmount = fetchIncentive(transaction);

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        transactionRepository.save(record);

        log.info("Transaction recorded: {} -> {}, amount={}, incentive={}",
                sender.getName(), recipient.getName(), transaction.getAmount(), incentiveAmount);
        log.info("Updated balance for {}: {}", sender.getName(), sender.getBalance());
        log.info("Updated balance for {}: {}", recipient.getName(), recipient.getBalance());

        return true;
    }

    private float fetchIncentive(Transaction transaction) {
        Incentive incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
        return incentive != null ? incentive.getAmount() : 0f;
    }

    /**
     * Looks up a user by name and logs their current balance.
     * Useful for debugging/inspection during test runs.
     */
    public void logBalanceByName(String name) {
        UserRecord user = userRepository.findByName(name);
        if (user == null) {
            log.info("No user found with name '{}'", name);
        } else {
            log.info("Balance for '{}': {}", user.getName(), user.getBalance());
        }
    }
}