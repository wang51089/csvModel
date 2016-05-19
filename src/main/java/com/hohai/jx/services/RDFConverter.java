package com.hohai.jx.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hohai.jx.vocabulary.CSVW;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.IRI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleNamespace;
import org.openrdf.model.vocabulary.DC;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResults;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.UUID;

/**
 * Created by wjh on 2016/4/10.
 */
public class RDFConverter {
    public String convert(ObjectNode groupOfTables) throws RepositoryException {
        Repository repository = new SailRepository(new MemoryStore());
        repository.initialize();
        ValueFactory valueFactory = repository.getValueFactory();
        RepositoryConnection connection = repository.getConnection();
        try {
            //1
            JsonNode id = groupOfTables.get("id");
            IRI G = null;
            if (id == null) {
                G = valueFactory.createIRI("_:G");
            } else {
                String gIRI = id.asText();
                G = valueFactory.createIRI(gIRI);
            }
            //2
            connection.add(G, RDF.TYPE, CSVW.TableGroup);
            //3
            Iterator<String> fieldNames = groupOfTables.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = groupOfTables.get(fieldName);
                if (fieldName.equals("notes") || fieldName.contains(":")) {
                    json2RDF(G, fieldName ,fieldNode, valueFactory, connection);
                }
            }
            //4
            ArrayNode tables = (ArrayNode) groupOfTables.get("tables");
            for (int i = 0; i < tables.size(); i++) {
                ObjectNode table = (ObjectNode) tables.get(i);
                //4.1
                JsonNode tableId = table.get("id");
                IRI T = null;
                if (tableId == null) {
                    T = valueFactory.createIRI("_:table" + i);
                } else {
                    String tableIRI = id.asText();
                    T = valueFactory.createIRI(tableIRI);
                }
                //4.2
                connection.add(G, CSVW.table, T);
                //4.3
                connection.add(T, RDF.TYPE, CSVW.Table);
                //4.4
                String tableUrlString = table.get("url").asText();
                IRI tableIRI = valueFactory.createIRI( tableUrlString );
                connection.add(T, CSVW.url, tableIRI );
                //4.5
                fieldNames = table.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode fieldNode = table.get(fieldName);
                    if (fieldName.equals("notes") || fieldName.contains(":")) {
                        json2RDF(T, fieldName ,fieldNode, valueFactory, connection);
                    }
                }
                //4.6
                ArrayNode rows = (ArrayNode) table.get("rows");
                for (int j = 0; j < rows.size(); j++) {
                    ObjectNode row = (ObjectNode) rows.get(j);
                    //4.6.1
                    IRI R = valueFactory.createIRI("_:row" + j);
                    //4.6.2
                    connection.add(T, CSVW.row, R);
                    //4.6.3
                    connection.add(R, RDF.TYPE, CSVW.Row);
                    //4.6.4
                    JsonNode n = row.get("number");
                    connection.add(R, CSVW.rownum, valueFactory.createLiteral(n.asInt()));
                    //4.6.5
                    JsonNode ns = row.get("sourceNumber");
                    connection.add(R, CSVW.url, valueFactory.createIRI(tableUrlString + "#row=" + ns.asInt()));
                    //4.6.6
                    ArrayNode titles = (ArrayNode) row.get("titles");
                    if( titles != null ){
                        Iterator<JsonNode> iterator = titles.iterator();
                        while (iterator.hasNext()){
                            String title = iterator.next().asText();
                            connection.add(R , CSVW.title , valueFactory.createLiteral(title));
                        }
                    }
                    //4.6.7
                    fieldNames = row.fieldNames();
                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        JsonNode fieldNode = row.get(fieldName);
                        if (fieldName.equals("notes") || fieldName.contains(":")) {
                            json2RDF(R, fieldName ,fieldNode, valueFactory, connection);
                        }
                    }
                    //4.6.8
                    IRI S = valueFactory.createIRI("_:sDef"+j);
                    ArrayNode cells = (ArrayNode) row.get("cells");
                    for( int k = 0 ; k < cells.size() ; k++ ){
                        ObjectNode cell = (ObjectNode) cells.get(k);
                        JsonNode aboutURL = cell.get("aboutURL");
                        //4.6.8.1
                        if( aboutURL!= null ){
                            S = valueFactory.createIRI(aboutURL.asText());
                        }
                        //4.6.8.2
                        connection.add(R , CSVW.describes , S);
                        //4.6.8.3
                        JsonNode propertyURL = cell.get("propertyURL");
                        IRI P = null;
                        if( propertyURL!= null ){
                            P = valueFactory.createIRI(propertyURL.asText());
                        }else {
                            JsonNode colName = table.get("columns").get(k).get("name");
                            P = valueFactory.createIRI(tableUrlString + "#column=" + colName.asText());
                        }
                        //4.6.8.4
                        JsonNode valueURL = cell.get("valueURL");
                        if( valueURL!= null ){
                            connection.add(S , P , valueFactory.createIRI(valueURL.asText()));
                        }else if( cell.get("stringValue")!= null ){
                            //4.6.8.7 i didnt take the list value into consideration, cz its rare and too conplicated.
                            String value = cell.get("stringValue").asText();
                            connection.add(S , P , valueFactory.createLiteral(value));
                        }
                    }
                }
            }
            RepositoryResult<Statement> statements = connection.getStatements(null , null , null , true);
            Model model = QueryResults.asModel(statements);
            model.setNamespace("rdf" , RDF.NAMESPACE);
            model.setNamespace("rdfs" , RDFS.NAMESPACE);
            model.setNamespace("csvw" , CSVW.NAMESPACE);
            model.setNamespace("dc" , DC.NAMESPACE);
            model.setNamespace("dcat" ,"http://www.w3.org/ns/dcat#");
            model.setNamespace("foaf" , FOAF.NAMESPACE);
            model.setNamespace("schema" , "http://schema.org/");
            model.setNamespace("xsd" , "http://www.w3.org/2001/XMLSchema#");

            StringWriter sw = new StringWriter();
            Rio.write(model, sw, RDFFormat.RDFXML);
            return sw.toString();
        } catch (RDFHandlerException e) {
            e.printStackTrace();
        } catch (QueryEvaluationException e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return  null;
    }

    private void json2RDF(IRI subject, String property, JsonNode value, ValueFactory valueFactory, RepositoryConnection connection) throws RepositoryException {
        IRI propertyIRI = valueFactory.createIRI(property);
        //1.property is already an absolute URL
        //2
        if( value.getNodeType() == JsonNodeType.ARRAY ){
            Iterator<JsonNode> arrays = ((ArrayNode) value).iterator();
            while (arrays.hasNext()){
                json2RDF(subject , property , arrays.next() , valueFactory , connection);
            }
        }
        //3.
        if( value.getNodeType() == JsonNodeType.OBJECT && value.get("@value") != null ){
            JsonNode lit = value.get("@value");
            connection.add(subject , propertyIRI , valueFactory.createLiteral(lit.asText()));
        }else if(  value.getNodeType() == JsonNodeType.OBJECT  ){
            //4.1
            IRI S = null;
            if( value.get("@id") != null ){
                S = valueFactory.createIRI( value.get("@id").asText() );
            }else {
                S = valueFactory.createIRI( "_:" + UUID.randomUUID());
            }
            connection.add(subject , propertyIRI , S);
            //4.2
            if(value.get("@type") != null){
                ArrayNode type = (ArrayNode) value.get("@type");
                for( int i = 0 ; i < type.size() ; i++ ){
                    IRI Ti = valueFactory.createIRI( type.get(i).asText() );
                    connection.add(S , RDF.TYPE , Ti);
                }
            }
            //4.3
            Iterator<String> fieldNames = value.fieldNames();
            while (fieldNames.hasNext()){
                String key = fieldNames.next();
                JsonNode val = value.get(key);
                if( !key.startsWith("@") ){
                    json2RDF( S , key , val , valueFactory , connection );
                }
            }
        }else {
            //5
            connection.add(subject , propertyIRI , valueFactory.createLiteral(value.asText()));
        }
    }
}
