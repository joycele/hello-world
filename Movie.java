import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Movie {

	private String id;
	private String title;
	private String year;
	private List<String> directors; 
	private List<String> genres;
	private HashMap<String, Actor> actors;
	
	public Movie() {
		directors = new ArrayList<String>();
		genres = new ArrayList<String>();
		actors = new HashMap<String, Actor>();
	}
	
	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getYear() {
		return this.year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	
	public void addDirector(String director) {
		directors.add(director);
	}
	public List<String> getDirectors() {
		return this.directors;
	}
	
	public void addGenre(String genre) {
		genres.add(genre);
	}
	public List<String> getGenres(){
		return this.genres;
	}
	
	public void addActor(String stagename) {
		if(actors.containsKey(stagename))
		{
			System.out.println("Duplicate Stagename: " + stagename);
		}
		else
		{
			Actor a = new Actor();
			a.setStagename(stagename);
			actors.put(stagename, a);
		}
	}
	
	public void updateActor(String stagename, String firstName, 
			String lastName, String birthYear) {
		if(actors.containsKey(stagename))
		{
			actors.get(stagename).setFirstName(firstName);
			actors.get(stagename).setLastName(lastName);
			actors.get(stagename).setBirthYear(birthYear);
		}
	}
	
	public HashMap<String, Actor> getActors(){
		return this.actors;
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Movie Details - ");
		sb.append("Title: " + getTitle());
		sb.append(", ");
		sb.append("FID: " + getId());
		sb.append(", ");
		sb.append("Year: " + getYear());
		for(int i = 0; i < directors.size(); ++i)
		{
			sb.append(", ");
			sb.append("Director: " + directors.get(i));
		}
		for(int i = 0; i < genres.size(); ++i)
		{
			sb.append(", ");
			sb.append("Genre: " + genres.get(i));
		}
		for(HashMap.Entry<String, Actor> entry : actors.entrySet())
		{
			Actor a = entry.getValue();
			sb.append(", ");
			sb.append("Stagename: " + a.getStagename());
			sb.append(", ");
			sb.append("FirstName: " + a.getFirstName());
			sb.append(", ");
			sb.append("LastName: " + a.getLastName());
			sb.append(", ");
			sb.append("BirthYear: "  + a.getBirthYear());
		}
		return sb.toString();
	}
}
