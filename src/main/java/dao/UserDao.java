package dao;

import model.Role;
import model.User;
import util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDao {
    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    private static final String SQL_CHECK_ADMIN_EXISTS = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
    private static final String SQL_INSERT_USER = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";
    private static final String SQL_FIND_BY_LOGIN = "SELECT * FROM users WHERE login = ?";
    private static final String SQL_DELETE_USER = "DELETE FROM users WHERE id = ?";
    private static final String SQL_DELETE_OTP = "DELETE FROM otp_codes WHERE user_id = ?";
    private static final String SQL_GET_ALL_USERS_EXCLUDING_ADMINS = "SELECT id, login, password_hash, role FROM users WHERE role != 'ADMIN'";
    private static final String SQL_FIND_BY_ID = "SELECT * FROM users WHERE id = ?";

    public boolean adminExists() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_CHECK_ADMIN_EXISTS)) {
            rs.next();
            boolean exists = rs.getInt(1) > 0;
            logger.info("Admin exists: " + exists);
            return exists;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking if admin exists", e);
            throw e;
        }
    }

    public void save(User user) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USER)) {
            ps.setString(1, user.login());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.role().toString().toUpperCase());
            ps.executeUpdate();
            logger.info("User saved: " + user.login());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving user: " + user.login(), e);
            throw e;
        }
    }

    public User findByLogin(String login) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_LOGIN)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role"))
                );
                logger.info("User found: " + user.login());
                return user;
            } else {
                logger.info("User not found: " + login);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding user by login: " + login, e);
            throw e;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteOtpStmt = conn.prepareStatement(SQL_DELETE_OTP);
                 PreparedStatement deleteUserStmt = conn.prepareStatement(SQL_DELETE_USER)) {

                deleteOtpStmt.setInt(1, userId);
                deleteOtpStmt.executeUpdate();

                deleteUserStmt.setInt(1, userId);
                deleteUserStmt.executeUpdate();
                conn.commit();
                logger.info("User deleted: " + userId);
                return true;
            } catch (SQLException e) {
                conn.rollback();
                logger.log(Level.SEVERE, "Error deleting user with ID: " + userId, e);
                throw e;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error establishing database connection for user deletion", e);
            throw e;
        }
    }

    public List<User> getAllUsersExcludingAdmins() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_ALL_USERS_EXCLUDING_ADMINS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
            logger.info("Retrieved " + users.size() + " users excluding admins.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving users excluding admins", e);
            throw e;
        }
        return users;
    }

    public User findById(int userId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role"))
                );
                logger.info("User found: " + user.login());
                return user;
            } else {
                logger.info("User not found: ID " + userId);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error finding user by ID: " + userId, e);
            throw e;
        }
    }

}

