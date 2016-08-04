package managers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class LanguageManager {
	
	/**
	 * Singleton.
	 */
	public static LanguageManager sharedInstance = new LanguageManager();
	
	/**
	 * Private default constructor.
	 */
	private LanguageManager() { }
	
	/**
	 * Map storing localized strings.
	 */
	private HashMap<String, String> strings = new HashMap<>();
	
	/**
	 * Load localization
	 * @param file file to parse
	 * @return success
	 */
	public boolean loadLocalization(String file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.length() == 0) continue;
		    	if (line.startsWith("/*")) continue;
		    	
		    	String[] tmp = line.split(" = ");
		    	
		    	if (tmp.length < 2) return false;
		    	if (tmp[0].length() < 2) return false;
		    	if (tmp[1].length() < 3) return false;
		    	
		    	String key = tmp[0].substring(1, tmp[0].length() - 1);
		    	String value = tmp[1].substring(1, tmp[1].length() - 2);
		    	
		    	strings.put(key, value);
		    }
		    return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get localized string for key
	 * @param key
	 * @return
	 */
	public String localizedStringForKey(String key) {
		if (strings.containsKey(key)) 
			return strings.get(key);
		return key;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public static String localize(String key) {
		return sharedInstance.localizedStringForKey(key);
	}
}
