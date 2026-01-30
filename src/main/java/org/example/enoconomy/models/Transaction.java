package org.example.enoconomy.models;

import java.sql.Timestamp;
import java.util.UUID;

public class Transaction {

    private final int id;
    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final double amount;
    private final String type;
    private final String description;
    private final Timestamp createdAt;

    public Transaction(int id, UUID senderUuid, UUID receiverUuid, double amount, String type, String description, Timestamp createdAt) {
        this.id = id;
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public UUID getReceiverUuid() {
        return receiverUuid;
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}

