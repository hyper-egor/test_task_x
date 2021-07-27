/*
    Очень простой классик для облегчения вызова логгировний..
 */
package com.hyper.cachingprox;

import java.util.Date;

/**
 *
 * @author Egor
 */
public class Logger {
    
    public static void log(String s)
    {
        String theLine = new Date() + " [" + Thread.currentThread().getName() + "] " + s;
        System.out.println( theLine );
    }    
}
