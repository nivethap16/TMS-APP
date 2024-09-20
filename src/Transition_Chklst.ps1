param(
    [string]$serverInput,
    [string]$customerInput,
    [string]$servicesInput,
    [string]$fileageServerName,
    [string]$fileageUsername,
    [string]$fileagePassword,
    [string]$fileageDatabaseName,
    [string]$dbServerName,
    [string]$dbUsername,
    [string]$dbPassword,
    [string]$dbDatabaseName,
    [string]$scriptPath,
    [string]$username,
    [string]$password
)

# Convert the comma-separated strings into arrays
$servers = $serverInput.Split(',')
$servicesName = $servicesInput.Split(',')
$outputFilePath = "src/transresult.html"

# Define SQL query parameters with multiple sets of parameters
$sqlQueries = @(
    @{
        ServerName = $fileageServerName
        DatabaseName = $fileageDatabaseName
        Username = $fileageUsername
        Password = $fileagePassword
        SQLQuery = "SELECT * FROM [DBMonitor].[dbo].[Monitor]"
        Topic = "Fileage Monitor"
    },
    @{
        ServerName = $dbServerName
        DatabaseName = $dbDatabaseName
        Username = $dbUsername
        Password = $dbPassword
        SQLQuery = "SELECT * FROM [Monitor].[dbo].[Monitor]"
        Topic = "DBMonitor"
    }
)

# Convert password to a secure string
$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential ($username, $securePassword)

# Function to run multiple SQL queries with different parameters and output in table format
function Run-Query {
    param (
        [array]$queryParameters  # Array of query parameter sets
    )
    $results = @()
    foreach ($params in $queryParameters) {
        $connectionString = "Server=$($params.ServerName);Database=$($params.DatabaseName);User Id=$($params.Username);Password=$($params.Password);"
        try {
            $connection = New-Object System.Data.SqlClient.SqlConnection
            $connection.ConnectionString = $connectionString
            $connection.Open()
            $command = $connection.CreateCommand()
            $command.CommandText = $params.SQLQuery
            $dataAdapter = New-Object System.Data.SqlClient.SqlDataAdapter $command
            $dataSet = New-Object System.Data.DataSet
            $dataAdapter.Fill($dataSet)
            $queryResult = $dataSet.Tables[0]
            $connection.Close()
 
            # Convert the DataTable to HTML table format with Topic heading
            $htmlTable = "<h4>$($params.Topic) - Server: $($params.ServerName)</h4><table class='table table-bordered'><thead><tr>"
            foreach ($col in $queryResult.Columns) {
                $htmlTable += "<th>$($col.ColumnName)</th>"
            }
            $htmlTable += "</tr></thead><tbody>"
            foreach ($row in $queryResult.Rows) {
                $htmlTable += "<tr>"
                foreach ($col in $queryResult.Columns) {
                    $htmlTable += "<td>$($row[$col.ColumnName])</td>"
                }
                $htmlTable += "</tr>"
            }
            $htmlTable += "</tbody></table>"
 
            $results += @{
                Server = $params.ServerName
                Database = $params.DatabaseName
                Topic = $params.Topic
                Result = $htmlTable
            }
 
        } catch {
            $results += @{
                Server = $params.ServerName
                Database = $params.DatabaseName
                Topic = $params.Topic
                Error = "Failed to execute the Query: $_"
            }
        } finally {
            if ($connection.State -eq 'Open') {
                $connection.Close()
            }
        }
    }
    return $results
}


# Function to execute remote commands and collect results
function Execute-RemoteCommands {
    param (
        [array]$servers,
        [array]$servicesName,
        [PSCredential]$credential
    )
    $results = @()
    foreach ($server in $servers) {
        try {
            $scriptBlock = {
                param ($servicesName)
                function Services-Check {
                    param ($servicesName)
                    $serviceReport = @()
                    foreach ($serviceName in $servicesName) {
                        $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
                        if ($service) {
                            $status = if ($service.Status -eq 'Running') { 'Running' } else { 'Not Running' }
                            $serviceReport += @{ ServiceName = $serviceName; Status = $status }
                        } else {
                            $serviceReport += @{ ServiceName = $serviceName; Status = "Not Found" }
                        }
                    }
                    return $serviceReport
                }
                function List-ScheduledTasks {
                    try {
                        Import-Module ScheduledTasks
                        $tasks = Get-ScheduledTask | Where-Object { $_.State -eq 'Ready' -and $_.Enabled -eq $true }
                        return $tasks | ForEach-Object { @{ TaskName = $_.TaskName; State = $_.State; Enabled = $_.Enabled } }
                    } catch {
                        return @{ Error = "Cannot Find Scheduled Task" }
                    }
                }
                function Print-FileContent {
                    $Drives = @("F", "D")
                    $results = @()
                    foreach ($Drive in $Drives) {
                        $fullPath = "${Drive}:/Programs/AutoDBRefresh/DBRefresh.xml"
                        if (Test-Path -Path $fullPath) {
                            try {
                                [xml]$xmlContent = Get-Content -Path $fullPath
                                $results += $xmlContent.OuterXml
                            } catch {
                                $results += "Failed to read or parse the AutomateDB file"
                            }
                        } else {
                            $results += "AutomateDB File not found"
                        }
                    }
                    return $results
                }
                function Check-ArchiveSettings {
                    $keyName = 'ArchiveSettings'
                    $archiveReport = @()
                    $drives = @("F", "D")
                    foreach ($drive in $drives) {
                        $dirPath = "${drive}:/Programs/ArchivingProgram/ArchivingProgram.exe.config"
                        if (Test-Path $dirPath) {
                            $content = Get-Content -Path $dirPath
                            $xmlContent = [xml]$content
                            $node = $xmlContent.SelectSingleNode("//add[@key='${keyName}']")
                            if ($node -ne $null) {
                                $archiveReport += $node.value
                            } else {
                                $archiveReport += "Key not found"
                            }
                        }
                    }
                    return $archiveReport
                }
                $serviceReport = Services-Check -servicesName $servicesName
                $scheduledTasksReport = List-ScheduledTasks
                $fileContentReport = Print-FileContent
                $archiveSettingsReport = Check-ArchiveSettings
                return @{
                    ServiceReport = $serviceReport
                    ScheduledTasksReport = $scheduledTasksReport
                    FileContentReport = $fileContentReport
                    ArchiveSettingsReport = $archiveSettingsReport
                }
            }
            $output = Invoke-Command -ComputerName $server -Credential $credential -ScriptBlock $scriptBlock -ArgumentList (,$servicesName)
            $results += @{
                Server = $server
                Output = $output
            }
        } catch {
            $results += @{
                Server = $server
                Error = $_.Exception.Message
            }
        }
    }
    return $results
}

