package ru.otus.ohmyval.homeworks.chat.server;

public interface AuthenticationService {
    String getNicknameByLoginAndPassword(String login, String password);
    boolean register(String login, String password, String nickname, Role role);
    boolean isLoginAlreadyExist(String login);
    boolean isNicknameAlreadyExist(String nickname);
    boolean isUserRoleAdmin(ClientHandler clientHandler);
    boolean changeNickname(ClientHandler clientHandler, String newNickname);
    boolean addToBan(String banNickname);
    boolean removeFromBan(String banNickname);
}
