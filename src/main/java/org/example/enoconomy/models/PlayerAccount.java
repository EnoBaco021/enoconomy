package org.example.enoconomy.models;

import java.sql.Timestamp;
import java.util.UUID;

public class PlayerAccount {

    private final UUID uuid;
    private String username;
    private double balance;
    private final Timestamp createdAt;
    private Timestamp lastSeen;

    public PlayerAccount(UUID uuid, String username, double balance, Timestamp createdAt, Timestamp lastSeen) {
        this.uuid = uuid;
        this.username = username;
        this.balance = balance;
        this.createdAt = createdAt;
        this.lastSeen = lastSeen;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Timestamp lastSeen) {
        this.lastSeen = lastSeen;
    }
}
