package service;

import config.Config;
import dao.UserDao;
import model.AuthToken;
import model.Role;
import model.User;
import util.PasswordUtil;
import util.TokenUtil;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    private final UserDao userDao = new UserDao();
    private final int tokenTtlSeconds = Config.getInt("token.ttl.seconds");

    public void register(String login, String password, String role) throws Exception {
        if (role.equalsIgnoreCase(Role.ADMIN.toString()) && userDao.adminExists()) {
            logger.log(Level.WARNING, "Attempt to register an admin when one already exists.");
            throw new Exception("Администратор уже существует");
        }

        if (userDao.findByLogin(login) != null) {
            logger.log(Level.WARNING, "User registration failed: login already taken - " + login);
            throw new Exception("Пользователь с таким логином уже существует");
        }

        String passwordHash = PasswordUtil.hash(password, login);
        User user = new User(0, login, passwordHash, Role.valueOf(role));
        userDao.save(user);
        logger.info("User registered successfully: " + login);
    }

    public AuthToken login(String login, String password) throws Exception {
        User user = userDao.findByLogin(login);
        if (user == null) {
            logger.log(Level.WARNING, "Login attempt failed: user not found - " + login);
            throw new Exception("Пользователь не найден");
        }

        String hashed = PasswordUtil.hash(password, login);
        if (!user.passwordHash().equals(hashed)) {
            logger.log(Level.WARNING, "Login attempt failed: incorrect password for user - " + login);
            throw new Exception("Неверный пароль");
        }

        String token = TokenUtil.generateToken(user.id(), user.role().toString(), tokenTtlSeconds);
        logger.info("User logged in successfully: " + login);
        return new AuthToken(token, System.currentTimeMillis() + tokenTtlSeconds * 1000L);
    }

    public String delete(int userId) throws Exception {
        User user = userDao.findById(userId);
        if (user == null) {
            logger.log(Level.WARNING, "User deletion failed: user not found - " + userId);
            return "Пользователь не удален";
        }

        if (user.role() == Role.ADMIN) {
            logger.log(Level.WARNING, "Attempt to delete an admin user - " + userId);
            throw new Exception("Невозможно удалить администратора");
        }

        if (userDao.deleteUser(userId)) {
            logger.info("User deleted successfully: " + userId);
            return "Пользователь успешно удален";
        }

        logger.log(Level.WARNING, "User deletion failed: user not found - " + userId);
        return "Пользователь не удален";
    }

    public List<User> users() throws Exception {
        List<User> userList = userDao.getAllUsersExcludingAdmins();
        logger.info("Retrieved list of users excluding admins. Count: " + userList.size());
        return userList;
    }

    public boolean isLoginTaken(String login) throws Exception {
        return userDao.findByLogin(login) != null;
    }
}

