import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;



/**
 * Servlet implementation class ShoppingCartServlet
 */
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    // Create a dataSource which is registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;
    
    /**
     * handles GET requests to add and show the item list information
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        response.setContentType("application/json"); // Response mime type
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    	
    	HttpSession session = request.getSession();
		User customer = (User) session.getAttribute("user");
		HashMap<String, Integer> cart = customer.getShoppingCart();
		String todelete = request.getParameter("delete");
		System.out.println(cart);
		
		if (cart.containsKey(todelete)) {
			cart.remove(todelete);
		}
		
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
        	JsonArray jsonArray = new JsonArray();
            
            for (HashMap.Entry<String, Integer> entry : cart.entrySet()) {
            	String id = entry.getKey();
            	Integer num = entry.getValue();
            	String query = "select movies.id, movies.title, movies.year,"
            			+ "movies.director from movies where movies.id=?;";
            	PreparedStatement statement = dbcon.prepareStatement(query);
            	statement.setString(1, id);
            	ResultSet rs = statement.executeQuery();
            	rs.next();
            	
                String movieTitle = rs.getString("title");
        		String movieYear = rs.getString("year");
        		String movieDirector = rs.getString("director");
        		String movieId = rs.getString("id");
        			
        		JsonObject jsonObject = new JsonObject();
        		jsonObject.addProperty("movieId", movieId);
                jsonObject.addProperty("movieTitle", movieTitle);
                jsonObject.addProperty("movieYear", movieYear);
                jsonObject.addProperty("movieDirector", movieDirector);
                jsonObject.addProperty("count", num.toString());
                    
                jsonArray.add(jsonObject);
                rs.close();
                statement.close();
            	
            }
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);
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

