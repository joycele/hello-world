
public class Actor {
	private String stagename;
	private String firstName;
	private String lastName;
	private String birthYear;
	private String id;
	public Actor()
	{
		
	}
	
	public String getId() {
		return this.id;
	}
	public void setid(String id) {
		this.id = id;
	}
	
	public String getStagename() {
		return this.stagename;
	}
	public void setStagename(String stagename) {
		this.stagename = stagename;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getBirthYear() {
		return this.birthYear;
	}
	public void setBirthYear(String birthYear) {
		this.birthYear = birthYear;
	}
	
	public String toString() {
		return "FID: " + id + ", " + "Stagename: " + stagename + ", "  
				+ "First: " + firstName + ", " + "Last: " + lastName;
	}
	
}
