package com.aj.investment.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final String DB_NAME = "aj_investment";

    public static void initialize() {

        try {
        DBConnection.ensureDatabaseExists();
    } catch (SQLException e) {
        System.err.println("Could not create database: " + e.getMessage());
        return;
    }
        
         // Now connect and create tables — but use direct JDBC with DB in URL
    String url = "jdbc:mysql://localhost:3306/" + DB_NAME + 
                 "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
        stmt.executeUpdate("USE `aj_investment`"); // select the database

            System.out.println("Connected to " + DB_NAME + ". Starting table verification...");

            // 1. LogdataStatus
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS LogdataStatus (
                        id     INT AUTO_INCREMENT PRIMARY KEY,
                        status VARCHAR(50) NOT NULL
                    ) ENGINE=InnoDB
                """);

                String[] statuses = {
                        "Active",
                        "Locked",
                        "Cancelled",
                        "1 Failed Login Attempt",
                        "2 Failed Login Attempt"
                };

                for (String s : statuses) {
                    stmt.executeUpdate(
                        "INSERT INTO LogdataStatus (status) " +
                        "SELECT '" + s + "' WHERE NOT EXISTS " +
                        "(SELECT 1 FROM LogdataStatus WHERE status = '" + s + "')"
                    );
                }

                System.out.println("Check complete: LogdataStatus");

            } catch (SQLException e) {
                System.err.println("Error with LogdataStatus: " + e.getMessage());
            }

            // 2. Userroles
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Userroles (
                        id       INT AUTO_INCREMENT PRIMARY KEY,
                        roleName VARCHAR(50) NOT NULL
                    ) ENGINE=InnoDB
                """);

                String[] roles = {"Admin", "Manager", "Employee", "Guest"};

                for (String r : roles) {
                    stmt.executeUpdate(
                        "INSERT INTO Userroles (roleName) " +
                        "SELECT '" + r + "' WHERE NOT EXISTS " +
                        "(SELECT 1 FROM Userroles WHERE roleName = '" + r + "')"
                    );
                }

                System.out.println("Check complete: Userroles");

            } catch (SQLException e) {
                System.err.println("Error with Userroles: " + e.getMessage());
            }

            // 3. Address
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Address (
                        id         INT AUTO_INCREMENT PRIMARY KEY,
                        street     VARCHAR(255),
                        city       VARCHAR(100),
                        province   VARCHAR(100),
                        postalCode VARCHAR(20),
                        country    VARCHAR(100),
                        longitude  DECIMAL(9,6),
                        latitude   DECIMAL(9,6)
                    ) ENGINE=InnoDB
                """);

                System.out.println("Check complete: Address");

            } catch (SQLException e) {
                System.err.println("Error with Address: " + e.getMessage());
            }

            // 4. Logdata
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Logdata (
                        id             INT AUTO_INCREMENT PRIMARY KEY,
                        firstName      VARCHAR(255),
                        lastName       VARCHAR(255),
                        email          VARCHAR(255) UNIQUE,
                        telephone      VARCHAR(20),
                        username       VARCHAR(255) UNIQUE,
                        password       VARCHAR(255),
                        status_id      INT,
                        profilePicture VARCHAR(255),
                        creationTime   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX (status_id),
                        FOREIGN KEY (status_id)
                            REFERENCES LogdataStatus(id)
                            ON DELETE SET NULL
                    ) ENGINE=InnoDB
                """);

                System.out.println("Check complete: Logdata");

            } catch (SQLException e) {
                System.err.println("Error with Logdata: " + e.getMessage());
            }

            // 5. Clients
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Clients (
                        id           INT AUTO_INCREMENT PRIMARY KEY,
                        logDataId    INT,
                        creationTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX (logDataId),
                        FOREIGN KEY (logDataId)
                            REFERENCES Logdata(id)
                            ON DELETE SET NULL
                    ) ENGINE=InnoDB
                """);

                System.out.println("Check complete: Clients");

            } catch (SQLException e) {
                System.err.println("Error with Clients: " + e.getMessage());
            }

            // 6. Services
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Services (
                        id            INT AUTO_INCREMENT PRIMARY KEY,
                        name          VARCHAR(255),
                        creationTime  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        employeeId    INT,
                        serviceTypeID INT
                    ) ENGINE=InnoDB
                """);

                System.out.println("Check complete: Services");

            } catch (SQLException e) {
                System.err.println("Error with Services: " + e.getMessage());
            }

            // 7. ClientID_ServicesID
            try {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ClientID_ServicesID (
                        id        INT AUTO_INCREMENT PRIMARY KEY,
                        clientId  INT,
                        serviceId INT,
                        INDEX (clientId),
                        INDEX (serviceId),
                        FOREIGN KEY (clientId)
                            REFERENCES Clients(id)
                            ON DELETE CASCADE,
                        FOREIGN KEY (serviceId)
                            REFERENCES Services(id)
                            ON DELETE CASCADE
                    ) ENGINE=InnoDB
                """);

                System.out.println("Check complete: ClientID_ServicesID");

            } catch (SQLException e) {
                System.err.println("Error with ClientID_ServicesID: " + e.getMessage());
            }

            System.out.println("Initialization process finished.");

        } catch (SQLException e) {
            System.err.println("Connection Error: " + e.getMessage());
        }
    }
}