

import java.io.IOException;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Servlet implementation class ConfirmationServlet
 */
@WebServlet(name="ConfirmationServlet", urlPatterns="/api/confirmation")
public class ConfirmationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// Create a dataSource which registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		response.setContentType("application/json"); // Response mime type
		// Set standard HTTP/1.1 no-cache headers.
		response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
    	HttpSession session = request.getSession();
		User customer = (User) session.getAttribute("user");
		HashMap<String, Integer> cart = customer.getShoppingCart();
		
		String first = request.getParameter("firstname");
		String last = request.getParameter("lastname");
		String card = request.getParameter("card");
		String exp = request.getParameter("exp");
		
		// Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		if (cart.size() == 0) {
			JsonArray jsonArray = new JsonArray();
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("statement", "Shopping cart is empty");
			jsonObject.addProperty("backToPage", "<a href='\\fabflix\\index.html'> << Continue Browsing Movies <a/>");
			jsonArray.add(jsonObject);
			out.write(jsonArray.toString());
			response.setStatus(200);
			out.close();
			return;
		}
		
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
			String query = "select count(*) as valid from creditcards as c "
					+ "where c.id = ? and c.firstName = ? and c.lastName = ? "
					+ "and c.expiration = ?";
			
			PreparedStatement statement = dbcon.prepareStatement(query);
			statement.setString(1, card);
			statement.setString(2, first);
			statement.setString(3, last);
			statement.setString(4, exp);
			
			ResultSet rs = statement.executeQuery();
			JsonArray jsonArray = new JsonArray();
			
			while(rs.next()) {
				String valid = rs.getString("valid");
				JsonObject jsonObject = new JsonObject();
		
				if (valid.equals("1")) {
					jsonObject.addProperty("statement", "Confirmation successful, purchase complete!");
					jsonObject.addProperty("backToPage", "<a href='\\fabflix\\index.html'> << Continue Browsing Movies <a/>");
					jsonArray.add(jsonObject);
					
					for (HashMap.Entry<String, Integer> entry : cart.entrySet()) {
						for (int i = 0; i < entry.getValue(); i++) {
							String email = customer.getUsername();
							String password = customer.getPassword();
							String getCustId = "select customers.id from customers where "
									+ "customers.email =? and customers.password =? limit 1;";
							PreparedStatement s = dbcon.prepareStatement(getCustId);
							s.setString(1, email);
							s.setString(2, password);
							ResultSet r = s.executeQuery();
							r.next();
							String customerId = r.getString("id");
							s.close();
							r.close();
							DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
							LocalDate localDate = LocalDate.now();
							String date = dtf.format(localDate);
							String insert = "INSERT INTO sales (customerId, movieId, saleDate) " +
									"VALUES (?, ?, ?);";
							PreparedStatement update = dbcon.prepareStatement(insert);
							update.setString(1, customerId);
							update.setString(2, entry.getKey());
							update.setString(3, date);
							update.executeUpdate();
							update.close();
						}
		            	String movie_q = "select movies.id, movies.title, movies.year,"
		            			+ "movies.director from movies where movies.id=?;";
		            	
		            	PreparedStatement getmovieinfo = dbcon.prepareStatement(movie_q);
		            	getmovieinfo.setString(1, entry.getKey());
		            	ResultSet movie_rs = getmovieinfo.executeQuery();
		            	
		            	movie_rs.next();
		            	String movieTitle = movie_rs.getString("title");
		        		String movieYear = movie_rs.getString("year");
		        		String movieDirector = movie_rs.getString("director");
		        		String movieId = movie_rs.getString("id");
		  
		        		jsonObject.addProperty("movieId", movieId);
		                jsonObject.addProperty("movieTitle", movieTitle);
		                jsonObject.addProperty("movieYear", movieYear);
		                jsonObject.addProperty("movieDirector", movieDirector);
		                jsonObject.addProperty("count", cart.get(entry.getKey()));
		                
		                jsonArray.add(jsonObject); 
					}
					
				} else {
					jsonObject.addProperty("statement", "Invalid card information, purchase has been declined.");
					jsonObject.addProperty("backToPage", "<a href='\\fabflix\\customer-info.html'> << Back to Checkout <a/>");
					jsonArray.add(jsonObject);
				}
	
			}
			
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

			rs.close();
			statement.close();
			dbcon.close();
		
		} catch (Exception e) {
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("errorMessage", e.getMessage());
			out.write(jsonObject.toString());

			// set response status to 500 (Internal Server Error)
			response.setStatus(500);
		}
		out.close();
	}
}
