package com.hohai.jx.window;

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
    private JFrame jFrame = new JFrame("csvAnnotator");
    private JMenuBar jMenuBar = new JMenuBar();
    JMenu file = new JMenu("文件");
    JMenuItem newItem = new JMenuItem("新建");
    JMenuItem openItem = new JMenuItem("打开");
    JMenuItem saveItem = new JMenuItem("保存");
    JMenuItem exitItem = new JMenuItem("退出");
    JMenu format = new JMenu("格式");
    JCheckBoxMenuItem autoWrap = new JCheckBoxMenuItem("自动换行");
    JMenuItem copyItem = new JMenuItem("复制");
    JMenuItem pasteItem = new JMenuItem("粘贴");
    JMenu edit = new JMenu("编辑");
    JMenuItem add = new JMenuItem("增加");
    JMenuItem delete = new JMenuItem("删除");
    JMenuItem modify = new JMenuItem("修改");
    JMenuItem undo = new JMenuItem("撤销");
    JMenu operation = new JMenu("操作");
    JMenuItem modelItem = new JMenuItem("建模");
    JMenuItem toRDFItem = new JMenuItem("生成RDF");
    JMenuItem saveResult = new JMenuItem("保存结果");
    JMenu help = new JMenu("帮助");
    JMenuItem helpItem = new JMenuItem("help");
    //文件选取对话框
    FileDialog openFile = new FileDialog(jFrame, "选择元数据文件", FileDialog.LOAD);
    FileDialog saveFile = new FileDialog(jFrame, "保存输出结果", FileDialog.SAVE);
    //窗口内容
    //左边
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("METADATA");
    JTree jTree = new JTree(rootNode);
    DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
    JScrollPane left = new JScrollPane(jTree);
    JPanel leftPanel = new JPanel();
    JLabel leftLabel = new JLabel("元数据：");
    //右边
    JLabel rightLable = new JLabel("元数据标注的CSV数据：");
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
        leftPanel.add(leftLabel , BorderLayout.NORTH);
        leftPanel.add(left , BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(300 , 400));
        //right
        right.setPreferredSize(new Dimension(700, 400));
        jSplitPane.resetToPreferredSizes();
        jSplitPane.setDividerSize(1);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(rightLable , BorderLayout.NORTH);
        rightPanel.add(right , BorderLayout.CENTER);

        jFrame.add(jSplitPane);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);

        //添加事件监听器
        //保存结果事件
        toRDFItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RDFConverter rdfConverter = new RDFConverter();
                String content = rdfConverter.convert(groupOfTables);
                jTextArea.setText("");
                jTextArea.append(content);
            }
        });


        saveResult.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveFile.setVisible(true);
                String fileString = saveFile.getDirectory()+saveFile.getFile();

                File file = new File(fileString);
                if( !file.exists() ){
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
                if( printWriter!=null ){
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
                displayModel();
                toRDFItem.setEnabled(true);
            }

            private void displayModel() {
                ArrayNode rows1 = null;
                ArrayNode columns1 = null;
                jTextArea.append("table group annotations:\n");
                appendAnnoatationsToTextArea(groupOfTables);
                jTextArea.append("\n");
                ArrayNode tables = (ArrayNode) groupOfTables.get("tables");
                for( int i = 0 ; i < tables.size() ; i++ ){
                    jTextArea.append("table[" + i + "] annoatations:\n");
                    ObjectNode table = (ObjectNode) tables.get(i);
                    appendAnnoatationsToTextArea(table);
                    jTextArea.append("\n");
                    ArrayNode rows = (ArrayNode) table.get("rows");
                    rows1 = rows;
                    for( int j = 0 ; j < rows.size() ; j++ ){
                        jTextArea.append("table[" + i +"]-row[" + j + "] annoatations:\n");
                        ObjectNode row = (ObjectNode) rows.get(j);
                        appendAnnoatationsToTextArea(row);
                        jTextArea.append("\n");
                    }
                    ArrayNode columns = (ArrayNode) table.get("columns");
                    columns1 = columns;
                    for( int k = 0 ; k < columns.size() ; k++ ){
                        jTextArea.append("table[" + i +"]-column[" + k + "] annoatations:\n");
                        ObjectNode column = (ObjectNode) columns.get(k);
                        appendAnnoatationsToTextArea(column);
                        jTextArea.append("\n");
                    }

                    for( int m = 0 ; m < rows1.size() ; m++ ){
                        for( int n = 0 ; n < columns1.size() ; n++ ){
                            jTextArea.append("table[" + i +"]-cell[" + m + "," + n + "] annoatations:\n");
                            ObjectNode cell = (ObjectNode) rows.get(m).get("cells").get(n);
                            appendAnnoatationsToTextArea(cell);
                            jTextArea.append("\n");
                        }
                    }
                }
            }

            private void appendAnnoatationsToTextArea(ObjectNode objectNode) {
                Iterator<String> fieldNames = objectNode.fieldNames();

                while (fieldNames.hasNext()){
                    String fieldName = fieldNames.next();
                    if( fieldName.equals("tables") || fieldName.equals("rows") || fieldName.equals("columns") || fieldName.equals("cells") ){
                        continue;
                    }else if( fieldName.equals( "table" ) ){
                        jTextArea.append("\ttable --> table[0]");
                        jTextArea.append("\n");
                    }else if(  fieldName.equals( "row" ) ){
                        ObjectNode row = (ObjectNode) objectNode.get(fieldName);
                        int m = row.get("number").asInt();
                        jTextArea.append("\trow --> row[" + m +"]");
                        jTextArea.append("\n");
                    }else if(  fieldName.equals( "column" ) ){
                        ObjectNode column = (ObjectNode) objectNode.get(fieldName);
                        int m = column.get("number").asInt();
                        jTextArea.append("\tcolumn --> column[" + m +"]");
                        jTextArea.append("\n");
                    }else if( fieldName.equals( "primaryKey" ) ){
                        ArrayNode primaryKey = (ArrayNode) objectNode.get(fieldName);
                        int p = primaryKey.get(0).get("row").get("number").asInt();
                        int q = primaryKey.get(0).get("column").get("number").asInt();
                        jTextArea.append("\tprimaryKey --> C["+p+"."+q+"]");
                        jTextArea.append("\n");
                    } else {
                        JsonNode jsonNode = objectNode.get(fieldName);
                        String jsonString = jsonNode.toString();
                        jTextArea.append("\t"+fieldName + " --> " + jsonString);
                        jTextArea.append("\n");
                    }
                }
            }

            public String jsonFormatter(String uglyJSONString){
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
                JLabel label1 = new JLabel("属性名：");
                JTextField propertyName = new JTextField();
                propertyName.setColumns(3);
                JLabel label2 = new JLabel("属性值：");
                JTextField propertyValue = new JTextField();
                propertyValue.setColumns(3);
                JPanel jPanel = new JPanel();
                jPanel.add(label1);
                jPanel.add(propertyName);
                jPanel.add(label2);
                jPanel.add(propertyValue);
                int result = JOptionPane.showConfirmDialog(jFrame, jPanel, "创建新属性", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String name = propertyName.getText().trim();
                    String value = propertyValue.getText().trim();
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
                    if (selectedNode == null) {
                        JOptionPane.showConfirmDialog(jFrame, "没有选择需要加入的父节点！", null, JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
                    }
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(name + " --> " + value);
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
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + " --> (Object)");
                        treeNode.add(newChild);
                        buildObjectTreeNode((ObjectNode) fieldNode, newChild);
                    } else if (fieldNode.getNodeType() == JsonNodeType.ARRAY) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + " --> (Array)");
                        treeNode.add(newChild);
                        buildArrayTreeNode((ArrayNode) fieldNode, newChild);
                    } else {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(fieldName + " --> " + fieldNode.asText());
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
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + " --> (Object)");
                        treeNode.add(newChild);
                        buildObjectTreeNode((ObjectNode) jsonNode, newChild);
                    } else if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + " --> (Array)");
                        treeNode.add(newChild);
                        buildArrayTreeNode((ArrayNode) jsonNode, newChild);
                    } else {
                        DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(i + " --> " + jsonNode.asText());
                        treeNode.add(newChild);
                    }
                    i++;
                }
            }
        });

    }


}
