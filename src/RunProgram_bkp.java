import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import static spark.Spark.*;

public class RunProgram {
    private static final String SERVER = "tsba460203001";
    private static final String DATABASE = "DBMonitor";
    private static final String USERNAME = "hu_DBMonitor";
    private static final String PASSWORD = "t6yYGWFVfM3d9wEm";
    private static final String BACKUP_DIR = "backup/";

    public static void main(String[] args) {
        System.out.println("Starting server...");

        // Setup REST API server
        port(8060);
        enableCORS("*", "GET,POST,PUT,DELETE,OPTIONS", "Content-Type,Authorization,X-Requested-With");

        // Endpoint to fetch dropdown values
        get("/fetchCustomers", (req, res) -> {
            res.type("application/json");
            ArrayList<String> customerNames = fetchCustomerNames();
            Map<String, ArrayList<String>> responseMap = new HashMap<>();
            responseMap.put("Customers", customerNames);
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(responseMap);
            System.out.println("Customers JSON Response: " + jsonResponse);
            return jsonResponse;
        });

        // Endpoint to fetch server details and distinct component names based on customer selection
        get("/fetchServers", (req, res) -> {
            res.type("application/json");
            String selectedCustomer = req.queryParams("selectedCustomer");
			Map<String, String> response = new HashMap<>();
            if (selectedCustomer == null || selectedCustomer.isEmpty()) {
                return "{\"error\": \"Selected customer parameter missing or empty\"}";
            }
            // Fetch server names and properties
            Map<String, Object> responseMap = fetchServerDetails(selectedCustomer);
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(responseMap);
            System.out.println("Response JSON: " + jsonResponse);  // Log the response JSON
            return jsonResponse;
        });

        // Endpoint to store values in the database and modify properties files
        post("/storeDetails", (req, res) -> {
            res.type("application/json");
            Map<String, String> requestData = new Gson().fromJson(req.body(), Map.class);
            String customer = requestData.get("customer");
            String serverName = requestData.get("serverName");
            String path = requestData.get("path");
            String componentName = requestData.get("componentName");
            String referenceKey = requestData.get("referenceKey");

            Map<String, String> response = new HashMap<>();

            if (!fileExists(path, serverName, componentName)) {
                response.put("error", "File does not exist");
                return new Gson().toJson(response);
            }
            if (!tagExistsInFile(path, serverName, componentName, referenceKey)) {
                response.put("error", "Tag does not exist in file");
                return new Gson().toJson(response);
            }

            boolean success = storeDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, customer, serverName, path, componentName, referenceKey);
            if (success) {
                response.put("message", "Details stored and file updated successfully");
            } else {
                response.put("error", "Failed to store details");
            }

            return new Gson().toJson(response);
        });

