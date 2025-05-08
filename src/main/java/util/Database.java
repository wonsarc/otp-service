package util;

import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    static {
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("PostgreSQL JDBC-драйвер загружен успешно.");
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL JDBC-драйвер не найден", e);
            throw new RuntimeException("PostgreSQL JDBC-драйвер не найден", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        logger.info("Попытка установить соединение с базой данных.");
        Connection connection = DriverManager.getConnection(
                Config.get("db.url"),
                Config.get("db.username"),
                Config.get("db.password")
        );
        logger.info("Соединение с базой данных установлено успешно.");
        return connection;
    }
}
