package com.kronos.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationMap
{
    private static Properties properties = null;
    private static final Logger log = LoggerFactory.getLogger(ConfigurationMap.class);

    static
    {
        properties = new Properties();
        InputStream stream = null;
        try
        {
            stream = ConfigurationMap.class.getClassLoader().getResourceAsStream("config.properties");
            properties.load(stream);
        }
        catch (IOException e)
        {
            log.error("Error occoured while loading properties: " + e);
        }
        finally
        {
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                log.error("Error while closing the connection");
            }
        }
        log.info("loadConfigurations: Loading properties file");
    }

    public static String getProperty(String propertyName)
    {
        String propertyValue = properties.getProperty(propertyName);

        return propertyValue;
    }

    public static void setProperty(String propertyName, String value)
    {
        properties.setProperty(propertyName, value);

    }

}
