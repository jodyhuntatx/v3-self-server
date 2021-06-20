import java.sql.*;

public class DBLayer {
   static String DB_URL;
   static String DB_USER;
   static String DB_PASSWORD;

   public static ResultSet query (String querySql) {
      try (
	Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        Statement stmt = conn.createStatement();
      ) {
         return stmt.executeQuery(querySql); 
      } catch (SQLException e) {
         e.printStackTrace();
      } 
      return null;
   }

   public static void update (String updateSql) {
      try (
	Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        Statement stmt = conn.createStatement();
      ) {
         stmt.executeUpdate(updateSql); 
      } catch (SQLException e) {
         e.printStackTrace();
      } 
   }
}
