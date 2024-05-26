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
    private List<User> tempBanUsers;
    private List<User> permBanUsers;

    public InMemoryAuthenticationService() {
        this.tempBanUsers = new ArrayList<>();
        this.permBanUsers = new ArrayList<>();
        this.users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            this.users.add(new User("login" + i, "pass" + i, "nick" + i, Role.USER));
        }
        users.get(0).setRole(Role.ADMIN);
        users.get(1).setRole(Role.ADMIN);

    }

    @Override
    public synchronized String getNicknameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.nickname;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean register(String login, String password, String nickname, Role role) {
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
    public synchronized boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean isNicknameAlreadyExist(String nickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean isUserRoleAdmin(ClientHandler clientHandler) {
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
    public synchronized boolean addToTempBan(String banNickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(banNickname) && !tempBanUsers.contains(u)) {
                tempBanUsers.add(u);

                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void removeFromTempBan(String banNickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(banNickname) && tempBanUsers.contains(u)) {
                tempBanUsers.remove(u);
                return;
            }
        }
    }

    @Override
    public synchronized boolean addToPermBan(String banNickname) {
        for (User u : users) {
            if (u.nickname.equalsIgnoreCase(banNickname) && !permBanUsers.contains(u)) {
                permBanUsers.add(u);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean isNicknameInTempBan(String banNickname) {
        for (User u : tempBanUsers) {
            if (u.nickname.equalsIgnoreCase(banNickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean isNicknameInPermBan(String banNickname) {
        for (User u : permBanUsers) {
            if (u.nickname.equalsIgnoreCase(banNickname)) {
                return true;
            }
        }
        return false;
    }
}
