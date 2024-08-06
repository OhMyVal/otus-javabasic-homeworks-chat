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
    private static final String USER_REGISTER_QUERY = "INSERT INTO users (login, password, nickname) values (?, ?, ?)";
    private static final String USER_QUERY = "SELECT * FROM users where login = ?";
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
            fillDataBaseWithUsers(connection);
            extractUserRoles(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fillDataBaseWithUsers(Connection connection) throws SQLException {
        for (int i = 1; i <= 10; i++) {
            try (PreparedStatement ps = connection.prepareStatement(USER_REGISTER_QUERY)) {
                addUserToDataBase(i, ps);
                try (PreparedStatement preparedStatement = connection.prepareStatement(USER_QUERY)) {
                    preparedStatement.setString(1, "login" + i);
                    try (ResultSet usersResultSet = preparedStatement.executeQuery()) {
                        while (usersResultSet.next()) {
                            User user = addUserToList(usersResultSet);
                            if (i <= 2) {
                                setRoleAdmin(connection, user);
                            } else {
                                setRoleUser(connection, user);
                            }
                        }

                    }
                }
            }

        }
    }

    private static void addUserToDataBase(int i, PreparedStatement ps) throws SQLException {
        ps.setString(1, "login" + i);
        ps.setString(2, "pass" + i);
        ps.setString(3, "nick" + i);
        ps.execute();
    }

    private User addUserToList(ResultSet usersResultSet) throws SQLException {
        int id = usersResultSet.getInt("id");
        String login = usersResultSet.getString("login");
        String password = usersResultSet.getString("password");
        String nickname = usersResultSet.getString("nickname");
        User user = new User(id, login, password, nickname);
        users.add(user);
        return user;
    }

    private static void setRoleAdmin(Connection connection, User user) throws SQLException {
        try (PreparedStatement preparedSt = connection.prepareStatement(USER_ROLE_ADMIN_QUERY)) {
            preparedSt.setInt(1, user.getId());
            preparedSt.execute();
        }
    }

    private static void setRoleUser(Connection connection, User user) throws SQLException {
        try (PreparedStatement preparedSt = connection.prepareStatement(USER_ROLE_USER_QUERY)) {
            preparedSt.setInt(1, user.getId());
            preparedSt.execute();
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
            registerNewUser(login, password, nickname, connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void registerNewUser(String login, String password, String nickname, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(USER_REGISTER_QUERY)) {
            addUserToDatabase(login, password, nickname, ps);
            try (PreparedStatement preparedStatement = connection.prepareStatement(USER_QUERY)) {
                preparedStatement.setString(1, login);
                try (ResultSet usersResultSet = preparedStatement.executeQuery()) {
                    while (usersResultSet.next()) {
                        User user = addUserToList(usersResultSet);
                        setRoleUser(connection, user);
                        addUserRoleToList(connection, user);
                    }
                }
            }
        }
    }

    private static void addUserToDatabase(String login, String password, String nickname, PreparedStatement ps) throws SQLException {
        ps.setString(1, login);
        ps.setString(2, password);
        ps.setString(3, nickname);
        ps.execute();
    }

    private void addUserRoleToList(Connection connection, User user) throws SQLException {
        try (PreparedStatement pStatement = connection.prepareStatement(USER_ROLE_QUERY)) {
            pStatement.setInt(1, user.getId());
            try (ResultSet newResultSet = pStatement.executeQuery()) {
                while (newResultSet.next()) {
                    int roleId = newResultSet.getInt("id");
                    String title = newResultSet.getString("title");
                    user.setRole(new Role(roleId, title));
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
