package com.hohai.jx.rdfVocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Created by wjh on 2016/4/10.
 */
public class CSVW {
    public static final String uri = "http://www.w3.org/2006/02/22-csvw#";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Resource TableGroup;
    public static final Resource Table;
    public static final Resource Row;
    public static final Property table;
    public static final Property url;
    public static final Property row;
    public static final Property rownum;
    public static final Property title;
    public static final Property describes;
    static {
        TableGroup = m.createResource("http://www.w3.org/2006/02/22-csvw#TableGroup");
        Table = m.createResource("http://www.w3.org/2006/02/22-csvw#Table");
        Row = m.createResource("http://www.w3.org/2006/02/22-csvw#Row");
        table = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "table");
        url = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "url");
        row = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "row");
        rownum = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "rownum");
        title = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "title");
        describes = m.createProperty("http://www.w3.org/2006/02/22-csvw#" , "describes");
    }

    public String getURI(){
        return "http://www.w3.org/2006/02/22-csvw#";
    }
}
