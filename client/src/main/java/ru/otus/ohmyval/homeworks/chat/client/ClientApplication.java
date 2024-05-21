package ru.otus.ohmyval.homeworks.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ClientApplication {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (
                Socket socket = new Socket("localhost", 8189);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Подключились к серверу");
            new Thread(() -> {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("kk:mm:ss");
                    String time = simpleDateFormat.format(new Date());
                    while (true) {
                        String inMessage = in.readUTF();
                        System.out.println(inMessage + " " + time);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            while (true) {
                String msg = scanner.nextLine();
                out.writeUTF(msg);
                if (msg.equals("/exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
