import org.xml.sax.*;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.xml.parsers.*;

import java.io.IOException;
import org.xml.sax.SAXException;

public class parseXML {
	static HashMap<String, Movie> movies;
	static HashMap<String, String> stagenames;
	Document maindom;
	Document actordom;
	Document castsdom;
	
	public parseXML() {
		movies = new HashMap<>();
		stagenames = new HashMap<>();
	}
	
	private void parseXmlFile()
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilderFactory actordbf = DocumentBuilderFactory.newInstance();
		DocumentBuilderFactory castsdbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			DocumentBuilder actordb = actordbf.newDocumentBuilder();
			DocumentBuilder castsdb = castsdbf.newDocumentBuilder();
			
			maindom = db.parse("mains243.xml");
			actordom = actordb.parse("actors63.xml");
			castsdom = castsdb.parse("casts124.xml");
			
		} catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch(SAXException se) {
			se.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
	private void parseDocument() {
		Element docEle = maindom.getDocumentElement();
		
		NodeList filmList = docEle.getElementsByTagName("film");
		if(filmList != null && filmList.getLength() > 0) 
		{
			for(int i = 0; i < filmList.getLength(); ++i)
			{
				Element e1 = (Element) filmList.item(i);
				
				Movie m = getMovie(e1);
				if(m != null)
					movies.put(m.getId(), m);
				
			}
		}
		
		//Begin parsing casts xml
		Element castsdocEle = castsdom.getDocumentElement();
		NodeList castList = castsdocEle.getElementsByTagName("m");
		if(castList != null && castList.getLength() > 0)
		{
			for(int i = 0; i < castList.getLength(); ++i)
			{
				Element c1 = (Element) castList.item(i);
				addStagename(c1);
			}
		}
		
		//Begin parsing actors xml
		Element actorsdocEle = actordom.getDocumentElement();
		NodeList actorList = actorsdocEle.getElementsByTagName("actor");
		if(actorList != null && actorList.getLength() > 0)
		{
			for(int i = 0; i < actorList.getLength(); ++i)
			{
				Element a1 = (Element) actorList.item(i);
				updateActor(a1);
			}
		}
		
	}
	
	private Movie getMovie(Element movieE1) {
		String id = getTextValue(movieE1, "fid");
		
		if(movies.containsKey(id))
		{
			System.out.println(id + " already exists");
			return null;
		}
		
		String title = getTextValue(movieE1, "t");
		if(title == null)
		{
			System.out.println("null title: " + id);
			return null;
		}
		String year = getTextValue(movieE1, "year");
		if(year == null)
		{
			return null;
		}
		List<String> directors = new ArrayList<String>();
		List<String> genres = new ArrayList<String>();
		
		NodeList dirList = movieE1.getElementsByTagName("dir");
		if(dirList != null && dirList.getLength() > 0)
		{
			for(int i = 0; i < dirList.getLength(); ++i)
			{
				Element d1 = (Element) dirList.item(i);
				if(getTextValue(d1, "dirn") == null)
				{
					return null;
				}
				directors.add(getTextValue(d1, "dirn"));
			}
		}
		
		NodeList genreList = movieE1.getElementsByTagName("cats");
		if(genreList != null && genreList.getLength() > 0)
		{
			for(int i = 0; i < genreList.getLength(); ++i)
			{
				Element g1 = (Element) genreList.item(i);
				genres.add(getTextValue(g1, "cat"));
			}
		}
		
		Movie m = new Movie();
	
		m.setId(id);
		m.setTitle(title);
		m.setYear(year);
		
		for(int i = 0; i < directors.size(); ++i)
		{
			m.addDirector(directors.get(i));
		}
		
		for(int i = 0; i < genres.size(); ++i)
		{
			m.addGenre(genres.get(i));
		}
		
		return m;
	}
	
	private void addStagename(Element c1) {
		String id = getTextValue(c1, "f");
		String stagename = getTextValue(c1, "a");
		if(movies.containsKey(id))
		{
			movies.get(id).addActor(stagename);
			stagenames.put(stagename, id);
		}
	}
	
	private void updateActor(Element a1) {
		String stagename = getTextValue(a1, "stagename");
		String firstName = getTextValue(a1, "firstname");
		String lastName = getTextValue(a1, "familyname");
		String birthYear = getTextValue(a1, "dob");
		if(stagenames.containsKey(stagename))
		{
			String movieID = stagenames.get(stagename);
			movies.get(movieID).updateActor(stagename, firstName, lastName, birthYear);
			
		}
		
		
	}
	
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList n1 = ele.getElementsByTagName(tagName);
		if(n1 != null && n1.getLength() > 0)
		{
			Element e1 = (Element) n1.item(0);
			if(e1.getFirstChild() != null)
				textVal = e1.getFirstChild().getNodeValue();
		}
		return textVal;
	}
	
	private void insertIntoDB()
	{
		String loginUser = "root";
		String loginPassword = "otxxrsh0uig";
		String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			Connection dbcon = DriverManager.getConnection(loginUrl, loginUser, loginPassword);
			HashSet<String> starset = new HashSet<String>();
			HashSet<String> genreset = new HashSet<String>();
			
			final int batchSize = 500;
			int count = 0;
			int gcount = 0;
			int acount = 0;
			
            Statement statement = dbcon.createStatement();
            String query = "select max(id) from movies";
            ResultSet rs = statement.executeQuery(query);
            rs.next();
            String max_id = rs.getString("max(id)");
            Integer id_int = Integer.parseInt(max_id.substring(2)) + 1;
            String new_id = max_id.substring(0,2) + id_int.toString();
            rs.close();
            statement.close();
            
            Statement gstate = dbcon.createStatement();
            String gquery = "select max(id) from genres";
            ResultSet grs = gstate.executeQuery(gquery);
            grs.next();
            String gmax_id = grs.getString("max(id)");
            Integer gid_int = Integer.parseInt(gmax_id) + 1;
            grs.close();
            gstate.close();
            
            Statement getnewa = dbcon.createStatement();
            String amaxquery = "select max(id) from stars";
            ResultSet ars = getnewa.executeQuery(amaxquery);
            ars.next();
            String amax_id = ars.getString("max(id)");
            Integer aid_int = Integer.parseInt(amax_id.substring(2)) + 1;
            String anew_id = max_id.substring(0,2) + aid_int.toString();
            ars.close();
            getnewa.close();
			
			//Query Statements
            String movieQuery = "Insert into movies (id, title, year, director) " 
					+ "values (?, ?, ?, ?);";
            PreparedStatement ps = dbcon.prepareStatement(movieQuery);
            
            String genreQuery = "Insert into genres (id, name) values(?, ?);";
            PreparedStatement genreStatement = dbcon.prepareStatement(genreQuery);
            
            String g_in_mQuery = "Insert into genres_in_movies (genreId, movieId) values(?, ?);";
            PreparedStatement g_in_mStatement = dbcon.prepareStatement(g_in_mQuery);
            
            String starQuery = "Insert into stars(id, name, birthYear) values(?, ?, ?);";
            PreparedStatement starStatement = dbcon.prepareStatement(starQuery);
            
            String s_in_mQuery = "Insert into stars_in_movies (starId, movieId) values (?, ?);";
            PreparedStatement s_in_mStatement = dbcon.prepareStatement(s_in_mQuery);
            
            String ratingQuery = "Insert into ratings values (?, ?, ?);";
            PreparedStatement ratingStatement = dbcon.prepareStatement(ratingQuery);
            
			for(HashMap.Entry<String, Movie> entry : movies.entrySet())
			{
				Movie mov = entry.getValue();
				if(!isInteger(mov.getYear()))
				{
					continue;
				}
				if(mov.getTitle() == null)
				{
					continue;
				}
				if(mov.getDirectors() == null)
				{
					continue;
				}
				if(mov.getDirectors().size() == 0)
				{
					continue;
				}
				//add set statements
				Integer tempid = id_int + count;
				String insertId = max_id.substring(0,2) + tempid.toString();
				ps.setString(1, insertId);
				ps.setString(2, mov.getTitle());
				ps.setInt(3, Integer.parseInt(mov.getYear()));
				ps.setString(4, mov.getDirectors().get(0));
				ps.addBatch();
				
				ratingStatement.setString(1, insertId);
				ratingStatement.setFloat(2, 0);
				ratingStatement.setInt(3, 0);
				ratingStatement.addBatch();
				
				//Go through genres
				List<String> genres = mov.getGenres();
				for(int i = 0; i < genres.size(); ++i)
				{	
					if(genres.get(i) == null)
					{
						continue;
					}
					//Check if genre exists already

					if(!genreset.contains(genres.get(i)))
					{
						genreset.add(genres.get(i));
						//setup for inputting genre id
						
						genreStatement.setInt(1, gid_int + gcount);
						genreStatement.setString(2, genres.get(i));
						genreStatement.addBatch();
						
						g_in_mStatement.setInt(1, gid_int + gcount);
						g_in_mStatement.setString(2, insertId);
						g_in_mStatement.addBatch();
						gcount++;
					}
					
				}//end genre for
				
				HashMap<String, Actor> actors = mov.getActors();
				for(HashMap.Entry<String, Actor> e : actors.entrySet())
				{
					Actor a = e.getValue();
					if(a.getStagename() == null)
					{
						continue;
					}
					//Check if actor exists already
					
		            if(!starset.contains(a.getStagename()))
					{
		            	starset.add(a.getStagename());
		            	
		            	Integer tempaid = aid_int + acount;
						String ainsertId = amax_id.substring(0,2) + tempaid.toString();
		                starStatement.setString(1, ainsertId);
		                starStatement.setString(2, a.getStagename());
		                if(a.getBirthYear() != null && isInteger(a.getBirthYear()))
		                {
		                	starStatement.setInt(3, Integer.parseInt(a.getBirthYear()));
		                }
		                else
		                {
		                	starStatement.setString(3, null);
		                }
		                starStatement.addBatch();
		                
		                s_in_mStatement.setString(1, ainsertId);
						s_in_mStatement.setString(2, insertId);
						s_in_mStatement.addBatch();
		                acount++;
					}
		            
				}//end actor for
				count++;
				if(count % batchSize == 0)
				{
					ps.executeBatch();
					ratingStatement.executeBatch();
					genreStatement.executeBatch();
					g_in_mStatement.executeBatch();
					starStatement.executeBatch();
					s_in_mStatement.executeBatch();
					System.out.println(count);
				}
			}
			
			ps.executeBatch();
			ratingStatement.executeBatch();
			genreStatement.executeBatch();
			g_in_mStatement.executeBatch();
			starStatement.executeBatch();
			s_in_mStatement.executeBatch();
			ps.close();
			genreStatement.close();
			g_in_mStatement.close();
			starStatement.close();
			s_in_mStatement.close();
			dbcon.close();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}
	
	public void run() {
		parseXmlFile();
		parseDocument();
		insertIntoDB();
	}
	
	public static void main(String[] args) {
		
		parseXML domp = new parseXML();
		domp.run();
		
		/*for(HashMap.Entry<String, Movie> entry : movies.entrySet())
		{
			Movie mov = entry.getValue();
			System.out.println(mov.toString());
		}*/
		System.out.println(movies.size());
		System.out.println("done");
	}
	
}
