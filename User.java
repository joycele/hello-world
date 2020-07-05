import java.util.HashMap;

/**
 * This User class only has the username field in this example.
 * You can add more attributes such as the user's shopping cart items.
 */
public class User {

    private final String username;
    private final String password;
    private HashMap<String, Integer> shoppingCart;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.shoppingCart = new HashMap<>();
    }
    
    public void addToCart(String movieID) {
    	
    	if (this.shoppingCart.containsKey(movieID)) {
    		this.shoppingCart.put(movieID, this.shoppingCart.get(movieID) + 1);
    	} else {
    		this.shoppingCart.put(movieID, 1);
    	}
    }

    public String getUsername() { return this.username; }
    public String getPassword() { return this.password;}
    public HashMap<String, Integer> getShoppingCart() { return this.shoppingCart; }
}

