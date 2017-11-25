/**
 * Copyright 2016 Toni Kocjan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package managers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class LanguageManager {

	public static LanguageManager sharedManager = new LanguageManager();
    private HashMap<String, String> translationMapping = new HashMap<>();
    private String delimiter = " = ";

	private LanguageManager() { }

	public boolean loadLocalization(String fileName) {
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.isEmpty()) continue;
		    	if (isComment(line)) continue;

                parseLine(line);
		    }
		    return true;
		} catch (IOException e) {
			System.err.println("Error opening localization file");
			return false;
		}
	}

	private void parseLine(String line) {
        String[] splitedString = line.split(delimiter);

        if (!isLegal(splitedString)) return;

        String key = splitedString[0].substring(1, splitedString[0].indexOf('"', 1));
        String value = splitedString[1].substring(1, splitedString[1].length() - 2);

        translationMapping.put(key, value);
    }

	private boolean isComment(String line) {
	    return line.startsWith("/*");
    }

    private boolean isLegal(String[] splitted) {
        if (splitted.length < 2) return false;
        if (splitted[0].length() < 2) return false;
        if (splitted[1].length() < 3) return false;
        return true;
    }

	public String localizedStringForKey(String key) {
		if (translationMapping.containsKey(key))
			return translationMapping.get(key);
		return key;
	}

	public String localizedStringForKey(String key, Object... args) {
		if (translationMapping.containsKey(key))
			return String.format(translationMapping.get(key), args);
		return key;
	}

	public static String localize(String key) {
		return sharedManager.localizedStringForKey(key);
	}
	
	public static String localize(String key, Object... args) {
		return sharedManager.localizedStringForKey(key, args);
	}
}
