package com.example.chatapp.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Utility class for Hibernate operations.
 * Provides safe access to the session factory and sessions.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    static {
        try {
            // Load Hibernate configuration from hibernate.cfg.xml
            Configuration configuration = new Configuration().configure();
            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
            System.err.println("Error initializing Hibernate session factory: " + e.getMessage());
            e.printStackTrace();
        }
    }