# Execute the remote commands and get the results
$remoteResults = Execute-RemoteCommands -servers $servers -servicesName $servicesName -credential $credential

# Run the SQL queries with different parameter sets and get the results
$sqlResults = Run-Query -queryParameters $sqlQueries

# Convert the results into HTML format
$dynamicContent = ""
 $dynamicContent += "<h4>Customer: $($customerInput)</h4>"

# Add remote command results
foreach ($result in $remoteResults) {
    $dynamicContent += "<h4>Server: $($result.Server)</h4>"
    if ($result.Error) {
        $dynamicContent += "<p>Error: $($result.Error)</p>"
    } else {
        $dynamicContent += "<h4>Service Report</h4>"
        $dynamicContent += "<table class='table table-bordered'><thead><tr><th>Service Name</th><th>Status</th></tr></thead><tbody>"
        foreach ($item in $result.Output.ServiceReport) {
            $dynamicContent += "<tr><td>$($item.ServiceName)</td><td>$($item.Status)</td></tr>"
        }
        $dynamicContent += "</tbody></table>"

        $dynamicContent += "<h4>Scheduled Tasks Report</h4>"
        $dynamicContent += "<table class='table table-bordered'><thead><tr><th>Task Name</th><th>State</th><th>Enabled</th></tr></thead><tbody>"
        foreach ($item in $result.Output.ScheduledTasksReport) {
            $dynamicContent += "<tr><td>$($item.TaskName)</td><td>$($item.State)</td><td>$($item.Enabled)</td></tr>"
        }
        $dynamicContent += "</tbody></table>"

        $dynamicContent += "<h4>File Content Report</h4>"
        $dynamicContent += "<pre>$($result.Output.FileContentReport -join "`n")</pre>"

        $dynamicContent += "<h4>Archive Settings Report</h4>"
        $dynamicContent += "<pre>$($result.Output.ArchiveSettingsReport -join "`n")</pre>"
    }
}

# Add SQL results
foreach ($result in $sqlResults) {
    if ($result.Error) {
        $dynamicContent += "<p>Error: $($result.Error)</p>"
    } else {
        $dynamicContent += $result.Result
    }
}

# Create the HTML content
$htmlContent = @"
<!DOCTYPE html>
<html lang="en">
<head>
<title>TMS App Configuration Report</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<link href="https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css" rel="stylesheet">
<link rel="stylesheet" href="transresult.css">
</head>
<body>
<nav class="navbar navbar-expand-sm">
<div class="container-fluid">
<div class="navbar-brand">
<img class="logo" src="picture/BlueYonder-logo.png" alt="BlueYonder Logo"/>
</div> 
<div class="heading">
<h3>TMS APP CONFIGURATION</h3>
</div>
</div>
</nav>
<div class="container-fluid">
<div>
<img class="background" src="picture/blue.jpg" width="100%" height="100%" alt="Background Image"/>
</div>
<a href="transchk.html"><i class='bx bxs-left-arrow-circle bx-md'></i></a>
<div id="dynamicContent" class="mt-3">
<!-- This is where the dynamic content will be inserted -->
            $dynamicContent
</div>
<div class="frame22 mt-3">
<button class="btn btn-outline-dark" onclick="generateWordWithHtml()">Download</button>
</div>
</div>
 
    <script src="https://cdnjs.cloudflare.com/ajax/libs/html-docx-js/0.8.0/html-docx.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/FileSaver.js/2.0.5/FileSaver.min.js"></script>
<script>
        function generateWordWithHtml() {
            const dynamicContent = document.getElementById('dynamicContent').innerHTML;
            const converted = htmlDocx.asBlob(dynamicContent);
            saveAs(converted, 'TMS_App_Report_with_HTML.docx');
        }
</script>
</body>
</html>
"@

# Write the HTML content to a file
$htmlContent | Out-File -FilePath $outputFilePath -Encoding utf8

Write-Host "Report generated at $outputFilePath"
