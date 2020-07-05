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
import java.util.ArrayList;
import java.sql.PreparedStatement;
/**
 * Servlet implementation class AndroidMovieServlet
 */

@WebServlet(name = "AndroidMovieServlet", urlPatterns = "/api/android-movies")
public class AndroidMovieServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    // Create a dataSource which registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	doPost(request, response);
    }
       
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
        response.setContentType("application/json"); // Response mime type
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();
        
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
        	System.out.println("android search: " + request.getParameter("search"));
        	String[] search = request.getParameter("search").split(" ");
        	StringBuilder sb = new StringBuilder();
        	
        	Integer offset = Integer.parseInt(request.getParameter("offset"));
        	String movieQuery = "SELECT id, title, year, director FROM movies WHERE "
        			+ "MATCH (title) AGAINST (? in boolean mode) LIMIT 10 OFFSET ?;";
        	
        	String countQuery = "select count(*) as count FROM movies WHERE MATCH (title) "
    				+ "AGAINST (? in boolean mode);";
        	
        	PreparedStatement cps = dbcon.prepareStatement(countQuery);
        	PreparedStatement ps = dbcon.prepareStatement(movieQuery);
        	ps.setInt(2, offset);
        	
        	if (search.length == 1) {
        		ps.setString(1, "+" + search[0] + "*");
        		cps.setString(1, "+" + search[0] + "*");
        	} 
        	else {
        		for (int i = 0; i < search.length; i++) {
        			sb.append("+" + search[i] + "* ");
        		}
        		ps.setString(1, sb.toString());
        		cps.setString(1, sb.toString());
        	}
        	System.out.println(cps.toString());
        	System.out.println(ps.toString());
        	ResultSet crs = cps.executeQuery();
    		ResultSet rs = ps.executeQuery();
    			
    		JsonArray jsonArray = new JsonArray();
    			
    		crs.next();
    		JsonObject jsonCountObject = new JsonObject();
    		jsonCountObject.addProperty("count", crs.getString("count"));
    		jsonArray.add(jsonCountObject);
    			
    		while(rs.next())
    		{
    			String movieTitle = rs.getString("title");
        		String movieYear = rs.getString("year");
        		String movieDirector = rs.getString("director");
        		String movieId = rs.getString("id");
        			
        		JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movieTitle", movieTitle);
                jsonObject.addProperty("movieYear", movieYear);
                jsonObject.addProperty("movieDirector", movieDirector);
                    
                String genreQuery = "select genres.name from genres, genres_in_movies "
        					+ "where genres.id = genres_in_movies.genreId and "
        					+ "movieId = ?;";
                PreparedStatement statement2 = dbcon.prepareStatement(genreQuery);
                statement2.setString(1, movieId);
                ResultSet genreSet = statement2.executeQuery();
                    
                StringBuilder genreStr = new StringBuilder();
                while(genreSet.next()) {
                    genreStr.append(genreSet.getString("name") + ", ");
                }
                jsonObject.addProperty("movieGenre", genreStr.toString().substring(0, genreStr.length()-2));
                    
                String starQuery = "select stars.name, stars.id from stars, stars_in_movies "
        					+ "where stars.id = stars_in_movies.starId and "
        					+ "movieId = ?;";
                PreparedStatement statement3 = dbcon.prepareStatement(starQuery);
                statement3.setString(1, movieId);
                ResultSet starSet = statement3.executeQuery();
                    
                StringBuilder starStr = new StringBuilder();
                while(starSet.next()) {
                    starStr.append(starSet.getString("name") + ", ");
                }
                jsonObject.addProperty("movieStar", starStr.toString().substring(0, starStr.length()-2));
                jsonArray.add(jsonObject);
                    
                genreSet.close();
                starSet.close();
                statement2.close();
                statement3.close();
                       
    		}//end while

            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);
            System.out.println(jsonArray.toString());
    		rs.close();
            ps.close();
            dbcon.close();
        	
        } catch (Exception e) {
        	// write error message JSON object to output
        	JsonObject jsonObject = new JsonObject();
        	jsonObject.addProperty("errorMessage", e.getMessage());
        	out.write(jsonObject.toString());
   			// set response status to 500 (Internal Server Error)
   			response.setStatus(500);
        	e.printStackTrace();
        }
        out.close();
	}
}
