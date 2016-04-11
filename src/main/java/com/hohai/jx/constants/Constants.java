package com.hohai.jx.constants;

/**
 * Created by wjh on 2016/4/10.
 */
public class Constants {
    public static final String content = "@base <file:///D:/tree-ops.csv>\n" +
            "@prefix rdf : <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
            "@prefix csvw : <http://www.w3.org/ns/csvw#> .\n" +
            "@prefix dc : <http://purl.org/dc/terms/> .\n" +
            "@prefix dcat : <http://www.w3.org/ns/dcat#> .\n" +
            "@prefix foaf : <http://xmlns.com/foaf/0.1/> .\n" +
            "@prefix schema : <http://schema.org/> .\n" +
            "@prefix xsd : <http://www.w3.org/2001/XMLSchema#> .\n" +
            "\n" +
            "_:tablegroup a csvw:TableGroup ;\n" +
            "\tcsvw:table _:T1 .\n" +
            "\n" +
            "_:T1 a csvw:Table ;\n" +
            "\tcsvw:url <file:///D:/tree-ops.csv> ;\n" +
            "\tdcat:keyword [ \n" +
            "\t\t\"tree\" ,\n" +
            "\t\t\"street\" ,\n" +
            "\t\t\"maintenance\"\n" +
            "\t\t] ;\n" +
            "\tdc:publisher _:publisher ;\n" +
            "\tdc:license <http://opendefinition.org/licenses/cc-by/> ;\n" +
            "\tdc:modified \"2010-12-31\"^^xsd:data ;\n" +
            "\tcsvw:row [ \n" +
            "\t\t_:R1 ,\n" +
            "\t\t_:R2 \n" +
            "\t\t] .\n" +
            "\n" +
            "_:publisher schema:name \"Example Municipality\" ;\n" +
            "\tschema:url <http://example.org> .\n" +
            "\n" +
            "_:R1 a csvw:Row ;\n" +
            "\tcsvw:rownum \"1\"^^xsd:integer ;\n" +
            "\tcsvw:url <#row=2> ;\n" +
            "\tcsvw:describes _:Sdef1 .\n" +
            "\t\n" +
            "_:R2 a csvw:Row ;\n" +
            "\tcsvw:rownum \"2\"^^xsd:integer ;\n" +
            "\tcsvw:url <#row=3> ;\n" +
            "\tcsvw:describes _:Sdef2 .\n" +
            "\n" +
            "_:Sdef1\n" +
            "\t<#GID> \"1\"^^xsd:string ;\n" +
            "\t<#on_street> \"ADDISON AV\"^^xsd:string ;\n" +
            "\t<#species> \"Celtis australis\"^^xsd:string ;\n" +
            "\t<#trim_cycle> \"Large Tree Routine Prune\"^^xsd:string ;\n" +
            "\t<#inventory_date> \"10/18/2010\"^^xsd:date .\n" +
            "\n" +
            "_:Sdef2\n" +
            "\t<#GID> \"2\"^^xsd:string ;\n" +
            "\t<#on_street> \"EMERSON ST\"\"^^xsd:string ;\n" +
            "\t<#species> \"Liquidambar styraciflua\"^^xsd:string ;\n" +
            "\t<#trim_cycle> \"Large Tree Routine Prune\"^^xsd:string ;\n" +
            "\t<#inventory_date> \"6/2/2010\"^^xsd:date .\n";
}
