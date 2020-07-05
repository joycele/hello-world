import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


/**
 * This IndexServlet is declared in the web annotation below, 
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "IndexServlet", urlPatterns = "/api/index")
public class IndexServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Create a dataSource which registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    /**
     * handles GET requests to add and show the item list information
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
            Statement statement = dbcon.createStatement();
            
            String query = "select genres.name from genres order by name asc;";
            ResultSet rs = statement.executeQuery(query);
            JsonArray jsonArray = new JsonArray();
            
            while (rs.next()) {
            	String genreName = rs.getString("name");
            	JsonObject jsonObject = new JsonObject();
            	jsonObject.addProperty("genreName", genreName);
            	jsonArray.add(jsonObject);
            }
            
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            rs.close();
            statement.close();
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