package com.ashisk.elecatbackup;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.sql.*;
import java.util.List;
import java.io.*;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;


import org.bukkit.event.*;

public class EleCatBackup extends JavaPlugin implements Listener{
    private Connection connection;
    public void say(String s){
        CommandSender sender = Bukkit.getConsoleSender();
        sender.sendMessage(s);
    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        if(!checkDatabaseExists()){
            createDatabase();
            createTable();
        }
        getServer().getPluginManager().registerEvents(this,this);
        saveAllPluginInfo();
        this.saveDefaultConfig();
        say("§6[§3ECatBackup§6]§7插件成功加载，运行良好!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(connection!=null){
            try{
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        say("§6[§3ECatBackup§6]§7插件已卸载,但是为什么?");
    }
    public Connection getConnection() throws SQLException{
        if (connection==null||connection.isClosed()){
            FileConfiguration configurationTemp=getConfig();
            String mysqlUrl= configurationTemp.getString("info.mysqlUrl");
            String mysqlUsername=configurationTemp.getString("info.mysqlUser");
            String mysqlPassword=configurationTemp.getString("info.mysqlAddress");
            connection=DriverManager.getConnection(mysqlUrl,mysqlUsername,mysqlPassword);
        }
        return connection;
    }

    private boolean checkDatabaseExists(){
        try{
            Connection connection=getConnection();
            PreparedStatement statement =connection.prepareStatement("use ServerPlugins");
            statement.executeUpdate();
            statement.close();
            connection.close();
            return true;
        }catch (SQLException e){
            return false;
        }
    }
    private void createDatabase(){
        try{
            Connection connection=getConnection();
            PreparedStatement statement =connection.prepareStatement("create database ServerPlugins;");
            statement.executeUpdate();
            statement.close();
            connection.close();
            say("[mysql]成功创建数据库");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    private void createTable() {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = null;
            statement=connection.prepareStatement("use ServerPlugins");
            statement.executeUpdate();
            statement.clearParameters();
            statement = connection.prepareStatement(
                    "create table plugin_info(" +
                            "PluginId int(4) not null primary key auto_increment," +
                            "PluginName varchar(20) unique key  not null," +
                            "PluginVersion varchar(20)," +
                            "PluginAuthor varchar(30)," +
                            "PluginComment longtext)");
            statement.executeUpdate();
            statement.close();
            connection.close();
            say("[mysql]成功创建表");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    private void savePluginInfo(Plugin plugin){
        String name=plugin.getName();
        String version=plugin.getDescription().getVersion();
        List<String> authors=plugin.getDescription().getAuthors();
        String author=String.join(",",authors);
        try{
            Connection connection=getConnection();
            PreparedStatement statement = null;
            statement=connection.prepareStatement("use ServerPlugins");
            statement.executeUpdate();
            statement.clearParameters();
            statement=connection.prepareStatement(
                    "insert ignore into plugin_info(PluginName,PluginVersion,Pluginauthor,PluginComment)values (?,?,?,NULL)"
            );
            statement.setString(1,name);
            statement.setString(2,version);
            statement.setString(3,author);
            statement.executeUpdate();
            statement.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    private void saveAllPluginInfo(){
        for (Plugin plugin:Bukkit.getPluginManager().getPlugins()) {
            savePluginInfo(plugin);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("testPlugins")&&sender.hasPermission("elecat.testPlugins")){
            if(args[0].equalsIgnoreCase("list")){
                if(!(sender instanceof Player)){
                    sender.sendMessage("conslse player");
                }else{
                    Player player=(Player) sender;
                    sender.sendMessage("ingame player");
                    if(sender.isOp()){
                        sender.sendMessage("admin player");
                    }else {
                        sender.sendMessage("common player");
                    }
                }
                try {
                    Connection connection = getConnection();
                    PreparedStatement statement = null;
                    statement=connection.prepareStatement("use ServerPlugins");
                    statement.executeUpdate();
                    statement.clearParameters();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM plugin_info");
                    sender.sendMessage("插件信息：");
                    while (resultSet.next()) {
                        int id=resultSet.getInt("PluginId");
                        String name = resultSet.getString("PluginName");
                        String version = resultSet.getString("PluginVersion");
                        String author = resultSet.getString("PluginAuthor");
                        String Comment = resultSet.getString("PluginComment");
                        sender.sendMessage("[ "+id+" ] "+"名称: " + name + ", 版本: " + version + ", 作者: " + author+",   |   备注: " + Comment);
                    }
                    resultSet.close();
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return true;
            }else if (args[0].equalsIgnoreCase("comment")){
                try{
                    Connection connection=getConnection();
                    PreparedStatement statement = null;
                    statement=connection.prepareStatement("use ServerPlugins");
                    statement.executeUpdate();
                    statement.clearParameters();
                    statement=connection.prepareStatement(
                            "update plugin_info set PluginComment=? where PluginID=?"
                    );
                    statement.setString(1,args[2]);
                    statement.setString(2,args[1]);
                    statement.executeUpdate();
                    statement.close();
                    connection.close();
                }catch (SQLException e){
                    e.printStackTrace();
                }
                say("[EleCat]操作成功!");
                return true;
            }else if (args[0].equalsIgnoreCase("admin")){
                if (args[1].equalsIgnoreCase("delete")){
                    try{
                        Connection connection=getConnection();
                        PreparedStatement statement = null;
                        statement=connection.prepareStatement("use ServerPlugins");
                        statement.executeUpdate();
                        statement.clearParameters();
                        statement=connection.prepareStatement(
                                "delete from plugin_info where PluginID=?"
                        );
                        statement.setString(1,args[2]);
                        statement.executeUpdate();
                        statement.close();
                        connection.close();
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                    say("[EleCat]操作成功!");
                    return true;
                }else if (args[1].equalsIgnoreCase("reload")){
                    try{
                        Connection connection=getConnection();
                        PreparedStatement statement =connection.prepareStatement("drop database ServerPlugins");
                        statement.executeUpdate();
                        statement.close();
                        if(!checkDatabaseExists()){
                            createDatabase();
                            createTable();
                            saveAllPluginInfo();
                        }
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                    say("[EleCat]表数据已重载");
                    return true;
                }else if (args[1].equalsIgnoreCase("UploadToXml")){
                    try{
                    Connection connection = getConnection();
                        PreparedStatement statement = null;
                        statement=connection.prepareStatement("use ServerPlugins");
                        statement.executeUpdate();
                        statement.clearParameters();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM plugin_info");
                    String fileNameXML=args[2];
                    XMLOutputFactory XMLoutputPlugin=XMLOutputFactory.newInstance();
                    XMLStreamWriter XMLWritePlugin= XMLoutputPlugin.createXMLStreamWriter(new FileWriter(fileNameXML+".xml"));
                    XMLWritePlugin.writeStartElement("Plugins");
                    while (resultSet.next()){
                        XMLWritePlugin.writeStartElement("ID");
                        int pluginId=resultSet.getInt("PluginId");
                        XMLWritePlugin.writeCharacters(String.valueOf(pluginId));
                        XMLWritePlugin.writeEndElement();
                        XMLWritePlugin.writeStartElement("Name");
                        String pluginName=resultSet.getString("PluginName");
                        if(pluginName!=null){
                        XMLWritePlugin.writeCharacters(String.valueOf(pluginName));}
                        XMLWritePlugin.writeEndElement();
                        XMLWritePlugin.writeStartElement("Version");
                        String pluginVersion=resultSet.getString("PluginVersion");
                        if(pluginVersion!=null){
                        XMLWritePlugin.writeCharacters(String.valueOf(pluginVersion));}
                        XMLWritePlugin.writeEndElement();
                        XMLWritePlugin.writeStartElement("Authors");
                        String pluginAuthor=resultSet.getString("PluginAuthor");
                        if(pluginAuthor!=null){
                        XMLWritePlugin.writeCharacters(String.valueOf(pluginAuthor));}
                        XMLWritePlugin.writeEndElement();
                        XMLWritePlugin.writeStartElement("Comment");
                        String pluginComment=resultSet.getString("PluginComment");
                        if(pluginComment!=null){
                        XMLWritePlugin.writeCharacters(String.valueOf(pluginComment));}
                        XMLWritePlugin.writeEndElement();
                    }XMLWritePlugin.writeEndElement();
                    XMLWritePlugin.writeEndDocument();
                    XMLWritePlugin.close();
                    say("[EleCat]成功将插件数据导出到XML文件");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return true;
                } else if (args[1].equalsIgnoreCase("openWeb")) {
                    try{
                        Connection connection=getConnection();
                        PreparedStatement statement = null;
                        statement=connection.prepareStatement("use ServerPlugins");
                        statement.executeUpdate();
                        statement.close();
                        HTMLGenerator htmlGenerator = new HTMLGenerator();
                        FileConfiguration configurationTemp=getConfig();
                        htmlGenerator.setPort(configurationTemp.getInt("info.webPort"));
                        htmlGenerator.setHostname(configurationTemp.getString("info.webHost"));
                        htmlGenerator.setConnection(connection);
                        String tableName = "plugin_info";
                        String htmlContent = htmlGenerator.generatePluginInfoTable(tableName, connection);
                        String serverUrl = htmlGenerator.startServerAndReturnURL(htmlContent);
                        say("Server URL: " + serverUrl);
                    }catch (SQLException e){
                        e.printStackTrace();
                    }
                    return true;
                }
                return true;
            }
            return true;
        }
        return false;
    }
}
