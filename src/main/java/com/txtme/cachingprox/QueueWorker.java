/*
    Объект - поток который занимается отправкой на сервак сообщений из очереди.
    Задача этого потока как можно быстрее разгребать очередь - доставать задания и передавать их в другие потоки - отправители
 */
package com.txtme.cachingprox;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Egor
 */
public class QueueWorker implements Runnable {
    
    // Эдемент в очереди типа String  - так конечно быть не должно - мы сильно упрощаем
    private ConcurrentLinkedQueue<Task> theQueue;
    
    private volatile long lastFeellBadTimestamp = 0;
    
    private int iterationNumber = 0;
    
    public QueueWorker()
    {
        theQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run()
    {
        Logger.log("Started");
        
        while (true) // вечный цикл это bad practice, но для теста сойдет
        {            
            // проверка на lastFeellBadTimestamp нужна чтобы не пытаться вечно без делеев заваливать сервак если он лег.
            // но как только сервак оживет - мы без делеев будем разгребать очередь до того как опустошим очередь ИЛИ сервак не даст нам ошибку
            while (System.currentTimeMillis() - lastFeellBadTimestamp > Main.RETRY_DELAY) 
            {
                Task task = theQueue.poll();
                if (task == null)
                    break;
                
                // Пока очередь не пуста - мы запускаем потоки отправки без задержек - все что есть.
                // По условиям задачи мы не ограничены в ресурсах, поэтому потоков сколько угодно.
                // TODO: Поправильному - тут надо прикрутить пул отправителей данных - если все потоки из пула заняты то тут мы должны подождать освободившегося
                DataSender dataSender = new DataSender(this, task);
                Thread dataSenderThread = new Thread(dataSender, dataSender.getName());
                dataSenderThread.start();
            }

            synchronized (this)
            {
                try {
                    wait( Main.RETRY_DELAY );
                } catch (Exception e)
                {   e.printStackTrace();
                }
            }
            iterationNumber++;
        }        
    }
    
    /** Добавить в очередь задачу 
     * Синхронизировать этот метод не надо т.к. сам объект очереди заточен под многопоточность
     */
    public void addTask(Task task)
    {
        // добавляем в очередь
        theQueue.offer( task );
        // отмечаем что сервак упал только что
        lastFeellBadTimestamp = System.currentTimeMillis();
    }
    
    // Сообщаем этому воркеру, что сервак чувствует себя хорошо и можно продолжить разгребать очередь если она есть
    public void notifyServerFeelsGood()
    {
        // сервак ожид - надо разбудить поток
        synchronized (this)
        {
            notifyAll();
        }
        // ...и сбрасываем метку времени сбоя
        lastFeellBadTimestamp = 0;
    }
}
