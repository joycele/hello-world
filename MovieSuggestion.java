

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

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
 * Servlet implementation class MovieSuggestion
 */
@WebServlet("/MovieSuggestion")
public class MovieSuggestion extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@Resource(name = "jdbc/moviedb")
    private DataSource dataSource;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MovieSuggestion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			
        	// setup the response json arrray
        	JsonArray jsonArray = new JsonArray();
        	// get the query string from parameter
        	String query = request.getParameter("query");
        	
    
        	// return the empty json array if query is null or empty
        	if (query == null || query.trim().isEmpty()) {
        		response.getWriter().write(jsonArray.toString());
        		return;
        	}	
        	String[] queryArr = query.split(" ");
        	StringBuilder sb = new StringBuilder();
        	
        	String q = "SELECT id, title from movies "
        			+ "WHERE MATCH (title) AGAINST (? in boolean mode) limit 10;";
            PreparedStatement ps = dbcon.prepareStatement(q);
            if(queryArr.length == 1)
            {
            	ps.setString(1, "+" + query + "*");	
            }
            else
            {
            	for(int i = 0; i < queryArr.length; ++i)
            	{
            		sb.append("+" + queryArr[i] + "* ");
            	}
            	ps.setString(1, sb.toString());
            }
            System.out.println(ps);
            ResultSet rs = ps.executeQuery();
        	ArrayList<String> checkDupe = new ArrayList<String>();
            while(rs.next())
			{
            	
            	String title = rs.getString("title");
            	String id = rs.getString("id");
            	if(!checkDupe.contains(title))
            	{
                	jsonArray.add(generateJsonObject(id, title));
                	checkDupe.add(title);
                	System.out.println(title);
            	}
			}
			rs.close();
			ps.close();
			dbcon.close();

			// search on superheroes and add the results to JSON Array
			// this example only does a substring match
			// TODO: in project 4, you should do full text search with MySQL to find the matches on movies and stars
			
			/*for (Integer id : superHeroMap.keySet()) {
				String heroName = superHeroMap.get(id);
				if (heroName.toLowerCase().contains(query.toLowerCase())) {
					jsonArray.add(generateJsonObject(id, heroName));
				}
			}*/
			
			response.getWriter().write(jsonArray.toString());
			return;
		} catch (Exception e) {
			System.out.println(e);
			response.sendError(500, e.getMessage());
		}
		
	}
	
	private static JsonObject generateJsonObject(String movieID, String movieName) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", movieName);
		
		JsonObject additionalDataJsonObject = new JsonObject();
		additionalDataJsonObject.addProperty("movieID", movieID);
		
		jsonObject.add("data", additionalDataJsonObject);
		return jsonObject;
	}
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
