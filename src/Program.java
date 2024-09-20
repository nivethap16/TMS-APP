import java.io.BufferedReader;
import java.io.*;
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

public class Program {
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
		
		scheduler.scheduleAtFixedRate(Program::checkScheduledRestarts, 0, 1, TimeUnit.MINUTES);

        // Setup REST API server
        port(8060);
        enableCORS("*", "GET,POST,PUT,DELETE,OPTIONS", "Content-Type,Authorization,X-Requested-With");
		
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
            /*Map<String, ArrayList<Map<String, String>>> propertiesInfo = fetchServerProperties(connection, selectedCustomer);
            responseMap.put("Properties", propertiesInfo);*/
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

                    // Parse the scheduledDateTime according to system's 24-hour format
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
                        // Kill the process
                        boolean restartResult = killRemoteProcess(remoteServer, user_name, pass_word, "src/kill-remote-process.ps1", processName, commandLine);

                        // Update the status in the database
                        String status = restartResult ? "Completed" : "Failed";
                        String resultMessage = restartResult ? "Process restarted successfully" : "Failed to restart process.";
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
	
	private static boolean killRemoteProcess(String remoteServer, String user_name, String pass_word, String scriptPath, String processName, String commandLine) {
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

        String line;
        // Output the standard input (success messages)
        while ((line = stdInput.readLine()) != null) {
            System.out.println("OUTPUT: " + line);
        }

        // Output the standard error (error messages)
        while ((line = stdError.readLine()) != null) {
            System.err.println("ERROR: " + line);
        }

        // Wait for the process to finish and check the exit code
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Process killed successfully.");
            return true;
        } else {
            System.out.println("Failed to kill process. Exit code: " + exitCode);
            return false;
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        return false;
    }
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