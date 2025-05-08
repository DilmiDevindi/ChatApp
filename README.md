# Chat Application

A Java-based chat application with client-server architecture using RMI and Hibernate.

## Hibernate Configuration

The application uses Hibernate for database operations. The session factory is configured in `hibernate.cfg.xml` with the name "someName".

## HibernateUtil Class

A utility class `HibernateUtil` has been added to provide safe access to the Hibernate session factory and sessions. This class handles the case where `SessionFactoryRegistry.getSessionFactory("someName")` returns null by falling back to the default session factory.

### Usage

Instead of directly calling:

```java
SessionFactoryRegistry.getSessionFactory("someName").openSession();
```

Which can cause a NullPointerException if the session factory is not found, use:

```java
// Get a session using the named session factory, with fallback to default
Session session = HibernateUtil.openSession("someName");

// Or use the default session factory
Session session = HibernateUtil.openSession();
```

The `HibernateUtil` class provides the following methods:

- `getSessionFactory()`: Get the default session factory
- `getSessionFactoryByName(String name)`: Get a session factory by name, falling back to the default if not found
- `openSession()`: Open a session using the default session factory
- `openSession(String name)`: Open a session using the named session factory, with fallback to default

## Server Implementation

The server initializes the Hibernate session factory in its constructor and uses it throughout the application for database operations.

## Database Configuration

The application uses MySQL with the following configuration:

- Database: chatdb
- Username: root
- Password: 1234
- Host: localhost
- Port: 3306

The database is created automatically if it doesn't exist when the server starts.