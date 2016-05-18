package com.kronos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronos.helper.Helper;
import com.kronos.utils.ConfigurationMap;
import com.kronos.utils.Constants;

public class JsonToCSVConvertor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToCSVConvertor.class);
    
    public static void main(String[] args)
    {
        String template_path = ConfigurationMap.getProperty("template_path");
        String export_report_path = ConfigurationMap.getProperty("export_report_path");
        String output_path = ConfigurationMap.getProperty("output_path");
        generatedCSV(template_path, export_report_path, output_path);
    }
    
    @SuppressWarnings("unchecked")
    public static void generatedCSV(String template_path, String export_report_path, String output_path)
    {
        LOGGER.info("Starting the execution of JSON to CSV");
        List<String> found = new ArrayList<String>();
        List<String> notFound = new ArrayList<String>();
        try
        {
            File[] directories = new File(template_path).listFiles(File::isDirectory);
            
            for(int fileNumber=0; fileNumber<directories.length; fileNumber++)
            {
                /*if(!directories[fileNumber].getName().equals("NotificationProfile"))
                {
                    continue;
                }*/
                
                String dest_directory = output_path + Constants.BACK_SLASH + directories[fileNumber].getName() + Constants.BACK_SLASH;
                
                String json_file = export_report_path + directories[fileNumber].getName()+Constants.BACK_SLASH+Constants.RESPONSE_JSON;
                
                File f = new File(json_file);

                if(f.exists())
                {
                    found.add(directories[fileNumber].getName());
                }
                else
                {
                    notFound.add(directories[fileNumber].getName());
                    continue;
                }
                
                File file = new File(dest_directory);
                if (!file.exists() && file.mkdirs()) 
                {
                    LOGGER.debug("Creating following directory: "+dest_directory);
                }
                
                FileUtils.cleanDirectory(file); 
                
                String masterFileName = directories[fileNumber].getName()+Constants.CSV_EXTENSION;
                String masterFilePath = directories[fileNumber].getAbsolutePath();
                String masterFileNameAndPath = masterFilePath+Constants.BACK_SLASH+masterFileName;
                
                FileWriter writer = new FileWriter(dest_directory + directories[fileNumber].getName() + Constants.CSV_EXTENSION);
                
                ObjectMapper mapper = new ObjectMapper();
                // read JSON from a file
                Map<Object, Object> map = mapper.readValue(new File(json_file),new TypeReference<Map<Object, Object>>() {});
                
                Set<Object> nestedCSV = new LinkedHashSet<Object>();
                List<Map<Object,Object>> obj = (List<Map<Object, Object>>) map.get(Constants.ITEMS_RETRIEVE_RESPONSES);
                List<Object> lstOfMap = new LinkedList<Object>();
                for (Object O : obj)
                {
                    Map<Object, Object> mapObj = (Map<Object, Object>) O;
                    Map<Object, Object> O1 = (Map<Object, Object>)mapObj.get(Constants.RESPONSEOBJECTNODE);
                    Map<Object, Object> O2 = (Map<Object, Object>)O1.get(directories[fileNumber].getName());
                    lstOfMap.add(O2);
                    for (Map.Entry<Object, Object> entry : O2.entrySet())
                    {
                        String key = entry.getKey().toString();
                        if(!key.startsWith(Constants.AT_THE_RATE))
                        {
                            nestedCSV.add(key);
                        }
                    }
                }
                
                //Main CSV
                Map<Object, List<Object>> superMap = Helper.readMasterTemplateAndColumnHeader(masterFileNameAndPath);
                
                Map<String,String> nestedColumnName = Helper.getHypenColumnName(superMap); 
                
                int jj = 1;
                for (Object O : lstOfMap)
                {
                    Map<Object, Object> mapObj = (Map<Object, Object>) O;
                    Map<Object, Object> mapObjNew = Helper.newHashMapWithoutAtARate(mapObj, nestedColumnName);
    
                    for (Map.Entry<Object, List<Object>> mapObjEntry : superMap.entrySet())
                    {
                        String key = mapObjEntry.getKey().toString();
                        List<Object> valueList = superMap.get(key);
                        Object value = mapObjNew.containsKey(key) ? mapObjNew.get(key) : key.equals(Constants.KEY) ? Constants.EMPTY_STRING+jj : Constants.EMPTY_STRING;
                        valueList.add(value);
                        superMap.put(key, valueList);
                    }
                    jj++;
                }
                
                Set<Object> keysFromTemplate = superMap.keySet();
                
                String keys = StringUtils.join(keysFromTemplate, Constants.COMMA);
                writer.append(keys);
                writer.append(Constants.NEW_LINE);
                
                for(int i=0;i<lstOfMap.size();i++)
                {
                    int j = 1;
                    int size = keysFromTemplate.size();
                    for(Object O : keysFromTemplate)
                    {
                        String key = O.toString().startsWith(Constants.AT_THE_RATE) ? O.toString().substring(1) : O.toString();
                        String value = superMap.get(key).get(i)!=null ? superMap.get(key).get(i).toString() : Constants.EMPTY_STRING;
                        value = value.contains(Constants.COMMA) ? Helper.putStringInQuotes(value) : value;
                        value = j!=size ? value+Constants.COMMA : value;
                        writer.append(value);
                        j++;
                    }
                    writer.append(Constants.NEW_LINE);
                }
                writer.flush();
                writer.close();
                
                
                //Nested CSV
                Map<Object, Map<Object,List<Attributes>>> nestedCSVMapwithHeader = Helper.readNestedTemplateAndColumnHeader(masterFilePath, masterFileName);
                
                Map<Object, Map<Object,List<Object>>> finalMap = new LinkedHashMap<Object, Map<Object,List<Object>>>();
                
                for (Map.Entry<Object, Map<Object,List<Attributes>>> mapObjEntry : nestedCSVMapwithHeader.entrySet())
                {
                    int parent = 1;
                    for (Object O : lstOfMap)
                    {
                        if (O instanceof Map)
                        {
                            Helper.recursive((Map<Object, Object>) O, finalMap, mapObjEntry.getKey().toString(), parent);
                        }
                        else if (O instanceof List<?>)
                        {
                            Helper.recursiveList((List<?>) O, finalMap, mapObjEntry.getKey().toString(), parent);
                        }
                        parent++;
                    }
                }
                
                for (Map.Entry<Object, Map<Object,List<Object>>> entry2 : finalMap.entrySet())
                {
                    String nestedCSVFile = entry2.getKey().toString();
                    
                    Map<Object,List<Object>> mapObj3 = ( Map<Object,List<Object>>) entry2.getValue();
                    for (Map.Entry<Object,List<Object>> O2 : mapObj3.entrySet())
                    {
                        int parent = (int) O2.getKey();
                        List<Object> value = O2.getValue();

                        Map<Object,List<Attributes>> nestedSuperMap = nestedCSVMapwithHeader.get(nestedCSVFile);
                        
                        Map<String,String> nestedColumnNameWithHypen = Helper.getHypenColumnNameForNested(nestedSuperMap); 
                        if(nestedSuperMap!=null)
                        {
                            for(Object O : value)
                            {
                                if (O instanceof Map)
                                {
                                    Map<Object, Object> mapObj1 = (Map<Object, Object>) O;
                                    Map<Object, Object> mapObjNew = Helper.newHashMapWithoutAtARate(mapObj1,nestedColumnNameWithHypen);
                                    Helper.fillNestedMap(nestedSuperMap, mapObjNew, parent);
                                }
                                else
                                {
                                    List<Object> mapObj1 = (List<Object>) O;
                                    for(Object O3 : mapObj1)
                                    {
                                        Map<Object, Object> mapObj4 = (Map<Object, Object>) O3;
                                        Map<Object, Object> mapObjNew = Helper.newHashMapWithoutAtARate(mapObj4,nestedColumnNameWithHypen);
                                        Helper.fillNestedMap(nestedSuperMap, mapObjNew, parent);
                                    }
                                }
                            }
                        }
                    }
                }
                
                for (Map.Entry<Object, Map<Object,List<Attributes>>> entry : nestedCSVMapwithHeader.entrySet())
                {
                    FileWriter writer1 = new FileWriter(dest_directory + entry.getKey() + Constants.CSV_EXTENSION,true);
                                    
                    Map<Object,List<Attributes>> map1 = entry.getValue();
                    
                    Set<Object> uniqueKeys1 = new LinkedHashSet<Object>();
                    int sizeOfRecords = 0;
                    for (Map.Entry<Object,List<Attributes>> entry1 : map1.entrySet())
                    {
                        uniqueKeys1.add(entry1.getKey());
                        sizeOfRecords = entry1.getValue().size();
                    }
                    
                    String keys1 = StringUtils.join(uniqueKeys1, Constants.COMMA);
                    writer1.append(keys1);
                    writer1.append(Constants.NEW_LINE);
    
                    for(int i=0;i<sizeOfRecords;i++)
                    {
                        int j = 0;
                        writer1.append((i+1)+Constants.COMMA);
                        int size = uniqueKeys1.size();
                        for(Object O : uniqueKeys1)
                        {
                            j++;
                            
                            if(O.equals(Constants.KEY) || O.equals(Constants.PARENT))
                                continue;
                            
                            int parent = map1.get(O).get(i).getParent();
                            String value = map1.get(O).get(i).getValue();
                            
                            if(j==3)
                                writer1.append(parent+Constants.COMMA);
                            
                            value = value==null ? Constants.EMPTY_STRING : value;
                            value = value.contains(Constants.COMMA) ? Helper.putStringInQuotes(value) : value;
                            value = j!=size ? value+Constants.COMMA : value;
                            
                            writer1.append(value);
                        }
                        writer1.append(Constants.NEW_LINE);
                    }
                    writer1.flush();
                    writer1.close();
                }
            }
        }
        catch (JsonGenerationException e)
        {
            LOGGER.error("JsonGenerationException:", e);
        }
        catch (JsonMappingException e)
        {
            LOGGER.error("JsonMappingException:", e);
        }
        catch (IOException e)
        {
            LOGGER.error("IOException:", e);
        }
        LOGGER.info("Found: "+found.toString());
        LOGGER.info("Not Found: "+notFound.toString());
        LOGGER.info("Ending the execution of JSON to CSV");
    }
}

