let selectedCustomer = '';
let selectedServer = '';
let selectedProcess = '';
let selectedCommand = '';
let prcustomer ='';
let prcommand ='';
let prprocess='';
let prserver='';
let fetchedDetails = {};
let fetchedProcess = {};
let fetchedCommand = {};

async function restartProcesses(prcustomer, prserver, prprocess, prcommand) {
    try {
        const url = 'http://localhost:8060/restartProcess';
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                customer: prcustomer,
                serverName: prserver,
                process: prprocess,
                command: prcommand
            })
        });

        const responseData = await response.json();

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        if (responseData.error) {
            throw new Error(responseData.error);
        }

        console.log(responseData); // Log response from backend
        alert(responseData.message || "Process killed successfully!");

    } catch (error) {
        console.error('Error killing process:', error);
        alert('Error killing process: ' + error.message);
    }
}

// Load customer dropdown data
async function loadDropdownData() {
    try {
        const response = await fetch('http://localhost:8060/fetchCustomers');
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
        console.log(data);
        const customerDropdown = document.getElementById('customerDropdown');
        customerDropdown.innerHTML = ''; // Clear existing options
        data.Customers.forEach(customer => {
            const option = document.createElement('li');
            option.classList.add('dropdown-item');
            option.textContent = customer;
            option.onclick = () => show(customer, '.customer-textBox', '#dropdownMenuButton1');
            customerDropdown.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading dropdown data:', error);
    }
}

document.addEventListener('DOMContentLoaded', loadDropdownData);

// Toggle dropdown display
function toggleDropdown(event) {
    const dropdownContent = event.currentTarget.nextElementSibling;
    dropdownContent.style.display = dropdownContent.style.display === 'block' ? 'none' : 'block';
}

// Fetch servers for the selected customer
async function fetchServersForCustomer(selectedCustomer) {
    try {
        console.log("Selected Customer:", selectedCustomer);
        const url = `http://localhost:8060/fetchServers?selectedCustomer=${encodeURIComponent(selectedCustomer)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const responseText = await response.text();
        if (responseText.trim() === '') {
            throw new Error('Empty response from server');
        }

        const data = JSON.parse(responseText);
        if (data.error) {
            throw new Error(data.error);
        }
        console.log("Response data:", data);
        fetchedDetails = data;
        populateServerDropdown(data.Servers);
        console.log("Response server data:", data.Servers);

    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
        alert('Error fetching servers: ' + error.message);
    }
}

function restartProcess() {
    prcustomer = selectedCustomer;
    prserver = selectedServer;
    prprocess = selectedProcess;
    prcommand = selectedCommand;

    // Check if any field is empty
    if (!prcustomer) {
        alert('Please select a customer.');
        return;
    }
    if (!prserver) {
        alert('Please select a server.');
        return;
    }
    if (!prprocess) {
        alert('Please select a process.');
        return;
    }
  

    // Store values in localStorage
    localStorage.setItem('selectedCustomer', prcustomer);
    localStorage.setItem('selectedServer', prserver);
    localStorage.setItem('selectedProcess', prprocess);
    localStorage.setItem('Command', prcommand);

    console.log(`Customer: ${prcustomer}`);
    console.log(`Server: ${prserver}`);
    console.log(`Process: ${prprocess}`);
    console.log(`Command: ${prcommand}`);

}


// Populate server dropdown with fetched servers
function populateServerDropdown(servers) {
    const serverDropdown = document.getElementById('serverDropdown');
    if (serverDropdown) {
        serverDropdown.innerHTML = ''; // Clear existing options

        servers.forEach(server => {
            const option = document.createElement('li');
            option.className = 'dropdown-item';
            option.textContent = server.serverName;
            option.onclick = () => show(server.serverName, '.server-textBox', '#serverDropdownButton');
            serverDropdown.appendChild(option);
        });
    }
}

// Show selected value in textbox and button
function show(value, selector, buttonId) {
    document.querySelector(selector).value = value;
    document.querySelector(buttonId).innerText = value;

    if (selector === '.customer-textBox') {
        selectedCustomer = value;
        localStorage.setItem('selectedCustomer', value);
        handleCustomerSelection(); // Execute loading functionality and fetch servers
    }
    if (selector === '.server-textBox') {
        selectedServer = value;
        localStorage.setItem('selectedServer', value);
        handleServerSelection(); // Execute loading functionality and fetch processes
    }
    if (selector === '.process-textBox') {
        // Extract the string before ".exe"
        selectedProcess = value;
        localStorage.setItem('selectedProcess', selectedProcess);
        handleProcessSelection();
    }
	if (selector === '.command-textBox') {
        // Extract the string before ".exe"
        selectedCommand = value;
        localStorage.setItem('selectedProcess', selectedCommand);
    }
    
    console.clear();
    console.log(`Selected Customer: ${selectedCustomer}`);
    console.log(`Selected Server: ${selectedServer}`);
    console.log(`Selected Process: ${selectedProcess}`);
	console.log(`Selected Command: ${selectedCommand}`);
}


// Handle customer selection and fetch servers
async function handleCustomerSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    await fetchServersForCustomer(selectedCustomer); // Fetch server details

    // Wait until fetchedDetails is populated
    while (Object.keys(fetchedDetails).length === 0) {
        console.log("Waiting for fetched details...");
        await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for 1 second
    }

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}

// Handle server selection and fetch processes
async function handleServerSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    await fetchProcessForServer(selectedServer); // Fetch server details

    // Wait until fetchedProcess is populated
    while (Object.keys(fetchedProcess).length === 0) {
        console.log("Waiting for fetched process details...");
        await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for 1 second
    }

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}

// Fetch processes for the selected server
async function fetchProcessForServer(selectedServer) {
    try {
        console.log("Selected server:", selectedServer);
        const url = `http://localhost:8060/fetchProcess?selectedServer=${encodeURIComponent(selectedServer)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const responseText = await response.text();
        if (responseText.trim() === '') {
            throw new Error('Empty response from server');
        }

        const data = JSON.parse(responseText);
        if (data.error) {
            throw new Error(data.error);
        }
        console.log("Response data:", data);
        fetchedProcess = data.processes || []; // Ensure fetchedProcess is an array
        populateProcessDropdown(data.processes);
        console.log("Response process data:", fetchedProcess);

    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
        alert('Error fetching processes: ' + error.message);
    }
}

// Populate process dropdown with fetched processes
function populateProcessDropdown(processes) {
    const processDropdown = document.getElementById('processDropdown');
    if (processDropdown) {
        processDropdown.innerHTML = ''; // Clear existing options

        processes.forEach(process => {
            const option = document.createElement('li');
            option.className = 'dropdown-item';
            option.textContent = process;
            option.onclick = () => show(process, '.process-textBox', '#processDropdownButton');
            processDropdown.appendChild(option);
        });
    }
}

async function handleProcessSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    // Fetch command details
    await fetchCommandForProcess(selectedProcess, selectedServer);

    // Wait until fetchedCommand is populated
    while (fetchedCommand.length === 0) {
        console.log("Waiting for fetched command details...");
        await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for 1 second
    }

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}

// Fetch processes for the selected server
async function fetchCommandForProcess(selectedProcess, selectedServer) {
    try {
        console.log("Selected Process:", selectedProcess);
        console.log("Selected Server:", selectedServer);

        // Encode both parameters for inclusion in the URL
        const url = `http://localhost:8060/fetchCommand?selectedServer=${encodeURIComponent(selectedServer)}&selectedProcess=${encodeURIComponent(selectedProcess)}`;
        
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const responseText = await response.text();
        if (responseText.trim() === '') {
            throw new Error('Empty response from server');
        }

        const data = JSON.parse(responseText);

        if (data.error) {
            throw new Error(data.error);
        }

        console.log("Response data:", data);
        
        // Update the global variable
        fetchedCommand = data.Commands.Commands || []; // Ensure fetchedCommand is an array

        // Populate dropdown with commands
        populateCommandDropdown(fetchedCommand);
        console.log("Response process data:", fetchedCommand);

    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
        alert('Error fetching command: ' + error.message);
    }
}

// Populate the command dropdown
function populateCommandDropdown(Commands) {
    const commandDropdown = document.getElementById('commandDropdown');
    if (commandDropdown) {
        commandDropdown.innerHTML = ''; // Clear existing options

        Commands.forEach(Command => {
            const option = document.createElement('li');
            option.className = 'dropdown-item';
            option.textContent = Command;
            option.onclick = () => show(Command, '.command-textBox', '#commandDropdownButton');
            commandDropdown.appendChild(option);
        });
    }
}



// Add event listeners for toggling dropdowns
document.querySelectorAll('.dropdown-toggle').forEach(dropdownToggle => {
    dropdownToggle.addEventListener('click', toggleDropdown);
});

function showConfirmDialog() {
            // Show the dialog and overlay
			restartProcess();
            document.getElementById('confirmDialog').style.display = 'block';
            document.getElementById('overlay').style.display = 'block';

            // Focus the No button by default
            document.getElementById('noButton').focus();
        }

        function closeConfirmDialog() {
            // Hide the confirm dialog
            document.getElementById('confirmDialog').style.display = 'none';
        }

        function showResultDialog(message, color) {
            // Show the result dialog with the message
            const resultMessageElement = document.getElementById('resultMessage');
            resultMessageElement.textContent = message;
            resultMessageElement.style.color = color;
            document.getElementById('resultDialog').style.display = 'block';

            // Automatically close the result dialog after 5 seconds
            setTimeout(closeResultDialog, 5000);
        }

        function closeResultDialog() {
            // Hide the result dialog and overlay
            document.getElementById('resultDialog').style.display = 'none';
            document.getElementById('overlay').style.display = 'none';
        }

        document.getElementById('yesButton').addEventListener('click', function() {
            closeConfirmDialog();
            showResultDialog("CONFIRMED!!", "green");
			restartProcesses(prcustomer, prserver, prprocess, prcommand);
        });

        document.getElementById('noButton').addEventListener('click', function() {
            closeConfirmDialog();
            showResultDialog("CANCELLED...", "red");
        });

        // Handle Enter key to press the No button by default
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Enter' && document.getElementById('confirmDialog').style.display === 'block') {
                document.getElementById('noButton').click();
            }
        });


        function openDialog() {
            // Show the dialog and overlay
			restartProcess();
            document.getElementById('dateTimeDialog').style.display = 'block';
            document.getElementById('overlay').style.display = 'block';
        }

        function closeDialog() {
           document.getElementById('dateTimeDialog').style.display = 'none';
            document.getElementById('overlay').style.display = 'none';
        }

      /* function saveData() {
    const date = document.getElementById('datePicker').value;
    let time = document.getElementById('timePicker').value;
    if (!date || !time) {
        alert('Please select a valid date and time.');
        return;
    }
 
    // Split the time into hours and minutes
    let [hours, minutes] = time.split(':').map(Number);
 
    // If the hours are less than 12 and an AM/PM distinction exists, handle it
    const isPM = hours >= 12;
    // Convert 12-hour format to 24-hour format
    if (isPM && hours !== 12) {
        hours -= 12;
    } else if (!isPM && hours === 12) {
        hours = 0;
    }
 
    // Format hours and minutes to ensure 2-digit format
    hours = hours < 10 ? `0${hours}` : hours;
    minutes = minutes < 10 ? `0${minutes}` : minutes;
 
    const formattedTime = `${hours}:${minutes}`;
    console.log(`Selected Date: ${date}`);
    console.log(`Selected Time: ${formattedTime}`);
    alert(`Date: ${date}\nTime: ${formattedTime}`);
    closeDialog(); // Close the dialog after saving data
}*/
 




