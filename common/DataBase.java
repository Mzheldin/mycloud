package geekbrains.java.cloud.common;

import org.sqlite.JDBC;
import java.sql.*;

public class DataBase {

    private Connection connection;
    private Statement statement;
    private final String folder = "_folder/";

    public void connect(){
        try {
            connection = DriverManager.getConnection(JDBC.PREFIX + "users.db");
            statement = connection.createStatement();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void disconnect(){
        try {
            statement.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public String getFolder(String logPass){
        try {
            String[] logPassArr = logPass.split(" ");
            String sqlQuery = "SELECT folder FROM users WHERE login = '"
                    + logPassArr[0] + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            System.out.println(resultSet.getString(1));
            return resultSet.getString(1);
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean getAuth(String logPass){
        boolean result = false;
        try {
            String[] logPassArr = logPass.split(" ");
            String sqlQuery = "SELECT * FROM users WHERE login = '"
                    + logPassArr[0] + "' AND pass = '" + logPassArr[1] + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);
            if (resultSet.next()) result = true;
            System.out.println(resultSet);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return result;
    }

    public boolean getReg(String logPass){
        int result = 0;
        try {
            if (!getAuth(logPass)){
                String[] logPassArr = logPass.split(" ");
                String sqlQuery = "INSERT INTO users (login, pass, folder) VALUES ('"
                        + logPassArr[0] + "', '" + logPassArr[1] +"', '" + logPassArr[0] + folder + "')";
                result = statement.executeUpdate(sqlQuery);
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return result == 1;
    }
}
