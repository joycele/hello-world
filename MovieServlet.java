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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.sql.PreparedStatement;

 
// Declaring a WebServlet called MovieServlet, which maps to url "/api/movies"
@WebServlet(name = "MovieServlet", urlPatterns = "/api/movies")
public class MovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L; 
    
    // Create a dataSource which registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	long startTimeTS = System.nanoTime();
    	System.out.println("TS Start time: " + startTimeTS);
        response.setContentType("application/json"); // Response mime type
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");

        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();
        
        String contextPath = getServletContext().getRealPath("/");
        String xmlFilePath = contextPath + "/log.txt";
        System.out.println(xmlFilePath);
        File file = new File(xmlFilePath);
        
        long totalTJtime = 0;
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
            
            
        	StringBuilder movieQuery = new StringBuilder();
        	/*movieQuery.append("Select movies.id, movies.title, movies.director, "
			+ "movies.year, ratings.rating from movies, ratings where "
			+ "movies.id = ratings.movieId");
        	*/
        	movieQuery.append("SELECT movies.id, movies.title, movies.director, "
        			+ "movies.year, ratings.rating from movies, ratings " 
        			+ "WHERE movies.id = ratings.movieId and MATCH (movies.title) AGAINST "
        			+ "(? in boolean mode);");
        	
        	PreparedStatement ps = dbcon.prepareStatement(movieQuery.toString());
          
        	String title = "";
            String year = "";
            String director = "";
            String star = "";
            ArrayList<String> params = new ArrayList<String>();
            if(request.getParameter("title") != null && request.getParameter("title").length() > 0)
            {
            	title = request.getParameter("title");
            	//movieQuery.append(" and movies.title like ?");
            	//params.add(title);
            	
            	String[] queryArr = title.split(" ");
            	if(queryArr.length == 1)
                {
                	ps.setString(1, "+" + title + "*");	
                }
                else
                {
                	StringBuilder sb = new StringBuilder();
                	for(int i = 0; i < queryArr.length; ++i)
                	{
                		sb.append("+" + queryArr[i] + "* ");
                	}
                	ps.setString(1, sb.toString());
                }
            	System.out.println(ps);
            	System.out.println("ITS HERE");
            	String cPath = getServletContext().getRealPath("/");
                String xPath = cPath + "/log.txt";
                System.out.println(xPath);
                System.out.println("ITS NOT HERE I GUESS");
            }
            if(request.getParameter("year") != null && request.getParameter("year").length() > 0)
            {
            	year = request.getParameter("year");
            	movieQuery.append(" and movies.year = ?");
            	params.add(year);
            }
            if(request.getParameter("director") != null && request.getParameter("director").length() > 0)
            {
            	director = request.getParameter("director");
            	movieQuery.append(" and movies.director like ?");
            	params.add(director);
            }
            
            if(request.getParameter("stars") != null && request.getParameter("stars").length() > 0)
            {
            	star = request.getParameter("stars");
            	movieQuery.append(" and movies.id in (select movies.id "
            			+ "from movies, stars, stars_in_movies where "
            			+ "movies.id = stars_in_movies.movieId and stars.id = "
            			+ "stars_in_movies.starId and stars.name like ?)");
            	params.add(star);
            }
            
            //movieQuery.append(" limit 50");
            movieQuery.append(";");
            
            
            //PreparedStatement ps = dbcon.prepareStatement(movieQuery.toString());

            /*for(int i = 0; i < params.size(); ++i)
            {
            	ps.setString(i+1, "%" + params.get(i) + "%");
            }*/
            
            
            System.out.println(ps.toString());
            long TJ1 = System.nanoTime();
			ResultSet rs = ps.executeQuery();
			long TJ2 = System.nanoTime();
			totalTJtime += (TJ2-TJ1);
			JsonArray jsonArray = new JsonArray();
			//ArrayList<String> checkDupe = new ArrayList<String>();
			while(rs.next())
			{
				
				String movieTitle = rs.getString("title");
    			String movieYear = rs.getString("year");
    			String movieDirector = rs.getString("director");
    			String movieRating = rs.getString("rating");
    			String movieId = rs.getString("id");

				JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movieTitle", movieTitle);
                jsonObject.addProperty("movieYear", movieYear);
                jsonObject.addProperty("movieDirector", movieDirector);
                jsonObject.addProperty("movieRating", movieRating);
                jsonObject.addProperty("movieId", movieId);
                
                String genreQuery = "select genres.name from genres, genres_in_movies "
    					+ "where genres.id = genres_in_movies.genreId and "
    					+ "movieId = ?;";
                PreparedStatement statement2 = dbcon.prepareStatement(genreQuery);
                statement2.setString(1, movieId);
                long TJ3 = System.nanoTime();
                ResultSet genreSet = statement2.executeQuery();
                long TJ4 = System.nanoTime();
                totalTJtime += (TJ4-TJ3);
                StringBuilder genreStr = new StringBuilder();
                while(genreSet.next()) {
                	genreStr.append("<a href=\"single-genre.html?name=" 
                        	+ genreSet.getString("name") + "\">" + genreSet.getString("name")
                        	+ "</a>");
                	genreStr.append("<br>");
                }
                jsonObject.addProperty("movieGenre", genreStr.toString());
                
                String starQuery = "select stars.name, stars.id from stars, stars_in_movies "
    					+ "where stars.id = stars_in_movies.starId and "
    					+ "movieId = ?;";
                PreparedStatement statement3 = dbcon.prepareStatement(starQuery);
                statement3.setString(1, movieId);
                long TJ5 = System.nanoTime();
                ResultSet starSet = statement3.executeQuery();
                long TJ6 = System.nanoTime();
                totalTJtime += (TJ6-TJ5);
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
				
			}//end while
			
			// write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);
            
			rs.close();
            ps.close();
            dbcon.close();
        }//end try
        catch(Exception e)
        {
        	// write error message JSON object to output
        	JsonObject jsonObject = new JsonObject();
        	jsonObject.addProperty("errorMessage", e.getMessage());
        	out.write(jsonObject.toString());
   			// set response status to 500 (Internal Server Error)
   			response.setStatus(500);
        	e.printStackTrace();
        }
        System.out.println("Total TJ time: " + totalTJtime);
        long endTimeTS = System.nanoTime();
        System.out.println("TS End time: " + endTimeTS);
        long elapsedTimeTS = endTimeTS - startTimeTS;
        System.out.println("TS Elapsed time: " + elapsedTimeTS);
        
        
        //file.createNewFile();
        PrintWriter pw = null; 
        if(file.exists() && !file.isDirectory()) {
        	pw = new PrintWriter(new FileOutputStream(new File(xmlFilePath), true));
        }
        else {
        	pw = new PrintWriter(xmlFilePath);
        }
        pw.println(totalTJtime + " " + elapsedTimeTS);
        pw.close();
        out.close();

        //FileOutputStream fout = new FileOutputStream(file);
        //fout.write((totalTJtime + " " + elapsedTimeTS + "\n").getBytes());
        //fout.close();
    }
}