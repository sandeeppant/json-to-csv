package com.kronos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kronos.helper.Helper;
import com.kronos.utils.ConfigurationMap;
import com.kronos.utils.Constants;

/**
* <h1>Json To CSV Convertor</h1>
* The Json To CSV Convertor program converts JSON file to CSV format.
* 
* @author  Sandeep Pant
* @version 1.0
* @since   2016-05-20 
*/

public class JsonToCSVConvertor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToCSVConvertor.class);
    
    public static void main(String[] args)
    {
        String templatePath = ConfigurationMap.getProperty("template_path");
        String exportReportPath = ConfigurationMap.getProperty("export_report_path");
        String outputPath = ConfigurationMap.getProperty("output_path");
        generatedCSV(templatePath, exportReportPath, outputPath);
    }
    
    @SuppressWarnings("unchecked")
    public static void generatedCSV(String templatePath, String exportReportPath, String outputPath)
    {
        LOGGER.info("Starting the execution of JSON to CSV");
        List<String> found = new ArrayList<String>();
        List<String> notFound = new ArrayList<String>();
        try
        {
            File[] directories = new File(templatePath).listFiles(File::isDirectory);
            
            for(int fileNumber=0; fileNumber<directories.length; fileNumber++)
            {
                /*if(!directories[fileNumber].getName().equals("APIHolidayProfile"))
                {
                    continue;
                }*/
                
                String destDirectory = outputPath + Constants.BACK_SLASH + directories[fileNumber].getName() + Constants.BACK_SLASH;
                
                String jsonFile = exportReportPath + directories[fileNumber].getName()+Constants.BACK_SLASH+Constants.RESPONSE_JSON;
                
                File f = new File(jsonFile);

                if(f.exists())
                {
                    found.add(directories[fileNumber].getName());
                }
                else
                {
                    notFound.add(directories[fileNumber].getName());
                    continue;
                }
                
                File file = new File(destDirectory);
                if (!file.exists() && file.mkdirs()) 
                {
                    LOGGER.debug("Creating following directory: "+destDirectory);
                }
                
                FileUtils.cleanDirectory(file); 
                
                String masterFileName = directories[fileNumber].getName()+Constants.CSV_EXTENSION;
                String masterFilePath = directories[fileNumber].getAbsolutePath();
                String masterFileNameAndPath = masterFilePath+Constants.BACK_SLASH+masterFileName;
                
                ObjectMapper mapper = new ObjectMapper();
                // read JSON from a file
                Map<Object, Object> map = mapper.readValue(new File(jsonFile),new TypeReference<Map<Object, Object>>() {});
                
                Set<Object> nestedCSV = new LinkedHashSet<Object>();
                List<Map<Object,Object>> obj = (List<Map<Object, Object>>) map.get(Constants.ITEMS_RETRIEVE_RESPONSES);
                List<Object> lstOfMap = new LinkedList<Object>();
                for (Object O : obj)
                {
                    Map<Object, Object> mapObj = (Map<Object, Object>) O;
                    Map<Object, Object> O1 = (Map<Object, Object>)mapObj.get(Constants.RESPONSE_OBJECT_NODE);
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
                
                Helper.writeToMainCSV(superMap, lstOfMap, destDirectory, masterFileName);
                
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
                
                /*List<Object> lstOfNestedMap = new LinkedList<Object>();
                Map<Object, Map<Object,List<Object>>> finalMapdataz = new LinkedHashMap<Object, Map<Object,List<Object>>>(finalMap);*/
                
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
                Helper.writeToNestedCSV(nestedCSVMapwithHeader, destDirectory);
            }
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

