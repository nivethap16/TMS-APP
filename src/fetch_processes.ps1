param (
    [string]$remoteServer,
    [string]$username,
    [string]$password
)

$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential($username, $securePassword)

Invoke-Command -ComputerName $remoteServer -Credential $credential -ScriptBlock {
    Get-WmiObject -Class Win32_Process | ForEach-Object {
        $processId = $_.ProcessId
        try {
            $owner = $_.GetOwner()
            if ($owner) {
                $userName = $owner.User
                if ($userName -like '*TMS_APP*') {
                    $processName = $_.Name
                    Write-Output $processName
                }
            } else {
                Write-Output "Owner information not available for process ID ${processId}"
            }
        } catch {
            Write-Output "Error getting owner for process ID ${processId}: $($_.Exception.Message)"
        }
    }
}
