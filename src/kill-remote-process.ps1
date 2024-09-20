param (
    [string]$processName,    # The process name to find
    [string]$commandLine     # The command line to match (passed from Java)
)

# Debug: Output received parameters
Write-Output "Debug: Received processName: $processName"
Write-Output "Debug: Received commandLine: $commandLine"

try {
    $processes = Get-WmiObject Win32_Process -Filter "Name = '$processName'"

    # Debug: Output process retrieval status
    Write-Output "Debug: Retrieved processes: $($processes.Count) processes found."
}
catch {
    # Debug: Output error if process retrieval fails
    Write-Output "Error: Failed to retrieve processes. Error details: $_"
}

if ($processes.Count -eq 0) {
    Write-Output "Warning: No processes found matching processName: $processName"
}

foreach ($process in $processes) {
    # Debug: Output the command line of the current process
    Write-Output "Debug: Checking process ID: $($process.ProcessId), CommandLine: $($process.CommandLine)"

    # Strict comparison of the full command line, including quotes and trimming whitespace
    if ($process.CommandLine.Trim() -eq $commandLine.Trim()) {
        Write-Output "Debug: Match found for process ID: $($process.ProcessId). CommandLine matches exactly."

        # Try to kill the process using Stop-Process (PowerShell native)
        try {
            Write-Output "Attempting to stop process with Process ID: $($process.ProcessId)"
            Stop-Process -Id $process.ProcessId -Force
            Write-Output "Process ID $($process.ProcessId) stopped successfully."
        }
        catch {
            Write-Output "Error: Failed to stop process ID: $($process.ProcessId). Error details: $_"
        }
    }
    else {
        Write-Output "Debug: No match for process ID: $($process.ProcessId). CommandLine does not match."
    }
}
