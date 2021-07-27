/*      Объект который занимается чтением данных от клиента и далее отправлояет задачу передать 
    полученное на основной сервер
*/

package com.hyper.cachingprox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Egor
 */
public class DataReceiver implements Runnable {
    
    // Это просто для удобства отладки нашего просто теста - порядколвый номер потока..
    public static AtomicInteger RECEIVER_SEQ_NUM = new AtomicInteger(0);
    
    private Socket mySocket;
    private InputStreamReader in = null;
    private BufferedWriter out = null;
   
    // имя данного потока - для дебаг целей
    private String name;
    
    // это товарищь, который заведует очередью
    private QueueWorker queueWorker;
    
    public DataReceiver(QueueWorker queueWorker, Socket sock)
    {
        this.queueWorker = queueWorker;
        this.mySocket = sock;        
        this.name = "DataReceiver " + RECEIVER_SEQ_NUM.addAndGet( 1 );
    }

    @Override
    public void run()
    {
        Logger.log("Started");
        try {
            in = new InputStreamReader(mySocket.getInputStream());
            out = new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()));

            // Без этого блока далее in.read() будет блокироваться. 
            // Если делать чтение хотя бы на HttpURLConnection или более высокоуровневых либах то можно сделать аккуратнее..
            out.write(Main.DEFAULT_GOOD_HTTP_RESP);
            out.flush();

            int character;
            StringBuilder data = new StringBuilder();   // Builder быстрее чем Buffer т.к. не синхронизируется. нам тут синхронизация не нужна
            while ((character = in.read()) != -1) {
                data.append((char) character);
            }
            // Здесь мы очень сильно упрощаем - для нас запрос это просто строка. 
            // По хорошему надо запомнить запрос (или "задачу") как объект и полями HTTP headers, HTTP method  и т.д...
            
            // пробросить эти данные на сервер
            // --- 
            // Можно было бы начать соединение с серваком прямо в этом потоке, НО 
            // т.к. отправка может занимать время, на которое мы запрем наш клиентский поток (число которых в пуле д.б. ограничено)
            // мы создадим отдельный поток на отправку, а этот закроем - вернем в наш пока виртуальный пул входных соединений...
            DataSender dataSender = new DataSender(queueWorker, new Task(data.toString()) );
            Thread dataSenderThread = new Thread(dataSender, dataSender.getName());
            dataSenderThread.start();
        } catch (Exception e)
        {   // По хорошему надо бы отдельно ловить исключения разных типов.
            Logger.log("Error exchanging data with client: " + e.getMessage());            
        } finally
        {
            try {
                in.close();
                out.close();
                mySocket.close();
            } catch (Exception e)
            {
                Logger.log("Couldn't Exceptionaly close client's socket: " + e.getMessage());
            }
        }    
        Logger.log("Finished");
    }    

    public String getName() {
        return name;
    }
}