function addRow() {
    var table = document.getElementById("dynamicTable").getElementsByTagName('tbody')[0];
    var newRow = table.insertRow();

    var cols = ['customername','servername','processname', 'commandline', 'restartdate','restarttime'];

    cols.forEach(function(col) {
        var cell = newRow.insertCell();
        var input = document.createElement("input");
        input.type = "text";
        input.name = col + '[]';
        input.className = "form-control";
        cell.appendChild(input);
    });

    

    // Adding the delete button
    var actionsCell = newRow.insertCell();
    var deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "btn";
    deleteButton.innerHTML = "Delete";
    deleteButton.onclick = function() { deleteRow(this); };
    actionsCell.appendChild(deleteButton);
}

// Function to delete a row from the table
function deleteRow(button) {
    var row = button.closest('tr');
    row.parentNode.removeChild(row);
}

function editRow(button) {
  var row = button.parentNode.parentNode;
  var inputs = row.querySelectorAll('input');

  inputs.forEach(input => {
    input.disabled = !input.disabled;
    if (!input.disabled) {
      input.focus();
    }
  });
}

function deleteRow(button) {
  var row = button.parentNode.parentNode;
  row.parentNode.removeChild(row);
}
function saveData() {
    const selectedDate = document.getElementById('datePicker').value;
    const selectedTime = document.getElementById('timePicker').value;

    if (!selectedDate || !selectedTime) {
        alert("Please select both date and time.");
        return;
    }

    // Detect if the time has an AM/PM period (12-hour format)
    const timeFormat = selectedTime.match(/(AM|PM)$/i) ? '12-hour' : '24-hour';

    // Combine date and time
    const formattedDateTime = `${selectedDate} ${selectedTime}`;  // Use time as entered by the user

    const requestBody = {
        customer: selectedCustomer,
        serverName: selectedServer,
        processName: selectedProcess,
        commandLine: selectedCommand,
        dateTime: formattedDateTime,  // Send the date-time as entered by the user
        status: 'Scheduled',
        resultMessage: 'Restart has been scheduled',  // Initially empty until the process completes
        timeFormat: timeFormat  // Pass the detected time format
    };

    // Send the data to the server to store restart details
    fetch('http://localhost:8060/storeRestartDetails', {  // Modify this to your actual endpoint
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert("Process restart scheduled successfully.");
        } else {
            alert("Failed to schedule the process restart.");
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
}



 
function closeDialog() {
    // Close the dialog
    document.getElementById('dateTimeDialog').style.display = 'none';
}

// Function to populate the table with the fetched data
// Function to populate the table with the fetched data
function populateRestartTable(restartData) {
    const tableBody = document.querySelector("#dynamicTableThread tbody");
 
    // Clear any existing rows
    tableBody.innerHTML = "";
 
    restartData.forEach(data => {
        const row = document.createElement("tr");
 
        // Create input elements and set their values
        const customerNameInput = document.createElement('input');
        customerNameInput.type = 'text';
        customerNameInput.name = 'customername[]';
        customerNameInput.classList.add('form-control');
        customerNameInput.value = data.customerName;
 
        const serverNameInput = document.createElement('input');
        serverNameInput.type = 'text';
        serverNameInput.name = 'servername[]';
        serverNameInput.classList.add('form-control');
        serverNameInput.value = data.serverName;
 
        const processNameInput = document.createElement('input');
        processNameInput.type = 'text';
        processNameInput.name = 'processname[]';
        processNameInput.classList.add('form-control');
        processNameInput.value = data.processName;
 
        const commandLineInput = document.createElement('input');
        commandLineInput.type = 'text';
        commandLineInput.name = 'commandline[]';
        commandLineInput.classList.add('form-control');
        commandLineInput.value = data.commandLine;  // Safe handling of double quotes
 
        const restartDateInput = document.createElement('input');
        restartDateInput.type = 'text';
        restartDateInput.name = 'restartdate[]';
        restartDateInput.classList.add('form-control');
        restartDateInput.value = data.scheduledDateTime.split(' ')[0];
 
        const restartTimeInput = document.createElement('input');
        restartTimeInput.type = 'text';
        restartTimeInput.name = 'restarttime[]';
        restartTimeInput.classList.add('form-control');
        restartTimeInput.value = data.scheduledDateTime.split(' ')[1];
 
        // Create row structure
        row.appendChild(createTableCell(customerNameInput));
        row.appendChild(createTableCell(serverNameInput));
        row.appendChild(createTableCell(processNameInput));
        row.appendChild(createTableCell(commandLineInput));  // Safely insert commandline input
        row.appendChild(createTableCell(restartDateInput));
        row.appendChild(createTableCell(restartTimeInput));
 
        const actionCell = document.createElement('td');
        actionCell.innerHTML = `
<button type="button" class="btn" onclick="editRow(this)">Edit</button>
<button type="button" class="btn" onclick="deleteRow(this)">Delete</button>
        `;
        row.appendChild(actionCell);
 
        tableBody.appendChild(row);
    });
	
	if (tableBody.children.length === 0) {
        const noDataRow = `
		     <tr>
							    <td><input type="text" name="customername[]" class="form-control"></td>
								<td><input type="text" name="servername[]" class="form-control"></td>
                                <td><input type="text" name="processname[]" class="form-control"></td>
                                <td><input type="text" name="commandline[]" class="form-control"></td>
                                <td><input type="text" name="restartdate[]" class="form-control"></td>
								<td><input type="text" name="restarttime[]" class="form-control"></td>
                                <td>
                                    <button type="button" class="btn" onclick="editRow(this)">Edit</button>
                                    <button type="button" class="btn" onclick="deleteRow(this)">Delete</button>
                                </td>
								
                            </tr>
            <tr id="noDataRow">
                <td colspan="7" style="text-align: center;">No data available</td>
            </tr>
        `;
        tableBody.innerHTML = noDataRow;
    }
}
 
// Helper function to create a table cell and append an element to it
function createTableCell(element) {
    const cell = document.createElement('td');
    cell.appendChild(element);
    return cell;
}
 
// Function to fetch data from the backend
function fetchRestartDetails() {
    fetch('http://localhost:8060/fetchRestartDetails')  // Adjust API path
        .then(response => response.json())
        .then(data => {
            populateRestartTable(data);
        })
        .catch(error => console.error('Error fetching restart details:', error));
}

document.addEventListener('DOMContentLoaded', () => {
    fetchRestartDetails();
});
function editRow(button) {
    const row = button.closest('tr');
    const inputs = row.querySelectorAll('input');
    const isEditing = button.textContent === 'Edit';

    if (isEditing) {
        // Enable editing in the selected row and change button text to 'Save'
        inputs.forEach(input => input.removeAttribute('disabled'));
        button.textContent = 'Save';
    } else {
        // Disable editing and mark the row as updated
        inputs.forEach(input => input.setAttribute('disabled', true));
        button.textContent = 'Edit';
        // Optionally, you could also update the row status or save data here
        updateRowData(row);
    }
}

function deleteRow(button) {
    const row = button.closest('tr');
    if (confirm('Are you sure you want to delete this record?')) {
        // Remove the row from the table
        row.remove();
        // Optionally, send a request to the backend to delete the record from the database
        const rowData = {
            customerName: row.querySelector('input[name="customername[]"]').value,
            serverName: row.querySelector('input[name="servername[]"]').value,
            processName: row.querySelector('input[name="processname[]"]').value,
            commandLine: row.querySelector('input[name="commandline[]"]').value,
            restartDate: row.querySelector('input[name="restartdate[]"]').value,
            restartTime: row.querySelector('input[name="restarttime[]"]').value,
        };

        fetch('http://localhost:8060/deleteRecord', {  // Modify this to your actual endpoint for deletion
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(rowData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Record deleted successfully');
            } else {
                alert('Error deleting record');
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
    }
}

function updateRowData(row) {
    const rowData = {
        customerName: row.querySelector('input[name="customername[]"]').value,
        serverName: row.querySelector('input[name="servername[]"]').value,
        processName: row.querySelector('input[name="processname[]"]').value,
        commandLine: row.querySelector('input[name="commandline[]"]').value,
        restartDate: row.querySelector('input[name="restartdate[]"]').value,
        restartTime: row.querySelector('input[name="restarttime[]"]').value,
    };

    console.log('Updated row data:', rowData);
}

function saveTableData() {
    const rows = document.querySelectorAll('#tableBody tr');
    const dataToSave = [];

    rows.forEach(row => {
        const rowData = {
            customerName: row.querySelector('input[name="customername[]"]').value,
            serverName: row.querySelector('input[name="servername[]"]').value,
            processName: row.querySelector('input[name="processname[]"]').value,
            commandLine: row.querySelector('input[name="commandline[]"]').value,
            restartDate: row.querySelector('input[name="restartdate[]"]').value,
            restartTime: row.querySelector('input[name="restarttime[]"]').value,
        };
        dataToSave.push(rowData);
    });

    fetch('http://localhost:8060/updateRecords', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dataToSave)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('Records updated successfully');
        } else {
            alert('Error updating records');
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
}


function editRow(button) {
    const row = button.closest('tr');
    const inputs = row.querySelectorAll('input');
    const isEditing = button.textContent === 'Edit';

    if (isEditing) {
        // Enable editing in the selected row and change button text to 'Save'
        inputs.forEach(input => input.removeAttribute('disabled'));
        button.textContent = 'Save';
    } else {
        // Disable editing and mark the row as updated
        inputs.forEach(input => input.setAttribute('disabled', true));
        button.textContent = 'Edit';
        // Optionally, you could also update the row status or save data here
        updateRowData(row);
    }
}

function deleteRow(button) {
    const row = button.closest('tr');
    if (confirm('Are you sure you want to delete this record?')) {
        // Remove the row from the table
        row.remove();
        // Optionally, send a request to the backend to delete the record from the database
        const rowData = {
            customerName: row.querySelector('input[name="customername[]"]').value,
            serverName: row.querySelector('input[name="servername[]"]').value,
            processName: row.querySelector('input[name="processname[]"]').value,
            commandLine: row.querySelector('input[name="commandline[]"]').value,
            restartDate: row.querySelector('input[name="restartdate[]"]').value,
            restartTime: row.querySelector('input[name="restarttime[]"]').value,
        };

        fetch('/deleteRecord', {  // Modify this to your actual endpoint for deletion
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(rowData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Record deleted successfully');
            } else {
                alert('Error deleting record');
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
    }
}

function updateRowData(row) {
    const rowData = {
        customerName: row.querySelector('input[name="customername[]"]').value,
        serverName: row.querySelector('input[name="servername[]"]').value,
        processName: row.querySelector('input[name="processname[]"]').value,
        commandLine: row.querySelector('input[name="commandline[]"]').value,
        restartDate: row.querySelector('input[name="restartdate[]"]').value,
        restartTime: row.querySelector('input[name="restarttime[]"]').value,
    };

    console.log('Updated row data:', rowData);
}

function saveTableData() {
    const rows = document.querySelectorAll('#tableBody tr');
    const dataToSave = [];

    rows.forEach(row => {
        const rowData = {
            customerName: row.querySelector('input[name="customername[]"]').value,
            serverName: row.querySelector('input[name="servername[]"]').value,
            processName: row.querySelector('input[name="processname[]"]').value,
            commandLine: row.querySelector('input[name="commandline[]"]').value,
            restartDate: row.querySelector('input[name="restartdate[]"]').value,
            restartTime: row.querySelector('input[name="restarttime[]"]').value,
        };
        dataToSave.push(rowData);
    });

    fetch('/updateRecords', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(dataToSave)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('Records updated successfully');
        } else {
            alert('Error updating records');
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
}

// Sidebar toggle function
const sidebarToggle = () => {
	document.body.classList.toggle('sidebar-open')
}

// Sidebar trigger
const sidebarTrigger = document.getElementById('main-header__sidebar-toggle')

// Add the event listener for the sidebar toggle
sidebarTrigger.addEventListener('click', sidebarToggle)



// Sidebar collapse function
const sidebarCollapse = () => {
	document.body.classList.toggle('sidebar-collapsed')
}

// Sidebar trigger
const sidebarCollapseTrigger = document.getElementById('sidebar__collapse')

// Add the event listener for the sidebar toggle
sidebarCollapseTrigger.addEventListener('click', sidebarCollapse)



// Theme switcher function
const switchTheme = () => {
	// Get root element and data-theme value
	const rootElem = document.documentElement
	let dataTheme = rootElem.getAttribute('data-theme'),
		newTheme

	newTheme = (dataTheme === 'light') ? 'dark' : 'light'

	// Set the new HTML attribute
	rootElem.setAttribute('data-theme', newTheme)

	// Set the new local storage item
	localStorage.setItem("theme", newTheme)
}

// Add the event listener for the theme switcher
//document.querySelector('#sidebar__theme-switcher').addEventListener('click', switchTheme)





 




