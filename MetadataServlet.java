
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Servlet implementation class MetadataServlet
 */
@WebServlet(name = "MetadataServlet", urlPatterns = "/api/metadata-dashboard")
public class MetadataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    // Create a dataSource which is registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;
	

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
    	response.setContentType("application/json"); // Response mime type
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();
        
        try {
    		// Get a connection from dataSource
            //Connection dbcon = dataSource.getConnection();
        	// the following few lines are for connection pooling
            // Obtain our environment naming context

            Context initCtx = new InitialContext();

            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            if (envCtx == null)
                out.println("envCtx is NULL");

            // Look up our data source
            DataSource ds = (DataSource) envCtx.lookup("jdbc/TestDB");

            // the following commented lines are direct connections without pooling
            //Class.forName("org.gjt.mm.mysql.Driver");
            //Class.forName("com.mysql.jdbc.Driver").newInstance();
            //Connection dbcon = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

            if (ds == null)
                out.println("ds is null.");

            Connection dbcon = ds.getConnection();
            if (dbcon == null)
                out.println("dbcon is null.");
        	// Declare our statement
            Statement getTableStatement = dbcon.createStatement();
            
            String getTablesQuery = "show tables from moviedb";
            ResultSet tables = getTableStatement.executeQuery(getTablesQuery);
            JsonArray mainArray = new JsonArray();
            
            while (tables.next()) {
            	String tableName = tables.getString("Tables_in_moviedb");
            	
            	Statement getColumnStatement = dbcon.createStatement();
            	String getColumnsQuery = "show columns from " + tableName + ";";
            	ResultSet columns = getColumnStatement.executeQuery(getColumnsQuery);
            	JsonArray columnArray = new JsonArray();
            	JsonObject tableObject = new JsonObject();
            	
            	while (columns.next()) {
            		
            		JsonObject jsonColumns = new JsonObject();
            		
            		String field = columns.getString("Field");
            		String type = columns.getString("Type");
            		String isNull = columns.getString("Null");
            		String key = columns.getString("Key");
            		String defaultValue = columns.getString("Default");
            		String extra = columns.getString("Extra");
            		
            		jsonColumns.addProperty("Field", field);
            		jsonColumns.addProperty("Type", type);
            		jsonColumns.addProperty("isNull", isNull);
            		jsonColumns.addProperty("Key", key);
            		jsonColumns.addProperty("Default", defaultValue);
            		jsonColumns.addProperty("Extra", extra);
            		
            		columnArray.add(jsonColumns);
            	}
            	tableObject.add(tableName, columnArray);
            	mainArray.add(tableObject);
            	getColumnStatement.close();
            	columns.close();
            }
            System.out.println(mainArray.toString());
            // write JSON string to output
            out.write(mainArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);
            
            tables.close();
            getTableStatement.close();
            dbcon.close();
        	
        } catch (Exception e) {
        	
        	// write error message JSON object to output
        	JsonObject jsonObject = new JsonObject();
        	jsonObject.addProperty("errorMessage", e.getMessage());
        	out.write(jsonObject.toString());

        	// set response status to 500 (Internal Server Error)
        	response.setStatus(500);
        }
        out.close();
	}


}
