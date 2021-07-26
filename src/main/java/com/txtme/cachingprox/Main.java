/*
 * Тестовое задание txt.me
Исходная формулировка:

Вводные: есть клиенты и бекэнд, взаимодействие происходит по rest. 
Возникающие нештатные ситуации: бекенд оказывается недоступным на N секунд, за это время клиенты отправляют 100-1000 сообщений системе
Задача: реализовать кеширующую прокси с очередью, обеспечить гарантирующее доставку всех запросов на бекенд после его восстановления.
Использовать только java без сторонних библиотек.
Приложение должно быть многопоточным с функциями кеширования.

 */
package com.txtme.cachingprox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Egor 26.07.2021
 */
public class Main {
    
    // По хорошему надо бы выносить в конфиг...
    public final static String DEFAULT_GOOD_HTTP_RESP = "HTTP/1.1 200 OK\r\n";
    public final static int PROXY_SERVER_PORT = 8088;
    // Это максимальное время между попытками нашего прокси отправить данные на сервак (если очередь не пуста)
    public final static long RETRY_DELAY = 1_000L;
    
    public static String SERVER_URL = "http://localhost:8089";
    
    // таймауты на исходяшее соединение
    public final static int CONNECT_TIMEOUT = 1_000;        // 1 sec
    public final static int READ_TIMEOUT = 1_000;           // 1 sec
    
    // Это так для красоты - может как то захотим потом аккуратно выключать сервак
    private volatile boolean signal2close = false;
    
    private QueueWorker queueWorker;
    
    private static ServerSocket server; // серверсокет
    
    public Main()
    {
        queueWorker = new QueueWorker();
    }
    
    /** Запустить отдельный поток который разбирает очередь задач на отправку */
    public void runQueueqWorker()
    {
        Thread queueWorkerThread = new Thread(queueWorker, "QueueWorker");
        queueWorkerThread.start();
    }
    
    /**  */
    private void runServer()
    {
        try {
            try  {
                server = new ServerSocket( PROXY_SERVER_PORT );
                Logger.log("Server started at port " + PROXY_SERVER_PORT);
                
                while (!signal2close)
                {
                    // По хорошему тут настраиваемый пул потоков обслуживающих запросы извне
                    Socket clientSocket = server.accept();
                    Logger.log("Client accepted");
                    DataReceiver dataReceiver = new DataReceiver(queueWorker, clientSocket);
                    Thread clentThread = new Thread(dataReceiver, dataReceiver.getName());
                    clentThread.start();
                }
            } finally {
                server.close();
                Logger.log("Server closed");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    /**
     * 
     */
    public static void main(String[] args)
    {
        Main main = new Main();
        main.runQueueqWorker();
        main.runServer();
    }
}
