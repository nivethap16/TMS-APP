param (
     [string]$servername = "tsba430201001",
    [string]$username = "1038618",
    [string]$password = "User#Pass*07",
    [string]$processname = "dsc.exe" 
)
 
try {
    # Convert the plain text password to a secure string
    $securePassword = ConvertTo-SecureString $password -AsPlainText -Force
    $credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)

    # Create a session to the remote server
    $session = New-PSSession -ComputerName $servername -Credential $credential

    # Execute the command on the remote server
    $result = Invoke-Command -Session $session -ScriptBlock {
        param($processname)
        Get-WmiObject Win32_Process | Where-Object { $_.Name -eq $processname } | Select-Object CommandLine
    } -ArgumentList $processname

    # Check if any result is returned
    if ($result) {
        # Extract CommandLine values
        $commandLines = $result | ForEach-Object { $_.CommandLine }
        
        # Create JSON object
        $jsonResult = @{
            Commands = $commandLines
        } | ConvertTo-Json

        # Output the JSON result
        Write-Output $jsonResult
    } else {
        # Create JSON object for no results
        $jsonResult = @{
            message = "No process named '$processname' was found on $servername."
        } | ConvertTo-Json

        # Output the JSON result
        Write-Output $jsonResult
    }

    # Remove the session
    Remove-PSSession -Session $session
} catch {
    # Create JSON object for error
    $jsonResult = @{
        message = "Error fetching details from $servername $_"
    } | ConvertTo-Json

    # Output the JSON result
    Write-Output $jsonResult
}
