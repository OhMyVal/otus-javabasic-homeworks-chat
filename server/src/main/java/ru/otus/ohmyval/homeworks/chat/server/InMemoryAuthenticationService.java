package ru.otus.ohmyval.homeworks.chat.server;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuthenticationService implements AuthenticationService {
    private class User {
        private String login;
        private String password;
        private String nickname;
        private Role role;

        private void setNickname(String nickname) {
            this.nickname = nickname;
        }

        private void setRole(Role role) {
            this.role = role;
        }

        public User(String login, String password, String nickname, Role role) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
            this.role = role;
        }
    }

    private List<User> users;
    private List<User> banUsers;

    public InMemoryAuthenticationService() {
        this.banUsers = new ArrayList<>();
        this.users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            this.users.add(new User("login" + i, "pass" + i, "nick" + i, Role.USER));
        }
        users.get(0).setRole(Role.ADMIN);
        users.get(1).setRole(Role.ADMIN);

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.nickname;
            }
        }
        return null;
    }

    @Override
    public boolean register(String login, String password, String nickname, Role role) {
        if (isLoginAlreadyExist(login)) {
            return false;
        }
        if (isNicknameAlreadyExist(nickname)) {
            return false;
        }
        users.add(new User(login, password, nickname, role));
        return true;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNicknameAlreadyExist(String nickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserRoleAdmin(ClientHandler clientHandler) {
        String senderNickname = clientHandler.getNickname();
        for (User u : users) {
            if (u.nickname.equals(senderNickname) && u.role.equals(Role.ADMIN)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean changeNickname(ClientHandler clientHandler, String newNickname) {
        for (User u : users) {
            if (u.nickname.equals(clientHandler.getNickname())) {
                u.setNickname(newNickname);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean addToBan(String banNickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(banNickname) && !banUsers.contains(u)) {
                banUsers.add(u);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean removeFromBan(String banNickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(banNickname) && banUsers.contains(u)) {
                banUsers.remove(u);
                return true;
            }
        }
        return false;
    }
}
