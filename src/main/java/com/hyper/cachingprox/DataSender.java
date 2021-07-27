/*
    Класс занимается тем что отправляет данные на основной сервак
 */
package com.hyper.cachingprox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Egor
 */
public class DataSender implements Runnable {
    
    // Это просто для удобства отладки нашего просто теста - порядколвый номер потока..
    public static AtomicInteger SENDER_SEQ_NUM = new AtomicInteger(0);
    
    // это товарищь, который заведует очередью
    private QueueWorker queueWorker;
    
    // имя данного потока - для дебаг целей
    private String name;
    
    // задача на отправку
    private Task task2send;
    
    public DataSender(QueueWorker queueWorker, Task task2send)
    {
        this.queueWorker = queueWorker;
        this.task2send = task2send;
        this.name = "DataSender " + SENDER_SEQ_NUM.addAndGet( 1 );
    }
    

    @Override
    public void run() 
    {
        Logger.log("Started");
        boolean suxxess = false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL( Main.SERVER_URL );
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout( Main.CONNECT_TIMEOUT );
            conn.setReadTimeout( Main.READ_TIMEOUT );
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write( task2send.getTheData() );
            writer.flush();
            writer.close();

            if (conn.getResponseCode() == 200)
                suxxess = true;            
            conn.getInputStream().close();
        } catch (Exception e)
        {   // В данном упрощенном случае любое исключение просто считаем ошибкой отправки - т.е. будет отправлять снова.
            Logger.log("Error sending data to server: " + e.getMessage());
        } finally {
            try {
                // стараемся освободить ресурс..
                conn.disconnect();
            } catch (Exception e)
            {
                e.printStackTrace();
                // Пока отлоггируем вот так неаккуратно чтобы просто по логам увидеть что мы накосячили и поправить
            }
        }
        
        if (!suxxess)
        {
            // TODO: Добавить в очередь на переотправку
            queueWorker.addTask( task2send );
            Logger.log("Fineshed ERR");
        } else
        {
            Logger.log("Fineshed OK");
            queueWorker.notifyServerFeelsGood();
        }
    }    

    public String getName() {
        return name;
    }
}
