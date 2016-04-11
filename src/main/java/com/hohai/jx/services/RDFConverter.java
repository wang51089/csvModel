package com.hohai.jx.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.hohai.jx.constants.Constants;
import com.hohai.jx.rdfVocabularies.CSVW;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
/**
 * Created by wjh on 2016/4/10.
 */
public class RDFConverter {
    public String convert(ObjectNode groupOfTables){
        /*Model model = ModelFactory.createDefaultModel();
        Resource tableGroup = model.createResource();
        tableGroup.addProperty(RDF.type, "csvw:TableGroup");*/
        return Constants.content;
    }
}
