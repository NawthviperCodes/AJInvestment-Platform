/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.aj.investment.servlet;

 
import com.aj.investment.db.DatabaseInitializer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class InitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=================================");
        System.out.println("Application starting...");
        System.out.println("Initializing database...");
        System.out.println("=================================");
        DatabaseInitializer.initialize();  // creates DB + all tables
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("=================================");
        System.out.println("Application stopped.");
        System.out.println("=================================");
    }
}