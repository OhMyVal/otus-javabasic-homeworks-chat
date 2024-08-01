package ru.otus.ohmyval.homeworks.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAuthenticationService implements AuthenticationService {
    private class User {
        private int id;
        private String login;
        private String password;
        private String nickname;
        private Role role;

        public int getId() {
            return id;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }

        public String getNickname() {
            return nickname;
        }

        public Role getRole() {
            return role;
        }

        private void setRole(Role role) {
            this.role = role;
        }

        public User(int id, String login, String password, String nickname) {
            this.id = id;
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    public class Role {
        private int id;
        private String title;

        public Role(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }


    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/chat";
    private static final String USERS_ADD_QUERY = "INSERT INTO users (login, password, nickname) values ('login' + ?, 'pass' + ?, 'nick' + ?)";
    private static final String USER_REGISTER_QUERY = "INSERT INTO users (login, password, nickname) values (?, ?, ?)";
    private static final String USER_ROLE_ADMIN_QUERY = "INSERT INTO user_role (user_id, role_id) values (?, '1')";
    private static final String USER_ROLE_USER_QUERY = "INSERT INTO user_role (user_id, role_id) values (?, '2')";
    private static final String USER_ROLE_QUERY = "select r.id as id, r.title as title from user_role ur left join roles r ON r.id=ur.role_id where ur.user_id = ?";

    private List<User> users;

    public DatabaseAuthenticationService() {
        this.users = new ArrayList<>();
        databaseOperation();
    }

    public void databaseOperation() {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "")) {
            fillUsers(connection);
            setRoleAdmin(connection);
            setRoleUser(connection);
            extractUserRoles(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void fillUsers(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USERS_ADD_QUERY)) {
            for (int i = 1; i <= 10; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i);
                ps.setInt(3, i);
                try (ResultSet usersResultSet = ps.executeQuery()) {
                    while (usersResultSet.next()) {
                        int id = usersResultSet.getInt("id");
                        String login = usersResultSet.getString("login");
                        String password = usersResultSet.getString("password");
                        String nickname = usersResultSet.getString("nickname");
                        User user = new User(id, login, password, nickname);
                        users.add(user);
                    }
                }
            }
        }
    }
    private static void setRoleAdmin(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USER_ROLE_ADMIN_QUERY)) {
            for (int i = 1; i <= 2; i++) {
                ps.setInt(1, i);
                ps.execute();
            }
        }
    }
    private static void setRoleUser(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USER_ROLE_USER_QUERY)) {
            for (int i = 3; i <= 10; i++) {
                ps.setInt(1, i);
                ps.execute();
            }
        }
    }
    private void extractUserRoles(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USER_ROLE_QUERY)) {
            for (User user : users) {
                ps.setInt(1, user.getId());
                try (ResultSet usersResultSet = ps.executeQuery()) {
                    while (usersResultSet.next()) {
                        int id = usersResultSet.getInt("id");
                        String title = usersResultSet.getString("title");
                        user.setRole(new Role(id, title));
                    }
                }
            }
        }
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
    public boolean register(String login, String password, String nickname) {
        if (isLoginAlreadyExist(login)) {
            return false;
        }
        if (isNicknameAlreadyExist(nickname)) {
            return false;
        }
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "")) {
            addNewUser(login, password, nickname, connection);

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void addNewUser(String login, String password, String nickname, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USER_REGISTER_QUERY)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nickname);
            try (ResultSet usersResultSet = ps.executeQuery()) {
                int id = usersResultSet.getInt("id");
                String addedLogin = usersResultSet.getString("login");
                String addedPassword = usersResultSet.getString("password");
                String addedNickname = usersResultSet.getString("nickname");
                User user = new User(id, addedLogin, addedPassword, addedNickname);
                users.add(user);
                try (PreparedStatement preparedStatement = connection.prepareStatement(USER_ROLE_USER_QUERY)){
                    preparedStatement.setInt(1, user.getId());
                    preparedStatement.execute();
                }
                try (PreparedStatement pStatement = connection.prepareStatement(USER_ROLE_QUERY)) {
                    pStatement.setInt(1, user.getId());
                        try (ResultSet newResultSet = pStatement.executeQuery()) {
                                int roleId = newResultSet.getInt("id");
                                String title = newResultSet.getString("title");
                                user.setRole(new Role(roleId, title));
                    }
                }
            }
        }
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
            if (u.nickname.equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserRoleAdmin(ClientHandler clientHandler) {
        String senderNickname = clientHandler.getNickname();
        for (User u : users) {
            if (u.nickname.equals(senderNickname) && u.role.getTitle().equalsIgnoreCase("admin")) {
                return true;
            }
        }
        return false;
    }
}
