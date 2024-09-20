import java.io.BufferedReader;
import java.io.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.*;
import java.sql.SQLException;
import java.sql.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.List;
import java.util.Date; 
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;
import static spark.Spark.post;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Route;
import static spark.Spark.get;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import static spark.Spark.*;

public class RunProgram {
    private static final String SERVER = "tsba460203001";
    private static final String DATABASE = "DBMonitor";
    private static final String USERNAME = "hu_DBMonitor";
    private static final String PASSWORD = "t6yYGWFVfM3d9wEm";
	private static final String user_name = "fmxprod";
	private static final String pass_word = "Pr0d@dmin";
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final Gson GSON = new Gson();
	private static final Gson gson = new Gson(); 

    public static void main(String[] args) {
        System.out.println("Starting server...");
		
		scheduler.scheduleAtFixedRate(RunProgram::checkScheduledRestarts, 0, 1, TimeUnit.MINUTES);

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

        // Endpoint to fetch processes based on server selection
			get("/fetchProcess", (req, res) -> {
            res.type("application/json");
            String selectedServer = req.queryParams("selectedServer");
            if (selectedServer == null || selectedServer.isEmpty()) {
                res.status(400);
                return "{\"error\": \"Selected server parameter missing or empty\"}";
            }
 
            try {
			
                Map<String, Object> responseMap = fetchProcesses(selectedServer);
                String jsonResponse = new ObjectMapper().writeValueAsString(responseMap);
                System.out.println("Response JSON: " + jsonResponse);
                return jsonResponse;
            } catch (IOException e) {
                res.status(500);
                e.printStackTrace();
                return "{\"error\": \"Internal Server Error\"}";
            }
        });
		
		// Endpoint to fetch processes based on server selection
		get("/fetchCommand", (Request req, Response res) -> {
    res.type("application/json");

    String selectedServer = req.queryParams("selectedServer");
    String selectedProcess = req.queryParams("selectedProcess");

    if (selectedServer == null || selectedServer.isEmpty()) {
        res.status(400);
        return "{\"error\": \"Selected server parameter missing or empty\"}";
    }
    if (selectedProcess == null || selectedProcess.isEmpty()) {
        res.status(400);
        return "{\"error\": \"Selected process parameter missing or empty\"}";
    }

    try {
        String output = executeCommandPowerShell(selectedServer, user_name, pass_word, selectedProcess);

        // Initialize ObjectMapper to handle JSON conversion
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = new HashMap<>();

        if (output != null && !output.isEmpty()) {
            // Convert JSON string to Map
            Map<String, Object> resultMap = objectMapper.readValue(output, Map.class);
            responseMap.put("Commands", resultMap); // Add the parsed JSON to the response
        } else {
            res.status(404);
            responseMap.put("error", "No output received from PowerShell script. Please check the server name and network connectivity.");
        }

        // Convert responseMap to JSON string
        String jsonResponse = objectMapper.writeValueAsString(responseMap);
        System.out.println("Response JSON: " + jsonResponse);
        return jsonResponse;

    } catch (IOException e) {
        res.status(500);
        e.printStackTrace();
        return "{\"error\": \"Internal Server Error. Please check the server logs for more details.\"}";
    }
});



        // Endpoint to store values in the database
        post("/storeDetails", (req, res) -> {
    res.type("application/json");

    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());
    String customer = (String) requestData.get("customer");
    String serverName = (String) requestData.get("serverName");
    String originalFilePath = (String) requestData.get("path");
    String componentName = (String) requestData.get("componentName");
    List<String> referenceKey = (List<String>) requestData.get("referenceKey");
    String tag = (String) requestData.get("Tag");
	System.out.println(tag);

    // Modify path if it contains a hyphen
    String pathWithoutHyphen = originalFilePath;
    if (originalFilePath != null && originalFilePath.contains("#")) {
        pathWithoutHyphen = originalFilePath.replaceAll("#.*", ""); // Remove hyphen and characters after it
    }

    Map<String, String> response = new HashMap<>();
    System.out.println("newPath: " + pathWithoutHyphen);

    boolean validationResult;

    if (tag != null && !tag.isEmpty()) {
        // Scenario: Tag variable contains a value
        validationResult = checkValuesExistence(pathWithoutHyphen, serverName, componentName, referenceKey, tag);
    } else {
        // Scenario: Tag variable does not contain a value
        if (!fileExists(pathWithoutHyphen, serverName, componentName)) {
            response.put("error", "File does not exist");
            return new Gson().toJson(response);
        }
        validationResult = tagExistsInFile(pathWithoutHyphen, serverName, componentName, referenceKey);
    }

    if (!validationResult) {
        response.put("error", (tag != null && !tag.isEmpty()) ? "Tags or keys does not exist in file" : "keys does not exist in file");
        return new Gson().toJson(response);
    }

    // Perform storeDetailsInDatabase only if validationResult is true
    if (validationResult) {
        String result = storeDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, customer, serverName, originalFilePath, componentName, referenceKey);
        System.out.println(result);
        if ("success".equals(result)) {
            response.put("message", "Details stored successfully");
        } else if ("duplicate".equals(result)) {
            response.put("error", "The configuration already exists.");
        } else {
            response.put("error", "Failed to store details");
        }
    }

    return new Gson().toJson(response);
});


post("/storeThreadDetails", (req, res) -> {
            res.type("application/json");
            Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());
            String customer = (String) requestData.get("customer");
            String serverName = (String) requestData.get("serverName");
            String originalFilePath = (String) requestData.get("path");
            String componentName = (String) requestData.get("componentName");
            String threadKey = (String) requestData.get("threadKey");
            String tag = (String) requestData.get("Tag");
            System.out.println(tag);

            // Modify path if it contains a hyphen
            String pathWithoutHyphen = originalFilePath;
            if (originalFilePath != null && originalFilePath.contains("#")) {
                pathWithoutHyphen = originalFilePath.replaceAll("#.*", ""); // Remove hyphen and characters after it
            }

            Map<String, String> response = new HashMap<>();
            System.out.println("newPath: " + pathWithoutHyphen);

            boolean validationResult;

            if (tag != null && !tag.isEmpty()) {
                // Scenario: Tag variable contains a value
                validationResult = checkValuesthreadExistence(pathWithoutHyphen, serverName, threadKey, tag);
            } else {
                // Scenario: Tag variable does not contain a value
                if (!threadfileExists(pathWithoutHyphen, serverName)) {
                    response.put("error", "File does not exist");
                    return new Gson().toJson(response);
                }
                validationResult = tagthreadExistsInFile(pathWithoutHyphen, serverName, threadKey);
            }

            if (!validationResult) {
                response.put("error", (tag != null && !tag.isEmpty()) ? "Tags or keys do not exist in file" : "keys do not exist in file");
                return new Gson().toJson(response);
            }

            // Perform storeDetailsInDatabase only if validationResult is true
            if (validationResult) {
                String result = storethreadDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, customer, serverName, originalFilePath, componentName, threadKey);

                if ("success".equals(result)) {
                    response.put("message", "Details stored successfully");
                } else if ("duplicate".equals(result)) {
                    response.put("error", "The configuration already exists.");
                } else {
                    response.put("error", "Failed to store details");
                }
            }

            return new Gson().toJson(response);
        });
		
		
	post("/storeCustomDetails", (req, res) -> {
    res.type("application/json");
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());

    String customer = (String) requestData.get("customer");
    String serverName = (String) requestData.get("serverName");
    String Path = (String) requestData.get("path");
    String componentName = (String) requestData.get("componentName");
    List<String> customKey = (List<String>) requestData.get("customKey"); // Accepting customKey as a List
    String tag = (String) requestData.get("Tag");

    Map<String, String> response = new HashMap<>();

    // Extract outerTag and innerTag from customKey
    String outerTag = customKey.get(0); // First value
    String innerTag = customKey.get(1); // Second value

    // Scenario: File does not exist
    if (!customfileExists(Path, serverName)) {
        response.put("error", "File does not exist");
        return new Gson().toJson(response);
    }

    // Validate XML tags
    boolean validationResult = tagCustomExistsInFile(Path, serverName, outerTag, innerTag);

    if (!validationResult) {
        response.put("error", (tag != null && !tag.isEmpty()) ? "Tags do not exist in the file" : "Keys do not exist in the file");
        return new Gson().toJson(response);
    }

    // Perform storeDetailsInDatabase only if validationResult is true
    if (validationResult) {
        String result = storeCustomDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, customer, serverName, Path, componentName,customKey);

        if ("success".equals(result)) {
            response.put("message", "Details stored successfully");
        } else if ("duplicate".equals(result)) {
            response.put("error", "The configuration already exists.");
        } else {
            response.put("error", "Failed to store details");
        }
    }

    return new Gson().toJson(response);
});

	 
    post("/processFormData", (req, res) -> {
    res.type("application/json");
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());

    String changeNumber = (String) requestData.get("changeNumber");
    String jid = (String) requestData.get("jid");
    String modifiedName = (String) requestData.get("modifiedName");
    String path = (String) requestData.get("path");
    String selectedCustomer = (String) requestData.get("selectedCustomer");
	String tags = (String) requestData.get("tag");

    // Deserialize serverName field as a String
    String serverName = (String) requestData.get("serverNames");

    // Deserialize upgradedMemory as a list of strings
     List<String> upgradedMemory = (List<String>) requestData.get("upgradedMemory");

    System.out.println("Received data from client:");
    System.out.println("Change Number: " + changeNumber);
    System.out.println("JID: " + jid);
    System.out.println("Upgraded Memory: " + upgradedMemory);
    System.out.println("Modified Name: " + modifiedName);
    System.out.println("Server Name: " + serverName);
    System.out.println("Path: " + path);
    System.out.println("Selected Customer: " + selectedCustomer);
	System.out.println("tag: " + tags);

    // Process server name to create a list with both node 1 and node 2 versions
    List<String> serverNamesStr = new ArrayList<>();
    serverNamesStr.add(serverName);

    char lastChar = serverName.charAt(serverName.length() - 1);
    String modifiedServerName = serverName;

    // Replace the last character if it is '1' or '2'
    if (lastChar == '1') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '2';
    } else if (lastChar == '2') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '1';
    }

    // Add the modified server name to the list
    serverNamesStr.add(modifiedServerName);

    // Remove duplicates while preserving order
    Set<String> uniqueSet = new LinkedHashSet<>(serverNamesStr);
    serverNamesStr.clear();
    serverNamesStr.addAll(uniqueSet);

    System.out.println("Final Server Names: " + serverNamesStr);

    // Process each server name to build the file path
    Map<String, String> response = new HashMap<>();
    boolean overallSuccess = true;

    for (String currentServerName : serverNamesStr) {
        File file = new File("//" + currentServerName + "/" + path);

        try {
            // Process upgradedMemory values
            if (upgradedMemory.size() >= 2) {
                backupAndModifyPropertiesFile(file, "-Xms", tags, upgradedMemory.get(0));
                backupAndModifyPropertiesFile(file, "-Xmx", tags, upgradedMemory.get(1));
            } else {
                throw new Exception("Insufficient memory upgrade values provided.");
            }

            boolean success = DetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, selectedCustomer, serverNamesStr, jid, modifiedName, changeNumber, upgradedMemory);
            if (!success) {
                overallSuccess = false;
                response.put("error", "Failed to store details for server: " + currentServerName);
                break;
            }
        } catch (Exception e) {
            overallSuccess = false;
            response.put("error", "Exception occurred for server: " + currentServerName + ". Error: " + e.getMessage());
            break;
        }
    }

    if (overallSuccess) {
        response.put("message", "Details stored and file updated successfully for all servers");
    }

    return new Gson().toJson(response);
});

post("/processThreadData", (req, res) -> {
    res.type("application/json");
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());

    // Extract data from request
    String changeNumber = (String) requestData.get("changeNumber");
    String jid = (String) requestData.get("jid");
    String modifiedName = (String) requestData.get("modifiedName");
    String path = (String) requestData.get("path");
    String selectedCustomer = (String) requestData.get("selectedCustomer");
    String key = (String) requestData.get("key");
    String tags = (String) requestData.get("tag");
    String serverName = (String) requestData.get("serverNames");
    String upgradedThread = (String) requestData.get("upgradedThread");

    List<String> serverNamesStr = new ArrayList<>();
    serverNamesStr.add(serverName);
    
    char lastChar = serverName.charAt(serverName.length() - 1);
    String modifiedServerName = serverName;

    if (lastChar == '1') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '2';
    } else if (lastChar == '2') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '1';
    }
    serverNamesStr.add(modifiedServerName);

    Set<String> uniqueSet = new LinkedHashSet<>(serverNamesStr);
    serverNamesStr.clear();
    serverNamesStr.addAll(uniqueSet);

    Map<String, String> response = new HashMap<>();
    boolean overallSuccess = true;

    for (String currentServerName : serverNamesStr) {
        File file = new File("//" + currentServerName + "/" + path);

        try {
            backupAndModifyThreadFile(file, key, tags, upgradedThread);
            boolean success = ThreadDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, selectedCustomer, serverNamesStr, jid, modifiedName, changeNumber, upgradedThread);
            if (!success) {
                throw new Exception("Failed to store thread details for server: " + currentServerName);
            }
        } catch (Exception e) {
            overallSuccess = false;
            response.put("error", e.getMessage());
            break;
        }
    }

    if (overallSuccess) {
        response.put("message", "Details stored and file updated successfully for all servers");
    }

    return new Gson().toJson(response);
});

post("/processCustomData", (req, res) -> {
    res.type("application/json");

    // Deserialize the request body to a Map
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());

    // Extract individual fields from the request
    String changeNumber = (String) requestData.get("changeNumber");
    String jid = (String) requestData.get("jid");
    String modifiedName = (String) requestData.get("modifiedName");
    String path = (String) requestData.get("path");
    String selectedCustomer = (String) requestData.get("selectedCustomer");
    String serverName = (String) requestData.get("serverNames");
    String upgradedValue = (String) requestData.get("upgradedValue");

    // Deserialize the keys from the JSON request
    List<String> key = (List<String>) requestData.get("key");

    // Debugging statements
    System.out.println("Received data from client:");
    System.out.println("Change Number: " + changeNumber);
    System.out.println("JID: " + jid);
    System.out.println("Upgraded value: " + upgradedValue);
    System.out.println("Modified Name: " + modifiedName);
    System.out.println("Server Name: " + serverName);
    System.out.println("Path: " + path);
    System.out.println("Selected Customer: " + selectedCustomer);
    System.out.println("Keys: " + key);

    // Process server names
    List<String> serverNamesStr = new ArrayList<>();
    serverNamesStr.add(serverName);

    char lastChar = serverName.charAt(serverName.length() - 1);
    String modifiedServerName = serverName;

    if (lastChar == '1') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '2';
    } else if (lastChar == '2') {
        modifiedServerName = serverName.substring(0, serverName.length() - 1) + '1';
    }

    serverNamesStr.add(modifiedServerName);
    Set<String> uniqueSet = new LinkedHashSet<>(serverNamesStr);
    serverNamesStr.clear();
    serverNamesStr.addAll(uniqueSet);
    System.out.println("Final server names: " + serverNamesStr);

    // Process each server name
    Map<String, String> response = new HashMap<>();
    boolean overallSuccess = true;

    for (String currentServerName : serverNamesStr) {
        File file = new File("//" + currentServerName + "/" + path);
        System.out.println("Processing file at path: " + file.getAbsolutePath());

        try {
            // Process the file and update tags
            backupAndChangeTagValue(file.getAbsolutePath(), key, upgradedValue);

            boolean success = CustomDetailsInDatabase(SERVER, DATABASE, USERNAME, PASSWORD, selectedCustomer, serverNamesStr, jid, modifiedName, changeNumber, upgradedValue);
            if (!success) {
                overallSuccess = false;
                response.put("error", "Failed to store details for server: " + currentServerName);
                System.out.println("Failed to store details for server: " + currentServerName);
                break;
            }
        } catch (Exception e) {
            overallSuccess = false;
            response.put("error", "Exception occurred for server: " + currentServerName + ". Error: " + e.getMessage());
            System.out.println("Exception occurred for server: " + currentServerName + ". Error: " + e.getMessage());
            e.printStackTrace();  // Print stack trace for more detailed debugging
            break;
        }
    }

    if (overallSuccess) {
        response.put("message", "Details stored and file updated successfully for all servers");
        System.out.println("Details stored and file updated successfully for all servers");
    }

    return new Gson().toJson(response);
});



