

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

import com.google.gson.JsonObject;

/**
 * Servlet implementation class AddStarServlet
 */
@WebServlet(name = "AddStarServlet", urlPatterns = "/api/add-star-dashboard")
public class AddStarServlet extends HttpServlet {
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
        
		String star = request.getParameter("name");
		String birth = request.getParameter("birth");
		
		try {
   
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
			Statement statement = dbcon.createStatement();
            String query = "select max(id) from stars";
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            String max_id = rs.getString("max(id)");
            Integer id_int = Integer.parseInt(max_id.substring(2)) + 1;
            String new_id = max_id.substring(0,2) + id_int.toString();
            rs.close();
            statement.close();
            
            String insert;
            if (birth.isEmpty()) {
            	insert = "INSERT INTO stars (id, name) VALUES (?, ?)";
            } else {
            	insert = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            }
            PreparedStatement update = dbcon.prepareStatement(insert);
            update.setString(1, new_id);
            update.setString(2, star);
            if (!birth.isEmpty()) {
            	update.setString(3, birth);
            } 
            update.executeUpdate();
            update.close();
            dbcon.close();
            
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("confirm", "Success: " + star + " has been inserted into stars table");
			out.write(jsonObject.toString());
            
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


