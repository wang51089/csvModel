package com.hohai.jx.vocabulary;

import org.openrdf.model.IRI;
import org.openrdf.model.Namespace;
import org.openrdf.model.impl.SimpleNamespace;
import org.openrdf.model.impl.SimpleValueFactory;

/**
 * Created by wjh on 2016/4/25.
 */
public class CSVW {
    public static final String NAMESPACE = "http://www.w3.org/ns/csvw#";
    public static final String PREFIX = "csvw";
    public static final Namespace NS = new SimpleNamespace("csvw", "http://www.w3.org/ns/csvw#");
    public static final IRI TableGroup;
    public static final IRI table;
    public static final IRI Table;
    public static final IRI url;
    public static final IRI row;
    public static final IRI Row;
    public static final IRI rownum;
    public static final IRI title;
    public static final IRI describes;

    public CSVW() {
    }

    static {
        SimpleValueFactory factory = SimpleValueFactory.getInstance();
        TableGroup = factory.createIRI("http://www.w3.org/ns/csvw#", "TableGroup");
        table = factory.createIRI("http://www.w3.org/ns/csvw#", "table");
        Table = factory.createIRI("http://www.w3.org/ns/csvw#", "Table");
        url = factory.createIRI("http://www.w3.org/ns/csvw#", "url");
        row = factory.createIRI("http://www.w3.org/ns/csvw#", "row");
        Row = factory.createIRI("http://www.w3.org/ns/csvw#", "Row");
        rownum = factory.createIRI("http://www.w3.org/ns/csvw#", "rownum");
        title = factory.createIRI("http://www.w3.org/ns/csvw#", "title");
        describes = factory.createIRI("http://www.w3.org/ns/csvw#", "describes");
    }
}