post("/restartProcess", (req, res) -> {
    res.type("application/json");
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());
    
    String customer = (String) requestData.get("customer");
    String remoteServer = (String) requestData.get("serverName");
    String processName = (String) requestData.get("process");
    String commandLine = (String) requestData.get("command");
    String scriptPath = "src/kill-remote-process.ps1";

    System.out.println(commandLine);

    // Initialize response map
    Map<String, String> response = new HashMap<>();

    // Call the killRemoteProcess function
    ProcessResult result = killRemoteProcess(remoteServer, user_name, pass_word, scriptPath, processName, commandLine);
    
    // Check if the process restart succeeded
    if (!result.isSuccess()) {
        // If it failed, return the error message
        response.put("error", "Failed to Restart Process: " + result.getMessage());
    } else {
        // If it succeeded, return success message
        response.put("message", "Process restarted successfully");
    }

    // Return the response as JSON
    return new Gson().toJson(response);
});

		
		
post("/executePowerShell", (Request req, Response res) -> {
    res.type("application/json");
 
    // Parse the JSON request body
    Map<String, Object> requestData = new Gson().fromJson(req.body(), new TypeToken<Map<String, Object>>() {}.getType());
    List<String> serverInput = (List<String>) requestData.get("serverInput");
    String customerInput = (String) requestData.get("customerInput");
    List<String> servicesInput = (List<String>) requestData.get("servicesInput");
    String fileageServerName = (String) requestData.get("fileageServerName");
    String fileageUsername = (String) requestData.get("fileageUsername");
    String fileagePassword = (String) requestData.get("fileagePassword");
    String fileageDatabaseName = (String) requestData.get("fileageDatabaseName");
    String dbServerName = (String) requestData.get("dbServerName");
    String dbUsername = (String) requestData.get("dbUsername");
    String dbPassword = (String) requestData.get("dbPassword");
    String dbDatabaseName = (String) requestData.get("dbDatabaseName");
 
      String scriptPath = "src/Transition_Chklst.ps1";

    // Execute the PowerShell script and capture the output
    String scriptOutput = executePowerShellScript(
        serverInput, customerInput, servicesInput, fileageServerName, fileageUsername, fileagePassword,
        fileageDatabaseName, dbServerName, dbUsername, dbPassword, dbDatabaseName, scriptPath, user_name, pass_word
    );

    // Create a map to hold the output
    Map<String, String> outputMap = new HashMap<>();
    outputMap.put("output", scriptOutput);

    // Return the script output as a JSON response
    return new Gson().toJson(outputMap);
});


 post("/saveData", (request, response) -> {
            // Parse JSON into Java Map
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = gson.fromJson(request.body(), mapType);

            // Extract data from the map
            String applicationName = (String) data.get("ApplicationName");
            String installationDrive = (String) data.get("installationDrive");
            List<Map<String, String>> tableRows = (List<Map<String, String>>) data.get("tableRows");

            Connection connection = null;
            PreparedStatement preparedStatement = null;

            try {
                // Establish the database connection
                connection = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);

                // Prepare the SQL INSERT statement
                String insertQuery = "INSERT INTO AppHost (APPLICATION_NAME, DEFAULT_DRIVE, COMPONENT_NAME, PATH, FILENAME, SPECIFIC_TAG, REFERENCE_KEY, CONFIG_TYPE) "
                                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                preparedStatement = connection.prepareStatement(insertQuery);

                // Loop through the table rows and add each row to the batch
                for (Map<String, String> row : tableRows) {
                    preparedStatement.setString(1, applicationName);
                    preparedStatement.setString(2, installationDrive);
                    preparedStatement.setString(3, row.get("componentName"));
                    preparedStatement.setString(4, row.get("path"));
                    preparedStatement.setString(5, row.get("filename"));
                    preparedStatement.setString(6, row.get("specificTag"));
                    preparedStatement.setString(7, row.get("referenceKey"));
                    preparedStatement.setString(8, row.get("configType"));
                    preparedStatement.addBatch();
                }

                // Execute the batch of inserts
                preparedStatement.executeBatch();

                // Return a success response
                response.type("application/json");
                return gson.toJson(createResponseMap("success", null));
            } catch (SQLException e) {
                e.printStackTrace();
                response.status(500);
                return gson.toJson(createResponseMap("error", e.getMessage()));
            } finally {
                // Close resources
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
		
	get("/getData", (Request req, Response res) -> {
            String selectedCustomer = req.queryParams("customer"); // Assuming customer is passed as a query parameter

            JsonObject responseJson = new JsonObject();

            try (Connection connection = DriverManager.getConnection(
                    "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

                // Fetch application names
                JsonArray applicationArray = fetchApplicationNames(connection);
                responseJson.add("applications", applicationArray);
				System.out.println(applicationArray);

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"Error retrieving data: " + e.getMessage() + "\"}";
            }

            res.type("application/json");
            return responseJson.toString();
        });
		
	get("/fetchComponents", (req, res) -> {
            res.type("application/json");
            String selectedAppname = req.queryParams("selectedAppname");
            if (selectedAppname == null || selectedAppname.isEmpty()) {
                return "{\"error\": \"Selected customer parameter missing or empty\"}";
            }
            // Fetch server names and properties
            Map<String, Object> responseMap = fetchComponentsDetails(selectedAppname);
            Gson gson = new Gson();
            String jsonResponse = gson.toJson(responseMap);
            System.out.println("Response JSON: " + jsonResponse);  // Log the response JSON
            return jsonResponse;
        });
 
  get("/checkServerAssignment", (req, res) -> {
    String serversParam = req.queryParams("serverNames");
    String customerName = req.queryParams("customerName");
 
    System.out.println("Received request for servers: " + serversParam);
    System.out.println("Customer name: " + customerName);
 
    if (serversParam == null || serversParam.isEmpty()) {
        res.status(400);
        return gson.toJson(new ErrorResponse("Server names are required"));
    }
 
    List<String> serverNames;
    try {
        // Parse JSON array
        serverNames = gson.fromJson(serversParam, new TypeToken<List<String>>() {}.getType());
    } catch (Exception e) {
        res.status(400);
        return gson.toJson(new ErrorResponse("Invalid server names format"));
    }
 
    if (serverNames.isEmpty()) {
        res.status(400);
        return gson.toJson(new ErrorResponse("No server names provided"));
    }
 
    List<String> assignedServers = getAssignedServers(serverNames, customerName);
 
    // Remove duplicates
    Set<String> uniqueServers = new HashSet<>(assignedServers);
    List<String> uniqueServersList = new ArrayList<>(uniqueServers);
 
    res.type("application/json");
    return gson.toJson(new AssignmentResponse(uniqueServersList));
});

post("/insertEntries", (request, response) -> {
    // Parse the request body as JSON
    Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);

    // Extract data from the request
    String selectedAppname = (String) requestBody.get("selectedAppname");
    String customerName = (String) requestBody.get("customerName");
    List<String> serverNames = (List<String>) requestBody.get("serverNames");
    List<String> componentNames = (List<String>) requestBody.get("componentNames");

    // Store the details in the database
    try (Connection conn = getConnection()) {
        // Fetch component details including config type
        ArrayList<Map<String, Object>> componentDetails = fetchComponentsDetailsFromDB(conn, selectedAppname);

        // Loop through each server and component to insert them based on config type
        for (String serverName : serverNames) {
            for (String componentName : componentNames) {
                // Find the corresponding component details for this component
                Map<String, Object> componentDetail = getComponentDetail(componentDetails, componentName);

                if (componentDetail != null) {
                    // Insert into the corresponding table based on config type
                    insertIntoConfigTable(conn, selectedAppname, customerName, serverName, componentDetail);
                }
            }
        }

        // Create a success response
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("status", "success");
        successResponse.put("message", "Data successfully inserted.");
        
        response.status(200);
        return gson.toJson(successResponse);

    } catch (SQLException e) {
        e.printStackTrace();
        
        // Handle specific SQL exception for duplicate entries
        Map<String, Object> errorResponse = new HashMap<>();
        if (e.getSQLState().equals("23505") || e.getSQLState().startsWith("23")) { // Adjust depending on your DB
            errorResponse.put("status", "error");
            errorResponse.put("message", "Duplicate entry. The data already exists.");
        } else {
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to insert data.");
        }
        
        response.status(500);
        return gson.toJson(errorResponse);
    }
});



get("/getAppData", (Request req, Response res) -> {
    JsonObject responseJson = new JsonObject();

    try (Connection connection = DriverManager.getConnection(
            "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

        // Fetch distinct application names from the three tables
        ArrayList<String> applicationNames = fetchDistinctApplicationNames(connection);
        
        // Convert the application names to a JSON array
        JsonArray applicationArray = new JsonArray();
        for (String appName : applicationNames) {
            applicationArray.add(appName);
        }
        
        responseJson.add("applications", applicationArray);
        System.out.println(applicationArray);

    } catch (Exception e) {
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"Error retrieving data: " + e.getMessage() + "\"}";
    }

    res.type("application/json");
    return responseJson.toString();
});

get("/fetchAppCustomers", (req, res) -> {
    res.type("application/json");

    // Get the selected application name from query parameters
    String selectedApplicationname = req.queryParams("selectedApplicationname");

    List<String> customerDetails;
    try (Connection connection = DriverManager.getConnection(
            "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

        // Fetch customer names based on the selected application name
        customerDetails = fetchCustomerDetailsFromDB(connection, selectedApplicationname);

    } catch (SQLException e) {
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"Error retrieving data: " + e.getMessage() + "\"}";
    }

    // Create a map with a single key "customers" and the list of customer names
    Map<String, List<String>> responseMap = new HashMap<>();
    responseMap.put("customers", customerDetails);

    // Convert the response map to JSON
    Gson gson = new Gson();
    String jsonResponse = gson.toJson(responseMap);
    System.out.println("Customers JSON Response: " + jsonResponse);
    return jsonResponse;
});

 get("/fetchCustServers", (req, res) -> {
    res.type("application/json");

    // Get the selected customer name from the query parameter
    String selectedCustomer = req.queryParams("selectedcustomerDropdown");

    ArrayList<String> serverDetails;
    try (Connection connection = DriverManager.getConnection(
            "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

        // Fetch server names based on the selected customer name
        serverDetails = fetchServerFromDB(connection, selectedCustomer);

    } catch (SQLException e) {
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"Error retrieving data: " + e.getMessage() + "\"}";
    }

    // Prepare the JSON response under the key "servers"
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("servers", serverDetails);
    String jsonResponse = new Gson().toJson(responseMap);

    System.out.println("Servers JSON Response: " + jsonResponse);
    return jsonResponse;
});


get("/fetchComponentData", (request, response) -> {
            String applicationName = request.queryParams("applicationName");
            String customerName = request.queryParams("customerName");
            String serverName = request.queryParams("serverName");

            // Fetch data from three different tables
            List<ComponentData> appConfigData = fetchComponentDataFromDB(applicationName, customerName, serverName, "AppConfig");
            List<ComponentData> appConfigCustomData = fetchComponentDataFromDB(applicationName, customerName, serverName, "AppConfigCustom");
            List<ComponentData> appConfigThreadsData = fetchComponentDataFromDB(applicationName, customerName, serverName, "AppConfigThreads");

            // Prepare a response object with all three lists
            ComponentDataResponse responseData = new ComponentDataResponse(appConfigData, appConfigCustomData, appConfigThreadsData);

            // Convert the response to JSON
            Gson gson = new Gson();
            response.type("application/json");
            return gson.toJson(responseData);
        });
post("/updateConfig", (Request request, Response response) -> {
    response.type("application/json");

    // Parse JSON request
    JsonObject jsonObject = gson.fromJson(request.body(), JsonObject.class);
    String table = jsonObject.get("table").getAsString();
    JsonArray data = jsonObject.getAsJsonArray("data");

    String sql;

    try (Connection conn = getConnection()) {
        for (int i = 0; i < data.size(); i++) {
            JsonObject row = data.get(i).getAsJsonObject();

            // Extract values from JSON object
            String componentName = row.get("componentName").getAsString();
            String path = row.get("path").getAsString();
            String filename = row.get("filename").getAsString();
            String specificTag = row.has("specificTag") ? row.get("specificTag").getAsString() : "";

            // Initialize referenceKeys
            List<String> referenceKeys = new ArrayList<>();
            if (row.has("referenceKey")) {
                String referenceKey = row.get("referenceKey").getAsString();
                // Split the referenceKey by commas
                String[] splitKeys = referenceKey.split(",");
                // Add each split key to the list after trimming whitespace
                for (String key : splitKeys) {
                    referenceKeys.add(key.trim());
                }
            }

            String modifiedReferenceKey = "";

            // Modify values based on table
            String modifiedPath = path;

            if (!filename.isEmpty()) {
                // Append filename to path
                modifiedPath = path + "/" + filename;
            }

            // Handle specific logic for custom table (appConfigCustomTable)
            if ("appConfigCustomTable".equalsIgnoreCase(table)) {
                JsonArray jsonArray = new JsonArray();

                // Add the specificTag if present
                if (specificTag != null && !specificTag.isEmpty()) {
                    jsonArray.add(specificTag);
                }

                // Append all referenceKeys to the JSON array
                for (String key : referenceKeys) {
                    jsonArray.add(key);
                }

                // Convert the JSON array to a string
                modifiedReferenceKey = jsonArray.toString();
            } else if ("appConfigTable".equalsIgnoreCase(table)) {
                // For appConfigTable, format as a JSON array like ["-Xms", "-Xmx"]
                JsonArray jsonArray = new JsonArray();
                for (String key : referenceKeys) {
                    jsonArray.add(key);
                }
                modifiedReferenceKey = jsonArray.toString(); // Convert to string
            } else if ("appConfigThreadsTable".equalsIgnoreCase(table)) {
                // For appConfigThreadsTable, handle referenceKey as a simple string (e.g., "numThreads")
                if (!referenceKeys.isEmpty()) {
                    modifiedReferenceKey = referenceKeys.get(0); // Get the first reference key
                } else {
                    modifiedReferenceKey = ""; // Handle empty case
                }
            }

            System.out.println("ReferenceKey to be updated: " + modifiedReferenceKey);

            // Apply the specificTag to the path if necessary
            if ("appConfigTable".equalsIgnoreCase(table) || "appConfigThreadsTable".equalsIgnoreCase(table)) {
                if (specificTag != null && !specificTag.isEmpty()) {
                    modifiedPath = modifiedPath + "#" + specificTag;
                }
            }

            // Set the SQL query based on the table name
            switch (table) {
                case "appConfigTable":
                    sql = "UPDATE AppConfig SET COMPONENT_NAME = ?, PATH = ?, REFERENCE_KEY = ? WHERE TempID = ?";
                    break;
                case "appConfigThreadsTable":
                    sql = "UPDATE AppConfigThreads SET COMPONENT_NAME = ?, PATH = ?, REFERENCE_KEY = ? WHERE TempID = ?";
                    break;
                case "appConfigCustomTable":
                    sql = "UPDATE AppConfigCustom SET COMPONENT_NAME = ?, PATH = ?, REFERENCE_KEY = ? WHERE TempID = ?";
                    break;
                default:
                    throw new SQLException("Unknown table: " + table);
            }

            // Execute the update
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, componentName);
                pstmt.setString(2, modifiedPath);
                pstmt.setString(3, modifiedReferenceKey);
                pstmt.setInt(4, row.get("tempId").getAsInt()); // Assuming there's an ID field
                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new SQLException("Update failed: No rows affected for TempID " + row.get("tempId").getAsInt());
                }
            }
        }

        // Return success response
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("status", "success");
        return gson.toJson(successResponse);

    } catch (SQLException e) {
        // Handle SQL exceptions
        e.printStackTrace();
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("status", "error");
        errorResponse.addProperty("message", e.getMessage());
        return gson.toJson(errorResponse);
    }
});



// Backend method to handle the deletion of records
post("/deleteConfig", (Request request, Response response) -> {
    response.type("application/json");

    // Parse JSON request
    JsonObject jsonObject = gson.fromJson(request.body(), JsonObject.class);
    String table = jsonObject.get("table").getAsString();
    int id = jsonObject.get("id").getAsInt(); // Get the single record ID

    try (Connection conn = getConnection()) {
        String sql = "";

        // Define SQL based on table name
        switch (table) {
            case "appConfigTable":
                sql = "DELETE FROM AppConfig WHERE TempID = ?";
                break;
            case "appConfigThreadsTable":
                sql = "DELETE FROM AppConfigThreads WHERE TempID = ?";
                break;
            case "appConfigCustomTable":
                sql = "DELETE FROM AppConfigCustom WHERE TempID = ?";
                break;
            default:
                throw new SQLException("Unknown table: " + table);
        }

        // Execute the delete
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }

        // Return success response
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("status", "success");
        return gson.toJson(successResponse);

    } catch (SQLException e) {
        // Handle SQL exceptions
        e.printStackTrace();
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("status", "error");
        errorResponse.addProperty("message", e.getMessage());
        return gson.toJson(errorResponse);
    }
});
	
		get("/healthcheck", (req, res) -> "Service is up and running");
		
post("/storeRestartDetails", (req, res) -> {
    res.type("application/json");

    // Parse request body
    Map<String, String> requestBody = gson.fromJson(req.body(), Map.class);
    String customer = requestBody.get("customer");
    String server = requestBody.get("serverName");
    String processName = requestBody.get("processName");
    String commandLine = requestBody.get("commandLine");
    String formattedDateTime = requestBody.get("dateTime");  // Date-time from the frontend
    String status = requestBody.get("status");
    String resultMessage = requestBody.get("resultMessage");
    String timeFormat = requestBody.get("timeFormat");  // 12-hour or 24-hour format

    // Convert dateTime into the correct format based on timeFormat
    LocalDateTime dateTime;
    if ("12-hour".equalsIgnoreCase(timeFormat)) {
        // Parse 12-hour format (e.g., 2024-09-10 10:30 AM)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        dateTime = LocalDateTime.parse(formattedDateTime, formatter);
    } else {
        // Parse 24-hour format (e.g., 2024-09-10 14:30)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        dateTime = LocalDateTime.parse(formattedDateTime, formatter);
    }

    // Store the data into the database
    boolean success = storeRestartDetailsInDatabase(customer, server, processName, commandLine, formattedDateTime, status, resultMessage);

    // Return response
    Map<String, Object> response = new HashMap<>();
    response.put("success", success);
    response.put("message", success ? "Process restart details stored successfully." : "Failed to store process restart details.");
    return gson.toJson(response);
});

get("/fetchRestartDetails", (req, res) -> {
    List<Map<String, String>> restartDetails = fetchRestartDetails();
    res.type("application/json");
    return new Gson().toJson(restartDetails);
});

     get("/getRestartDetails", (req, res) -> {
            res.type("application/json");
 
            // Fetch restart details from the database
            List<Map<String, String>> restartDetails = getRestartDetailsFromDatabase();
 
            // Return the fetched details as JSON
            return new Gson().toJson(restartDetails);
        });
		
           post("/updateRecords", (req, res) -> {
        res.type("application/json");
        try {
            // Parse the incoming JSON request
            List<Map<String, String>> records = gson.fromJson(req.body(), new TypeToken<List<Map<String, String>>>() {}.getType());

            boolean allUpdated = true;
            for (Map<String, String> record : records) {
                String customerName = record.get("customerName");
                String serverName = record.get("serverName");
                String processName = record.get("processName");
                String commandLine = record.get("commandLine");
                String restartDate = record.get("restartDate");
                String restartTime = record.get("restartTime");

                String formattedDateTime = restartDate + " " + restartTime;

                // Call your method to update the record in the database
                boolean success = updateRecordInDatabase(customerName, serverName, processName, commandLine, formattedDateTime);
                if (!success) {
                    allUpdated = false;
                }
            }

            // Prepare response
            Map<String, String> response = new HashMap<>();
            response.put("success", String.valueOf(allUpdated));
            response.put("message", allUpdated ? "Records updated successfully" : "Error updating some records");

            return gson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("success", "false");
            errorResponse.put("message", "Error processing request");
            return gson.toJson(errorResponse);
        }
    });

    // Route to delete a record
    post("/deleteRecord", (req, res) -> {
        res.type("application/json");
        try {
            // Parse the incoming JSON request
            List<Map<String, String>> records = gson.fromJson(req.body(), new TypeToken<List<Map<String, String>>>() {}.getType());

            // Since you expect a single record, let's handle it this way:
            if (!records.isEmpty()) {
                Map<String, String> record = records.get(0);  // Get the first record
                String customerName = record.get("customerName");
                String serverName = record.get("serverName");
                String processName = record.get("processName");

                // Call your method to delete the record from the database
                boolean success = deleteRecordFromDatabase(customerName, serverName, processName);

                // Prepare response
                Map<String, String> response = new HashMap<>();
                response.put("success", String.valueOf(success));
                response.put("message", success ? "Record deleted successfully" : "Failed to delete record");

                return gson.toJson(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("success", "false");
                errorResponse.put("message", "No records found in request");
                return gson.toJson(errorResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("success", "false");
            errorResponse.put("message", "Error processing request");
            return gson.toJson(errorResponse);
        }
    });
	
	get("/fetchRestartHistoryDetails", (req, res) -> {
    List<Map<String, String>> restartHistoryDetails = fetchRestartHistoryDetails();
    res.type("application/json");
    return new Gson().toJson(restartHistoryDetails);
});

get("/fetchComponentsMemory", (Request req, Response res) -> {
    String selectedApplicationname = req.queryParams("selectedApplicationname"); // Fetching application name from the query parameters

    JsonObject responseJson = new JsonObject(); // Create a JSON object to hold the response

    // Check if the 'selectedApplicationname' parameter is present and valid
    if (selectedApplicationname == null || selectedApplicationname.isEmpty()) {
        return "{\"error\": \"Selected application parameter missing or empty\"}";
    }

    try (Connection connection = DriverManager.getConnection(
            "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

        // Fetch components based on the selected application name
        JsonArray componentsArray = fetchComponentsMemoryDetails(connection, selectedApplicationname);
        responseJson.add("components", componentsArray); // Add the components to the response JSON

        System.out.println("Response JSON: " + responseJson.toString());  // Log the response JSON

    } catch (Exception e) {
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"Error retrieving data: " + e.getMessage() + "\"}";
    }

    res.type("application/json");
    return responseJson.toString();
});

// Spark Route to handle the GET request and return the data as JSON
get("/fetchHostedDetails", (req, res) -> {
    String componentName = req.queryParams("component");
    List<Map<String, String>> hostDetails = fetchHostedDetails(componentName);
    res.type("application/json");
    return new Gson().toJson(hostDetails);
});



post("/saveComponentDetails", (request, response) -> {
            // Parse the request body as JSON
            Map<String, Object> requestBody = gson.fromJson(request.body(), Map.class);
            boolean isSuccess = false;  // Initialize success flag

            // Extract necessary values from the request body
            String configType = (String) requestBody.get("configType");
            String customerName = (String) requestBody.get("selectedCustomer");
            String serverName = (String) requestBody.get("selectedServer");
            String path = (String) requestBody.get("path");
            String componentName = (String) requestBody.get("selectedComponent");
            String appName = (String) requestBody.get("applicationName");
			String filename = (String) requestBody.get("filename");

            // Get referenceKey and specificTag from the request body
            List<String> referenceKeys = (List<String>) requestBody.get("referenceKey");
            String specificTag = (String) requestBody.get("specificTag");

            // Modified path and reference key variables
            String modifiedPath = path + "/" + filename ;
			System.out.println(modifiedPath);
			System.out.println(appName);
            String modifiedReferenceKey = gson.toJson(referenceKeys);

            // Handle configType-specific logic
            if ("thread".equalsIgnoreCase(configType)) {
                // For 'thread' config, take the first value of referenceKeys and store as a single string
                if (!referenceKeys.isEmpty()) {
                    modifiedReferenceKey = referenceKeys.get(0);
                }
            } else if ("custom".equalsIgnoreCase(configType)) {
                // For 'custom' config, format as JSON array and add merging logic
                if (specificTag != null && !specificTag.isEmpty()) {
                    JsonArray jsonArray = new JsonArray();
                    
                    // Add the specificTag to the JSON array
                    jsonArray.add(specificTag);
                    
                    // Add existing referenceKeys to the JSON array
                    for (String key : referenceKeys) {
                        jsonArray.add(key);
                    }

                    // Convert the JSON array to a string
                    modifiedReferenceKey = jsonArray.toString();
                } else {
                    modifiedReferenceKey = gson.toJson(referenceKeys);
                }
            }

            // Apply specificTag to path for 'memory' and 'thread' config types
            if ("memory".equalsIgnoreCase(configType) || "thread".equalsIgnoreCase(configType)) {
                if (specificTag != null && !specificTag.isEmpty()) {
                    modifiedPath = path + "#" + specificTag;
                }
            }

            // Prepare the SQL query based on the config type
            String sql;
            switch (configType) {
                case "memory":
                    sql = "INSERT INTO AppConfig (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
                    break;
                case "thread":
                    sql = "INSERT INTO AppConfigThreads (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
                    break;
                case "custom":
                    sql = "INSERT INTO AppConfigCustom (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
                    break;
                default:
                    response.status(400);  // Bad Request for unknown config type
                    return gson.toJson("Unknown config type: " + configType);
            }

            // Execute the prepared statement
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, customerName);
                pstmt.setString(2, serverName);
                pstmt.setString(3, modifiedPath);
                pstmt.setString(4, componentName);
                pstmt.setString(5, modifiedReferenceKey);
                pstmt.setString(6, appName);
                pstmt.executeUpdate();
                isSuccess = true;  // Mark as successful if no exception occurs
            } catch (SQLException e) {
                e.printStackTrace(); // Log the error for debugging purposes
                response.status(500);  // Internal Server Error for database errors
                return gson.toJson("Error saving component details: " + e.getMessage());
            }

            // Set response based on success flag
            if (isSuccess) {
                response.status(200);  // Success
                return gson.toJson("Component details saved successfully.");
            } else {
                response.status(500);  // Failure
                return gson.toJson("Failed to save component details.");
            }
        });
		
		get("/fetchHostedComponentDetails", (req, res) -> {
    String applicationName = req.queryParams("application_name");
    List<Map<String, String>> hostComponentDetailsList = fetchHostedComponentDetails(applicationName);
    res.type("application/json");
    return new Gson().toJson(hostComponentDetailsList);
});

post("/updateComponentConfig", (Request request, Response response) -> {
    response.type("application/json");

    // Parse JSON request
    JsonObject jsonObject = gson.fromJson(request.body(), JsonObject.class);
    JsonArray data = jsonObject.getAsJsonArray("data"); // Get data array

    String sql;

    try (Connection conn = getConnection()) {
        for (JsonElement element : data) {
            JsonObject row = element.getAsJsonObject();

            // Extract values from JSON object
            String componentName = row.get("componentName").getAsString();
            String path = row.get("path").getAsString();
            String filename = row.get("filename").getAsString();
            String specificTag = row.get("specificTag").getAsString();
            String referenceKey = row.has("referenceKey") ? row.get("referenceKey").getAsString() : "";
            int tempId = row.get("tempId").getAsInt(); // Assuming there's an ID field

            // Set the SQL query for the AppHost table
            sql = "UPDATE AppHost SET COMPONENT_NAME = ?, PATH = ?, FILENAME = ?, SPECIFIC_TAG = ?, REFERENCE_KEY = ? WHERE TEMP_ID = ?";

            // Execute the update
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, componentName);
                pstmt.setString(2, path);
                pstmt.setString(3, filename);
                pstmt.setString(4, specificTag);
                pstmt.setString(5, referenceKey);
                pstmt.setInt(6, tempId);

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new SQLException("Update failed: No rows affected for TempID " + tempId);
                }
            }
        }

        // Return success response
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("status", "success");
        return gson.toJson(successResponse);

    } catch (SQLException e) {
        // Handle SQL exceptions
        e.printStackTrace();
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("status", "error");
        errorResponse.addProperty("message", e.getMessage());
        return gson.toJson(errorResponse);
    }
});





