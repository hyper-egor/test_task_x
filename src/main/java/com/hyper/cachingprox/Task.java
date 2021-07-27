/*
    Это элемент очереди, описывающий задачу которую надовыполнить
 */
package com.hyper.cachingprox;

/**
 *
 * @author Egor
 */
public class Task {
    
    // Сами данные что надо отправить. Сильное упрощение... надо бы разделять HTTP метод, URI, headers и т.д...
    private String theData;
    
    public Task(String theData)
    {
        this.theData = theData;        
    }

    public String getTheData() {
        return theData;
    }
}
