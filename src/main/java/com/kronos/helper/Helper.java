package com.kronos.helper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kronos.Attributes;
import com.kronos.utils.Constants;

public class Helper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Helper.class);

    private Helper()
    {

    }

    public static Map<Object, List<Object>> readMasterTemplateAndColumnHeader(String csvFile)
    {
        Map<Object, List<Object>> superMap = new LinkedHashMap<Object, List<Object>>();

        BufferedReader br = null;
        String line = Constants.EMPTY_STRING;
        String cvsSplitBy = Constants.COMMA;

        try
        {
            br = new BufferedReader(new FileReader(csvFile));
            if ((line = br.readLine()) != null)
            {
                // use comma as separator
                String[] header = line.split(cvsSplitBy);
                for (int i = 0; i < header.length; i++)
                {
                    List<Object> emptyList = new LinkedList<Object>();
                    superMap.put(header[i], emptyList);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("FileNotFoundException:", e);
        }
        catch (IOException e)
        {
            LOGGER.error("IOException:", e);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException e)
                {
                    LOGGER.error("IOException:", e);
                }
            }
        }
        return superMap;
    }

    public static Map<Object, Map<Object, List<Attributes>>> readNestedTemplateAndColumnHeader(String csvPath, String masterFile)
    {
        Map<Object, Map<Object, List<Attributes>>> nestedSuperMap = new LinkedHashMap<Object, Map<Object, List<Attributes>>>();

        try
        {
            Files.walk(Paths.get(csvPath)).forEach(
                filePath -> {
                    if (Files.isRegularFile(filePath)
                        && filePath.getFileName().toString().endsWith(Constants.CSV_EXTENSION)
                        && !filePath.getFileName().toString().equals(masterFile))
                    {
                        BufferedReader br = null;
                        String line = Constants.EMPTY_STRING;
                        String cvsSplitBy = Constants.COMMA;
                        try
                        {
                            br = new BufferedReader(new FileReader(filePath.toString()));
                            Object fileNameWithOutExt = FilenameUtils
                                .removeExtension(filePath.getFileName().toString());
                            Map<Object, List<Attributes>> superMap = new LinkedHashMap<Object, List<Attributes>>();
                            if ((line = br.readLine()) != null)
                            {
                                // use comma as separator
                                String[] header = line.split(cvsSplitBy);
                                for (int i = 0; i < header.length; i++)
                                {
                                    superMap.put(header[i], new LinkedList<Attributes>());
                                }
                            }

                            nestedSuperMap.put(fileNameWithOutExt, superMap);
                        }
                        catch (FileNotFoundException e)
                        {
                            LOGGER.error("FileNotFoundException:", e);
                        }
                        catch (IOException e)
                        {
                            LOGGER.error("IOException:", e);
                        }
                        finally
                        {
                            if (br != null)
                            {
                                try
                                {
                                    br.close();
                                }
                                catch (IOException e)
                                {
                                    LOGGER.error("IOException:", e);
                                }
                            }
                        }
                    }
                });
        }
        catch (IOException e)
        {
            LOGGER.error("IOException:", e);
        }
        return nestedSuperMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> newHashMapWithoutAtARate(Map<Object, Object> mapObj, Map<String,String> nestedColumnName)
    {
        Map<Object, Object> mapObjNew = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> mapObjEntry : mapObj.entrySet())
        {
            String key = mapObjEntry.getKey().toString();
            String newKey = key.startsWith(Constants.AT_THE_RATE) ? key.substring(1) : key;
            Object value = mapObjEntry.getValue();
            
            if(!key.startsWith(Constants.AT_THE_RATE) && nestedColumnName!=null)
            {
                for (Map.Entry<String, String> entry : nestedColumnName.entrySet())
                {
                    String[] splitNestedColumn = entry.getValue().split(Constants.HYPEN);
                    if(!splitNestedColumn[0].equals(key))
                        continue;
                    Map<Object, Object> map = new LinkedHashMap<Object, Object>();
                    for(int i=1;i<splitNestedColumn.length-1;i++)
                    {
                        if (mapObjEntry.getValue() instanceof Map)
                        {
                            map = getValuesFromMap((Map<Object, Object>) mapObjEntry.getValue(),splitNestedColumn[i]);
                        }
                    }
                    for (Map.Entry<Object,Object> entry1 : map.entrySet())
                    {
                        if (entry1.getValue() instanceof String || entry1.getValue() instanceof Integer)
                        {
                            String key1 = entry1.getKey().toString().startsWith(Constants.AT_THE_RATE) ? entry1.getKey().toString().substring(1) : entry1.getKey().toString();
                            if(entry.getValue().split(Constants.HYPEN)[entry.getValue().split(Constants.HYPEN).length-1].equals(key1))
                            {
                                value = entry1.getValue();
                                mapObjNew.put(entry.getKey(), value);
                                break;
                            }
                        }
                    }
                }
            }
            mapObjNew.put(newKey, value);
        }
        return mapObjNew;
    }

    @SuppressWarnings("unchecked")
    public static Map<Object,Object> getValuesFromMap(Map<Object,Object> hypenMap, String key)
    {
        if (hypenMap.get(key) instanceof Map)
        {
            Map<Object,Object> map  = (Map<Object, Object>) hypenMap.get(key);
            return map;
        }
        else if (hypenMap.get(key) instanceof List<?>)
        {
            Map<Object, Object> map = new LinkedHashMap<Object,Object>();
            for (Object lst : (List<?>) hypenMap.get(key))
            {
                Map<Object, Object> map1 = (Map<Object, Object>) lst;
                for (Map.Entry<Object, Object> entry1 : map1.entrySet())
                {
                    if (map.containsKey(entry1.getKey()))
                    {
                        Object value = map.get(entry1.getKey());
                        value = value + Constants.COLON +entry1.getValue();
                        map.put(entry1.getKey(), value);
                    }
                    else
                    {
                        map = (Map<Object, Object>) lst;
                    }
                }
            }
            return map;
        }
        else
        {
            return new LinkedHashMap<Object,Object>();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Map<Object, Map<Object, List<Object>>> recursive(Map<Object, Object> map, Map<Object, Map<Object, List<Object>>> finalMap, String searchParam, int parent)
    {
        for (Map.Entry<Object, Object> entry : map.entrySet())
        {
            checkAndUpdateMap(finalMap, entry.getKey(), entry.getValue(), searchParam, parent);
            if (entry.getValue() instanceof Map)
            {
                recursive((Map<Object, Object>) entry.getValue(), finalMap, searchParam, parent);
            }
        }
        return finalMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Map<Object, List<Object>>> recursiveList(List<?> list, Map<Object, Map<Object, List<Object>>> finalMap, String searchParam, int parent)
    {
        for (Object lst : list)
        {
            Map<Object, Object> map = (Map<Object, Object>) lst;
            for (Map.Entry<Object, Object> entry : map.entrySet())
            {
                checkAndUpdateMap(finalMap, entry.getKey(), entry.getValue(), searchParam, parent);
                if (entry.getValue() instanceof Map)
                {
                    recursive((Map<Object, Object>) entry.getValue(), finalMap, searchParam, parent);
                }
            }
        }
        return finalMap;
    }

    public static void checkAndUpdateMap(Map<Object, Map<Object, List<Object>>> finalMap, Object key, Object value,
        String searchParam, int parent)
    {
        if (key.equals(searchParam))
        {
            if (finalMap.containsKey(key))
            {
                Map<Object, List<Object>> valueMap = finalMap.get(key);
                List<Object> valueList = valueMap.get(parent);
                if (valueList == null) valueList = new ArrayList<Object>();
                valueList.add(value);
                valueMap.put(parent, valueList);
                finalMap.put(key, valueMap);
            }
            else
            {
                Map<Object, List<Object>> valueMap = new LinkedHashMap<Object, List<Object>>();
                List<Object> valueList = new ArrayList<Object>();
                valueList.add(value);
                valueMap.put(parent, valueList);
                finalMap.put(key, valueMap);
            }
        }
    }
    
    public static String putStringInQuotes(String value)
    {
        value = Constants.QUOTE + value + Constants.QUOTE;
        return value;
    }
    
    public static Map<String,String> getHypenColumnName(Map<Object, List<Object>> superMap)
    {
        Map<String,String> hypenColumnName = new LinkedHashMap<String,String>();
        for (Map.Entry<Object, List<Object>> entry : superMap.entrySet())
        {
            String key = entry.getKey().toString();
            if(key.contains(Constants.HYPEN))
            {
                hypenColumnName.put(key,key);
            }
        }
        return hypenColumnName;
    }
    
    public static Map<String,String> getHypenColumnNameForNested(Map<Object, List<Attributes>> superMap)
    {
        Map<String,String> hypenColumnName = new LinkedHashMap<String,String>();
        for (Map.Entry<Object, List<Attributes>> entry : superMap.entrySet())
        {
            String key = entry.getKey().toString();
            if(key.contains(Constants.HYPEN))
            {
                hypenColumnName.put(key,key);
            }
        }
        return hypenColumnName;
    }
    
    public static void fillNestedMap(Map<Object,List<Attributes>> nestedSuperMap, Map<Object, Object> mapObjNew, int parent)
    {
        for (Map.Entry<Object, List<Attributes>> entry1 : nestedSuperMap.entrySet())
        {
            Object obj1 = mapObjNew.get(entry1.getKey());
            List<Attributes> nestedList = entry1.getValue();
            if(obj1==null)
            {
                nestedList.add(new Attributes(parent,Constants.EMPTY_STRING));
            }
            else
            {
                nestedList.add(new Attributes(parent,obj1.toString()));
            }
        }
    }
}
