import java.sql.*;

public class Main {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement psGetCostByName;

    public static void main(String[] args) {
        try{
            connect();
            dropTable();
            createTable();
            prepareData();
            prepareStatements();
            getPriceByName("item10");
            updatePriceByName("item20", 999);
            getPriceByName("item20");
            selectInPriceRange(10, 700);
        }catch(SQLException e){
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }finally{
            disconnect();
        }
    }
    private static void selectInPriceRange(int min, int max) throws SQLException{
        ResultSet rs = statement.executeQuery("SELECT name FROM goods WHERE price >= " + min + " AND price <= " + max + ";");
        while(rs.next()) System.out.println(rs.getString(1));
    }
    private static void updatePriceByName(String name, int newPrice) {
        try{
            statement.executeUpdate("UPDATE goods SET price = " + newPrice + " WHERE name = '" + name +"';");
        }catch(SQLException e){
            System.out.println("Не возможно изменить цену товара!");
        }
    }
    private static void getPriceByName(String name) throws SQLException{
        ResultSet rs = statement.executeQuery("SELECT price FROM goods WHERE name = '" + name +"';");
        if(rs.next()) System.out.println(rs.getInt(1));
        else System.out.println("Такого товара нет!");
    }

    private static void prepareData() throws SQLException{
        connection.setAutoCommit(false);
        for(int i = 0; i < 10000; i++){
            statement.executeUpdate("INSERT INTO goods(itemID, name, price) VALUES (" + i + ", 'item" + i + "'," + i*10 +");");
        }
        connection.setAutoCommit(true);
    }

    private static void createTable() throws SQLException{
        statement.execute("CREATE TABLE IF NOT EXISTS goods (id INTEGER PRIMARY KEY AUTOINCREMENT, itemID INTEGER NOT NULL, name TEXT, price INTEGER);");
    }
    private static void dropTable() throws SQLException{
        statement.execute("DROP TABLE IF EXISTS goods;");
    }
    public static void prepareStatements() throws SQLException{
        psGetCostByName = connection.prepareStatement("SELECT price FROM goods WHERE name=?;");
    }

    public static void connect() throws SQLException, ClassNotFoundException{
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:jcpl2hw.db");
        statement = connection.createStatement();
    }
    public static void disconnect(){
        try{
            statement.close();
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

}
