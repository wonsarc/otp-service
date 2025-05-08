package model;


public record User(int id, String login, String passwordHash, Role role) {
}
