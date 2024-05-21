package ru.otus.ohmyval.homeworks.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;


    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                if (tryToAuthenticate()) {
                    communicate();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

//    private void communicate() throws IOException {
//        while (true) {
//            String msg = in.readUTF();
//            if (msg.startsWith("/exit")) {
//                break;
//            }
//            if (msg.startsWith("/")) {
//                if (msg.startsWith("/w ")) {
//                    String[] parts = msg.split(" ", 3);
//                    if (parts.length != 3) {
//                        sendMessage("Некорректный формат запроса");
//                        continue;
//                    }
//                    String receiverName = parts[1];
//                    String targetMessage = parts[2];
//                    server.sendPrivateMessage(this, receiverName, targetMessage);
//                    continue;
//                }
//                if (msg.startsWith("/changenick ")) {
//                    String[] parts = msg.split(" ", 2);
//                    if (parts.length != 2) {
//                        sendMessage("Некорректный формат запроса");
//                        continue;
//                    }
//                    String newNickname = parts[1];
//                    if (server.getAuthenticationService().isNicknameAlreadyExist(newNickname)) {
//                        sendMessage("Указанный никнейм уже занят");
//                        continue;
//                    }
//                    if (server.getAuthenticationService().changeNickname(this, newNickname)) {
//                        server.broadcastMessage(nickname + " изменил никнейм на " + newNickname);
//                        this.nickname = newNickname;
//                        continue;
//                    } else {
//                        sendMessage("Не удалось сменить никнейм");
//                    }
//                    continue;
//                }
//                if (msg.startsWith("/kick ")) {
//                    String[] parts = msg.split(" ", 2);
//                    if (parts.length != 2) {
//                        sendMessage("Некорректный формат запроса");
//                        continue;
//                    }
//                    if (server.getAuthenticationService().isUserRoleAdmin(this)) {
//                        String deletedNickname = parts[1];
//                        server.kick(deletedNickname);
//                    } else {
//                        sendMessage("Недостаточно прав доступа");
//                    }
//                    continue;
//                }
//            }
//            server.broadcastMessage(nickname + ": " + msg);
//        }
//
//    }

    private void communicate() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/exit")) {
                break;
            }
            if (msg.startsWith("/")) {
                serviceCommands(msg);
                continue;
            }
            server.broadcastMessage(nickname + ": " + msg);
        }
    }

    private void serviceCommands(String msg) {
        if (msg.startsWith("/w ")) {
            String[] parts = msg.split(" ", 3);
            if (parts.length != 3) {
                sendMessage("Некорректный формат запроса");
                return;
            }
            String receiverName = parts[1];
            String targetMessage = parts[2];
            server.sendPrivateMessage(this, receiverName, targetMessage);
            return;
        }
        if (msg.startsWith("/changenick ")) {
            String[] parts = msg.split(" ", 2);
            if (parts.length != 2) {
                sendMessage("Некорректный формат запроса");
                return;
            }
            String newNickname = parts[1];
            if (server.getAuthenticationService().isNicknameAlreadyExist(newNickname)) {
                sendMessage("Указанный никнейм уже занят");
                return;
            }
            if (server.getAuthenticationService().changeNickname(this, newNickname)) {
                server.broadcastMessage(nickname + " изменил никнейм на " + newNickname);
                this.nickname = newNickname;
                return;
            } else {
                sendMessage("Не удалось сменить никнейм");
            }
        }
        if (server.getAuthenticationService().isUserRoleAdmin(this)) {
            adminCommands(msg);
        } else {
            sendMessage("Недостаточно прав доступа");
        }
    }

    private void adminCommands(String msg) {
//        if (msg.startsWith("/shutdown")) {
//            server.serverShutdown();
//            return;
//        }
        String[] parts = msg.split(" ", 2);
        if (parts.length != 2) {
            sendMessage("Некорректный формат запроса");
            return;
        }
        if (msg.startsWith("/kick ")) {
            String deletedNickname = parts[1];
            if (!server.kick(deletedNickname)) {
                sendMessage("Не удалось удалить пользователя");
            }
            return;
        }
        if (msg.startsWith("/ban ")) {
            String banNickname = parts[1];
            if (server.getAuthenticationService().addToBan(banNickname)) {
                server.kick(banNickname);
            } else {
                sendMessage("Не удалось забанить пользователя");
            }
        }
    }


    private boolean tryToAuthenticate() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/auth ")) {
                String[] tokens = msg.split(" ");
                if (tokens.length != 3) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = server.getAuthenticationService().getNicknameByLoginAndPassword(login, password);
                if (nickname == null) {
                    sendMessage("Неправильный логин/пароль");
                    continue;
                }
                if (server.isNicknameBusy(nickname)) {
                    sendMessage("Указанная учетная запись уже занята. Попробуйте зайти позднее");
                    continue;
                }
                this.nickname = nickname;
                server.subscribe(this);
                sendMessage(nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.startsWith("/register ")) {
                String[] tokens = msg.split(" ");
                if (tokens.length != 4) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = tokens[3];
                if (server.getAuthenticationService().isLoginAlreadyExist(login)) {
                    sendMessage("Указанный логин уже занят");
                    continue;
                }
                if (server.getAuthenticationService().isNicknameAlreadyExist(nickname)) {
                    sendMessage("Указанный никнейм уже занят");
                    continue;
                }
                if (!server.getAuthenticationService().register(login, password, nickname, Role.USER)) {
                    sendMessage("Не удалось пройти регистрацию");
                    continue;
                }
                this.nickname = nickname;
                server.subscribe(this);
                sendMessage("Вы успешно зарегистрировались! " + nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.equals("/exit")) {
                return false;
            } else {
                sendMessage("Вам необходимо авторизоваться");
            }
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
