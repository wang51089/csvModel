package com.hohai.jx.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by wjh on 2016/4/9.
 */
public class CSVParser {
    public static Map<String, String> context = new HashMap<String, String>() {
        {
            put("csvw", "http://www.w3.org/ns/csvw#");
            put("dc", "http://purl.org/dc/terms/");
            put("dcat", "http://www.w3.org/ns/dcat#");
            put("foaf", "http://xmlns.com/foaf/0.1/");
            put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            put("schema", "http://schema.org/");
            put("xsd", "http://www.w3.org/2001/XMLSchema#");
        }
    };
    public static String encoding = "utf-8";
    public static String[] lineTerminators = new String[]{"\r\n", "\n"};
    public static String quoteChar = "\"";
    public static boolean doubleQuote = true;
    public static int skipRows = 0;
    public static String commentPrefix = "#";
    public static boolean header = true;
    public static int headerRowCount = 1;
    public static String delimiter = ",";
    public static int skipColumns = 0;
    public static boolean skipBlankRows = false;
    public static boolean skipInitialSpace = false;
    public static String trim = "true";
    public static String escapeCharacter = "\\";
    //meta data
    private ObjectNode metaData = null;
    private ObjectNode table = null;

    public CSVParser(ObjectNode metaRootObject) {
        metaData = metaRootObject;
    }

    public void setDefaultDialect() {
        encoding = "utf-8";
        lineTerminators = new String[]{"\r\n", "\n"};
        quoteChar = "\"";
        doubleQuote = true;
        skipRows = 0;
        commentPrefix = "#";
        header = true;
        headerRowCount = 1;
        delimiter = ",";
        skipColumns = 0;
        skipBlankRows = false;
        skipInitialSpace = false;
        trim = "true";
        escapeCharacter = "\\";
    }

    public CSVParser(String metaFilePath) throws IOException {
        File metaFile = new File(metaFilePath);
        ObjectMapper objectMapper = new ObjectMapper();
        metaData = (ObjectNode) objectMapper.readTree(metaFile);
    }

    /**
     * entry: build the model
     *
     * @return
     * @throws Exception
     */
    public ObjectNode createTabularModel() throws Exception {
        JsonNode type = metaData.get("@type");
        JsonNode tables = metaData.get("tables");
        if ((type != null && "TableGroup".equalsIgnoreCase(type.asText())) || tables != null) {
            table = createAnnotatedTables(metaData);
        } else {
            ObjectNode convertedMetaData = new ObjectMapper().createObjectNode();
            convertedMetaData.put("@type", "TableGroup");
            ArrayNode tablesNode = convertedMetaData.putArray("tables");
            metaData.put("@type", "Table");
            tablesNode.add(metaData);
            table = createAnnotatedTables(convertedMetaData);
        }
        return table;
    }

    private void setDialect(JsonNode dialect) {
        encoding = dialect.get("encoding").asText(encoding);
        ArrayNode lineTerminatorsNode = (ArrayNode) dialect.get("lineTerminators");
        if (lineTerminatorsNode != null) {
            lineTerminators = new String[lineTerminatorsNode.size()];
            for (int i = 0; i < lineTerminatorsNode.size(); i++) {
                lineTerminators[i] = lineTerminatorsNode.get(i).asText();
            }
        }
        quoteChar = dialect.get("quoteChar").asText(quoteChar);
        doubleQuote = dialect.get("doubleQuote").asBoolean(doubleQuote);
        skipRows = dialect.get("skipRows").asInt(skipRows);
        commentPrefix = dialect.get("commentPrefix").asText(commentPrefix);
        header = dialect.get("header").asBoolean(header);
        headerRowCount = dialect.get("headerRowCount").asInt(headerRowCount);
        delimiter = dialect.get("delimiter").asText(delimiter);
        skipColumns = dialect.get("skipColumns").asInt(skipColumns);
        skipBlankRows = dialect.get("skipBlankRows").asBoolean(skipBlankRows);
        skipInitialSpace = dialect.get("skipInitialSpace").asBoolean(skipInitialSpace);
        trim = dialect.get("trim").asText(trim);
    }

