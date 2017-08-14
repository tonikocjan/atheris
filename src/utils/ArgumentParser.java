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

package utils;

import java.util.HashMap;

public class ArgumentParser {

    private HashMap<String, String> arguments = new HashMap<>();

    public ArgumentParser(String[] args) {
        for (int argc = 0; argc < args.length; argc++) {
            String arg = args[argc];

            if (arg.startsWith("--")) {
                parseArgument(arg);
            }
            else {
                arguments.put("sourceFileName", arg);
            }
        }
    }

    private void parseArgument(String arg) {
        int indexOfEqual = arg.indexOf('=');

        String argumentName = arg.substring(2, indexOfEqual);
        String argumentValue = arg.substring(indexOfEqual + 1);

        arguments.put(argumentName, argumentValue);
    }

    public String valueFor(String argumentName) {
        return arguments.get(argumentName);
    }

    public boolean containsArgument(String argument) {
        return arguments.containsKey(argument);
    }

}
