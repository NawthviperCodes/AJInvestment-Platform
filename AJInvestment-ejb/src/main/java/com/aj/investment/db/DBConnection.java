package com.aj.investment.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class DBConnection {

    private static final String DB_NAME = "aj_investment";

    // Used only by DatabaseInitializer to create the database
    public static void ensureDatabaseExists() throws SQLException {
        // Connect without a database to create it
        try {
            Connection conn = getBaseConnection();
            conn.createStatement().executeUpdate(
                "CREATE DATABASE IF NOT EXISTS `" + DB_NAME + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
            System.out.println("Database '" + DB_NAME + "' ensured.");
            conn.close();
        } catch (Exception e) {
            throw new SQLException("Could not create database: " + e.getMessage(), e);
        }
    }

    // Gets connection via JNDI datasource (used by DatabaseInitializer after DB exists)
    public static Connection getConnection() throws SQLException {
        try {
            DataSource ds = (DataSource) new InitialContext().lookup("java:/jdbc/AJInvestmentDS");
            return ds.getConnection();
        } catch (Exception e) {
            throw new SQLException("JNDI lookup failed: " + e.getMessage(), e);
        }
    }

    // Base connection without database — for DB creation only
    private static Connection getBaseConnection() throws SQLException {
        try {
            // Use JNDI datasource which connects to / (no database in URL now)
            DataSource ds = (DataSource) new InitialContext().lookup("java:/jdbc/AJInvestmentDS");
            return ds.getConnection();
        } catch (Exception e) {
            throw new SQLException("Base connection failed: " + e.getMessage(), e);
        }
    }
}