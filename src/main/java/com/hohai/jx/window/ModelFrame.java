package com.hohai.jx.window;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hohai.jx.services.CSVParser;
import com.hohai.jx.services.RDFConverter;
import org.openrdf.repository.RepositoryException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Iterator;

/**
 * Created by wjh on 2016/4/8.
 */
public class ModelFrame {
    //菜单
    private JFrame jFrame = new JFrame("csv2rdf");
    private JMenuBar jMenuBar = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem newItem = new JMenuItem("new");
    JMenuItem openItem = new JMenuItem("open");
    JMenuItem saveItem = new JMenuItem("save");
    JMenuItem exitItem = new JMenuItem("exit");
    JMenu format = new JMenu("Format");
    JCheckBoxMenuItem autoWrap = new JCheckBoxMenuItem("autowrap");
    JMenuItem copyItem = new JMenuItem("copy");
    JMenuItem pasteItem = new JMenuItem("paste");
    JMenu edit = new JMenu("Edit");
    JMenuItem add = new JMenuItem("add");
    JMenuItem delete = new JMenuItem("delete");
    JMenuItem modify = new JMenuItem("modify");
    JMenuItem undo = new JMenuItem("undo");
    JMenu operation = new JMenu("Operation");
    JMenuItem modelItem = new JMenuItem("csv to annotated csv");
    JMenuItem toRDFItem = new JMenuItem("convert csv to rdf");
    JMenuItem saveResult = new JMenuItem("save result");
    JMenu help = new JMenu("help");
    JMenuItem helpItem = new JMenuItem("links");
    //文件选取对话框
    FileDialog openFile = new FileDialog(jFrame, "Select metadata file", FileDialog.LOAD);
    FileDialog saveFile = new FileDialog(jFrame, "Save the metadata file", FileDialog.SAVE);
    //窗口内容
    //左边
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("METADATA");
    JTree jTree = new JTree(rootNode);
    DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
    JScrollPane left = new JScrollPane(jTree);
    JPanel leftPanel = new JPanel();
    JLabel leftLabel = new JLabel(" Metadata:");
    //右边
    JLabel rightLable = new JLabel(" Output Result");
    JTextArea jTextArea = new JTextArea();
    JScrollPane right = new JScrollPane(jTextArea);
    JPanel rightPanel = new JPanel();
    JSplitPane jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, leftPanel, rightPanel);

    //data model
    ObjectNode metaRootObject = null;
    ObjectNode groupOfTables = null;

    public void init() {
        //菜单
        file.add(newItem);
        file.add(openItem);
        file.add(saveItem);
        file.add(exitItem);
        format.add(autoWrap);
        format.addSeparator();
        format.add(copyItem);
        format.add(pasteItem);
        edit.add(add);
        edit.add(delete);
        edit.add(modify);
        edit.add(undo);
        modelItem.setEnabled(false);
        operation.add(modelItem);
        toRDFItem.setEnabled(false);
        operation.add(toRDFItem);
        operation.add(saveResult);
        help.add(helpItem);
        jMenuBar.add(file);
        jMenuBar.add(format);
        jMenuBar.add(edit);
        jMenuBar.add(operation);
        jMenuBar.add(help);
        jFrame.setJMenuBar(jMenuBar);
        //内容
        //left
        jTree.setEditable(true);
        jTree.setShowsRootHandles(true);
        //left.setPreferredSize(new Dimension(300, 400));
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(leftLabel, BorderLayout.NORTH);
        leftPanel.add(left, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(300, 400));
        //right
        right.setPreferredSize(new Dimension(700, 400));
        jSplitPane.resetToPreferredSizes();
        jSplitPane.setDividerSize(1);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(rightLable, BorderLayout.NORTH);
        rightPanel.add(right, BorderLayout.CENTER);

        jFrame.add(jSplitPane);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);

        //添加事件监听器
        //保存结果事件
        toRDFItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RDFConverter rdfConverter = new RDFConverter();
                String content = null;
                try {
                    content = rdfConverter.convert(groupOfTables);
                } catch (RepositoryException e1) {
                    e1.printStackTrace();
                }
                jTextArea.setText("");
                if( content != null ){
                    jTextArea.append(content);
                }else {
                    jTextArea.append("converter output is null");
                }

            }
        });


        saveResult.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveFile.setVisible(true);
                String fileString = saveFile.getDirectory() + saveFile.getFile();

                File file = new File(fileString);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(file);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }
                PrintWriter printWriter = new PrintWriter(fileOutputStream);
                String content = jTextArea.getText();
                printWriter.write(content);
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        });

        //建模事件
        modelItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CSVParser csvParser = null;
                csvParser = new CSVParser(metaRootObject);
                try {
                    groupOfTables = csvParser.createTabularModel();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                //display model in jTextArrea
                try {
                    displayModel();
                } catch (JsonProcessingException e1) {
                    e1.printStackTrace();
                }
                toRDFItem.setEnabled(true);
            }

            private void displayModel() throws JsonProcessingException {
                jTextArea.append("table group annotations:\n");
                Iterator<String> fieldNames = groupOfTables.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    if (fieldName.equals("tables")) {
                        continue;
                    } else {
                        JsonNode jsonNode = groupOfTables.get(fieldName);
                        String jsonString = jsonNode.toString();
                        jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString);
                        jTextArea.append("\n");
                    }
                }
                jTextArea.append("\n");
                ArrayNode tables = (ArrayNode) groupOfTables.get("tables");
                for (int i = 0; i < tables.size(); i++) {
                    jTextArea.append("table[" + (i + 1) + "] annoatations:\n");
                    ObjectNode table = (ObjectNode) tables.get(i);
                    fieldNames = table.fieldNames();

                    while (fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        if (fieldName.equals("rows") || fieldName.equals("columns")) {
                            continue;
                        } else {
                            JsonNode jsonNode = table.get(fieldName);
                            String jsonString = jsonNode.toString();
                            jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString);
                            jTextArea.append("\n");
                        }
                    }
                    jTextArea.append("\n");
                    ArrayNode rows = (ArrayNode) table.get("rows");
                    for (int j = 0; j < rows.size(); j++) {
                        jTextArea.append("row[" + (j + 1) + "] of table[" + (i + 1) + "] has annoatations:\n");
                        ObjectNode row = (ObjectNode) rows.get(j);
                        fieldNames = row.fieldNames();

                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (fieldName.equals("cells")) {
                                continue;
                            } else if (fieldName.equals("table")) {
                                jTextArea.append("\ttable   `s value is   table[" + i + "]");
                                jTextArea.append("\n");
                            } else if (fieldName.equals("primaryKey")) {
                                JsonNode jsonNode = row.get("primaryKey");
                                ArrayNode jsonNodes = (ArrayNode) jsonNode;
                                if (jsonNodes.size() > 0) {
                                    StringBuilder jsonString = new StringBuilder();
                                    for (int ii = 0; ii < jsonNodes.size(); ii++) {
                                        jsonString.append(jsonNodes.get(ii).asText() + ",");
                                    }
                                    jsonString.deleteCharAt(jsonString.length() - 1);
                                    jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString.toString());
                                    jTextArea.append("\n");
                                }
                            } else if( fieldName.equals("referencedRows") ){

                            }else {
                                JsonNode jsonNode = row.get(fieldName);
                                String jsonString = jsonNode.toString();
                                jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString);
                                jTextArea.append("\n");
                            }
                        }
                        jTextArea.append("\n");
                    }
                    ArrayNode columns = (ArrayNode) table.get("columns");
                    for (int k = 0; k < columns.size(); k++) {
                        jTextArea.append("column[" + (k + 1) + "] of table[" + (i + 1) + "] has annoatations:\n");
                        ObjectNode column = (ObjectNode) columns.get(k);
                        fieldNames = column.fieldNames();

                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (fieldName.equals("cells")) {
                                continue;
                            } else if (fieldName.equals("table")) {
                                jTextArea.append("\ttable   `s value is   table[" + i + "]");
                                jTextArea.append("\n");
                            } else {
                                JsonNode jsonNode = column.get(fieldName);
                                String jsonString = jsonNode.toString();
                                jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString);
                                jTextArea.append("\n");
                            }
                        }
                        jTextArea.append("\n");
                    }

                    for (int m = 0; m < rows.size(); m++) {
                        for (int n = 0; n < columns.size(); n++) {
                            jTextArea.append("table[" + (i + 1) + "], cell[" + (m + 1) + "," + (n + 1) + "] has annotations:\n");
                            ObjectNode cell = (ObjectNode) rows.get(m).get("cells").get(n);
                            Iterator<String> fieldNames1 = cell.fieldNames();

                            while (fieldNames1.hasNext()) {
                                String fieldName = fieldNames1.next();
                                if (fieldName.equals("table")) {
                                    jTextArea.append("\ttable   `s value is   table[" + i + "]");
                                    jTextArea.append("\n");
                                } else if (fieldName.equals("row")) {
                                    ObjectNode row = (ObjectNode) cell.get("row");
                                    int rowNum = row.get("number").asInt();
                                    jTextArea.append("\trow   `s value is   row[" + rowNum + "]");
                                    jTextArea.append("\n");
                                } else if (fieldName.equals("column")) {
                                    ObjectNode column = (ObjectNode) cell.get("column");
                                    int columnNum = column.get("number").asInt();
                                    jTextArea.append("\tcolumn   `s value is   column[" + columnNum + "]");
                                    jTextArea.append("\n");
                                } else {
                                    JsonNode jsonNode = cell.get(fieldName);
                                    String jsonString = jsonNode.toString();
                                    jTextArea.append("\t" + fieldName + "   `s value is   " + jsonString);
                                    jTextArea.append("\n");
                                }
                            }
                            jTextArea.append("\n");
                        }
                    }
                }
            }


            public String jsonFormatter(String uglyJSONString) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(uglyJSONString);
                String prettyJsonString = gson.toJson(je);
                return prettyJsonString;
            }
        });

        //增加元数据
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JLabel label1 = new JLabel("property name:");
                JTextField propertyName = new JTextField();
                propertyName.setColumns(3);
                JLabel label2 = new JLabel("property value:");
                JTextField propertyValue = new JTextField();
                propertyValue.setColumns(3);
                JPanel jPanel = new JPanel();
                jPanel.add(label1);
                jPanel.add(propertyName);
                jPanel.add(label2);
                jPanel.add(propertyValue);
                int result = JOptionPane.showConfirmDialog(jFrame, jPanel, "create new annotation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String name = propertyName.getText().trim();
                    String value = propertyValue.getText().trim();
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
                    if (selectedNode == null) {
                        JOptionPane.showConfirmDialog(jFrame, "please select the node to add properties", null, JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name + " > " + value);
                    selectedNode.add(newNode);
                    TreeNode[] nodes = treeModel.getPathToRoot(newNode);
                    TreePath treePath = new TreePath(nodes);
                    jTree.scrollPathToVisible(treePath);
                    jTree.updateUI();

                    //添加到数据模型
                    ObjectNode newNodeParent = (ObjectNode) metaRootObject.findValue(selectedNode.toString().split("-")[0].trim());
                    newNodeParent.put(name, value);
                }
            }
        });

        //修改元数据
        modify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TreePath treePath = jTree.getSelectionPath();
                jTree.startEditingAtPath(treePath);
                //修改数据模型
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                String fieldName = selectedNode.toString().split("-")[0].trim();
                String newValue = selectedNode.toString().split(">")[1].trim();
                ObjectNode objectNode = metaRootObject.findParent(fieldName);
                objectNode.remove(fieldName);
                objectNode.put(fieldName, newValue);
            }
        });

        //删除元数据
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
                treeModel.removeNodeFromParent(selectedNode);
                //修改数据模型
                String fieldName = selectedNode.toString().split("-")[0].trim();
                ObjectNode objectNode = metaRootObject.findParent(fieldName);
                objectNode.remove(fieldName);
            }
        });

        //打开元数据
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFile.setVisible(true);
                String fileString = openFile.getDirectory() + openFile.getFile();

                File metaFile = new File(fileString);
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    metaRootObject = (ObjectNode) objectMapper.readTree(metaFile);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                rootNode.removeAllChildren();
                buildObjectTreeNode(metaRootObject, rootNode);
                jTree.updateUI();
                jTree.expandRow(0);
                modelItem.setEnabled(true);
            }

            void buildObjectTreeNode(ObjectNode objectNode, DefaultMutableTreeNode treeNode) {
                Iterator<String> propertyNames = objectNode.fieldNames();
                while (propertyNames.hasNext()) {
                    String fieldName = propertyNames.next();
                    JsonNode fieldNode = objectNode.get(fieldName);
                    if (fieldNode.getNodeType() == JsonNodeType.OBJECT) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + "   >   (Object)");
                        treeNode.add(newChild);
                        buildObjectTreeNode((ObjectNode) fieldNode, newChild);
                    } else if (fieldNode.getNodeType() == JsonNodeType.ARRAY) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + "   >   (Array)");
                        treeNode.add(newChild);
                        buildArrayTreeNode((ArrayNode) fieldNode, newChild);
                    } else {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + "   >   " + fieldNode.asText());
                        treeNode.add(newChild);
                    }
                }
            }

            private void buildArrayTreeNode(ArrayNode arrayNode, DefaultMutableTreeNode treeNode) {
                Iterator<JsonNode> elements = arrayNode.elements();
                int i = 0;
                while (elements.hasNext()) {
                    JsonNode jsonNode = elements.next();
                    if (jsonNode.getNodeType() == JsonNodeType.OBJECT) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + "   >   (Object)");
                        treeNode.add(newChild);
                        buildObjectTreeNode((ObjectNode) jsonNode, newChild);
                    } else if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + "   >   (Array)");
                        treeNode.add(newChild);
                        buildArrayTreeNode((ArrayNode) jsonNode, newChild);
                    } else {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + "   >   " + jsonNode.asText());
                        treeNode.add(newChild);
                    }
                    i++;
                }
            }
        });

    }


}