post("/deleteComponentConfig", (Request request, Response response) -> {
    response.type("application/json");

    // Parse JSON request
    JsonObject jsonObject = gson.fromJson(request.body(), JsonObject.class);
    int id = jsonObject.get("id").getAsInt(); // Get the record ID

    try (Connection conn = getConnection()) {
        // SQL query to delete from AppHost table
        String sql = "DELETE FROM AppHost WHERE TEMP_ID = ?";

        // Execute the delete operation
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted == 0) {
                throw new SQLException("Delete failed: No rows affected for TEMP_ID " + id);
            }
        }

        // Return success response
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("status", "success");
        return gson.toJson(successResponse);

    } catch (SQLException e) {
        // Handle SQL exceptions
        e.printStackTrace();
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("status", "error");
        errorResponse.addProperty("message", e.getMessage());
        return gson.toJson(errorResponse);
    }
});

post("/saveComponentAppDetails", (Request request, Response response) -> {
    response.type("application/json");

    // Parse the request body as JSON
    JsonObject jsonObject = gson.fromJson(request.body(), JsonObject.class);

    // Extract necessary values from the request body
    String configType = jsonObject.get("configType").getAsString();
    String path = jsonObject.get("path").getAsString();
    String componentName = jsonObject.get("selectedComponent").getAsString();
    String appName = jsonObject.get("applicationName").getAsString();
    String filename = jsonObject.get("filename").getAsString();
    String specificTag = jsonObject.get("specificTag").getAsString();

    // Convert referenceKey list (array) to a comma-separated string
    JsonArray referenceKeysArray = jsonObject.getAsJsonArray("referenceKey");
    StringBuilder referenceKeysBuilder = new StringBuilder();
    for (JsonElement key : referenceKeysArray) {
        if (referenceKeysBuilder.length() > 0) {
            referenceKeysBuilder.append(",");
        }
        referenceKeysBuilder.append(key.getAsString());
    }
    String modifiedReferenceKey = referenceKeysBuilder.toString();
	System.out.println(configType);
    
    // Prepare SQL query
    String sql = "INSERT INTO AppHost (PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME, SPECIFIC_TAG,FILENAME,CONFIG_TYPE) VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, path);
        pstmt.setString(2, componentName);
        pstmt.setString(3, modifiedReferenceKey); // Store referenceKey as a comma-separated string
        pstmt.setString(4, appName);
        pstmt.setString(5, specificTag);
		pstmt.setString(6, filename);
		pstmt.setString(7, configType);

        pstmt.executeUpdate();
        response.status(200);  // Success
        return gson.toJson("Component details saved successfully.");
    } catch (SQLException e) {
        e.printStackTrace(); // Log the error
        response.status(500);  // Internal Server Error
        return gson.toJson("Error saving component details: " + e.getMessage());
    }
});



}

private static List<Map<String, String>> fetchHostedComponentDetails(String applicationName) throws SQLException {
    List<Map<String, String>> hostComponentDetailsList = new ArrayList<>();
    String sql = "SELECT COMPONENT_NAME, PATH, FILENAME, SPECIFIC_TAG, REFERENCE_KEY, CONFIG_TYPE, TEMP_ID FROM [DBMonitor].[dbo].[AppHost] WHERE APPLICATION_NAME = ?";

    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        // Set the applicationName parameter in the SQL query
        stmt.setString(1, applicationName);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> hostComponentDetails = new HashMap<>();
				 hostComponentDetails.put("componentName", rs.getString("COMPONENT_NAME"));
                hostComponentDetails.put("path", rs.getString("PATH"));
                hostComponentDetails.put("filename", rs.getString("FILENAME"));
                hostComponentDetails.put("specificTag", rs.getString("SPECIFIC_TAG"));
                hostComponentDetails.put("referenceKey", rs.getString("REFERENCE_KEY"));
				hostComponentDetails.put("configType", rs.getString("CONFIG_TYPE"));
				hostComponentDetails.put("tempId", rs.getString("TEMP_ID"));
                hostComponentDetailsList.add(hostComponentDetails);
            }
        }
    }
    return hostComponentDetailsList;
}


