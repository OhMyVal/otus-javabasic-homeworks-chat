package ru.otus.ohmyval.homeworks.chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAuthenticationService implements AuthenticationService {
    private class User {
        private String login;
        private String password;
        private String nickname;
        private Role role;

        private void setRole(Role role) {
            this.role = role;
        }

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
//            this.role = role;
        }
    }
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/chat";
    private static final String USERS_ADD_QUERY = "INSERT * INTO users (login, password, nickname) values ('login' + ?, 'pass' + ?, 'nick' + ?)";
    private static final String USERS_QUERY = "SELECT * FROM users"; // делаем запрос в БД о пользователях (из табл 1)
    private static final String USER_ROLES_QUERY = "select r.id as id, r.name as name from user_to_role ur left join roles r ON r.id=ur.role_id where ur.user_id = ?";

    private List<User> users;
    public void databaseConnection() {
    try(Connection connection = DriverManager.getConnection(DATABASE_URL, "postgres", "")) {
        try (PreparedStatement ps = connection.prepareStatement(USERS_ADD_QUERY)){
            for (int i = 1; i <= 10; i++) {
                ps.setInt(1, i);
                try(ResultSet usersResultSet = ps.executeQuery()) {
                    while(usersResultSet.next()) {
                        String login = usersResultSet.getString("login");
                        String password = usersResultSet.getString("password");
                        String nickname = usersResultSet.getString("nickname");
                        User user = new User(login, password, nickname);
                        users.add(user);
                    }
                }
            }
        }
        try(Statement statement = connection.createStatement() ) { // создали объект, класса Statement
            try(ResultSet usersResultSet = statement.executeQuery(USERS_QUERY)) { // выполняем запрос - подаем его в аргументы,
                // записываем в переменную usersResultSet
                while(usersResultSet.next()) { //используем итератор; пока получаем следующий элемент, то записываем его
                    int id = usersResultSet.getInt("id"); // берем значение из первой колонки нашей таблицы
                    // (колонки нумеруются с единицы); можно указать как название колонки, так и ее номер
                    String email = usersResultSet.getString(2); // используем обычный String
                    String password = usersResultSet.getString(3);
                    User user = new User(id, password, email); // создали нового юзера с полями
                    users.add(user); // добавили его в лист
                }

            }
        }
        // продолжаем работать внутри созданного подключения к БД Connection connection
        try (PreparedStatement ps = connection.prepareStatement(USER_ROLES_QUERY)) { // создаем объект PreparedStatement ps,
            // подаем наш запрос в аргументы
            for (User user : users) { // проходимся по списку пользователей, для каждого из них создаем PreparedStatement
                List<Role> roles = new ArrayList<>(); // создали список ролей (он новый для каждого пользователя)
                ps.setInt(1, user.getId()); // у нашего запроса ps устанавливаем вместо ? параметр 1
                // (т.е. это первый человек из таблицы user_to_role), и запрашиваю id этого человека
                try(ResultSet usersResultSet = ps.executeQuery()) { // выполняем запрос, он без аргументов, записали в usersResultSet
                    while(usersResultSet.next()) { //используем итератор
                        int id = usersResultSet.getInt("id"); //нас интересует колонка 3 и 4 (они относятся только к табличке Roles)
                        String name = usersResultSet.getString("name");
                        Role role = new Role(id, name); // создали новый объект класса Role, с id и name
                        roles.add(role); // добавили в список ролей новый объект
                    }
                    user.setRoles(roles); // подаем пользователю его список ролей (у каждого пользователя он свой)
                }
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    }

    public DatabaseAuthenticationService() {
        this.users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            this.users.add(new User("login" + i, "pass" + i, "nick" + i, Role.USER));
        }
        users.get(0).setRole(Role.ADMIN);
        users.get(1).setRole(Role.ADMIN);

    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (DatabaseAuthenticationService.User u : users) {
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
        users.add(new DatabaseAuthenticationService.User(login, password, nickname, role));
        return true;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (DatabaseAuthenticationService.User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNicknameAlreadyExist(String nickname) {
        for (DatabaseAuthenticationService.User u : users) {
            if (u.nickname.equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserRoleAdmin(ClientHandler clientHandler) {
        String senderNickname = clientHandler.getNickname();
        for (DatabaseAuthenticationService.User u : users) {
            if (u.nickname.equals(senderNickname) && u.role.equals(Role.ADMIN)) {
                return true;
            }
        }
        return false;
    }
}
