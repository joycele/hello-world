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
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


// Declaring a WebServlet called SingleMovieServlet, which maps to url "/api/single-movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;

	// Create a dataSource which registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response) 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json"); // Response mime type
		// Set standard HTTP/1.1 no-cache headers.
		response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
		HttpSession session = request.getSession();
		User customer = (User) session.getAttribute("user");
		String item = request.getParameter("item");
		System.out.println(item);
		if (item != null) {
			customer.addToCart(item);
		}
		
		// Retrieve parameter id from url request.
		String id = request.getParameter("id");

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
			// Construct a query with parameter represented by "?"
			String query = "select movies.title, movies.year, movies.director,"
    				+ "ratings.rating, movies.id from movies, ratings "
    				+ "where movies.id = ratings.movieId "
    				+ "and movies.id = ?;";
			
			// Declare our statement
			PreparedStatement statement = dbcon.prepareStatement(query);

			// Set the parameter represented by "?" in the query to the id we get from
			// url, 1 indicates the first "?" in the query
			statement.setString(1, id);

			// Perform the query
			ResultSet rs = statement.executeQuery();

			JsonArray jsonArray = new JsonArray();

			// Iterate through each row of rs
			while (rs.next()) {

				String movieId = rs.getString("id");
				String movieTitle = rs.getString("title");
				String movieYear = rs.getString("year");
				String movieDirector = rs.getString("director");
				String movieRating = rs.getString("rating");

				// Create a JsonObject based on the data we retrieve from rs

				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("movie_id", movieId);
				jsonObject.addProperty("movie_title", movieTitle);
				jsonObject.addProperty("movie_year", movieYear);
				jsonObject.addProperty("movie_director", movieDirector);
				jsonObject.addProperty("movie_rating", movieRating);
				
				String genreQuery = "select genres.name from genres, genres_in_movies "
    					+ "where genres.id = genres_in_movies.genreId and movieId = ?;";
				PreparedStatement statement2 = dbcon.prepareStatement(genreQuery);
				statement2.setString(1, movieId);
                ResultSet genreSet = statement2.executeQuery();
                
                StringBuilder genreStr = new StringBuilder();
                while(genreSet.next()) {
                	genreStr.append("<a href=\"single-genre.html?name=" 
                        	+ genreSet.getString("name") + "\">" + genreSet.getString("name")
                        	+ "</a>");
                	genreStr.append("<br>");
                } 
                jsonObject.addProperty("movie_Genre", genreStr.toString());
				
				String starQuery = "select stars.name, stars.id from stars, stars_in_movies "
    					+ "where stars.id = stars_in_movies.starId and movieId = ?;";
				PreparedStatement statement3 = dbcon.prepareStatement(starQuery);
				statement3.setString(1, movieId);
                ResultSet starSet = statement3.executeQuery();
                
                StringBuilder starStr = new StringBuilder();
                while(starSet.next()) {
                	starStr.append("<a href=\"single-star.html?id=" 
                	+ starSet.getString("id") + "\">" + starSet.getString("name")
                	+ "</a>");
                	starStr.append("<br>");
                }
                jsonObject.addProperty("movie_Star", starStr.toString());
				jsonArray.add(jsonObject);
				
                genreSet.close();
                starSet.close();
                statement2.close();
                statement3.close();
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