private static List<Map<String, String>> fetchHostedDetails(String componentName) throws SQLException {
    List<Map<String, String>> hostDetailsList = new ArrayList<>();
    String sql = "SELECT PATH, FILENAME, SPECIFIC_TAG, REFERENCE_KEY FROM [DBMonitor].[dbo].[AppHost] WHERE COMPONENT_NAME = ?";

    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        // Set the componentName parameter in the SQL query
        stmt.setString(1, componentName);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> hostDetails = new HashMap<>();
                hostDetails.put("path", rs.getString("PATH"));
                hostDetails.put("filename", rs.getString("FILENAME"));
                hostDetails.put("specificTag", rs.getString("SPECIFIC_TAG"));
                hostDetails.put("referenceKey", rs.getString("REFERENCE_KEY"));
                hostDetailsList.add(hostDetails);
            }
        }
    }
    return hostDetailsList;
}



 // Method to fetch memory components for a given application
private static JsonArray fetchComponentsMemoryDetails(Connection connection, String selectedApplicationname) throws SQLException {
    String sqlQuery = "SELECT DISTINCT COMPONENT_NAME FROM [DBMonitor].[dbo].[AppHost] WHERE APPLICATION_NAME = ?";
    JsonArray jsonArray = new JsonArray();

    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
        preparedStatement.setString(1, selectedApplicationname); // Set the application name in the query

        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                // Add the fetched component name to the JSON array
                jsonArray.add(resultSet.getString("COMPONENT_NAME"));
            }
        }
    }
    return jsonArray; // Return the array of components
}


private static  List<Map<String, String>> fetchRestartHistoryDetails() throws SQLException {
    List<Map<String, String>> restartHistoryDetailsList = new ArrayList<>();
    String sql = "SELECT customerName, serverName, processName, commandLine, scheduledDateTime, status, resultMessage FROM RestartSchedule";
 
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
 
        while (rs.next()) {
            Map<String, String> restartHistoryDetails = new HashMap<>();
            restartHistoryDetails.put("customerName", rs.getString("customerName"));
            restartHistoryDetails.put("serverName", rs.getString("serverName"));
            restartHistoryDetails.put("processName", rs.getString("processName"));
            restartHistoryDetails.put("commandLine", rs.getString("commandLine"));
            restartHistoryDetails.put("scheduledDateTime", rs.getString("scheduledDateTime"));
            restartHistoryDetails.put("status", rs.getString("status"));
			restartHistoryDetails.put("resultMessage", rs.getString("resultMessage"));
            restartHistoryDetailsList.add(restartHistoryDetails);
        }
    }
 
    return restartHistoryDetailsList;
}

private static boolean updateRecordInDatabase(String customerName, String serverName, String processName, String commandLine, String formattedDateTime) throws SQLException {
        String sql = "UPDATE RestartSchedule SET commandLine = ?, scheduledDateTime = ? WHERE customerName = ? AND serverName = ? AND processName = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, commandLine);
            stmt.setString(2, formattedDateTime);
            stmt.setString(3, customerName);
            stmt.setString(4, serverName);
            stmt.setString(5, processName);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

private static boolean deleteRecordFromDatabase(String customerName, String serverName, String processName) throws SQLException {
        String sql = "DELETE FROM RestartSchedule WHERE customerName = ? AND serverName = ? AND processName = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerName);
            stmt.setString(2, serverName);
            stmt.setString(3, processName);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

private static List<Map<String, String>> getRestartDetailsFromDatabase() {
        List<Map<String, String>> restartList = new ArrayList<>();
        String sql = "SELECT customerName, serverName, processName, commandLine, scheduledDateTime, status FROM RestartSchedule";
 
        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
 
            while (rs.next()) {
                Map<String, String> restartDetails = new HashMap<>();
                restartDetails.put("customerName", rs.getString("customerName"));
                restartDetails.put("serverName", rs.getString("serverName"));
                restartDetails.put("processName", rs.getString("processName"));
                restartDetails.put("commandLine", rs.getString("commandLine"));
 
                // Split datetime into date and time
                String[] dateTime = rs.getString("scheduledDateTime").split(" ");
                restartDetails.put("restartDate", dateTime[0]);
                restartDetails.put("restartTime", dateTime[1]);
 
                restartDetails.put("status", rs.getString("status"));
 
                restartList.add(restartDetails);
            }
 
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return restartList;
    }


   
private static  List<Map<String, String>> fetchRestartDetails() throws SQLException {
    List<Map<String, String>> restartDetailsList = new ArrayList<>();
    String sql = "SELECT customerName, serverName, processName, commandLine, scheduledDateTime FROM RestartSchedule where status='Scheduled'";
 
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
 
        while (rs.next()) {
            Map<String, String> restartDetails = new HashMap<>();
            restartDetails.put("customerName", rs.getString("customerName"));
            restartDetails.put("serverName", rs.getString("serverName"));
            restartDetails.put("processName", rs.getString("processName"));
            restartDetails.put("commandLine", rs.getString("commandLine"));
            restartDetails.put("scheduledDateTime", rs.getString("scheduledDateTime"));
            restartDetailsList.add(restartDetails);
        }
    }
 
    return restartDetailsList;
}
   
	
         private static void checkScheduledRestarts() {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {
        // Get the current system time
        LocalDateTime now = LocalDateTime.now();

        // Query for any scheduled restarts that are due
        String sql = "SELECT customerName, serverName, processName, commandLine, scheduledDateTime FROM RestartSchedule WHERE scheduledDateTime <= ? AND status = 'Scheduled'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Format the current time according to the system's 24-hour format for querying the database
            DateTimeFormatter systemFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");  // Assuming system uses 24-hour format
            stmt.setString(1, now.format(systemFormatter));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String customer = rs.getString("customerName");
                    String remoteServer = rs.getString("serverName");
                    String processName = rs.getString("processName");
                    String commandLine = rs.getString("commandLine");
                    String scheduledDateTimeStr = rs.getString("scheduledDateTime");

                    // Parse the scheduledDateTime according to the system's 24-hour format
                    LocalDateTime scheduledTime;
                    try {
                        scheduledTime = LocalDateTime.parse(scheduledDateTimeStr, systemFormatter);
                    } catch (DateTimeParseException e) {
                        System.err.println("Error parsing scheduled date-time: " + e.getMessage());
                        continue;  // Skip this record if there's a parsing error
                    }

                    System.out.println("Scheduled Time: " + scheduledTime);

                    // Check if the current time matches or exceeds the scheduled time
                    if (now.isEqual(scheduledTime) || now.isAfter(scheduledTime)) {
                        // Kill the process using killRemoteProcess and get the result
                        ProcessResult restartResult = killRemoteProcess(remoteServer, user_name, pass_word, "src/kill-remote-process.ps1", processName, commandLine);

                        // Update the status in the database based on the result
                        String status = restartResult.isSuccess() ? "Completed" : "Failed";
                        String resultMessage = restartResult.isSuccess() ? "Process restarted successfully" : "Failed to restart process: " + restartResult.getMessage();
                        
                        // Update the restart status in the database
                        updateRestartStatusInDatabase(customer, remoteServer, processName, status, resultMessage);
                    }
                }
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        // Log error (optional)
    }
}

	
private static boolean storeRestartDetailsInDatabase(String customer, String server, String processName, String commandLine, String formattedDateTime, String status, String resultMessage) {
    String sql = "INSERT INTO RestartSchedule (customerName, serverName, processName, commandLine, scheduledDateTime, status, resultMessage) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
         
        stmt.setString(1, customer);
        stmt.setString(2, server);
        stmt.setString(3, processName);
        stmt.setString(4, commandLine);
        stmt.setString(5, formattedDateTime);  // Store the date-time as entered
        stmt.setString(6, status);
        stmt.setString(7, resultMessage);

        // Check if the insert was successful by verifying that at least one row was affected
        int rowsAffected = stmt.executeUpdate();
        
        // Return true if the insert was successful, otherwise false
        return rowsAffected > 0;
        
    } catch (SQLException e) {
        e.printStackTrace();
        return false;  // Return false in case of an error
    }
}


    // Method to update restart status in the database
    private static void updateRestartStatusInDatabase(String customer, String server, String processName, String status, String resultMessage) throws SQLException {
        String sql = "UPDATE RestartSchedule SET status = ?, resultMessage = ? WHERE customerName = ? AND serverName = ? AND processName = ?";
        
        // Use the correct JDBC connection format for SQL Server
        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, status);
            stmt.setString(2, resultMessage);
            stmt.setString(3, customer);
            stmt.setString(4, server);
            stmt.setString(5, processName);
            stmt.executeUpdate();
        }catch (SQLException e) {
            throw new SQLException("Error updating restart status in database: " + e.getMessage());
        }
    }

private static List<ComponentData> fetchComponentDataFromDB(String applicationName, String customerName, String serverName, String tableName) {
    List<ComponentData> componentDataList = new ArrayList<>();
    String url = "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE;

    try (Connection conn = DriverManager.getConnection(url, USERNAME, PASSWORD)) {
        // Include the 'Tempid' field in the query
        String sql = String.format("SELECT Tempid, component_name, path, reference_key FROM %s WHERE application_name = ? AND customer_name = ? AND server_name = ?", tableName);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, applicationName);
            stmt.setString(2, customerName);
            stmt.setString(3, serverName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ComponentData componentData = new ComponentData();

                    // Set the tempId (which is the record id in the database)
                    componentData.setTempId(rs.getInt("Tempid"));
                    componentData.setComponentName(rs.getString("component_name"));
                    
                    String path = rs.getString("path");

                    if (tableName.equals("AppConfig") || tableName.equals("AppConfigThreads")) {
                        // Handle path splitting for AppConfig and AppConfigThreads
                        String referenceKey = rs.getString("reference_key");
                        componentData.setReferenceKey(referenceKey);

                        if (path != null) {
                            // Check if path contains "#"
                            String[] pathParts;
                            if (path.contains("#")) {
                                // Split by "#", and the part after "#" is the specific tag
                                String[] splitByHash = path.split("#", 2);
                                pathParts = splitPathForAppConfig(splitByHash[0]); // Split the path part
                                componentData.setSpecificTag(splitByHash[1]); // The part after "#" is the specific tag
                            } else {
                                // If no "#", just split normally
                                pathParts = splitPathForAppConfig(path);
                                componentData.setSpecificTag(""); // No specific tag
                            }

                            // Set path and filename based on split parts
                            componentData.setPath(pathParts[0]);
                            componentData.setFilename(pathParts[1]);
                        } else {
                            componentData.setPath("");
                            componentData.setFilename("");
                            componentData.setSpecificTag("");
                        }

                    } else if (tableName.equals("AppConfigCustom")) {
                        // Handle splitting logic for reference_key that is a JSON array
                        String referenceKeyJson = rs.getString("reference_key");

                        if (referenceKeyJson != null) {
                            // Parse the JSON array
                            Gson gson = new Gson();
                            String[] refArray = gson.fromJson(referenceKeyJson, String[].class);

                            if (refArray != null && refArray.length == 2) {
                                // Store the first element in specificTag and the second in referenceKey
                                componentData.setSpecificTag(refArray[0]);
                                componentData.setReferenceKey(refArray[1]);
                            } else {
                                // Handle cases where the array is not of expected length
                                componentData.setSpecificTag("");
                                componentData.setReferenceKey(referenceKeyJson); // or handle as needed
                            }
                        }

                        if (path != null) {
                            // Handle path splitting for AppConfigCustom
                            String[] pathParts = splitPathForCustom(path);
                            componentData.setPath(pathParts[0]);
                            componentData.setFilename(pathParts[1]);
                        } else {
                            componentData.setPath("");
                            componentData.setFilename("");
                        }
                    }

                    // Add the componentData to the list
                    componentDataList.add(componentData);
                }
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return componentDataList;
}


    private static String[] splitPathForAppConfig(String path) {
        String specificTag = null;
        String filename = null;

        // Check for '#' in the path
        if (path.contains("#")) {
            int hashIndex = path.indexOf("#");
            specificTag = path.substring(hashIndex + 1);
            path = path.substring(0, hashIndex);
        }

        // Split by the last occurrence of '/'
        int lastSlashIndex = path.lastIndexOf("/");
        if (lastSlashIndex != -1) {
            filename = path.substring(lastSlashIndex + 1);
            path = path.substring(0, lastSlashIndex);
        }

        return new String[]{path, filename, specificTag};
    }

    private static String[] splitPathForCustom(String path) {
    String filename = null;
    String directoryPath = path;

    // Find the last occurrence of '/'
    int lastSlashIndex = path.lastIndexOf("/");
    if (lastSlashIndex != -1) {
        // Extract filename (right side of the last '/')
        filename = path.substring(lastSlashIndex + 1);

        // Extract path (left side of the last '/')
        directoryPath = path.substring(0, lastSlashIndex);
    }

    return new String[]{directoryPath, filename};
}


    private static String[] splitReferenceKeyForCustom(String referenceKey) {
        String specificTag = null;

        // Split by ','
        if (referenceKey.contains(",")) {
            int commaIndex = referenceKey.indexOf(",");
            specificTag = referenceKey.substring(0, commaIndex);
            referenceKey = referenceKey.substring(commaIndex + 1);
        }

        return new String[]{specificTag, referenceKey};
    }

    // Response model class to hold data from all three tables
    public static class ComponentDataResponse {
        private List<ComponentData> appConfigData;
        private List<ComponentData> appConfigCustomData;
        private List<ComponentData> appConfigThreadsData;

        public ComponentDataResponse(List<ComponentData> appConfigData, List<ComponentData> appConfigCustomData, List<ComponentData> appConfigThreadsData) {
            this.appConfigData = appConfigData;
            this.appConfigCustomData = appConfigCustomData;
            this.appConfigThreadsData = appConfigThreadsData;
        }

        public List<ComponentData> getAppConfigData() {
            return appConfigData;
        }

        public List<ComponentData> getAppConfigCustomData() {
            return appConfigCustomData;
        }

        public List<ComponentData> getAppConfigThreadsData() {
            return appConfigThreadsData;
        }
    }

    // ComponentData model class
    public static class ComponentData {
    private int tempId;  // This field will hold the unique ID for the record
    private String componentName;
    private String path;
    private String filename;
    private String specificTag;
    private String referenceKey;

    public int getTempId() {
        return tempId;
    }

    public void setTempId(int tempId) {
        this.tempId = tempId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSpecificTag() {
        return specificTag;
    }

    public void setSpecificTag(String specificTag) {
        this.specificTag = specificTag;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }
}
    private static ArrayList<String> fetchServerFromDB(Connection connection, String selectedCustomer) throws SQLException {
        ArrayList<String> serverDetails = new ArrayList<>();

        // SQL query to fetch distinct SERVER_NAME from three tables using UNION
        String serverDetailsQuery = 
            "SELECT DISTINCT SERVER_NAME FROM [DBMonitor].[dbo].[AppConfigCustom] WHERE CUSTOMER_NAME = ? " +
            "UNION " +
            "SELECT DISTINCT SERVER_NAME FROM [DBMonitor].[dbo].[AppConfig] WHERE CUSTOMER_NAME = ? " +
            "UNION " +
            "SELECT DISTINCT SERVER_NAME FROM [DBMonitor].[dbo].[AppConfigThreads] WHERE CUSTOMER_NAME = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(serverDetailsQuery)) {
            // Set the same customer name parameter for all three queries
            preparedStatement.setString(1, selectedCustomer);
            preparedStatement.setString(2, selectedCustomer);
            preparedStatement.setString(3, selectedCustomer);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    serverDetails.add(resultSet.getString("SERVER_NAME"));
                }
            }
        }
        return serverDetails;
    }
	
	private static List<String> fetchCustomerDetailsFromDB(Connection connection, String selectedApplicationname) throws SQLException {
    List<String> customerDetails = new ArrayList<>();
    
    // SQL query to fetch distinct CUSTOMER_NAME from three tables using UNION
    String customerDetailsQuery = 
        "SELECT DISTINCT CUSTOMER_NAME FROM [DBMonitor].[dbo].[AppConfigCustom] WHERE APPLICATION_NAME = ? " +
        "UNION " +
        "SELECT DISTINCT CUSTOMER_NAME FROM [DBMonitor].[dbo].[AppConfig] WHERE APPLICATION_NAME = ? " +
        "UNION " +
        "SELECT DISTINCT CUSTOMER_NAME FROM [DBMonitor].[dbo].[AppConfigThreads] WHERE APPLICATION_NAME = ?";

    try (PreparedStatement preparedStatement = connection.prepareStatement(customerDetailsQuery)) {
        preparedStatement.setString(1, selectedApplicationname);
        preparedStatement.setString(2, selectedApplicationname);
        preparedStatement.setString(3, selectedApplicationname);

        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                customerDetails.add(resultSet.getString("CUSTOMER_NAME"));
            }
        }
    }
    return customerDetails;
}

	
	private static ArrayList<String> fetchDistinctApplicationNames(Connection connection) throws SQLException {
    ArrayList<String> applicationNames = new ArrayList<>();
    
    // SQL query to fetch distinct APPLICATION_NAME from three tables using UNION
    String applicationNamesQuery = 
        "SELECT DISTINCT APPLICATION_NAME FROM [DBMonitor].[dbo].[AppConfigCustom] " +
        "UNION " +
        "SELECT DISTINCT APPLICATION_NAME FROM [DBMonitor].[dbo].[AppConfig] " +
        "UNION " +
        "SELECT DISTINCT APPLICATION_NAME FROM [DBMonitor].[dbo].[AppConfigThreads]";
    
    try (PreparedStatement preparedStatement = connection.prepareStatement(applicationNamesQuery)) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                applicationNames.add(resultSet.getString("APPLICATION_NAME"));
            }
        }
    }
    
    return applicationNames;
}
	
	// Get the component detail for a specific component name
