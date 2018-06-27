import java.sql.*;

public class DBHelper {

    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement ps;

    public static final String TABLE_NAME = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_PASS = "password";
    public static final String COLUMN_NICK = "nickname";
    public static final String DEFAULT_LOGIN = "login";
    public static final String DEFAULT_PASS = "password";
    public static final String DEFAULT_NICK = "client";

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
            statement = connection.createStatement();
            createTable(connection);
            prepareData();
            printOutTable();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void createTable(Connection conn) {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            String sqlStr = String.format("CREATE TABLE IF NOT EXISTS %s" +
                            " ( %s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "%s varchar(64)," +
                            " %s varchar(64)," +
                            " %s varchar(64));",
                    TABLE_NAME, COLUMN_ID, COLUMN_LOGIN,COLUMN_PASS,COLUMN_NICK);
            statement.execute(sqlStr);
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    private static void prepareData() throws SQLException{
        connection.setAutoCommit(false);
        for(int i = 1; i < 4; i++){
            String str = "INSERT INTO users(login, password, nickname) VALUES (" + " 'login" + i + "', 'pass" + i + "', 'client" + i +"'" + ");";
            statement.executeUpdate(str);
        }
        connection.setAutoCommit(true);
    }
    private static void printOutTable() throws SQLException {
        System.out.println("printOutTable");
        Statement stmt = null;
        stmt = connection.createStatement();
        String sql;
        sql = "SELECT login, password, nickname FROM users";
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()) {
            //Retrieve by column name
            String name = rs.getString(COLUMN_LOGIN);
            String pass = rs.getString(COLUMN_PASS);
            String nick = rs.getString(COLUMN_NICK);
            //Display values
            System.out.print("Name: " + name);
            System.out.print(" ,Pass: " + pass);
            System.out.println(" ,Nick: " + nick);
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void dropTable(Connection conn) {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            String sqlStr = String.format("DROP TABLE IF EXISTS %s;", TABLE_NAME);
            statement.execute(sqlStr);
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkUser(String username, String pass) {
        try {
            ps = connection.prepareStatement("SELECT COUNT(username) AS Count FROM users WHERE username = ? AND password = ?;");
            ps.setString(1, username);
            ps.setString(2, pass);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                if (resultSet.getInt("Count") > 0)
                    return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getNickByCredentials(String login, String pass){
        String nick = null;
        try {
            ps = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?;");
            ps.setString(1, login);
            ps.setString(2, pass);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                nick = resultSet.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nick;
    }
}