        post("/processFormData", (req, res) -> {
            res.type("application/json");
            Map<String, String> requestData = new Gson().fromJson(req.body(), Map.class);
            String changeNumber = requestData.get("changeNumber");
            String jid = requestData.get("jid");
            String upgradedMemory = requestData.get("upgradedMemory");
            String modifiedName = requestData.get("modifiedName");
            String serverNamesStr = requestData.get("serverNames");
			String path = requestData.get("path");
			String key = requestData.get("key");

            // You can optionally perform additional processing with the received data here
            System.out.println("Received data from client:");
            System.out.println("Change Number: " + changeNumber);
            System.out.println("JID: " + jid);
            System.out.println("Upgraded Memory: " + upgradedMemory);
            System.out.println("Modified Name: " + modifiedName);
            System.out.println("Server Names: " + serverNamesStr);
			System.out.println("Path: " + path);
			System.out.println("key: " + key);

            List<String> serverNamesStr = new Gson().fromJson(requestData.get("serverNames").toString(), new TypeToken<List<String>>() {}.getType());
            // Initialize serverNames with dummy valuesserverNamesStr
            for (int i = 0; i < 13; i++) {
                serverNamesStr.add(String.valueOf(i));
            }

            System.out.println("Initial ServerNames: " + serverNamesStr);
           if (serverNamesStr.size() > 12) {
			// Check if the value at index 9 is "1" and replace it with "2"
			if ("1".equals(serverNamesStr.get(12))) {
				serverNamesStr.set(12, "2");
				System.out.println("Modified ServerNames: " + serverNamesStr);
			} 
			// Check if the value at index 9 is "2" and replace it with "1"
			else if ("2".equals(serverNamesStr.get(12))) {
				serverNamesStr.set(12, "1");
				System.out.println("Modified ServerNames: " + serverNamesStr);
			}
		}


            System.out.println("Final ServerNames: " + serverNamesStr);

            File file = new File("//" + serverNamesStr + "/" + path + "/" + modifiedName + "JVMInit.properties");
            backupAndModifyPropertiesFile(file, upgradedMemory, key);
			boolean success = DetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, selectedCustomer, serverNames, jid, changeNumber, upgradedMemory, modifiedName);
            Map<String, String> response = new HashMap<>();
			if (success) {
                response.put("message", "Details stored and file updated successfully");
            } else {
                response.put("error", "Failed to store details");
            }

            return new Gson().toJson(response);
            // Return a response if needed
        });
    }

    private static String getComponentPath(List<String> componentNames, List<String> paths, String componentName) {
        // Create a map to store component names and their corresponding paths
        Map<String, String> componentPathMap = new HashMap<>();
        for (int i = 0; i < componentNames.size(); i++) {
            componentPathMap.put(componentNames.get(i), paths.get(i));
        }

        // Fetch the path for the given component name
        return componentPathMap.get(componentName);
    }

    private static boolean fileExists(String path, String serverName, String componentName) {
        if (path != null && path.length() > 2 && path.charAt(1) == ':') {
            path = path.substring(0, 1) + "$" + path.substring(2);
        }
        String filename = componentName + "JVMInit.properties";
        File file = new File("//" + serverName + "/" + path + "/" + filename);
        if (file.exists()) {
            System.out.println("File found: " + file.getPath());
        }
        return file.exists();
    }

    private static boolean tagExistsInFile(String path, String serverName, String componentName, String referenceKey) {
        try {
            if (path != null && path.length() > 2 && path.charAt(1) == ':') {
                path = path.substring(0, 1) + "$" + path.substring(2);
            }
            String filename = componentName + "JVMInit.properties";
            String filePath = "//" + serverName + "/" + path + "/" + filename;
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return content.contains(referenceKey);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean storeDetailsInDatabase(String server, String database, String username, String password,
                                                  String customer, String serverName, String path,
                                                  String componentName, String referenceKey) {
        // Implement your logic to store details in the database
        try (Connection connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
            String insertQuery = "INSERT INTO AppConfig (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setString(1, customer);
                preparedStatement.setString(2, serverName);
                preparedStatement.setString(3, path);
                preparedStatement.setString(4, componentName);
                preparedStatement.setString(5, referenceKey);
                int rowsAffected = preparedStatement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static ArrayList<String> fetchCustomerNames() {
        ArrayList<String> customerNames = new ArrayList<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DISTINCT Customer FROM AppDetails")) {
            while (resultSet.next()) {
                customerNames.add(resultSet.getString("Customer"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customerNames;
    }

    private static Map<String, Object> fetchServerDetails(String selectedCustomer) {
        Map<String, Object> responseMap = new HashMap<>();
        ArrayList<String> serverNames = new ArrayList<>();
        ArrayList<String> componentNames = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT ServerName, ComponentName, Path FROM AppConfig WHERE Customer = ?")) {
            preparedStatement.setString(1, selectedCustomer);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    serverNames.add(resultSet.getString("ServerName"));
                    componentNames.add(resultSet.getString("ComponentName"));
                    paths.add(resultSet.getString("Path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        responseMap.put("serverNames", serverNames);
        responseMap.put("componentNames", componentNames);
        responseMap.put("paths", paths);

        return responseMap;
    }

    private static Map<String, Object> fetchComponentInfo(Connection connection, String selectedComponent) {
        Map<String, Object> componentInfo = new HashMap<>();
        ArrayList<String> componentNames = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT ComponentName, Path FROM AppConfig WHERE ComponentName = ?")) {
            preparedStatement.setString(1, selectedComponent);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    componentNames.add(resultSet.getString("ComponentName"));
                    paths.add(resultSet.getString("Path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        componentInfo.put("componentNames", componentNames);
        componentInfo.put("paths", paths);

        return componentInfo;
    }

     private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
    }


    public static void enableCORS(final String origin, final String methods, final String headers) {
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.header("Access-Control-Allow-Credentials", "true");
        });

        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });
    }

			public static void backupAndModifyPropertiesFile(File file, String key, String newValue) {
			try {
				// Backup the file
				Path sourcePath = file.toPath();
				Path backupPath = Paths.get(file.getAbsolutePath() + ".bak");
				Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

				// Read the properties file
				StringBuilder contentBuilder = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					String line;
					while ((line = reader.readLine()) != null) {
						contentBuilder.append(line).append(System.lineSeparator());
					}
				}

				String content = contentBuilder.toString();

				// Modify the specific value
				Pattern pattern = Pattern.compile(Pattern.quote(key) + "=\\S+");
				Matcher matcher = pattern.matcher(content);
				if (matcher.find()) {
					String oldMemoryValue = matcher.group();
					System.out.println("Old Memory Value: " + oldMemoryValue);

					// Replace the old value with the new value
					String newMemoryValue = key + "=" + newValue;
					content = content.replace(oldMemoryValue, newMemoryValue);
					System.out.println("New Memory Value: " + newMemoryValue);
				} else {
					System.out.println("Key not found: " + key);
				}

				// Save the updated properties back to the file
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
					writer.write(content);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	private static void readPropertiesFiles(ArrayList<Map<String, String>> serverProperties, String serverName, Map<String, Object> componentInfo) {
       if (componentInfo == null || !componentInfo.containsKey("componentNames") || !componentInfo.containsKey("paths") || !componentInfo.containsKey("keys")) {
          System.out.println("Component information is missing or incomplete.");
          return;
		}

    List<String> componentNames = (List<String>) componentInfo.get("componentNames");
    List<String> paths = (List<String>) componentInfo.get("paths");
    List<String> keys = (List<String>) componentInfo.get("keys");


    for (int i = 0; i < componentNames.size(); i++) {
        String componentName = componentNames.get(i);
        String path = paths.get(i);
        String key = keys.get(i);

        // Ensure path and key are not null or empty before proceeding
        if (path != null && path.length() > 2 && path.charAt(1) == ':') {
                path = path.substring(0, 1) + "$" + path.substring(2);
            }
            String filename = componentName + "JVMInit.properties";
            File file = new File("//" + serverName + "/" + path + "/" + filename);
            if (file.exists()) {
                System.out.println("File found: " + file.getPath());
                Map<String, String> fileProperty = new HashMap<>();
                fileProperty.put("modifiedFileName", componentName);
                addMemoryValue(fileProperty, file, key, serverName);
                serverProperties.add(fileProperty);
            } else {
                System.out.println("File not found: " + file.getPath());
            }
        }
    }

    private static void addMemoryValue(Map<String, String> fileProperty, File file, String key, String serverName) {
        String memoryValue = readMemoryValue(file);
        fileProperty.put("memoryValue", memoryValue);
        fileProperty.put("serverName", serverName);
    }

    public static String readMemoryValue(File file) {
        String patternString = "(?<=JvmMs=)(.*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher;
        String memoryValue = "";

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (matcher.find()) {
                    memoryValue = matcher.group(1); // Use group(1) to get what's inside parentheses
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Properly handle IOException
        }

        return memoryValue;
    }
	
	
	private static boolean DetailsInDatabase(Connection connection, String server, String database, String username, String password,
                                                  String selectedCustomer, List<String> serverNames, String jid,
                                                  String changeNumber, String upgradedMemory) {
        // Implement your logic to store details in the database
        try (Connection connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
            String insertQuery = "INSERT INTO AppConfigHistory (CUSTOMER_NAME, SERVER_NAME, J_ID, CHANGE_NUMBER, UPGRADED_MEMORY, COMPONENT_NAME) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                for (String serverName : serverNames) {
                preparedStatement.setString(1, selectedCustomer);
                preparedStatement.setString(2, serverName); // Set server name
                preparedStatement.setString(3, jid);
                preparedStatement.setString(4, changeNumber);
                preparedStatement.setString(5, upgradedMemory);
				preparedStatement.setString(5, modifiedName);
                preparedStatement.addBatch(); // Add to batch
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}	
	
}