private static Map<String, Object> getComponentDetail(ArrayList<Map<String, Object>> componentDetails, String componentName) {
    for (Map<String, Object> compInfo : componentDetails) {
        if (componentName.equals(compInfo.get("componentName"))) {
            return compInfo;
        }
    }
    return null; // Return null if no component detail is found
}

    // Insert data into the appropriate table based on the config type
    private static void insertIntoConfigTable(Connection conn, String appName, String customerName, String serverName, Map<String, Object> componentDetail) throws SQLException {
    String configType = (String) componentDetail.get("configType");
    String sql;

    // Prepare modified values based on configType
    String modifiedPath = (String) componentDetail.get("path");
    
    // Convert referenceKey array to JSON string using Gson
    List<String> referenceKeys = (List<String>) componentDetail.get("referenceKey");
    String modifiedReferenceKey = new Gson().toJson(referenceKeys);

    String specificTag = (String) componentDetail.get("specificTag");
    if ("thread".equalsIgnoreCase(configType)) {
    // For 'thread' config, take the first value of referenceKeys and store it as a single string
    modifiedReferenceKey = referenceKeys.get(0); // Assume referenceKeys contains only one element
   
   } else if ("custom".equalsIgnoreCase(configType)) {
        // For 'custom' config, format as JSON array and add merging logic
        if (specificTag != null && !specificTag.isEmpty()) {
             JsonArray jsonArray = new JsonArray();
            
            // Add the specificTag to the JSON array
            jsonArray.add(specificTag);
            
            // Add existing referenceKeys to the JSON array
            for (String key : referenceKeys) {
                jsonArray.add(key);
            }

            // Convert the JSON array to a string
            modifiedReferenceKey = jsonArray.toString();
        } else {
            modifiedReferenceKey = new Gson().toJson(referenceKeys);
        }
    }

    // Apply the specificTag to the path if necessary
    if ("memory".equalsIgnoreCase(configType) || "thread".equalsIgnoreCase(configType)) {
        if (specificTag != null && !specificTag.isEmpty()) {
            modifiedPath = modifiedPath + "#" + specificTag;
        }
    }

    // Set the SQL query based on the config type
    switch (configType) {
        case "memory":
            sql = "INSERT INTO AppConfig (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
            break;
        case "thread":
            sql = "INSERT INTO AppConfigThreads (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
            break;
        case "custom":
            sql = "INSERT INTO AppConfigCustom (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY, APPLICATION_NAME) VALUES (?, ?, ?, ?, ?, ?)";
            break;
        default:
            throw new SQLException("Unknown config type: " + configType);
    }

    // Execute the prepared statement
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, customerName);
        pstmt.setString(2, serverName);
        pstmt.setString(3, modifiedPath);
        pstmt.setString(4, (String) componentDetail.get("componentName"));
        pstmt.setString(5, modifiedReferenceKey);
        pstmt.setString(6, appName);
        pstmt.executeUpdate();
    }
}


    private static List<String> getAssignedServers(List<String> serverNames, String currentCustomerName) {
        List<String> assignedServers = new ArrayList<>();
        String[] tables = {"AppConfig", "AppConfigThreads", "AppConfigCustom"};
        String query = "SELECT SERVER_NAME FROM %s WHERE SERVER_NAME = ? AND CUSTOMER_NAME <> ?";

        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD)) {

            for (String table : tables) {
                String formattedQuery = String.format(query, table);
                try (PreparedStatement preparedStatement = connection.prepareStatement(formattedQuery)) {
                    for (String serverName : serverNames) {
                        preparedStatement.setString(1, serverName);
                        preparedStatement.setString(2, currentCustomerName);

                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                assignedServers.add(serverName);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return assignedServers;
    }

    static class AssignmentResponse {
        private List<String> assignedServers;

        public AssignmentResponse(List<String> assignedServers) {
            this.assignedServers = assignedServers;
        }

        public List<String> getAssignedServers() {
            return assignedServers;
        }
    }

    static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
	private static Map<String, Object> fetchComponentsDetails(String selectedAppname) {
        Map<String, Object> responseMap = new HashMap<>();
        try (Connection connection = getConnection()) {
            ArrayList<Map<String, Object>> componentDetails = fetchComponentsDetailsFromDB(connection, selectedAppname);
            responseMap.put("components", componentDetails);
        } catch (SQLException e) {
            e.printStackTrace();
            responseMap.put("error", "Database error occurred");
        }
        return responseMap;
    }

private static ArrayList<Map<String, Object>> fetchComponentsDetailsFromDB(Connection connection, String selectedAppname) throws SQLException {
    ArrayList<Map<String, Object>> componentDetails = new ArrayList<>();
    String componentDetailsQuery = "SELECT DISTINCT COMPONENT_NAME, CONFIG_TYPE, PATH, FILENAME, SPECIFIC_TAG, REFERENCE_KEY FROM [DBMonitor].[dbo].[AppHost] WHERE APPLICATION_NAME = ?";
    
    try (PreparedStatement preparedStatement = connection.prepareStatement(componentDetailsQuery)) {
        preparedStatement.setString(1, selectedAppname);
        
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Map<String, Object> compInfo = new HashMap<>();
                compInfo.put("componentName", resultSet.getString("COMPONENT_NAME"));
                compInfo.put("configType", resultSet.getString("CONFIG_TYPE"));
                
                // Concatenate path and filename
                String path = resultSet.getString("PATH");
                String filename = resultSet.getString("FILENAME");
                String fullPath = (path != null ? path : "") + "/" + (filename != null ? filename : "");
                
                compInfo.put("path", fullPath);
                compInfo.put("filename", filename); // Optional: keep filename separately if needed
                compInfo.put("specificTag", resultSet.getString("SPECIFIC_TAG"));
                
                // Store the referenceKey in an array structure
                String referenceKey = resultSet.getString("REFERENCE_KEY");
                List<String> referenceKeys = new ArrayList<>();
                if (referenceKey != null && !referenceKey.isEmpty()) {
                    // Add each key to the list, preserving the format
                    referenceKeys = Arrays.asList(referenceKey.split(","));
                }
                compInfo.put("referenceKey", referenceKeys);
                
                componentDetails.add(compInfo);
            }
        }
    }
    return componentDetails;
}


    private static JsonArray fetchApplicationNames(Connection connection) throws SQLException {
    String sqlQuery = "SELECT DISTINCT APPLICATION_NAME FROM AppHost";
    JsonArray jsonArray = new JsonArray();

    try (PreparedStatement statement = connection.prepareStatement(sqlQuery);
         ResultSet resultSet = statement.executeQuery()) {

        while (resultSet.next()) {
            // Add application name directly to the JsonArray
            jsonArray.add(resultSet.getString("APPLICATION_NAME"));
        }
    }
    return jsonArray;
}

   

 
     // Utility method to create response map
    private static Map<String, String> createResponseMap(String status, String message) {
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", status);
        if (message != null) {
            responseMap.put("message", message);
        }
        return responseMap;
    }

    private static boolean fileExists(String pathWithoutHyphen, String serverName, String componentName) {
    String path1 = pathWithoutHyphen;
    if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
        path1 = path1.substring(0, 1) + "$" + path1.substring(2);
    }
    File file = new File("//" + serverName + "/" + path1);
    if (file.exists()) {
        System.out.println("File found: " + file.getPath());
        return true;
    }
    System.out.println("File not found: " + file.getPath());
    return false;
}
 
private static boolean tagExistsInFile(String pathWithoutHyphen, String serverName, String componentName, List<String> referenceKey) {
    String path1 = pathWithoutHyphen;
    try {
        if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
            path1 = path1.substring(0, 1) + "$" + path1.substring(2);
        }
        String filePath = "//" + serverName + "/" + path1;
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        for (String key : referenceKey) {
            if (!content.contains(key)) {
                System.out.println("Key not found: " + key);
                return false;
            }
        }
        return true;
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}
 
private static boolean checkValuesExistence(String pathWithoutHyphen, String serverName, String componentName, List<String> referenceKey, String tag) {
    String path1 = pathWithoutHyphen;
    try {
        if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
            path1 = path1.substring(0, 1) + "$" + path1.substring(2);
        }
        String filePath = "//" + serverName + "/" + path1;
        System.out.println(filePath);
        
        // File existence check
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return false;
        } else {
            System.out.println("File found: " + file.getPath());
        }

        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        Pattern tagPattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher tagMatcher = tagPattern.matcher(content);

        if (tagMatcher.find()) {
            String tagContent = tagMatcher.group(1);

            for (String key : referenceKey) {
                String keyValuePattern = key + "(\\d+[A-Za-z]*)";
                Pattern keyPattern = Pattern.compile(keyValuePattern);
                Matcher keyMatcher = keyPattern.matcher(tagContent);

                boolean keyFound = false;
                while (keyMatcher.find()) {
                    System.out.println(key + ": " + keyMatcher.group(1));
                    keyFound = true;
                }
                if (!keyFound) {
                    System.out.println(key + " not found.");
                    return false;
                }
            }
            return true;
        } else {
            System.out.println("Tag not found: " + tag);
            return false;
        }
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }
}


 private static boolean threadfileExists(String pathWithoutHyphen, String serverName) {
        String path1 = pathWithoutHyphen;
        if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
            path1 = path1.substring(0, 1) + "$" + path1.substring(2);
        }
        File file = new File("//" + serverName + "/" + path1);
        if (file.exists()) {
            System.out.println("File found: " + file.getPath());
            return true;
        }
        System.out.println("File not found: " + file.getPath());
        return false;
    }

    public static boolean tagthreadExistsInFile(String pathWithoutHyphen, String serverName, String threadKey) {
        String path1 = pathWithoutHyphen;
        try {
            // Adjust the path format
            if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
                path1 = path1.substring(0, 1) + "$" + path1.substring(2);
            }
            String filePath = "//" + serverName + "/" + path1;

            // Read the content of the file
            String content = new String(Files.readAllBytes(Paths.get(filePath)));

            Pattern variablePattern = Pattern.compile(Pattern.quote(threadKey) + "\\s*=\\s*\"(\\d+)\"");
            Matcher variableMatcher = variablePattern.matcher(content);

            if (!variableMatcher.find()) {
                System.out.println("Tag not found for key: " + threadKey);
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkValuesthreadExistence(String pathWithoutHyphen, String serverName, String threadKey, String tag) {
        String path1 = pathWithoutHyphen;
        try {
            // Adjust the path format
            if (path1 != null && path1.length() > 2 && path1.charAt(1) == ':') {
                path1 = path1.substring(0, 1) + "$" + path1.substring(2);
            }
            String filePath = "//" + serverName + "/" + path1;
            System.out.println(filePath);

            // File existence check
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File not found: " + filePath);
                return false;
            } else {
                System.out.println("File found: " + file.getPath());
            }

            // Read the content of the file
            String content = new String(Files.readAllBytes(Paths.get(filePath)));

            // Pattern to find the tag and its content
            Pattern tagPattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
            Matcher tagMatcher = tagPattern.matcher(content);

            if (tagMatcher.find()) {
                String tagContent = tagMatcher.group(1);

                // Check the threadKey in the tag content
                Pattern keyPattern = Pattern.compile(Pattern.quote(threadKey) + "\\s*=\\s*\"(\\d+)\"");
                Matcher keyMatcher = keyPattern.matcher(tagContent);

                boolean keyFound = false;
                while (keyMatcher.find()) {
                    System.out.println(threadKey + ": " + keyMatcher.group(1));
                    keyFound = true;
                }
                if (!keyFound) {
                    System.out.println(threadKey + " not found.");
                    return false;
                }
                return true;
            } else {
                System.out.println("Tag not found: " + tag);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
	
	
private static boolean customfileExists(String Path, String serverName) {
    if (Path != null && Path.length() > 2 && Path.charAt(1) == ':') {
        Path = Path.substring(0, 1) + "$" + Path.substring(2);
    }
    
    // Ensure the Path points to an XML file
    if (!Path.endsWith(".xml")) {
        Path += ".xml";
    }
    
    File file = new File("//" + serverName + "/" + Path);
    if (file.exists() && file.isFile()) {
        System.out.println("XML file found: " + file.getPath());
        return true;
    }
    
    System.out.println("XML file not found: " + file.getPath());
    return false;
}


    public static boolean tagCustomExistsInFile(String Path, String serverName, String outerTag, String innerTag) {
        try {
            // Adjust the path format
            if (Path != null && Path.length() > 2 && Path.charAt(1) == ':') {
                Path = Path.substring(0, 1) + "$" + Path.substring(2);
            }
            String filePath = "//" + serverName + "/" + Path;

            // Read the content of the file to ensure it exists
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("File not found: " + filePath);
                return false;
            }

            // Create a DocumentBuilderFactory and set up to parse the XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filePath);
            doc.getDocumentElement().normalize();

            // Create XPath instance
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();

            // Determine the correct XPath expression based on non-empty tags
            String expression;
            if (!outerTag.isEmpty() && !innerTag.isEmpty()) {
                expression = String.format("//%s/%s", outerTag, innerTag);
            } else if (!outerTag.isEmpty()) {
                expression = String.format("//%s", outerTag);
            } else if (!innerTag.isEmpty()) {
                expression = String.format("//%s", innerTag);
            } else {
                throw new IllegalArgumentException("Both outerTag and innerTag cannot be empty.");
            }

            // Compile and evaluate the XPath expression
            XPathExpression xPathExpression = xpath.compile(expression);
            NodeList nodeList = (NodeList) xPathExpression.evaluate(doc, XPathConstants.NODESET);

            // Check if the tag exists
            if (nodeList.getLength() == 0) {
                System.out.println("Tag not found for the provided tags: " + outerTag + "/" + innerTag);
                return false;
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
	
	private static String storeCustomDetailsInDatabase(String server, String database, String username, String password,
                                             String customer, String serverName, String originalPath,
                                             String componentName, List<String> customKey) {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfigCustom (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, customer);
            preparedStatement.setString(2, serverName);
            preparedStatement.setString(3, originalPath);
            preparedStatement.setString(4, componentName);
            preparedStatement.setString(5, new Gson().toJson(customKey)); // Convert list to JSON string
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0 ? "success" : "failure";
        }
    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return "duplicate";
        } else {
            e.printStackTrace();
            return "error";
        }
    }
}


 
private static String storeDetailsInDatabase(String server, String database, String username, String password,
                                             String customer, String serverName, String originalPath,
                                             String componentName, List<String> referenceKey) {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfig (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, customer);
            preparedStatement.setString(2, serverName);
            preparedStatement.setString(3, originalPath);
            preparedStatement.setString(4, componentName);
            preparedStatement.setString(5, new Gson().toJson(referenceKey)); // Convert list to JSON string
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0 ? "success" : "failure";
        }
    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return "duplicate";
        } else {
            e.printStackTrace();
            return "error";
        }
    }
}


