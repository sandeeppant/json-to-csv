package com.kronos;

public class Attributes
{
    private int key;
    private int parent;
    private String value;
    
    
    public Attributes()
    {
    }
    
    public Attributes(String value)
    {
        this.value = value;
    }
    
    public Attributes(int parent, String value)
    {
        this.parent = parent;
        this.value = value;
    }
    
    public Attributes(int key, int parent, String value)
    {
        super();
        this.key = key;
        this.parent = parent;
        this.value = value;
    }
    public int getKey()
    {
        return key;
    }
    public void setKey(int key)
    {
        this.key = key;
    }
    public int getParent()
    {
        return parent;
    }
    public void setParent(int parent)
    {
        this.parent = parent;
    }
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }
    
    
}
