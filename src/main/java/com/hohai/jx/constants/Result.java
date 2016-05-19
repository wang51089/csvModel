package com.hohai.jx.constants;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by wjh on 2016/5/16.
 */
public class Result {
    public ObjectNode getTable() {
        return table;
    }

    public void setTable(ObjectNode table) {
        this.table = table;
    }

    public ObjectNode getEmbeddedMetadata() {
        return embeddedMetadata;
    }

    public void setEmbeddedMetadata(ObjectNode embeddedMetadata) {
        this.embeddedMetadata = embeddedMetadata;
    }

    ObjectNode table ;
    ObjectNode embeddedMetadata;

}