private static String storethreadDetailsInDatabase(String server, String database, String username, String password,
                                             String customer, String serverName, String originalPath,
                                             String componentName, String threadKey) {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfigThreads (CUSTOMER_NAME, SERVER_NAME, PATH, COMPONENT_NAME, REFERENCE_KEY) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, customer);
            preparedStatement.setString(2, serverName);
            preparedStatement.setString(3, originalPath);
            preparedStatement.setString(4, componentName);
            preparedStatement.setString(5, threadKey); // Convert list to JSON string
            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0 ? "success" : "failure";
        }
    } catch (SQLException e) {
        if ("23000".equals(e.getSQLState())) {
            return "duplicate";
        } else {
            e.printStackTrace();
            return "error";
        }
    }
}



    private static ArrayList<String> fetchCustomerNames() {
        ArrayList<String> customerNames = new ArrayList<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DISTINCT CUSTOMER_NAME FROM [DBMonitor].[dbo].[AppConfig]")) {
            while (resultSet.next()) {
                customerNames.add(resultSet.getString("CUSTOMER_NAME"));
            }
            System.out.println("Fetched customer names: " + customerNames);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customerNames;
    }

    private static Map<String, Object> fetchServerDetails(String selectedCustomer) {
        Map<String, Object> responseMap = new HashMap<>();
        try (Connection connection = getConnection()) {
            ArrayList<Map<String, Object>> serverDetails = fetchServerDetailsFromDB(connection, selectedCustomer);
            responseMap.put("Servers", serverDetails);
            Map<String, ArrayList<Map<String, String>>> propertiesInfo = fetchServerProperties(connection, selectedCustomer);
            responseMap.put("Properties", propertiesInfo);
        } catch (SQLException e) {
            e.printStackTrace();
            responseMap.put("error", "Database error occurred");
        }
        return responseMap;
    }

    private static ArrayList<Map<String, Object>> fetchServerDetailsFromDB(Connection connection, String selectedCustomer) throws SQLException {
        ArrayList<Map<String, Object>> serverDetails = new ArrayList<>();
        String serverDetailsQuery = "SELECT DISTINCT SERVER_NAME FROM [DBMonitor].[dbo].[AppConfig] WHERE CUSTOMER_NAME = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(serverDetailsQuery)) {
            preparedStatement.setString(1, selectedCustomer);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> serverInfo = new HashMap<>();
                    serverInfo.put("serverName", resultSet.getString("SERVER_NAME"));
                    serverDetails.add(serverInfo);
                }
            }
        }
        return serverDetails;
    }
	
    public static Map<String, Object> fetchProcesses(String selectedServer) {
        Map<String, Object> responseMap = new HashMap<>();
        StringBuilder errorMessages = new StringBuilder();
        String remoteServer = selectedServer;
 
         try {
            // Remote server details
			System.out.println(remoteServer);
            // PowerShell script path
            String scriptPath = "src/fetch_processes.ps1"; // Replace with the actual script path

            // Construct the PowerShell command for remote execution
            String psCommand = String.format(
                "powershell.exe -File \"%s\" -remoteServer \"%s\" -username \"%s\" -password \"%s\"",
                scriptPath, remoteServer, user_name, pass_word);

            // Execute the PowerShell command
            Process process = Runtime.getRuntime().exec(psCommand);

            // Read all output lines from the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Set<String> processNames = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
				   if (line.contains("Error getting owner for process ID") || 
                    line.contains("Process information not available for process ID") || 
                    line.contains("Owner information not available for process ID")) {
                    // Skip these lines
                    continue;
					}else{
                processNames.add(line.trim());
					}
            }

            // Wait for the process to complete
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.err.println("PowerShell command exited with value: " + exitValue);
            }

            // Print distinct process names
            processNames.forEach(System.out::println);
// Prepare response map
            responseMap.put("processes", processNames);
            responseMap.put("serverName", remoteServer);
            if (errorMessages.length() > 0) {
                responseMap.put("errors", errorMessages.toString());
            }
 
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
 
        return responseMap;
	}
	
public static String executeCommandPowerShell(String selectedServer, String user_name, String pass_word, String selectedProcess) {
    StringBuilder output = new StringBuilder();
    try {
        // Path to the PowerShell script file
        String scriptPath = "src/Fetch-ProcessDetails.ps1";

        // Construct the command to execute the PowerShell script file with parameters
        String command = String.format(
            "powershell.exe -ExecutionPolicy Bypass -NoProfile -File \"%s\" -servername \"%s\" -username \"%s\" -password \"%s\" -processname \"%s\"",
            scriptPath, selectedServer, user_name, pass_word, selectedProcess
        );

        // Execute the PowerShell script
        Process powerShellProcess = Runtime.getRuntime().exec(command);
        powerShellProcess.getOutputStream().close();

        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        reader.close();

        // Capture any errors
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
        String errorLine;
        while ((errorLine = errorReader.readLine()) != null) {
            System.err.println("Error: " + errorLine);
        }
        errorReader.close();

        // Wait for the process to exit
        int exitCode = powerShellProcess.waitFor();
        if (exitCode != 0) {
            System.err.println("PowerShell script exited with code " + exitCode);
            return null;  // Indicate failure to the caller
        }

    } catch (Exception e) {
        e.printStackTrace();
        return null;  // Indicate failure to the caller
    }
    return output.toString().trim();  // Trim any trailing newlines
}

public static class ProcessResult {
    private boolean success;
    private String message;

    public ProcessResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}


private static ProcessResult killRemoteProcess(String remoteServer, String user_name, String pass_word, String scriptPath, String processName, String commandLine) {
    // Properly escape quotes around the executable path in the commandLine
    String formattedCommandLine = String.format("\"\"\"%s\"\" %s\"", commandLine.split(" ")[0], commandLine.substring(commandLine.indexOf(" ") + 1));

    // Build the PowerShell command, using triple quotes to escape properly
    String command = String.format(
        "powershell.exe -NoProfile -Command \"Invoke-Command -ComputerName %s -Credential (New-Object PSCredential ('%s', (ConvertTo-SecureString '%s' -AsPlainText -Force))) -FilePath '%s' -ArgumentList '%s', '%s'\"",
        remoteServer, user_name, pass_word, scriptPath, processName, formattedCommandLine
    );

    try {
        // Debug: Output the command line being executed
        System.out.println("Executing command: " + command);

        // Execute the PowerShell command
        Process process = Runtime.getRuntime().exec(command);

        // Capture the output and error streams
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder output = new StringBuilder();
        String line;

        // Output the standard input (success messages)
        while ((line = stdInput.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Output the standard error (error messages)
        StringBuilder errorOutput = new StringBuilder();
        while ((line = stdError.readLine()) != null) {
            errorOutput.append(line).append("\n");
        }

        // Wait for the process to finish and check the exit code
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Process killed successfully.");
            return new ProcessResult(true, output.toString().trim());
        } else {
            System.out.println("Failed to kill process. Exit code: " + exitCode);
            return new ProcessResult(false, errorOutput.toString().trim());
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return new ProcessResult(false, e.getMessage());
    }
}


    private static Map<String, ArrayList<Map<String, String>>> fetchServerProperties(Connection connection, String selectedCustomer) throws SQLException {
    Map<String, ArrayList<Map<String, String>>> propertiesInfo = new HashMap<>();
    Map<String, Map<String, List<String>>> serverComponentInfo = fetchComponentInfo(connection, selectedCustomer);
    Map<String, Map<String, List<String>>> threadComponentInfo = fetchThreadComponentInfo(connection, selectedCustomer);
	Map<String, Map<String, List<?>>> customComponentInfo = fetchCustomComponentInfo(connection, selectedCustomer);

    for (Map.Entry<String, Map<String, List<String>>> entry : serverComponentInfo.entrySet()) {
        String serverName = entry.getKey();
        Map<String, List<String>> componentInfo = entry.getValue();
        ArrayList<Map<String, String>> serverProperties = new ArrayList<>();
        readPropertiesFiles(serverProperties, serverName, componentInfo);

        propertiesInfo.put(serverName, serverProperties);
    }

    for (Map.Entry<String, Map<String, List<String>>> entry : threadComponentInfo.entrySet()) {
        String serverName = entry.getKey();
        Map<String, List<String>> threadInfo = entry.getValue();

        ArrayList<Map<String, String>> threadProperties = new ArrayList<>();
        readThreadPropertiesFiles(threadProperties, serverName, threadInfo);

        if (propertiesInfo.containsKey(serverName)) {
            propertiesInfo.get(serverName).addAll(threadProperties);
            propertiesInfo.get(serverName).addAll(threadProperties);
        } else {
            propertiesInfo.put(serverName, threadProperties);
        }
    }
	
	// Assume propertiesInfo is already defined as: 
// Map<String, ArrayList<Map<String, String>>> propertiesInfo = new HashMap<>();

        for (Map.Entry<String, Map<String, List<?>>> entry : customComponentInfo.entrySet()) {
        String serverName = entry.getKey();
        Map<String, List<?>> customInfo = entry.getValue();
    
        ArrayList<Map<String, String>> customProperties = new ArrayList<>();
        getTagValues(customProperties, serverName, customInfo);  // Pass customInfo as Map<String, List<?>>
    
        // Update propertiesInfo map
        if (propertiesInfo.containsKey(serverName)) {
            propertiesInfo.get(serverName).addAll(customProperties);
        } else {
            propertiesInfo.put(serverName, customProperties);
        }
    }

    return propertiesInfo;
}


    private static ArrayList<String> fetchServerNames(Connection connection, String selectedCustomer) throws SQLException {
        ArrayList<String> serverNames = new ArrayList<>();
        String query = "SELECT DISTINCT SERVER_NAME FROM [DBMonitor].[dbo].[AppConfig] WHERE CUSTOMER_NAME = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, selectedCustomer);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    serverNames.add(resultSet.getString("SERVER_NAME"));
                }
            }
        }
        return serverNames;
    }

    private static Map<String, Map<String, List<String>>> fetchComponentInfo(Connection connection, String selectedCustomer) throws SQLException {
        Map<String, Map<String, List<String>>> serverComponentInfo = new HashMap<>();
        ArrayList<String> serverNames = fetchServerNames(connection, selectedCustomer);

        for (String serverName : serverNames) {
            Map<String, List<String>> componentInfo = new HashMap<>();
            List<String> componentNames = new ArrayList<>();
            List<String> paths = new ArrayList<>();
            List<String> keys = new ArrayList<>();

            String componentDetailsQuery = "SELECT DISTINCT PATH, COMPONENT_NAME, REFERENCE_KEY FROM [DBMonitor].[dbo].[AppConfig] WHERE SERVER_NAME = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(componentDetailsQuery)) {
                preparedStatement.setString(1, serverName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        componentNames.add(resultSet.getString("COMPONENT_NAME"));
                        paths.add(resultSet.getString("PATH"));
                        keys.add(resultSet.getString("REFERENCE_KEY"));
                    }
                    System.out.println("Fetched component info for server: " + serverName + " -> " + componentNames + ", " + paths + ", " + keys);
                }
            }

            // Parse keys into lists of keys
            List<List<String>> parsedKeys = new ArrayList<>();
            for (String keyJson : keys) {
                List<String> keyList = new Gson().fromJson(keyJson, new TypeToken<List<String>>() {}.getType());
                parsedKeys.add(keyList);
            }

            componentInfo.put("componentNames", componentNames);
            componentInfo.put("paths", paths);
            componentInfo.put("keys", (List<String>) (List<?>) parsedKeys); // Casting to proper type
            serverComponentInfo.put(serverName, componentInfo);
        }

        return serverComponentInfo;
    }
	
	
   private static Map<String, Map<String, List<String>>> fetchThreadComponentInfo(Connection connection, String selectedCustomer) throws SQLException {
    Map<String, Map<String, List<String>>> threadcomponentInfo = new HashMap<>();
    ArrayList<String> serverNames = fetchServerNames(connection, selectedCustomer);

    for (String serverName : serverNames) {
        Map<String, List<String>> threadInfo = new HashMap<>();
        List<String> componentNames = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        String componentDetailsQuery = "SELECT DISTINCT PATH, COMPONENT_NAME, REFERENCE_KEY FROM [DBMonitor].[dbo].[AppConfigThreads] WHERE SERVER_NAME = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(componentDetailsQuery)) {
            preparedStatement.setString(1, serverName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    componentNames.add(resultSet.getString("COMPONENT_NAME"));
                    paths.add(resultSet.getString("PATH"));
                    keys.add(resultSet.getString("REFERENCE_KEY"));
                }
                System.out.println("Fetched component info for server: " + serverName + " -> " + componentNames + ", " + paths + ", " + keys);
            }
        }

        threadInfo.put("componentNames", componentNames);
        threadInfo.put("paths", paths);
        threadInfo.put("keys", keys); // Directly use the string variables as keys
        threadcomponentInfo.put(serverName, threadInfo);
    }

    return threadcomponentInfo;
}