    private ObjectNode addAnnotations(ObjectNode table, ObjectNode tableMetaData) throws Exception {
        Iterator<String> fieldNames = tableMetaData.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldNode = tableMetaData.get(fieldName);
            if (fieldName.equals("@context") || fieldName.equals("dialect") || fieldName.equals("@type") || fieldName.equals("tableSchema")) {
                ////do nothing
            } else if (fieldName.equals("@id")) {
                table.remove("@id");
                table.set("id", fieldNode);
            } else if (fieldName.contains(":")) {
                String[] names = fieldName.split(":");
                if (context.containsKey(names[0])) {
                    StringBuilder newName = new StringBuilder();
                    newName.append(context.get(names[0]));
                    newName.append(names[1]);
                    table.set(newName.toString(), fieldNode);
                }
            } else {
                table.set(fieldName, fieldNode);
            }
        }

        ObjectNode schemaNode = (ObjectNode) tableMetaData.get("tableSchema");
        //create column annotations
        ArrayNode schemaColumns = (ArrayNode) schemaNode.get("columns");
        ArrayNode tableColumns = (ArrayNode) table.get("columns");
        if (schemaColumns.size() != tableColumns.size()) {
            throw new Exception(" the model have different column number against the table schema! ");
        }
        int columnCount = tableColumns.size();
        for (int i = 0; i < columnCount; i++) {
            ObjectNode schemaColumn = (ObjectNode) schemaColumns.get(i);
            ObjectNode tableColumn = (ObjectNode) tableColumns.get(i);
            Iterator<String> names = schemaColumn.fieldNames();
            while (names.hasNext()) {
                String addingName = names.next();
                tableColumn.set(addingName, schemaColumn.get(addingName));
            }
        }
        //create foreign keys annotations
        ArrayNode foreignKeysNode = (ArrayNode) schemaNode.get("foreignKeys");
        if (foreignKeysNode != null) {
            // add annotation to the table
            table.set("foreignKeys", foreignKeysNode);
        }
        //create row annotations
        JsonNode primarykeyNode = schemaNode.get("primaryKey");
        if (primarykeyNode != null) {
            if (primarykeyNode.getNodeType().equals("ARRAY")) {
                Iterator<JsonNode> primaryIterator = ((ArrayNode) primarykeyNode).iterator();
                while (primaryIterator.hasNext()) {
                    String primayKey = primaryIterator.next().asText();
                    addPrimaryKey(table, primayKey);
                }
            } else {
                String primayKey = primarykeyNode.asText();
                addPrimaryKey(table, primayKey);
            }
        }
        JsonNode rowTitlesNode = schemaNode.get("rowTitles");
        if (rowTitlesNode != null) {
            if (rowTitlesNode.getNodeType().equals("ARRAY")) {
                Iterator<JsonNode> rowTitlesIterator = ((ArrayNode) rowTitlesNode).iterator();
                while (rowTitlesIterator.hasNext()) {
                    String rowTitle = rowTitlesIterator.next().asText();
                    addRowTitles(table, rowTitle);
                }
            } else {
                String rowTitle = rowTitlesNode.asText();
                addRowTitles(table, rowTitle);
            }
        }
        return table;
    }

    private ObjectNode addTableGroupAnnotations(ObjectNode tabularGroupModel, ObjectNode objectNode) throws Exception {
        Iterator<String> fieldNames = objectNode.fieldNames();
        // generate table annotations
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldNode = objectNode.get(fieldName);
            if (fieldName.equals("@context") || fieldName.equals("dialect") || fieldName.equals("@type") || fieldName.equals("tables")) {
                ////do nothing
            } else if (fieldName.equals("@id")) {
                table.remove("@id");
                table.set("id", fieldNode);
            } else if (fieldName.contains(":")) {
                String[] names = fieldName.split(":");
                if (context.containsKey(names[0])) {
                    StringBuilder newName = new StringBuilder();
                    newName.append(context.get(names[0]));
                    newName.append(names[1]);
                    tabularGroupModel.set(newName.toString(), fieldNode);
                }
            } else {
                tabularGroupModel.set(fieldName, fieldNode);
            }
        }
        return tabularGroupModel;
    }

    private List<Integer> getColNums(ObjectNode tableNode, ArrayNode referencingColumns) {
        List<Integer> colNums = new ArrayList<Integer>();
        ArrayNode columnsNode = (ArrayNode) tableNode.get("columns");
        Iterator<JsonNode> columnNames = referencingColumns.elements();

        while (columnNames.hasNext()) {
            String columnName = columnNames.next().asText();
            for (int i = 0; i < columnsNode.size(); i++) {
                ObjectNode columnNode = (ObjectNode) columnsNode.get(i);
                if (columnNode.get("name").equals(columnName)) {
                    colNums.add(i);
                }
            }
        }
        return colNums;
    }

    private ObjectNode getReferencedTable(ArrayNode tablesNode, String referencedTableURL) {
        Iterator<JsonNode> tableNodeI = tablesNode.iterator();
        while (tableNodeI.hasNext()) {
            ObjectNode tableNode = (ObjectNode) tableNodeI.next();
            if (tableNode.get("url").asText().equals(referencedTableURL)) {
                return tableNode;
            }
        }
        return null;
    }

    /**
     * create group tabular model according to the objectNode
     *
     * @param objectNode
     * @return
     */
    private ObjectNode createAnnotatedTables(ObjectNode objectNode) throws Exception {
        //create table group model
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode groupOfTables = objectMapper.createObjectNode();
        ArrayNode tablesNode = groupOfTables.putArray("tables");
        System.out.print("...");
        //parse the meta file to build model
        ArrayNode tables = (ArrayNode) objectNode.get("tables");

        JsonNode groupDialect = objectNode.get("dialect");
        JsonNode tableDirection = objectNode.get("tableDirection");
        JsonNode transformations = objectNode.get("transformations");
        JsonNode tableSchema = objectNode.get("tableSchema");
        if (tables != null) {
            Iterator<JsonNode> iterator = tables.iterator();
            while (iterator.hasNext()) {
                ObjectNode tableMetaData = (ObjectNode) iterator.next();
                if (groupDialect != null && tableMetaData.get("dialect") == null) {
                    tableMetaData.set("dialect", groupDialect);
                }
                if (tableDirection != null && tableMetaData.get("tableDirection") == null) {
                    tableMetaData.set("tableDirection", tableDirection);
                }
                if (transformations != null && tableMetaData.get("transformations") == null) {
                    tableMetaData.set("transformations", transformations);
                }
                if (transformations != null && tableMetaData.get("tableSchema") == null) {
                    tableMetaData.set("tableSchema", tableSchema);
                }
                ObjectNode table = parseTabularData(tableMetaData);
                ObjectNode annotatedTable = addAnnotations(table, tableMetaData);
                tablesNode.add(annotatedTable);
            }
        }
        addTableGroupAnnotations(groupOfTables, objectNode);
        return groupOfTables;

    }

    /**
     * parse the tabular file according to the dialect to get a simple tabular model
     *
     * @param tableMetaData
     * @return
     * @throws Exception
     */
    private ObjectNode parseTabularData(ObjectNode tableMetaData) throws Exception {
        //get the tabular file
        String urlString = tableMetaData.get("url").asText().trim();
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        //load the dialect
        ObjectNode dialect = (ObjectNode) tableMetaData.get("dialect");
        if (dialect != null) {
            setDialect(dialect);
        } else {
            setDefaultDialect();
        }

        //1.Create a new table T with the annotations
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode table = objectMapper.createObjectNode();
        ArrayNode rows = table.putArray("rows");
        ArrayNode columns = table.putArray("columns");


        int sourceRowNumber = 1;
        //5.Read the file using the encoding
        Reader reader = new InputStreamReader(inputStream, encoding);
        BufferedReader bufferedReader = new BufferedReader(reader);
        //6.Repeat the following the number of times indicated by skip rows
        sourceRowNumber += skipRows;
        for (int i = 0; i < skipRows; i++) {
            bufferedReader.readLine();
        }
        //7.Repeat the following the number of times indicated by header row count
        sourceRowNumber += headerRowCount;
        for (int i = 0; i < headerRowCount; i++) {
            bufferedReader.readLine();
        }
        //9.Set row number to 1.
        int rowNumber = 1;
        //10.While it is possible to read another row, do the following:
        String rowContent;
        while ((rowContent = bufferedReader.readLine()) != null) {
            int sourceColumnNumber = 1;
            if (commentPrefix != null && rowContent.startsWith(commentPrefix)) {
                //do nothing
            } else {
                //get  the   cells
                List<String> cellList = parseCells(rowContent);
                if (isEmptyRow(cellList) && skipBlankRows == true) {
                    continue;
                } else {
                    //create a  row  line 30
                    ObjectNode row = objectMapper.createObjectNode();
                    row.set("table", table);
                    row.put("number", rowNumber);
                    row.put("sourceNumber", sourceRowNumber);
                    ArrayNode primaryKey = row.putArray("primaryKey");
                    ArrayNode referencedRows = row.putArray("referencedRows");
                    ArrayNode rowCells = row.putArray("cells");
                    rows.add(row);

                    //get the  columns
                    cellList = skipSkipColumns(cellList);
                    sourceColumnNumber += skipColumns;
                    for (int i = 0; i < cellList.size(); i++) {
                        //create a column
                        ObjectNode columni = (ObjectNode) columns.get(i);
                        if (columni == null) {
                            columni = objectMapper.createObjectNode();
                            columni.set("table", table);
                            columni.put("number", i + 1);
                            columni.put("sourceNumber", sourceColumnNumber);
                            columni.putArray("cells");
                            columns.add(columni);
                        }
                        ArrayNode columnCells = (ArrayNode) columni.get("cells");

                        //create a cell
                        ObjectNode cell = objectMapper.createObjectNode();
                        cell.set("table", table);
                        cell.set("column", columni);
                        cell.set("row", row);
                        cell.put("stringValue", cellList.get(i));
                        cell.put("value", cellList.get(i));

                        //add the  cell to row cells  and   column cells
                        columnCells.add(cell);
                        rowCells.add(cell);
                        sourceColumnNumber++;
                    }
                    rowNumber++;
                }
            }
            sourceRowNumber++;
        }
        //12.Return the table T and the embedded metadata M
        return table;
    }

    private void addRowTitles(ObjectNode table, String rowTitle) {
        ArrayNode columnsNode = (ArrayNode) table.get("columns");
        ArrayNode rowsNode = (ArrayNode) table.get("rows");
        for (int i = 0; i < columnsNode.size(); i++) {
            if (columnsNode.get(i).get("name").equals(rowTitle)) {
                for (int j = 0; j < rowsNode.size(); j++) {
                    ArrayNode titlesRow = (ArrayNode) rowsNode.get(j).get("titles");
                    if (titlesRow == null) {
                        titlesRow = ((ObjectNode) rowsNode.get(i)).putArray("titles");
                    }
                    titlesRow.add(columnsNode.get(i));
                }
            }
        }
    }

    private void addPrimaryKey(ObjectNode table, String primayKey) {
        int primaryKeyColNum = -1;
        ArrayNode columnsNode = (ArrayNode) table.get("columns");
        for (int i = 0; i < columnsNode.size(); i++) {
            ObjectNode columnNode = (ObjectNode) columnsNode.get(i);
            String columnName = columnNode.get("name").asText();
            if (columnName.equals(primayKey)) {
                primaryKeyColNum = i;
                break;
            }
        }

        ArrayNode rowsNode = (ArrayNode) table.get("rows");
        for (int j = 0; j < rowsNode.size(); j++) {
            ObjectNode rowNode = (ObjectNode) rowsNode.get(j);
            ArrayNode primaryKeyRow = (ArrayNode) rowNode.get("primaryKey");
            if (primaryKeyRow == null) {
                primaryKeyRow = ((ObjectNode) rowNode).putArray("primaryKey");
            }
            primaryKeyRow.add(rowNode.get("cells").get(primaryKeyColNum));
        }

    }

    private void removePrefix(ObjectNode objectNode) {
        //deal with  common  properties
        Iterator<String> iterator = objectNode.fieldNames();
        List<String> fieldNames = new ArrayList<String>();
        while (iterator.hasNext()) {
            fieldNames.add(iterator.next());
        }
        for (String oldName : fieldNames) {
            JsonNode oldNode = objectNode.get(oldName);
            if (oldNode.getNodeType() == JsonNodeType.OBJECT) {
                removePrefix((ObjectNode) oldNode);
            }
            if (oldName.contains(":")) {
                String[] names = oldName.split(":");
                if (context.containsKey(names[0])) {
                    StringBuilder newName = new StringBuilder();
                    newName.append(context.get(names[0]));
                    newName.append(names[1]);
                    objectNode.set(newName.toString(), oldNode);
                    objectNode.remove(oldName);
                }
            }
        }

    }

    private boolean isEmptyRow(List<String> cells) {
        for (String cell : cells) {
            if (!"".equals(cell)) {
                return false;
            }
        }
        return true;
    }


    private List<String> parseCells(String rowContent) throws Exception {
        List<String> listCells = new ArrayList<String>();
        StringBuilder currenctCellValue = new StringBuilder();
        boolean quoted = false;
        char[] chars = rowContent.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == escapeCharacter.charAt(0)) {
                currenctCellValue.append(chars[i + 1]);
                i++;
            } else if (chars[i] == quoteChar.charAt(0)) {
                if (quoted == false) {
                    quoted = true;
                    if (currenctCellValue.length() > 0) {
                        throw new Exception(" currentCellValue is no empty! ");
                    }
                } else {
                    quoted = false;
                    if (chars[i + 1] != delimiter.charAt(0)) {
                        throw new Exception(" the end quoter is not ended with delimiter! ");
                    }
                }
            } else if (chars[i] == delimiter.charAt(0)) {
                if (quoted == true) {
                    currenctCellValue.append(chars[i]);
                } else {
                    String ce = conditionallyTrim(currenctCellValue);
                    listCells.add(ce);
                    currenctCellValue = new StringBuilder();
                }
            } else {
                currenctCellValue.append(chars[i]);
            }
        }
        String ce = conditionallyTrim(currenctCellValue);
        listCells.add(ce);
        return listCells;
    }

    private String conditionallyTrim(StringBuilder currenctCellValue) {
        if (trim.equals("true") || trim.equals("start")) {
            for (int i = 0; i < currenctCellValue.length(); i++) {
                if (currenctCellValue.charAt(i) == " ".charAt(0)) {
                    currenctCellValue.deleteCharAt(i);
                } else {
                    break;
                }
            }
        }
        if (trim.equals("true") || trim.equals("end")) {
            for (int i = currenctCellValue.length() - 1; i >= 0; i--) {
                if (currenctCellValue.charAt(i) == " ".charAt(0)) {
                    currenctCellValue.deleteCharAt(i);
                } else {
                    break;
                }
            }
        }
        return currenctCellValue.toString();
    }

    private List<String> skipSkipColumns(List<String> cells) {
        for (int i = 0; i < skipColumns; i++) {
            cells.remove(i);
        }
        return cells;
    }
}
