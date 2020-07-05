import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
 * Servlet implementation class SingleGenreServlet
 */
@WebServlet(name="SingleGenreServlet", urlPatterns="/api/single-genre")
public class SingleGenreServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// Create a dataSource which registered in web.xml
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
		
		// Retrieve parameter id from url request.
		String name = request.getParameter("name");

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
			String query = "select m.title, m.year, m.director, r.rating, m.id "
			    	+ "from movies as m, genres as g, genres_in_movies as gim, "
			    	+ "ratings as r where m.id = gim.movieId and r.movieId = m.id "
			    	+ "and gim.genreId = g.id and g.name = ? limit 25;";
						
			PreparedStatement statement = dbcon.prepareStatement(query);
			statement.setString(1, name);
			ResultSet rs = statement.executeQuery();

			JsonArray jsonArray = new JsonArray();
			
			while (rs.next()) {
				
				String movieId = rs.getString("id");
				String movieTitle = rs.getString("title");
				String movieYear = rs.getString("year");
				String movieDirector = rs.getString("director");
				String movieRating = rs.getString("rating");
				
				// Create a JsonObject based on the data we retrieve from rs
				
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("movieId", movieId);
				jsonObject.addProperty("movieTitle", movieTitle);
				jsonObject.addProperty("movieYear", movieYear);
				jsonObject.addProperty("movieDirector", movieDirector);
				jsonObject.addProperty("movieRating", movieRating);
				
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
                jsonObject.addProperty("movieGenre", genreStr.toString());
				
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
                jsonObject.addProperty("movieStar", starStr.toString());
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
			
		}
	}
}