private static Map<String, Map<String, List<?>>> fetchCustomComponentInfo(Connection connection, String selectedCustomer) throws SQLException {
        Map<String, Map<String, List<?>>> customComponentInfo = new HashMap<>();
        ArrayList<String> serverNames = fetchServerNames(connection, selectedCustomer);

        for (String serverName : serverNames) {
            Map<String, List<?>> customInfo = new HashMap<>();
            List<String> componentNames = new ArrayList<>();
            List<String> paths = new ArrayList<>();
            List<List<String>> parsedKeys = new ArrayList<>();

            String componentDetailsQuery = "SELECT DISTINCT PATH, COMPONENT_NAME, REFERENCE_KEY FROM [DBMonitor].[dbo].[AppConfigCustom] WHERE SERVER_NAME = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(componentDetailsQuery)) {
                preparedStatement.setString(1, serverName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        componentNames.add(resultSet.getString("COMPONENT_NAME"));
                        paths.add(resultSet.getString("PATH"));

                        // Parse REFERENCE_KEY from JSON or other formats to List<String>
                        String keyJson = resultSet.getString("REFERENCE_KEY");
                        List<String> keyList = new Gson().fromJson(keyJson, new TypeToken<List<String>>() {}.getType());
                        parsedKeys.add(keyList);
                    }
                    System.out.println("Fetched component info for server: " + serverName + " -> " + componentNames + ", " + paths + ", " + parsedKeys);
                }
            }

            customInfo.put("componentNames", componentNames);
            customInfo.put("paths", paths);
            customInfo.put("keys", parsedKeys);  // Store as List<List<String>>
            customComponentInfo.put(serverName, customInfo);
        }

        return customComponentInfo;
    }

    public static List<Map<String, String>> getTagValues(List<Map<String, String>> customProperties, String serverName, Map<String, List<?>> customInfo) {
    // Extracting data from customInfo
    List<String> componentNames = (List<String>) customInfo.get("componentNames");
    List<String> paths = (List<String>) customInfo.get("paths");
    List<List<String>> keysList = (List<List<String>>) customInfo.get("keys");  // Corrected the casting
 
    System.out.println(componentNames);
    System.out.println(paths);
    System.out.println(keysList);
 
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
 
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            List<String> keysForPath = i < keysList.size() ? keysList.get(i) : Collections.emptyList();
            String componentName = i < componentNames.size() ? componentNames.get(i) : "";
 
            // Adjust file path for Windows
            if (path != null && path.length() > 2 && path.charAt(1) == ':') {
                path = path.substring(0, 1) + "$" + path.substring(2);
            }
 
            File file = new File("//" + serverName + "/" + path);
 
            if (!file.exists()) {
                System.err.println("File not found: " + file.getAbsolutePath());
                continue;
            }
 
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();
 
            // Process each key pair for this path
            for (int j = 0; j < keysForPath.size(); j += 2) {
                String outerTag = keysForPath.get(j);
                String innerTag = (j + 1) < keysForPath.size() ? keysForPath.get(j + 1) : "";
 
                System.out.println("Outer Tag: " + outerTag);
                System.out.println("Inner Tag: " + innerTag);
                System.out.println("File Path: " + file.getAbsolutePath());
 
                String result = findInnerTag(outerTag, innerTag, file.getAbsolutePath());
                System.out.println("Result: " + result);
 
                if (!result.equals("No matching inner tag found")) {
                    Map<String, String> propertyMap = new HashMap<>();
                    propertyMap.put("modifiedFileName", componentName);
                    propertyMap.put("path", path);
                    propertyMap.put("value", result);
                    propertyMap.put("type", "custom");
                    propertyMap.put("serverName", serverName);
                    propertyMap.put("key", outerTag + (innerTag.isEmpty() ? "" : "," + innerTag));
                    customProperties.add(propertyMap);
                    System.out.println(customProperties);
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return customProperties;
}
 
// Helper class to store keys and component name
private static class PathKeyComponent {
    List<String> keys;
    String componentName;
 
    PathKeyComponent(List<String> keys, String componentName) {
        this.keys = keys;
        this.componentName = componentName;
    }
}


       public static String findInnerTag(String outerTag, String innerTag, String xmlFilePath) {
        boolean[] matchedByAttributeOuter = new boolean[1]; // Tracks outer tag match
        boolean[] matchedByAttributeInner = new boolean[1]; // Tracks inner tag match
        boolean outerTagFound = false; // Flag to check if outer tag was found

        try {
            // Create a DocumentBuilder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML file
            Document document = builder.parse(xmlFilePath);
            document.getDocumentElement().normalize();

            // Get all elements in the document
            NodeList allNodes = document.getElementsByTagName("*");

            // Search for outer tag first if provided
            if (outerTag != null && !outerTag.isEmpty()) {
                for (int i = 0; i < allNodes.getLength(); i++) {
                    Node node = allNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        // Check if this element matches the outerTag criteria (tag name, attribute name, or attribute value)
                        if (matchesCriteria(element, outerTag, matchedByAttributeOuter)) {
                            outerTagFound = true;

                            // Now search for the inner tag within this matched outer tag
                            if (innerTag != null && !innerTag.isEmpty()) {
                                NodeList innerNodes = element.getElementsByTagName(innerTag);
                                for (int j = 0; j < innerNodes.getLength(); j++) {
                                    Node innerNode = innerNodes.item(j);
                                    if (innerNode.getNodeType() == Node.ELEMENT_NODE) {
                                        Element innerElement = (Element) innerNode;

                                        // Call matchesCriteria for innerTag
                                        if (matchesCriteria(innerElement, innerTag, matchedByAttributeInner)) {
                                            // Process found inner tag
                                            String value = processFoundTags(innerElement, innerTag, matchedByAttributeInner[0]);

                                            // Check for attribute fallback (e.g., "value")
                                            if (value.isEmpty()) {
                                                String attrValue = innerElement.getAttribute("value");
                                                if (!attrValue.isEmpty()) {
                                                    return attrValue;  // Return the attribute value
                                                }
                                            }

                                            return value;  // Return the found value
                                        }
                                    }
                                }

                                // If no inner tag is found but outer tag matched, check inner attributes
                                String attrValue = element.getAttribute(innerTag);
                                if (!attrValue.isEmpty()) {
                                    return attrValue;
                                }
                            } else {
                                // If there is no innerTag, process the outer tag directly
                                return processFoundTags(element, outerTag, matchedByAttributeOuter[0]);
                            }
                        }
                    }
                }
            }

            // If the outer tag was not found, search for the inner tag directly
            if (!outerTagFound && innerTag != null && !innerTag.isEmpty()) {
                for (int i = 0; i < allNodes.getLength(); i++) {
                    Node node = allNodes.item(i);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        // Call matchesCriteria for innerTag
                        if (matchesCriteria(element, innerTag, matchedByAttributeInner)) {
                            String value = processFoundTags(element, innerTag, false);

                            // Fallback to checking for attribute "value" if no content is found
                            if (value.isEmpty()) {
                                String attrValue = element.getAttribute("value");
                                if (!attrValue.isEmpty()) {
                                    return attrValue;  // Return the attribute value
                                }
                            }

                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "No matching inner tag found";
    }

    // This method checks if the element matches the tag, attribute name, or attribute value criteria
    private static boolean matchesCriteria(Element element, String criteria, boolean[] matchedByAttribute) {
        // Check if the criteria matches the tag name
        if (element.getTagName().equals(criteria)) {
            matchedByAttribute[0] = false; // Tag name match
            return true;
        }

        // Check if the criteria matches an attribute name or value
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            String attrName = element.getAttributes().item(i).getNodeName();
            String attrValue = element.getAttribute(attrName);

            // Check for attribute name match
            if (attrName.equals(criteria)) {
                matchedByAttribute[0] = true;  // Attribute name match
                return true;
            }

            // Check for attribute value match
            if (attrValue.equals(criteria)) {
                matchedByAttribute[0] = true;  // Attribute value match
                return true;
            }
        }

        return false; // No match found
    }

    // This method processes found tags and returns the appropriate result
    private static String processFoundTags(Element element, String tag, boolean matchedByAttribute) {
        if (matchedByAttribute) {
            // Return attribute value if matched by attribute
            for (int j = 0; j < element.getAttributes().getLength(); j++) {
                String attrValue = element.getAttributes().item(j).getNodeValue();
                if (!attrValue.isEmpty()) {
                    return attrValue;  // Return attribute value
                }
            }
        }

        // Return text content if not matched by attribute
        String textContent = element.getTextContent().trim();
        if (!textContent.isEmpty()) {
            return textContent;  // Return tag's text content
        }

        return "";  // Return empty string if no match found
    }

    // This method retrieves tag values based on the provided XML file paths and keys


     public static void readPropertiesFiles(ArrayList<Map<String, String>> serverProperties, String serverName, Map<String, List<String>> componentInfo) {
    List<String> componentNames = componentInfo.get("componentNames");
    List<String> paths = componentInfo.get("paths");
    List<List<String>> keysList = (List<List<String>>) (List<?>) componentInfo.get("keys"); // Casting to proper type

    // Distinct memory type keys
    Set<String> distinctXmsKeys = new HashSet<>();
    Set<String> distinctXmxKeys = new HashSet<>();
    Set<String> distinctDefaultKeys = new HashSet<>();

    // Process each component
    for (int i = 0; i < componentNames.size(); i++) {
        String componentName = componentNames.get(i);
        String path = paths.get(i);
        List<String> keys = keysList.get(i);

        // Split path by "#"
        String[] pathParts = path.split("#");
        String originalPath = pathParts[0]; // Original path before "#"
        String Tag = (pathParts.length > 1) ? pathParts[1] : ""; // Path without hyphen

        if (originalPath != null && originalPath.length() > 2 && originalPath.charAt(1) == ':') {
            originalPath = originalPath.substring(0, 1) + "$" + originalPath.substring(2);
        }
        File file = new File("//" + serverName + "/" + originalPath);

        if (file.exists()) {
            System.out.println("File found: " + file.getPath());
            if (Tag != null && !Tag.isEmpty()) {
                // Check if the tag exists in the file content
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getPath())));
                    Pattern tagPattern = Pattern.compile("<" + Tag + ">(.*?)</" + Tag + ">", Pattern.DOTALL);
                    Matcher tagMatcher = tagPattern.matcher(content);

                    if (tagMatcher.find()) {
                        String tagContent = tagMatcher.group(1);

                        for (String key : keys) {
                            Map<String, String> fileProperty = new HashMap<>();
                            fileProperty.put("modifiedFileName", componentName);
                            fileProperty.put("path", originalPath); // Store without hyphen path
                            fileProperty.put("Tags", Tag); // Store tag
                            fileProperty.put("key", key);

                            // Determine memory type based on key prefix
                            String memoryType = determineMemoryType(key);
                            fileProperty.put("type", memoryType);

                            // Store distinct keys based on type
                            if ("Xms".equals(memoryType)) {
                                distinctXmsKeys.add(key);
                            } else if ("Xmx".equals(memoryType)) {
                                distinctXmxKeys.add(key);
                            } else {
                                distinctDefaultKeys.add(key);
                            }

                            // Search for key values within the tag content
                            addMemoryValueFromTagContent(fileProperty, tagContent, serverName);
                            serverProperties.add(fileProperty);
                        }
                    } else {
                        System.out.println("Tag not found in file: " + file.getPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Process the file without considering tags
                for (String key : keys) {
                    Map<String, String> fileProperty = new HashMap<>();
                    fileProperty.put("modifiedFileName", componentName);
                    fileProperty.put("path", originalPath); // Store without hyphen path
                    fileProperty.put("key", key);

                    // Determine memory type based on key prefix
                    String memoryType = determineMemoryType(key);
                    fileProperty.put("type", memoryType);

                    // Store distinct keys based on type
                    if ("Xms".equals(memoryType)) {
                        distinctXmsKeys.add(key);
                    } else if ("Xmx".equals(memoryType)) {
                        distinctXmxKeys.add(key);
                    } else {
                        distinctDefaultKeys.add(key);
                    }

                    addMemoryValue(fileProperty, file, serverName);
                    serverProperties.add(fileProperty);
                }
            }
        } else {
            System.out.println("File not found: " + file.getPath());
        }
    }

    // Example of printing distinct keys (can be replaced with other logic)
    System.out.println("Distinct Xms Keys: " + distinctXmsKeys);
    System.out.println("Distinct Xmx Keys: " + distinctXmxKeys);
    System.out.println("Distinct Default Keys: " + distinctDefaultKeys);
}
public static void readThreadPropertiesFiles(ArrayList<Map<String, String>> threadProperties, String serverName, Map<String, List<String>> threadInfo) {
        List<String> componentNames = threadInfo.get("componentNames");
        List<String> paths = threadInfo.get("paths");
        List<String> keys = threadInfo.get("keys");

        // Process each component
        for (int i = 0; i < componentNames.size(); i++) {
            String componentName = componentNames.get(i);
            String path = paths.get(i);
			String key = keys.get(i);
            

            // Split path by "#"
            String[] pathParts = path.split("#");
            String originalPath = pathParts[0]; // Original path before "#"
            String tag = (pathParts.length > 1) ? pathParts[1] : ""; // Path without hyphen

            if (originalPath != null && originalPath.length() > 2 && originalPath.charAt(1) == ':') {
                originalPath = originalPath.substring(0, 1) + "$" + originalPath.substring(2);
            }
            File file = new File("//" + serverName + "/" + originalPath);

            if (file.exists()) {
                System.out.println("File found: " + file.getPath());
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file.getPath())));

                    if (tag != null && !tag.isEmpty()) {
                        // Check if the tag exists in the file content
                        Pattern tagPattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
                        Matcher tagMatcher = tagPattern.matcher(content);

                        if (tagMatcher.find()) {
                            // Extract the content within the tag
                            String tagContent = tagMatcher.group(1);

                                Pattern variablePattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*\"(\\d+)\"");
                                Matcher variableMatcher = variablePattern.matcher(tagContent);

                                if (variableMatcher.find()) {
                                    String numericValue = variableMatcher.group(1).trim();
                                    Map<String, String> fileProperty = new HashMap<>();
                                    fileProperty.put("modifiedFileName", componentName);
                                    fileProperty.put("path", originalPath); // Store without hyphen path
                                    fileProperty.put("Tags", tag); // Store tag
                                    fileProperty.put("key", key);
                                    fileProperty.put("Thread", numericValue);
									fileProperty.put("serverName", serverName);
                                    threadProperties.add(fileProperty);
                                }
                            
                        } else {
                            System.out.println("Tag not found in file: " + file.getPath());
                        }
                    } else {
                       
                            Pattern variablePattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*\"(\\d+)\"");
                            Matcher variableMatcher = variablePattern.matcher(content);

                            if (variableMatcher.find()) {
                                String numericValue = variableMatcher.group(1).trim();
                                Map<String, String> fileProperty = new HashMap<>();
                                fileProperty.put("modifiedFileName", componentName);
                                fileProperty.put("path", originalPath); // Store without hyphen path
                                fileProperty.put("key", key);
                                fileProperty.put("Thread", numericValue);
								fileProperty.put("serverName", serverName);
                                threadProperties.add(fileProperty);
                            }
                        
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("File not found: " + file.getPath());
            }
        }
    }




    private static String determineMemoryType(String key) {
        if (key.startsWith("-Xms")) {
            return "Xms";
        } else if (key.startsWith("-Xmx")) {
            return "Xmx";
        } else {
            return "default";
        }
    }
 
