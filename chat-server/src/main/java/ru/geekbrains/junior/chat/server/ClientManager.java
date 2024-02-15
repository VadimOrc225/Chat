package ru.geekbrains.junior.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;


public class ClientManager implements Runnable {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;
    public static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clients.add(this);

            name = bufferedReader.readLine();
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Server: " + name + " подключился к чату.");
        } catch (Exception e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление клиента из коллекции
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Server: " + name + " покинул чат.");
    }

    /**
     * Отправка сообщения всем слушателям
     *
     * @param message сообщение
     */
    private void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            try {
                if (!client.equals(this) && message != null) {
                    //if (!client.name.equals(name) && message != null) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }
            } catch (Exception e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /**
     * Отправка сообщения одному клиенту через @Имя (Имя - кому)
     *
     * @param message - отправляемое сообщение
     * @param index   - индекс символа @ в строке
     */
    private void privateMessage(String message, int index) {
        char[] charArray = message.toCharArray();
        String nameBuffer = "";
        for (int i = index + 1; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == ' ') break;
            else nameBuffer = nameBuffer + c;
        }
        System.out.println("Попытка отправить частное сообщение пользователю с именем " + nameBuffer + ".");
        boolean flag = false;
        for (ClientManager client : clients) {
            try {
                if (!client.name.equals(name) && client.name.equals(nameBuffer) && message != null) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    flag = true;
                }

            } catch (Exception e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
            if (!flag) {
                for (ClientManager client1 : clients) {
                    try {
                        if (client1.name.equals(name)) {
                            client1.bufferedWriter.write("Клиента с именем " + nameBuffer + " нет.");
                            client1.bufferedWriter.newLine();
                            client1.bufferedWriter.flush();
                            flag = true;
                        }

                    } catch (Exception e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }

    }

    @Override
    public void run() {
        String messageFromClient;
        while (!socket.isClosed()) {
            try {
                // Чтение данных
                messageFromClient = bufferedReader.readLine();
                int index;
                if (messageFromClient.contains("@")) { // проверка на знак, означающий частное сообщение
                    index = messageFromClient.indexOf('@'); // индекс в строке знака, означающего частное сообщение
                    // Отправка данных одному клиенту
                    privateMessage(messageFromClient, index);
                } else
                    // Отправка данных всем слушателям
                    broadcastMessage(messageFromClient);
            } catch (Exception e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                //break;
            }
        }
    }
}

