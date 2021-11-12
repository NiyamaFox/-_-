package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    static ArrayList<User> users = new ArrayList<>();
    static ArrayList<String> messages = new ArrayList<>(); // Все сообщения в чате

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8188);
            System.out.println("Сервер запущен!");
            while (true) {
                Socket socket = serverSocket.accept(); // Ожидаем подключение клиента
                User currentUser = new User(socket);
                System.out.println("Клиент подключился!");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                currentUser.setOos(oos);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                currentUser.getOos().writeObject("Введите имя: ");
                String userName = in.readUTF();
                currentUser.setUserName(userName);
                users.add(currentUser);
                sendUserList();
                for (String s : messages) {
                    oos.writeObject(s); // После захода, клиенту отправляются все сообщения написанные до сего момента
                }
                currentUser.getOos().writeObject(userName + ", добро пожаловать в чат!");
                Thread thread = new Thread(new Runnable() { // Поток для клиента
                    @Override
                    public void run() {
                        try {
                            while (true) {
                                String request = in.readUTF(); // Ждём сообщение от клиента
                                String message = userName + ": " + request;
                                messages.add(message);
                                for (User user1 : users) {
                                    if (user1.getUuid().equals(currentUser.getUuid())) {
                                        continue;
                                    } else {
                                        user1.sendMessage(userName + ": " + request);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Пользователь " + currentUser.getUserName() + " вышел из чата");
                            users.remove(currentUser);
                            for (User user1 : users) {
                                try {
                                    user1.getOos().writeObject("Пользователь " + currentUser.getUserName() + " покинул чат");
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            }
                            sendUserList();
                        }
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendUserList() {
        String usersName = "**userList**";
        for (User user : users) {
            usersName += "//" + user.getUserName();
        }
        for (User user : users) {
            try {
                user.getOos().writeObject(usersName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}