private static void addMemoryValue(Map<String, String> fileProperty, File file, String serverName) {
        String[] memoryValues = readMemoryValues(file, "-Xms", "-Xmx");

        // Add memory value based on type
        String memoryType = fileProperty.get("type");
        if ("Xms".equals(memoryType)) {
            fileProperty.put("xmsmemory", memoryValues[0]);
        } else if ("Xmx".equals(memoryType)) {
            fileProperty.put("xmxmemory", memoryValues[1]);
        } else {
            fileProperty.put("memoryValue", memoryValues[0]); // Assuming default uses the first value
        }

        fileProperty.put("serverName", serverName);
    }

    private static void addMemoryValueFromTagContent(Map<String, String> fileProperty, String tagContent, String serverName) {
        String[] memoryValues = readMemoryValuesFromTag(tagContent, "-Xms", "-Xmx");

        // Add memory value based on type
        String memoryType = fileProperty.get("type");
        if ("Xms".equals(memoryType)) {
            fileProperty.put("xmsmemory", memoryValues[0]);
        } else if ("Xmx".equals(memoryType)) {
            fileProperty.put("xmxmemory", memoryValues[1]);
        } else {
            fileProperty.put("memoryValue", memoryValues[0]); // Assuming default uses the first value
        }

        fileProperty.put("serverName", serverName);
    }

    private static String[] readMemoryValuesFromTag(String tagContent, String key1, String key2) {
        String xmsMemoryValue = "";
        String xmxMemoryValue = "";

        Pattern combinedPattern = Pattern.compile(key1 + "(\\d+).*" + key2 + "(\\d+)");
        Pattern key1Pattern = Pattern.compile(key1 + "(\\d+)");
        Pattern key2Pattern = Pattern.compile(key2 + "(\\d+)");

        // Check for both keys in the same tag content
        Matcher combinedMatcher = combinedPattern.matcher(tagContent);
        if (combinedMatcher.find()) {
            xmsMemoryValue = combinedMatcher.group(1);
            xmxMemoryValue = combinedMatcher.group(2);
        } else {
            // Check for key1 in the tag content
            Matcher key1Matcher = key1Pattern.matcher(tagContent);
            if (key1Matcher.find()) {
                xmsMemoryValue = key1Matcher.group(1);
            }
            // Check for key2 in the tag content
            Matcher key2Matcher = key2Pattern.matcher(tagContent);
            if (key2Matcher.find()) {
                xmxMemoryValue = key2Matcher.group(1);
            }
        }

        return new String[]{xmsMemoryValue, xmxMemoryValue};
    }

    private static String[] readMemoryValues(File file, String key1, String key2) {
        String xmsMemoryValue = "";
        String xmxMemoryValue = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Pattern combinedPattern = Pattern.compile(key1 + "\\s*(\\d+).*" + key2 + "\\s*(\\d+)");
            Pattern key1Pattern = Pattern.compile(key1 + "\\s*(\\d+)");
            Pattern key2Pattern = Pattern.compile(key2 + "\\s*(\\d+)");
            while ((line = reader.readLine()) != null) {
                // Check for both keys in the same line
                Matcher combinedMatcher = combinedPattern.matcher(line);
                if (combinedMatcher.find()) {
                    xmsMemoryValue = combinedMatcher.group(1);
                    xmxMemoryValue = combinedMatcher.group(2);
                    break;
                }
                // Check for key1 in the line
                Matcher key1Matcher = key1Pattern.matcher(line);
                if (key1Matcher.find()) {
                    xmsMemoryValue = key1Matcher.group(1);
                }
                // Check for key2 in the line
                Matcher key2Matcher = key2Pattern.matcher(line);
                if (key2Matcher.find()) {
                    xmxMemoryValue = key2Matcher.group(1);
                }
                // If both values have been found, exit the loop
                if (!xmsMemoryValue.isEmpty() && !xmxMemoryValue.isEmpty()) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{xmsMemoryValue, xmxMemoryValue};
    }
	
	   public static void backupAndModifyPropertiesFile(File file, String key, String tags, String upgradedMemory) throws Exception {
    try {
        // Generate a timestamp for the backup
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        // Create the backup file path with the timestamp
        Path sourcePath = file.toPath();
        Path backupPath = Paths.get(file.getAbsolutePath() + "." + timestamp + ".bak");

        // Ensure file exists before proceeding
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        // Create a backup of the file
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backup created at: " + backupPath);

        // Read the properties file
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        }

        String content = contentBuilder.toString();
        System.out.println("Original file content:\n" + content);

        // Modify all occurrences of the specific value
        boolean found = false;

        if (tags != null && !tags.isEmpty()) {
            // If tag is provided, find and modify within the tag
            String tagPatternStr = "(<" + Pattern.quote(tags) + ">)(.*?)(</" + Pattern.quote(tags) + ">)";
            Pattern tagPattern = Pattern.compile(tagPatternStr, Pattern.DOTALL);
            Matcher tagMatcher = tagPattern.matcher(content);

            if (tagMatcher.find()) {
                do {
                    String tagContent = tagMatcher.group(2); // Content within the tag

                    // Find and replace the key within the tag content
                    String keyPatternStr = Pattern.quote(key) + "\\d+[mMgG]?";
                    Pattern keyPattern = Pattern.compile(keyPatternStr);
                    Matcher keyMatcher = keyPattern.matcher(tagContent);

                    while (keyMatcher.find()) {
                        String oldMemoryValue = keyMatcher.group();
                        System.out.println("Old Memory Value in Tag: " + oldMemoryValue);

                        String newMemoryValue = key + upgradedMemory; // Assuming upgradedMemory is in the correct format (e.g., "512M" or "1G")
                        tagContent = tagContent.replace(oldMemoryValue, newMemoryValue);
                        System.out.println("New Memory Value in Tag: " + newMemoryValue);
                        found = true;
                    }

                    // Replace the old tag content with the modified tag content
                    content = content.replace(tagMatcher.group(2), tagContent);
                } while (tagMatcher.find());
            } else {
                throw new Exception("Tag not found: " + tags);
            }
        } else {
            // If tag is not provided, modify the key in the whole file content
            String keyPatternStr = Pattern.quote(key) + "\\d+[mMgG]?";
            Pattern keyPattern = Pattern.compile(keyPatternStr);
            Matcher keyMatcher = keyPattern.matcher(content);

            while (keyMatcher.find()) {
                String oldMemoryValue = keyMatcher.group();
                System.out.println("Old Memory Value: " + oldMemoryValue);

                String newMemoryValue = key + upgradedMemory; // Assuming upgradedMemory is in the correct format (e.g., "512M" or "1G")
                content = content.replace(oldMemoryValue, newMemoryValue);
                System.out.println("New Memory Value: " + newMemoryValue);
                found = true;
            }
        }

        if (!found) {
            throw new Exception("Key not found: " + key);
        } else {
            // Save the updated properties back to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }

            // Debugging: Read and print the updated file content
            System.out.println("Updated file content:");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }

    } catch (IOException e) {
        throw new Exception("IO Error: Unable to read/write the file. " + e.getMessage(), e);
    } catch (Exception e) {
        throw new Exception("Error occurred while modifying properties file: " + e.getMessage(), e);
    }
}

	public static void backupAndModifyThreadFile(File file, String key, String tag, String upgradedThreadValue) throws Exception {
    try {
        // Generate a timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);

        // Create the backup file path with the timestamp
        Path sourcePath = file.toPath();
        Path backupPath = Paths.get(file.getAbsolutePath() + "." + timestamp + ".bak");
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
        System.out.println("Original file content:\n" + content);

        // Modify the specific value
        boolean found = false;

        if (tag != null && !tag.isEmpty()) {
            Pattern tagPattern = Pattern.compile("<" + Pattern.quote(tag) + ">(.*?)</" + Pattern.quote(tag) + ">", Pattern.DOTALL);
            Matcher tagMatcher = tagPattern.matcher(content);

            if (tagMatcher.find()) {
                do {
                    String tagContent = tagMatcher.group(1);

                    // Find and replace the key
                    Pattern variablePattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*\"(\\d+)\"");
                    Matcher variableMatcher = variablePattern.matcher(tagContent);

                    if (variableMatcher.find()) {
                        String oldThreadValue = variableMatcher.group(1).trim();
                        System.out.println("Old Thread Value in Tag: " + oldThreadValue);

                        String newThreadValue = upgradedThreadValue;
                        tagContent = tagContent.replace(oldThreadValue, newThreadValue);
                        System.out.println("New Thread Value in Tag: " + newThreadValue);
                        found = true;
                    }

                    content = content.replace(tagMatcher.group(1), tagContent);
                } while (tagMatcher.find());
            } else {
                throw new Exception("Tag not found: " + tag);
            }
        } else {
            // Modify the key in the whole file content if no tag is provided
            Pattern variablePattern = Pattern.compile(Pattern.quote(key) + "\\s*=\\s*\"(\\d+)\"");
            Matcher variableMatcher = variablePattern.matcher(content);

            if (variableMatcher.find()) {
                String oldThreadValue = variableMatcher.group(1).trim();
                System.out.println("Old Thread Value: " + oldThreadValue);

                String newThreadValue = upgradedThreadValue;
                content = content.replace(oldThreadValue, newThreadValue);
                System.out.println("New Thread Value: " + newThreadValue);
                found = true;
            } else {
                throw new Exception("Key not found: " + key);
            }
        }

        if (found) {
            // Save updated file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }
            System.out.println("File updated successfully.");
        }

    } catch (IOException e) {
        throw new Exception("File operation failed: " + e.getMessage());
    } catch (Exception e) {
        throw new Exception("Error modifying thread file: " + e.getMessage());
    }
}

	
	private static void backupAndChangeTagValue(String xmlFilePath, List<String> key, String newValue) {
    String outerTag = key.size() > 0 ? key.get(0) : "";
    String innerTag = key.size() > 1 ? key.get(1) : "";

    try {
        // Generate a timestamp for the backup
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // Create the backup file path with the timestamp
        Path sourcePath = Paths.get(xmlFilePath);
        Path backupPath = Paths.get(xmlFilePath + "_" + timestamp + ".bak");
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backup created at: " + backupPath);

        // Create a DocumentBuilderFactory and set up to parse the XML file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFilePath);
        doc.getDocumentElement().normalize();

        // STEP 1: Existing update logic (Modify first matched tag based on XPath expression)
        boolean found = updateUsingExistingLogic(doc, outerTag, innerTag, newValue);

        // STEP 2: If no tags are found using existing logic, proceed with the new logic
        if (!found) {
            System.out.println("No tags updated using existing logic. Proceeding with new logic.");
            boolean updated = findAndUpdateTags(doc, outerTag, innerTag, newValue);

            if (!updated) {
                System.out.println("No matching tags found to update using new logic.");
            } else {
                // Save the modified XML back to the file if updates were made using new logic
                saveUpdatedXML(doc, xmlFilePath);
            }
        } else {
            // Save the modified XML back to the file if updates were made using existing logic
            saveUpdatedXML(doc, xmlFilePath);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

private static boolean updateUsingExistingLogic(Document doc, String outerTag, String innerTag, String newValue) {
    try {
        // Create XPath instance
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // Determine the correct XPath expression based on non-empty tags
        String expression;
        if (!outerTag.isEmpty() && !innerTag.isEmpty()) {
            expression = String.format("//%s/%s", outerTag, innerTag);
        } else if (!outerTag.isEmpty()) {
            expression = String.format("//%s", outerTag);
        } else if (!innerTag.isEmpty()) {
            expression = String.format("//%s", innerTag);
        } else {
            throw new IllegalArgumentException("Both outerTag and innerTag cannot be empty.");
        }

        // Compile and evaluate the XPath expression
        XPathExpression xPathExpression = xpath.compile(expression);
        NodeList nodeList = (NodeList) xPathExpression.evaluate(doc, XPathConstants.NODESET);

        // Modify the value of the first matched tag (for simplicity)
        if (nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                String oldContent = nodeList.item(i).getTextContent();
                System.out.println("Old Content: " + oldContent);

                // Replace content
                nodeList.item(i).setTextContent(newValue);

                String newContent = nodeList.item(i).getTextContent();
                System.out.println("New Content: " + newContent);
            }
            return true; // Tag updated
        } else {
            System.out.println("No matching tags found using existing logic.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return false; // No tags updated
}

private static boolean findAndUpdateTags(Document doc, String outerTag, String innerTag, String newValue) {
    boolean[] matchedByAttributeOuter = new boolean[1]; // Track outer tag match
    boolean[] matchedByAttributeInner = new boolean[1]; // Track inner tag match
    boolean outerTagFound = false; // Flag to check if outer tag was found
    boolean tagUpdated = false; // Flag to check if any tag was updated

    try {
        // Get all elements in the document
        NodeList allNodes = doc.getElementsByTagName("*");

        // Search for outer tag first if provided
        if (outerTag != null && !outerTag.isEmpty()) {
            for (int i = 0; i < allNodes.getLength(); i++) {
                Node node = allNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Check if this element matches the outerTag criteria (tag name, attribute name, or attribute value)
                    if (updatematchesCriteria(element, outerTag, matchedByAttributeOuter)) {
                        outerTagFound = true;

                        // Now search for the inner tag within this matched outer tag
                        if (innerTag != null && !innerTag.isEmpty()) {
                            NodeList innerNodes = element.getElementsByTagName(innerTag);
                            for (int j = 0; j < innerNodes.getLength(); j++) {
                                Node innerNode = innerNodes.item(j);
                                if (innerNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element innerElement = (Element) innerNode;

                                    // Check if innerTag matches criteria
                                    if (updatematchesCriteria(innerElement, innerTag, matchedByAttributeInner)) {
                                        // Update the tag value
                                        innerElement.setTextContent(newValue);
                                        tagUpdated = true;
                                    }
                                }
                            }

                            // If no inner tag is found, check inner attributes
                            String attrValue = element.getAttribute(innerTag);
                            if (!attrValue.isEmpty()) {
                                element.setAttribute(innerTag, newValue);
                                tagUpdated = true;
                            }
                        } else {
                            // If no inner tag, update the outer tag directly
                            element.setTextContent(newValue);
                            tagUpdated = true;
                        }
                    }
                }
            }
        }

        // If outer tag not found, search for the inner tag directly
        if (!outerTagFound && innerTag != null && !innerTag.isEmpty()) {
            for (int i = 0; i < allNodes.getLength(); i++) {
                Node node = allNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Check if innerTag matches criteria
                    if (updatematchesCriteria(element, innerTag, matchedByAttributeInner)) {
                        // Update the tag value
                        element.setTextContent(newValue);
                        tagUpdated = true;
                    }
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return tagUpdated;
}

// This method checks if the element matches the tag, attribute name, or attribute value criteria
private static boolean updatematchesCriteria(Element element, String criteria, boolean[] matchedByAttribute) {
    // Check if the criteria matches the tag name
    if (element.getTagName().equals(criteria)) {
        matchedByAttribute[0] = false; // Tag name match
        return true;
    }

    // Check if the criteria matches an attribute name or value
    for (int i = 0; i < element.getAttributes().getLength(); i++) {
        String attrName = element.getAttributes().item(i).getNodeName();
        String attrValue = element.getAttribute(attrName);

        // Check for attribute name match
        if (attrName.equals(criteria)) {
            matchedByAttribute[0] = true; // Attribute name match
            return true;
        }

        // Check for attribute value match
        if (attrValue.equals(criteria)) {
            matchedByAttribute[0] = true; // Attribute value match
            return true;
        }
    }

    return false; // No match found
}

// Method to save the updated XML file
private static void saveUpdatedXML(Document doc, String xmlFilePath) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new File(xmlFilePath));
    transformer.transform(source, result);

    System.out.println("XML file updated successfully.");
}


    // Method to parse the tag value string into a List<String>
    public static List<String> parseTags(String tagValue) {
        // Remove the brackets and split by comma
        String cleanTagValue = tagValue.replaceAll("[\\[\\]']+", "");
        String[] tagArray = cleanTagValue.split(",\\s*");

        // Convert array to List
        List<String> tagList = new ArrayList<>();
        for (String tag : tagArray) {
            tagList.add(tag.trim());
        }
        return tagList;
    }

	public static String executePowerShellScript(
    List<String> serverInput, 
    String customerInput, 
    List<String> servicesInput, 
    String fileageServerName, 
    String fileageUsername, 
    String fileagePassword, 
    String fileageDatabaseName, 
    String dbServerName, 
    String dbUsername, 
    String dbPassword, 
    String dbDatabaseName, 
    String scriptPath, 
    String user_name, 
    String pass_word) {
    
    try {
        // Prepare the PowerShell command
        String command = String.format(
            "powershell.exe -File \"%s\" -serverInput \"%s\" -customerInput \"%s\" -servicesInput \"%s\" " +
            "-fileageServerName \"%s\" -fileageUsername \"%s\" -fileagePassword \"%s\" -fileageDatabaseName \"%s\" " +
            "-dbServerName \"%s\" -dbUsername \"%s\" -dbPassword \"%s\" -dbDatabaseName \"%s\" -username \"%s\" -password \"%s\"",
            scriptPath,
            String.join(",", serverInput),
            customerInput,
            String.join(",", servicesInput),
            fileageServerName, fileageUsername, fileagePassword, fileageDatabaseName,
            dbServerName, dbUsername, dbPassword, dbDatabaseName,
            user_name, pass_word
        );
        
        // Print command for debugging
        System.out.println("Executing command: " + command);

        // Execute the command
        Process powerShellProcess = Runtime.getRuntime().exec(command);

        // Capture the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
        String output = reader.lines().collect(Collectors.joining("\n"));

        // Capture errors
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
        String errorOutput = errorReader.lines().collect(Collectors.joining("\n"));
        
        // Log errors
        if (!errorOutput.isEmpty()) {
            System.err.println("PowerShell script error: " + errorOutput);
        }

        // Wait for the process to exit
        int exitCode = powerShellProcess.waitFor();
        if (exitCode == 0) {
            return output;
        } else {
            return "PowerShell script failed with exit code: " + exitCode + "\n" + errorOutput;
        }
    } catch (Exception e) {
        return "Error executing PowerShell script: " + e.getMessage();
    }
}



	private static boolean DetailsInDatabase(String server, String database, String username, String password, String selectedCustomer, List<String> serverNamesStr, String jid, String modifiedName, String changeNumber, List<String> upgradedMemory) {
    // Implement your logic to store details in the database
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfigHistory (CUSTOMER_NAME, SERVER_NAME, J_ID, CHANGE_NUMBER, UPGRADED_MEMORY, COMPONENT_NAME) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(insertQuery)) {
            for (String serverNameStr : serverNamesStr) {
                preparedStatement.setString(1, selectedCustomer);
                preparedStatement.setString(2, serverNameStr); // Set server name
                preparedStatement.setString(3, jid);
                preparedStatement.setString(4, changeNumber);
                
                // Convert upgradedMemory list to a comma-separated string
                String upgradedMemoryStr = String.join(", ", upgradedMemory);
                preparedStatement.setString(5, upgradedMemoryStr);
                
                preparedStatement.setString(6, modifiedName);
                
                preparedStatement.addBatch(); // Add to batch
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    return true;
}

private static boolean ThreadDetailsInDatabase(String server, String database, String username, String password, String selectedCustomer, List<String> serverNamesStr, String jid, String modifiedName, String changeNumber, String upgradedThread) {
    // Implement your logic to store details in the database
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfigThreadHistory (CUSTOMER_NAME, SERVER_NAME, J_ID, CHANGE_NUMBER, UPGRADED_THREADS, COMPONENT_NAME) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(insertQuery)) {
            for (String serverNameStr : serverNamesStr) {
                preparedStatement.setString(1, selectedCustomer);
                preparedStatement.setString(2, serverNameStr); // Set server name
                preparedStatement.setString(3, jid);
                preparedStatement.setString(4, changeNumber);
                preparedStatement.setString(5, upgradedThread);    
                preparedStatement.setString(6, modifiedName);               
                preparedStatement.addBatch(); // Add to batch
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    return true;
}

	private static boolean CustomDetailsInDatabase(String server, String database, String username, String password, String selectedCustomer, List<String> serverNamesStr, String jid, String modifiedName, String changeNumber, String upgradedValue) {
    // Implement your logic to store details in the database
    try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://" + server + ";databaseName=" + database, username, password)) {
        String insertQuery = "INSERT INTO AppConfigCustomHistory (CUSTOMER_NAME, SERVER_NAME, J_ID, CHANGE_NUMBER, UPGRADED_VALUES, COMPONENT_NAME) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(insertQuery)) {
            for (String serverNameStr : serverNamesStr) {
                preparedStatement.setString(1, selectedCustomer);
                preparedStatement.setString(2, serverNameStr); // Set server name
                preparedStatement.setString(3, jid);
                preparedStatement.setString(4, changeNumber);
                preparedStatement.setString(5, upgradedValue);    
                preparedStatement.setString(6, modifiedName);               
                preparedStatement.addBatch(); // Add to batch
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    return true;
}


    private static void enableCORS(final String origin, final String methods, final String headers) {
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
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            response.type("application/json");
        });
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE, USERNAME, PASSWORD);
    }
}
