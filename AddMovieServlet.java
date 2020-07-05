
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;

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
 * Servlet implementation class AddMovieServlet
 */
@WebServlet(name = "AddMovieServlet", urlPatterns = "/api/add-movie-dashboard")
public class AddMovieServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
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
        
        String title = request.getParameter("title");
        String director = request.getParameter("director");
        String year = request.getParameter("year");
        String genre = request.getParameter("genre");
        String star = request.getParameter("star");
        
        System.out.println(title + director + year + genre + star);
        
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
        	String checkQuery = "select COUNT(*) as count from movies where movies.title=? "
        			+ "and movies.director=? and movies.year=?;";
        	PreparedStatement check = dbcon.prepareStatement(checkQuery);
        	check.setString(1, title);
        	check.setString(2, director);
        	check.setString(3, year);
        	ResultSet r = check.executeQuery();
        	r.next();
        	String movie_exists = r.getString("count");
        	if (movie_exists.equals("0")) { // valid movie, call stored procedure
        		
        		String call = "CALL add_movie(?, ?, ?, ?, ?);";
        		CallableStatement caller = dbcon.prepareCall(call);
        		caller.setString(1, title);
        		caller.setString(2, director);
        		caller.setString(3, year);
        		caller.setString(4, genre);
        		caller.setString(5, star);
        		caller.execute();
        		
        		JsonObject jsonObject = new JsonObject();
        		jsonObject.addProperty("success", "Success: " + title + " added to database "
        				+ "(Director=" + director + " Year=" + year + ") with genre=" + genre
        				+ " and star=" + star);
        		
        		out.write(jsonObject.toString());
        		caller.close();	
        		
        	} else {  // movie already exists; do not call procedure
        		JsonObject jsonObject = new JsonObject();
        		jsonObject.addProperty("success", "0");
        		out.write(jsonObject.toString());
        	}
        	dbcon.close();
        	check.close();
        	r.close();
        	
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
