  
let selectedhost = '';
let customerName = '';
let selectedAppname = '';
let selectedserverDropdown ='';
let selectedcustomerDropdown ='';
let Appname ='';
let selectedValue ='';
let serverNames = {}; // Server names come from the tags array
let componentNames ={};
let fetchedApp = {};
let fetchedApplication ={};

// Function to add a new row to the table
function addRow() {
    var table = document.getElementById("dynamicTable").getElementsByTagName('tbody')[0];
    var newRow = table.insertRow();

    var cols = ['componentName', 'path', 'filename', 'specificTag', 'referenceKey'];

    cols.forEach(function(col) {
        var cell = newRow.insertCell();
        var input = document.createElement("input");
        input.type = "text";
        input.name = col + '[]';
        input.className = "form-control";
        cell.appendChild(input);
    });

    // Adding the Config Type dropdown
    var configTypeCell = newRow.insertCell();
    var select = document.createElement("select");
    select.name = "configType[]";
    select.className = "form-control";

    var options = ["MEMORY", "THREAD", "CUSTOM"];
    options.forEach(function(option) {
        var opt = document.createElement("option");
        opt.value = option.toLowerCase();
        opt.textContent = option;
        select.appendChild(opt);
    });

    configTypeCell.appendChild(select);

    // Adding the delete button
    var actionsCell = newRow.insertCell();
    var deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "btn ";
    deleteButton.innerHTML = "Delete";
    deleteButton.onclick = function() {deleterecord(this); };
    actionsCell.appendChild(deleteButton);
}

// Function to delete a row from the table
function deleterecord(button) {
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

function deleterecord(button) {
  var row = button.parentNode.parentNode;
  row.parentNode.removeChild(row);
}

function addRow() {
  var table = document.getElementById("dynamicTable").getElementsByTagName('tbody')[0];
  var newRow = table.insertRow();
  
  newRow.innerHTML = `
    <td><input type="text" name="componentName[]" class="form-control"></td>
    <td><input type="text" name="path[]" class="form-control"></td>
    <td><input type="text" name="filename[]" class="form-control"></td>
    <td><input type="text" name="specificTag[]" class="form-control"></td>
    <td><input type="text" name="referenceKey[]" class="form-control"></td>
    <td>
                          <select name="configType[]" class="form-control">
                              <option value="memory">MEMORY</option>
                              <option value="thread">THREAD</option>
                              <option value="custom">CUSTOM</option>
                              <!-- Add more options as needed -->
                          </select>
                      </td>
                      <td>
                          <button type="button" class="btn btn-outline-light form-control" onclick="deleterecord(this)">Delete</button>
                      </td>
  `;
}

function addAppRow() {
    var table = document.getElementById("appTable").getElementsByTagName('tbody')[0];
    var newRow = table.insertRow();

    newRow.innerHTML = `
	<td class="td3"><input type="text" name="tempId[]" class="form-control"></td>
        <td class="td3"><input type="text" name="componentName[]" class="form-control"></td>
        <td class="td3"><input type="text" name="path[]" class="form-control"></td>
        <td class="td3"><input type="text" name="filename[]" class="form-control"></td>
        <td class="td3"><input type="text" name="specificTag[]" class="form-control"></td>
        <td class="td3"><input type="text" name="referenceKey[]" class="form-control"></td>
        <td class="td3">
            <select name="configType[]" class="form-control">
                <option value="memory">MEMORY</option>
                <option value="thread">THREAD</option>
                <option value="custom">CUSTOM</option>
            </select>
        </td>
        <td class="td3">
            <button type="button" class="btn" onclick="saveAppRow(this)">Save</button>
            <button type="button" class="btn" onclick="deleteAppRow(this)">Delete</button>
        </td>
    `;
}



function addMemory() {
    var table = document.getElementById("appConfigTableBody");
    var newRow = table.insertRow();

    // Insert a new cell for each column
    var componentNameCell = newRow.insertCell();
    var componentSelectCell = newRow.insertCell();
    var pathCell = newRow.insertCell();
    var filenameCell = newRow.insertCell();
    var specificTagCell = newRow.insertCell();
    var referenceKeyCell = newRow.insertCell();
    var actionsCell = newRow.insertCell();

    // Create and append the input fields for the respective columns
    componentNameCell.innerHTML = `<input type="text" name="componentName[]" class="form-control">`;

    // Create the dropdown for components
    var dropdown = document.createElement("select");
    dropdown.name = "component[]";
    dropdown.className = "form-control memoryConfigDropdown";
    componentSelectCell.appendChild(dropdown);  // Append the dropdown to the cell

    // Add event listener for dropdown change
    dropdown.addEventListener('change', function() {
        var selectedValue = this.value;  // Get the selected value from the dropdown
        console.log("Memory Dropdown value changed:", selectedValue);

        // Send GET request to backend to fetch details for selected component
        fetch(`http://localhost:8060/fetchHostedDetails?component=${encodeURIComponent(selectedValue)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json();  // Parse the response as JSON
            })
            .then(data => {
                console.log("Fetched data:", data);

                // Populate the remaining cells in the row with the fetched data
                if (data.length > 0) {
                    var details = data[0];  // Assume only one row is returned

                    pathCell.innerHTML = `<input type="text" value="${details.path}" name="path[]" class="form-control">`;
                    filenameCell.innerHTML = `<input type="text" value="${details.filename}" name="filename[]" class="form-control">`;
                    specificTagCell.innerHTML = `<input type="text" value="${details.specificTag}" name="specificTag[]" class="form-control">`;
                    referenceKeyCell.innerHTML = `<input type="text" value="${details.referenceKey}" name="referenceKey[]" class="form-control">`;
                } else {
                    console.error('No details found for the selected component');
                }
            })
            .catch(error => {
                console.error('Error fetching data:', error);
            });
    });

    // Create and append the remaining input fields
    pathCell.innerHTML = `<input type="text" name="path[]" class="form-control">`;
    filenameCell.innerHTML = `<input type="text" name="filename[]" class="form-control">`;
    specificTagCell.innerHTML = `<input type="text" name="specificTag[]" class="form-control">`;
    referenceKeyCell.innerHTML = `<input type="text" name="referenceKey[]" class="form-control">`;

    // Create action buttons and append them
    actionsCell.innerHTML = `
    <button type="button" class="btn" onclick="saveRow(this, 'memory')">Save</button>
    <button type="button" class="btn" onclick="deleteRow(this)">Delete</button>
`;


    // Fetch the components for the selected application and populate the dropdown
    fetchMemoryComponentForApp(selectedApplicationname, dropdown);  // Pass the dropdown to be populated
}

        
		
function saveRow(button, configType) {
    var row = button.closest('tr'); // Find the closest row
    
    // Convert referenceKey input (comma-separated) into an array
    var referenceKeyInput = row.querySelector('input[name="referenceKey[]"]').value;
    var referenceKeyList = referenceKeyInput.split(',').map(key => key.trim()); // Split by comma and trim spaces

    var rowData = {
        path: row.querySelector('input[name="path[]"]').value,
        filename: row.querySelector('input[name="filename[]"]').value,
        specificTag: row.querySelector('input[name="specificTag[]"]').value,
        referenceKey: referenceKeyList, // Store as a list (array)
        configType: configType, // Pass the config type (memory/thread/custom)
        selectedComponent: row.querySelector('select[name="component[]"]').value,
        selectedServer: selectedserverDropdown, // Global variable for selected server
        selectedCustomer: selectedcustomerDropdown, // Global variable for selected customer
		applicationName: selectedApplicationname
    };

    console.log("Saving row data:", JSON.stringify(rowData));

    // Send the data to the backend via POST
    fetch('http://localhost:8060/saveComponentDetails', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(rowData) // Send the row data to the backend
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }
        return response.json(); // Parse the response as JSON
    })
    .then(data => {
        console.log("Data saved successfully:", data);
        alert("Data saved successfully!");
        // Handle any further actions after successful save
    })
    .catch(error => {
        console.error("Error saving data:", error);
        alert("Error saving data: " + error.message);
    });
}




function addThread() {
    var table = document.getElementById("appConfigThreadsTableBody");
    var newRow = table.insertRow();

    // Insert a new cell for each column
    var componentNameCell = newRow.insertCell();
    var componentSelectCell = newRow.insertCell();
    var pathCell = newRow.insertCell();
    var filenameCell = newRow.insertCell();
    var specificTagCell = newRow.insertCell();
    var referenceKeyCell = newRow.insertCell();
    var actionsCell = newRow.insertCell();

    // Create and append the input fields for the respective columns
    componentNameCell.innerHTML = `<input type="text" name="componentName[]" class="form-control">`;

    // Create the dropdown for components
    var dropdown = document.createElement("select");
    dropdown.name = "component[]";
    dropdown.className = "form-control threadConfigDropdown";
    
    // Add placeholder option and append the dropdown to the cell
    dropdown.innerHTML = `
        <option value="" disabled selected>Select component</option>
    `;
    componentSelectCell.appendChild(dropdown);

    // Dynamically populate dropdown with components using the fetchMemoryComponentForApp function
    fetchMemoryComponentForApp(selectedApplicationname, dropdown); 

    // Add event listener for dropdown change
    dropdown.addEventListener('change', function() {
        var selectedValue = this.value;  // Get the selected value from the dropdown
        console.log("Selected component:", selectedValue);

        // Send GET request to backend to fetch details for selected component
        fetch(`http://localhost:8060/fetchHostedDetails?component=${encodeURIComponent(selectedValue)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json();  // Parse the response as JSON
            })
            .then(data => {
                // Log the fetched data to verify
                console.log("Fetched data:", data);

                // Populate the remaining cells in the row with the fetched data
                if (data.length > 0) {
                    var details = data[0];  // Assume only one row is returned

                    pathCell.innerHTML = `<input type="text" value="${details.path}" name="path[]" class="form-control">`;
                    filenameCell.innerHTML = `<input type="text" value="${details.filename}" name="filename[]" class="form-control">`;
                    specificTagCell.innerHTML = `<input type="text" value="${details.specificTag}" name="specificTag[]" class="form-control">`;
                    referenceKeyCell.innerHTML = `<input type="text" value="${details.referenceKey}" name="referenceKey[]" class="form-control">`;
                } else {
                    console.error('No details found for the selected component');
                }
            })
            .catch(error => {
                console.error('Error fetching data:', error);
            });
    });

    // Create and append the remaining input fields (empty initially)
    pathCell.innerHTML = `<input type="text" name="path[]" class="form-control">`;
    filenameCell.innerHTML = `<input type="text" name="filename[]" class="form-control">`;
    specificTagCell.innerHTML = `<input type="text" name="specificTag[]" class="form-control">`;
    referenceKeyCell.innerHTML = `<input type="text" name="referenceKey[]" class="form-control">`;

    // Create action buttons and append them to the row
    actionsCell.innerHTML = `
    <button type="button" class="btn" onclick="saveRow(this, 'thread')">Save</button>
    <button type="button" class="btn" onclick="deleteRow(this)">Delete</button>
`;


}


// Add Custom row
function addCustom() {
    var table = document.getElementById("appConfigCustomTableBody");
    var newRow = table.insertRow();

    // Insert a new cell for each column
    var componentNameCell = newRow.insertCell();
    var componentSelectCell = newRow.insertCell();
    var pathCell = newRow.insertCell();
    var filenameCell = newRow.insertCell();
    var specificTagCell = newRow.insertCell();
    var referenceKeyCell = newRow.insertCell();
    var actionsCell = newRow.insertCell();

    // Create and append the input fields for the respective columns
    componentNameCell.innerHTML = `<input type="text" name="componentName[]" class="form-control">`;

    // Create the dropdown for components
    var dropdown = document.createElement("select");
    dropdown.name = "component[]";
    dropdown.className = "form-control customConfigDropdown";
    componentSelectCell.appendChild(dropdown);  // Append the dropdown to the cell

    // Add event listener for dropdown change
    dropdown.addEventListener('change', function() {
        var selectedValue = this.value;  // Get the selected value from the dropdown
        console.log("Custom Dropdown value changed:", selectedValue);

        // Send GET request to backend to fetch details for selected component
        fetch(`http://localhost:8060/fetchHostedDetails?component=${encodeURIComponent(selectedValue)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error("Network response was not ok");
                }
                return response.json();  // Parse the response as JSON
            })
            .then(data => {
                console.log("Fetched data:", data);

                // Populate the remaining cells in the row with the fetched data
                if (data.length > 0) {
                    var details = data[0];  // Assume only one row is returned

                    pathCell.innerHTML = `<input type="text" value="${details.path}" name="path[]" class="form-control">`;
                    filenameCell.innerHTML = `<input type="text" value="${details.filename}" name="filename[]" class="form-control">`;
                    specificTagCell.innerHTML = `<input type="text" value="${details.specificTag}" name="specificTag[]" class="form-control">`;
                    referenceKeyCell.innerHTML = `<input type="text" value="${details.referenceKey}" name="referenceKey[]" class="form-control">`;
                } else {
                    console.error('No details found for the selected component');
                }
            })
            .catch(error => {
                console.error('Error fetching data:', error);
            });
    });

    // Create and append the remaining input fields
    pathCell.innerHTML = `<input type="text" name="path[]" class="form-control">`;
    filenameCell.innerHTML = `<input type="text" name="filename[]" class="form-control">`;
    specificTagCell.innerHTML = `<input type="text" name="specificTag[]" class="form-control">`;
    referenceKeyCell.innerHTML = `<input type="text" name="referenceKey[]" class="form-control">`;

    // Create action buttons and append them
   actionsCell.innerHTML = `
    <button type="button" class="btn" onclick="saveRow(this, 'custom')">Save</button>
    <button type="button" class="btn" onclick="deleteRow(this)">Delete</button>
`;


    // Fetch the components for the selected application and populate the dropdown
    fetchMemoryComponentForApp(selectedApplicationname, dropdown);  // Pass the dropdown to be populated
}

document.getElementById('check').addEventListener('click', function() {
    const applicationName = document.getElementById('Appname').value;
    console.log(applicationName);
    // Check if Application Name is not empty
    if (applicationName.trim() === "") {
        alert("Please enter an Application Name before checking.");
    } else {
        // Call fetchDetails if Application Name is not empty
        fetchDetails();
    }
});

 function fetchDetails() {
        const applicationName = document.getElementById('Appname').value;

        // Perform an AJAX request to the backend
        fetch(`http://localhost:8060/fetchHostedComponentDetails?application_name=${encodeURIComponent(applicationName)}`)
            .then(response => response.json())
            .then(data => {
                const tableBody = document.querySelector('#appTable tbody');
                tableBody.innerHTML = ''; // Clear any previous data

                // Loop through the data and add a new row for each record
                data.forEach(item => {
                    const row = document.createElement('tr');

                    // Populate the new row with data
                    row.innerHTML = `
					<td class="td3"><input type="text" name="tempId[]" class="form-control" readonly value="${item.tempId || ''}"></td>
                        <td class="td3"><input type="text" name="componentName[]" class="form-control" readonly value="${item.componentName || ''}"></td>
                        <td class="td3"><input type="text" name="path[]" class="form-control" readonly value="${item.path || ''}"></td>
                        <td class="td3"><input type="text" name="filename[]" class="form-control" readonly value="${item.filename || ''}"></td>
                        <td class="td3"><input type="text" name="specificTag[]" class="form-control"  readonly value="${item.specificTag || ''}"></td>
                        <td class="td3"><input type="text" name="referenceKey[]" class="form-control" readonly value="${item.referenceKey || ''}"></td>
                        <td class="td3"><input type="text" name="configType[]" class="form-control" readonly value="${item.configType || ''}"></td>
                        <td class="td3">
                            <button type="button" class="btn" onclick="editAppRow(this)">Edit</button>
                            <button type="button" class="btn" onclick="deleteAppRow(this)">Delete</button>
                        </td>
                    `;

                    // Append the new row to the table
                    tableBody.appendChild(row);
                });
            })
            .catch(error => {
                console.error('Error fetching data:', error);
            });
    }
	
// Function to toggle editing mode and handle saving
// Function to edit and save row
function editAppRow(button) {
    var row = button.closest('tr'); // Find the closest row element
    var inputs = row.querySelectorAll('input'); // Select all input elements in the row
    var isReadOnly = inputs[0].hasAttribute('readonly'); // Check if inputs are readonly

    if (isReadOnly) {
        // Change to editable mode
        inputs.forEach(function(input) {
            input.removeAttribute('readonly'); // Remove readonly attribute
        });
        button.textContent = "Save"; // Change button text to "Save"
        row.setAttribute('data-updated', 'true'); // Mark the row as updated
        console.log("Editing mode activated.");
    } else {
        // Change to read-only mode
        var hasChanges = false;
        inputs.forEach(function(input) {
            if (input.value !== input.defaultValue) { // Check if value has changed
                hasChanges = true; // Set flag if any value has changed
            }
            input.setAttribute('readonly', true); // Add readonly attribute
        });
        button.textContent = "Edit"; // Change button text to "Edit"

        if (hasChanges) {
            row.setAttribute('data-updated', 'true'); // Mark the row as updated if there are changes
            console.log("Row marked as updated.");
        } else {
            row.removeAttribute('data-updated'); // Remove the data-updated attribute if no changes
            console.log("No changes detected. Row marked as not updated.");
        }
    }
}

document.getElementById('SubmitApp').addEventListener('click', function() {
    var rows = document.querySelectorAll('tbody tr[data-updated="true"]'); // Select only updated rows
    console.log('Rows to be processed:', rows);

    var tableData = [];

    rows.forEach(function(row) {
        var inputs = row.querySelectorAll('input');
        var rowData = {};

        inputs.forEach(function(input) {
            if (input.name === 'referenceKey[]') {
                // Handle referenceKey as an array
                if (!rowData.referenceKey) {
                    rowData.referenceKey = [];
                }
                rowData.referenceKey.push(input.value); // Capture referenceKey values as an array
            } else {
                rowData[input.name.replace('[]', '')] = input.value; // Capture other fields
            }
        });

        console.log('Row data:', rowData);

        tableData.push(rowData);
    });

    if (tableData.length > 0) {
        fetch('http://localhost:8060/updateComponentConfig', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                data: tableData // Send only the data
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(result => {
            if (result.status === 'success') {
                alert('Success: Configuration updated successfully!');
            } else if (result.status === 'error') {
                alert('Error: ' + result.message);
            }
        })
        .catch(error => {
            alert('Error: ' + error.message);
            console.error('Error:', error);
        });
    }
});



// Function to handle row deletion
function deleteAppRow(button) {
    if (confirm('Are you sure you want to delete this record?')) {
        const row = button.closest('tr');
        const inputs = row.querySelectorAll('input, select');
        const tempId = inputs[0].value; // Assuming the first input contains tempId

        // Send delete request
        fetch('http://localhost:8060/deleteComponentConfig', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: tempId })
        })
        .then(response => response.json())
        .then(result => {
            if (result.status === 'success') {
                alert('Record deleted successfully.');
                row.remove(); // Remove row from the table
            } else {
                alert('Deletion failed: ' + result.message);
            }
        })
        .catch(error => {
            console.error('Error deleting record:', error);
        });
    }
}

function saveAppRow(button) {
    var row = button.closest('tr'); // Find the closest row
    
    // Convert referenceKey input (comma-separated) into an array
    var referenceKeyInput = row.querySelector('input[name="referenceKey[]"]').value;
    var referenceKeyList = referenceKeyInput.split(',').map(key => key.trim()); // Split by comma and trim spaces
	const applicationName = document.getElementById('Appname').value;

    // Collect row data
    var rowData = {
        path: row.querySelector('input[name="path[]"]').value,
        filename: row.querySelector('input[name="filename[]"]').value,
        specificTag: row.querySelector('input[name="specificTag[]"]').value,
        referenceKey: referenceKeyList, // Store as a list (array)
        configType: row.querySelector('select[name="configType[]"]').value, // Get the selected config type
        selectedComponent: row.querySelector('input[name="componentName[]"]').value,
        applicationName: applicationName // Assuming Appname is defined elsewhere
    };

    console.log("Saving row data:", JSON.stringify(rowData));

    // Send the data to the backend via POST
    fetch('http://localhost:8060/saveComponentAppDetails', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(rowData) // Send the row data to the backend
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Network response was not ok");
        }
        return response.json(); // Parse the response as JSON
    })
    .then(data => {
        console.log("Data saved successfully:", data);
        alert("Data saved successfully!");
        // Handle any further actions after successful save
    })
    .catch(error => {
        console.error("Error saving data:", error);
        alert("Error saving data: " + error.message);
    });
}

function openTab(evt, tabName) {
    // Get all elements with class="tabcontent" and hide them
    const tabContent = document.getElementsByClassName('tabcontent');
    for (let i = 0; i < tabContent.length; i++) {
        tabContent[i].style.display = 'none';
    }

    // Get all elements with class="tablinks" and remove the class "active"
    const tabLinks = document.getElementsByClassName('tablinks');
    for (let i = 0; i < tabLinks.length; i++) {
        tabLinks[i].classList.remove('active');
    }

    // Show the current tab, and add an "active" class to the button that opened the tab
    document.getElementById(tabName).style.display = 'block';
    evt.currentTarget.classList.add('active');
}

// Automatically click on the appHost tab to show it by default
document.addEventListener('DOMContentLoaded', function() {
    const appHostButton = document.querySelector('#appHostButton');
    if (appHostButton) {
        appHostButton.click(); // Simulate a click on the appHost tab button
    }
});


function collectDataAndSubmit() {
    const ApplicationName = document.getElementById("Applicationname").value;
    const installationDrive = document.getElementById("defaulthost").value;

    const tableData = [];
    const table = document.getElementById("dynamicTable");
    for (let i = 1; i < table.rows.length; i++) {
        const row = table.rows[i];
        const path = row.cells[1].querySelector('input').value;

        const rowData = {
            componentName: row.cells[0].querySelector('input').value,
            path: installationDrive + ":" + "/" + path,  // Concatenate the drive and path
            filename: row.cells[2].querySelector('input').value,
            specificTag: row.cells[3].querySelector('input').value,
            referenceKey: row.cells[4].querySelector('input').value,
            configType: row.cells[5].querySelector('select').value
        };
        tableData.push(rowData);
    }

    const data = {
        ApplicationName: ApplicationName,
        installationDrive: installationDrive,
        tableRows: tableData
    };

    fetch('http://localhost:8060/saveData', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(data => {
        console.log('Success:', data);
        alert('Data has been successfully saved!');
    })
    .catch((error) => {
        console.error('Error:', error);
        alert('Something went wrong!');
    });
}


// Function to fetch data and populate the dropdown
async function loadApplicationData() {
    try {
        const response = await fetch('http://localhost:8060/getData');
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
        console.log(data);
        
        const customerDropdown = document.getElementById('appDropdown');
        customerDropdown.innerHTML = ''; // Clear existing options
        data.applications.forEach(application => {
            const option = document.createElement('li');
            option.classList.add('dropdown-item');
            option.textContent = application;
            option.onclick = () => show(application, '.app-textBox', '#dropdownMenuButton1');
            customerDropdown.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading dropdown data:', error);
    }
}

// Trigger loading of application data on dropdown button click
document.getElementById('dropdownMenuButton1').addEventListener('click', function() {
    loadApplicationData();
});


async function fetchComponentForApp(selectedAppname) {
            try {
                console.log("Selected App:", selectedAppname);
                const url = `http://localhost:8060/fetchComponents?selectedAppname=${encodeURIComponent(selectedAppname)}`;
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
                                                                fetchedApp = data.components || [];
                populateComponentDropdown(data.components);
                console.log("Response server data:", data.components);

            } catch (error) {
                console.error('There has been a problem with your fetch operation:', error);
                alert('Error fetching components: ' + error.message);
            }
        }

function show(value, selector, buttonId) {
    const selectedElement = document.querySelector(selector);
    const buttonElement = document.querySelector(buttonId);

    if (selectedElement && buttonElement) {
        selectedElement.value = value;
        buttonElement.innerText = value;

        if (selector === '.app-textBox') {
            selectedAppname = value;
            localStorage.setItem('selectedAppname', value);
            handleAppSelection(); // Fetch application-related data
        } else if (selector === '.host-textBox') {
            selectedhost = value;
            // Uncomment and implement if needed
            // handleHostSelection(); // Fetch host-related data
        } else if (selector === '.Application-textBox') {
            selectedApplicationname = value;
            localStorage.setItem('selectedApplicationname', value);
            handleApplicationSelection(); // Fetch customers related to application
        } else if (selector === '.customerDropdown-textBox') {
            selectedcustomerDropdown = value;
            localStorage.setItem('customerDropdown', value);
            // Uncomment and implement if needed
            handleServerSelection(); // Fetch servers if required
        }else if (selector === '.serverDropdown-textBox') {
            selectedserverDropdown = value;
            localStorage.setItem('serverDropdown', value);
            // Uncomment and implement if needed
            handleServerSelection(); // Fetch servers if required
        }

        console.clear();
        console.log("Selected app: " + selectedAppname);
        console.log("Selected host: " + selectedhost);
                                console.log("Selected Application: " + selectedApplicationname);
                                console.log("Selected customer: " + selectedcustomerDropdown);
                                console.log("Selected server: " + selectedserverDropdown);
    } else {
        console.error('Invalid selector or button ID');
    }
}

async function handleApplicationSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    await fetchCustomerForApplication(selectedApplicationname); // Fetch customer details

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}

async function fetchCustomerForApplication(selectedApplicationname) {
    try {
        console.log("Selected App:", selectedApplicationname);
        const url = `http://localhost:8060/fetchAppCustomers?selectedApplicationname=${encodeURIComponent(selectedApplicationname)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.json();
        if (data.error) {
            throw new Error(data.error);
        }

        console.log("Response data:", data);
        fetchedApplication = data.customers || [];
        populateCustomerDropdown(fetchedApplication);
        console.log("Response server data:", fetchedApplication);

    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
        alert('Error fetching components: ' + error.message);
    }
}

function populateCustomerDropdown(customers) {
    const customerDropdown = document.getElementById('customerDropdown');
    if (customerDropdown) {
        customerDropdown.innerHTML = ''; // Clear existing options only when necessary

        customers.forEach(customer => {
            const option = document.createElement('li');
            option.className = 'dropdown-item';
            option.textContent = customer;
            option.onclick = () => show(customer, '.customerDropdown-textBox', '#customerDropdownButton');
            customerDropdown.appendChild(option);
        });
    }
}

                                
function populateComponentDropdown(components) {
    const hostDropdown = document.getElementById('hostDropdownButton');
    const hostTextBox = document.querySelector('.host-textBox');
    const selectedItems = [];

    if (hostDropdown) {
        hostDropdown.innerHTML = ''; // Clear existing options

        components.forEach(component => {
            const div = document.createElement('div');
            div.className = 'dropdown-item';
            div.setAttribute('data-value', component.componentName);
            div.textContent = component.componentName;

            // Handle selection of components
            div.onclick = () => {
                if (!selectedItems.includes(component.componentName)) {
                    selectedItems.push(component.componentName);
                } else {
                    const index = selectedItems.indexOf(component.componentName);
                    if (index > -1) {
                        selectedItems.splice(index, 1);
                    }
                }
                hostTextBox.value = selectedItems.join(',');
                updateSelectedItems(selectedItems);
            };

            hostDropdown.appendChild(div);
        });
    }
}

// Helper function to update the selected items in the dropdown
function updateSelectedItems(selectedItems) {
    const selectedElement = document.querySelector('.select-selected');
    selectedElement.textContent = selectedItems.length > 0 ? selectedItems.join(', ') : 'Select options';
}

// Example of how to trigger fetchComponentForApp when an application is selected
document.getElementById('dropdownMenuButton1').addEventListener('click', function() {
    const selectedAppname = this.textContent.trim();
    fetchComponentForApp(selectedAppname);
});

async function handleAppSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    await fetchComponentForApp(selectedAppname); // Fetch server details

    // Wait until fetchedApp is populated
    while (Object.keys(fetchedApp).length === 0) {
        console.log("Waiting for fetched process details...");
        await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for 1 second
    }

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}


const ul = document.querySelector("#tags");
const input = ul.querySelector("input");
let tags = [];

createTag();

function createTag() {
    console.log("Creating tags list..."); // Debugging
    ul.querySelectorAll("li").forEach(li => {
        console.log(`Removing existing tag element: ${li.textContent}`); // Debugging
        li.remove();
    });
    
    tags.slice().reverse().forEach(tag => {
        console.log(`Adding tag: ${tag}`); // Debugging
        let liTag = `<li>${tag} <i class="uit uit-multiply" onclick="remove(this, '${tag}')"></i></li>`;
        ul.insertAdjacentHTML("afterbegin", liTag);
    });
    console.log("Tags after creation:", tags); // Debugging
}

function remove(element, tag) {
    console.log(`Removing tag: ${tag}`); // Debugging
    let index = tags.indexOf(tag);
    tags = [...tags.slice(0, index), ...tags.slice(index + 1)];
    element.parentElement.remove();
    console.log("Tags after removal:", tags); // Debugging
}

function addTag(e) {
    if (e.key === "Enter") {
        console.log(`Enter key pressed with input: ${e.target.value}`); // Debugging
        let tag = e.target.value.replace(/\s+/g, ' ').trim();
        if (tag.length > 1 && !tags.includes(tag)) {
            console.log(`Adding new tag: ${tag}`); // Debugging
            tag.split(',').forEach(tag => {
                tags.push(tag);
                createTag();
            });
        } else {
            console.log(`Tag is too short or already exists: ${tag}`); // Debugging
        }
        e.target.value = "";
    }
}

input.addEventListener("keyup", addTag);

// Function to get the values of all tags (for reference)
function getTags() {
    console.log("Getting all tags:", tags); // Debugging
    return tags;
}

async function storeCustomerHostDetailsInDatabase(selectedAppname, customerName, serverNames, componentNames) {
    try {
        const response = await fetch('http://localhost:8060/insertEntries', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                selectedAppname: selectedAppname,
                customerName: customerName,
                serverNames: serverNames, // Array of server names
                componentNames: componentNames // Array of component names
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            if (errorData.status === "error" && errorData.message.includes("Duplicate entry")) {
                alert('Duplicate entry. The data already exists.');
            } else {
                alert('Failed to insert data: ' + errorData.message);
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Response data:', data);
        alert('Data successfully submitted.');
    } catch (error) {
        console.error('Error submitting data:', error);
        alert('Error submitting data.');
    }
}


// Function to submit data
function submitData() {
     selectedAppname;/* retrieve selected app name */;
     customerName = getCustomerName();/* retrieve customer name */;
     serverNames = tags; // Server names come from the tags array
     componentNames = document.querySelector('.host-textBox').value.split(','); // Split components by comma

    storeCustomerHostDetailsInDatabase(selectedAppname, customerName, serverNames, componentNames);
}


document.addEventListener("DOMContentLoaded", function () {
    const selectedElement = document.querySelector('.select-selected');
    const itemsElement = document.querySelector('.select-items');
    const hostTextBox = document.querySelector('.host-textBox');

    // Toggle the visibility of the dropdown
    selectedElement.addEventListener('click', function () {
        this.classList.toggle('active');
        itemsElement.style.display = itemsElement.style.display === 'block' ? 'none' : 'block';
    });

    // Handle the selection of items
    itemsElement.addEventListener('click', function (e) {
        if (e.target.tagName === 'DIV') {
            e.target.classList.toggle('selected');
            updateSelectedItems();
        }
    });

    // Update selected items display and hidden input
    function updateSelectedItems() {
        const selectedItems = itemsElement.querySelectorAll('.selected');
        const selectedValues = [];
        selectedItems.forEach(item => {
            selectedValues.push(item.getAttribute('data-value'));
        });
        selectedElement.innerText = selectedValues.length > 0 ? selectedValues.join(', ') : 'Select options';
        hostTextBox.value = selectedValues.join(',');
    }

    // Close the dropdown if clicked outside
    document.addEventListener('click', function (e) {
        if (!selectedElement.contains(e.target)) {
            selectedElement.classList.remove('active');
            itemsElement.style.display = 'none';
        }
    });
});

  // Function to get the customer name input
    function getCustomerName() {
        const customerNameInput = document.getElementById('customerName');
        const customerName = customerNameInput.value.trim();
        return customerName;
    }

document.addEventListener('DOMContentLoaded', () => {

    // Function to check servers for the specific customer
    async function checkServersForCustomer(customerName, serverNames) {
    const validationMessage = document.getElementById('validationMessage');

    if (serverNames.length > 0) {
        try {
            // Properly format and encode the serverNames array
            const encodedServerNames = encodeURIComponent(JSON.stringify(serverNames));
            const url = `http://localhost:8060/checkServerAssignment?serverNames=${encodedServerNames}&customerName=${encodeURIComponent(customerName)}`;
            console.log(`Request URL: ${url}`); // Debugging line

            const response = await fetch(url, { method: 'GET' });

            console.log(`Response status: ${response.status}`); // Debugging line

            if (!response.ok) {
                throw new Error(`Network response was not ok. Status: ${response.status}`);
            }

            const data = await response.json();
            console.log('Response data:', data); // Debugging line

            if (data.error) {
                throw new Error(data.error);
            }

            if (data.assignedServers.length > 0) {
              
                alert(`The following servers are already assigned to another customer: ${data.assignedServers.join(', ')}.`);
            } else {
                
                alert(`All servers are available for assignment.`);
            }
        } catch (error) {
            console.error('Error checking server assignment:', error);
            alert('Error checking server assignment.');
        }
    }
}

// Event listener for input blur
const ul = document.getElementById('tags');
const input = ul.querySelector("input");

if (input) {
    console.log("Input field within 'tags' found."); // Debugging
    input.addEventListener('blur', async () => {
        console.log("Input field lost focus, triggering server check..."); // Debugging
        const serverNames = tags; // Use the tags array as server names
        const customerName = getCustomerName();
        console.log("Retrieved customer name:", customerName); // Debugging
        console.log("Current server names (tags):", serverNames); // Debugging

        if (!customerName) {
            alert('Please enter a customer name.');
            console.warn("Customer name is missing."); // Debugging
            return;
        }

        if (serverNames.length > 0) {
            await checkServersForCustomer(customerName, serverNames);
        } else {
            alert('Please enter at least one server name.');
            console.warn("No server names (tags) available to check."); // Debugging
        }
    });
} else {
    console.error('The input element within the "tags" list was not found.'); // Debugging
}
});

async function loadApplicationHostData() {
    try {
        const response = await fetch('http://localhost:8060/getAppData');
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
        console.log(data);
        
        const ApplicationDropdown = document.getElementById('ApplicationDropdown');
        ApplicationDropdown.innerHTML = ''; // Clear existing options

        data.applications.forEach(application => {
            const option = document.createElement('li');
            option.classList.add('dropdown-item');
            option.textContent = application;
            option.onclick = () => show(application, '.Application-textBox', '#dropdownAppMenuButton1');
            ApplicationDropdown.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading dropdown data:', error);
    }
}

document.getElementById('dropdownAppMenuButton1').addEventListener('click', function() {
    loadApplicationHostData();
});

async function handleServerSelection() {
    const loadingScreen = document.querySelector('.loading-screen');
    
    if (loadingScreen) {
        loadingScreen.style.display = 'flex'; // Show the loading screen
    }

    await fetchServerForCustomer(selectedcustomerDropdown); // Fetch customer details

    if (loadingScreen) {
        loadingScreen.style.display = 'none'; // Hide the loading screen
    }
}

async function fetchServerForCustomer(selectedcustomerDropdown) {
    try {
        console.log("Selected App:", selectedcustomerDropdown);
        const url = `http://localhost:8060/fetchCustServers?selectedcustomerDropdown=${encodeURIComponent(selectedcustomerDropdown)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.json();
        if (data.error) {
            throw new Error(data.error);
        }

        console.log("Response data:", data);
        fetchedServers = data.servers || [];
        populateServerDropdown(fetchedServers);
        console.log("Response server data:", fetchedServers);

    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
        alert('Error fetching components: ' + error.message);
    }
}


function populateServerDropdown(servers) {
    const serverDropdown = document.getElementById('serverDropdown');
    if (serverDropdown) {
        serverDropdown.innerHTML = ''; // Clear existing options only when necessary

        servers.forEach(server => {
            const option = document.createElement('li');
            option.className = 'dropdown-item';
            option.textContent = server;
            option.onclick = () => show(server, '.serverDropdown-textBox', '#serverDropdownButton');
            serverDropdown.appendChild(option);
        });
    }
}


document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM fully loaded and parsed');
    
    // Select the loading screen element
    const loadingScreen = document.querySelector('.loading-screen');
    const checkButton = document.getElementById('checkButton');
    
    console.log('Check button:', checkButton);

    // Function to show the loading screen
    function showLoading() {
        console.log('Showing loading screen');
        loadingScreen.style.display = 'flex';
    }

    // Function to hide the loading screen
    function hideLoading() {
        console.log('Hiding loading screen');
        loadingScreen.style.display = 'none';
    }
	
	function populateTable(tableId, data) {
    const tableBody = document.querySelector(`#${tableId} tbody`);
    tableBody.innerHTML = ''; // Clear existing rows

    if (data && data.length > 0) {
        data.forEach(item => {
            // Safely parse referenceKey if it's a string that looks like an array
			  const row = document.createElement('tr');
              row.setAttribute('data-id', item.id); // Set data-id attribute
            let referenceKey = item.referenceKey;
            if (typeof referenceKey === 'string' && referenceKey.startsWith('[') && referenceKey.endsWith(']')) {
                try {
                    referenceKey = JSON.parse(referenceKey).join(', ');
                } catch (error) {
                    console.error('Failed to parse referenceKey:', error);
                    referenceKey = item.referenceKey; // Fallback to the original string
                }
            }

           

            row.innerHTML = `
			    <td><input type="text" name="tempId[]" class="form-control" value="${item.tempId || ''}" readonly></td>
                <td><input type="text" name="componentName[]" class="form-control" value="${item.componentName || ''}" readonly></td>
                <td><input type="text" name="path[]" class="form-control" value="${item.path || ''}" readonly></td>
                <td><input type="text" name="filename[]" class="form-control" value="${item.filename || ''}" readonly></td>
                <td><input type="text" name="specificTag[]" class="form-control" value="${item.specificTag || ''}" readonly></td>
                <td><input type="text" name="referenceKey[]" class="form-control" value="${referenceKey || ''}" readonly></td>
                <td>
                    <button type="button" class="btn btn-outline-light form-control" onclick="editRow(this)">Edit</button>
                    <button type="button" class="btn btn-outline-light form-control" onclick="deleteRow(this)">Delete</button>
                </td>
            `;

            tableBody.appendChild(row);
        });
    } else {
        console.log(`No data available to populate the table with ID: ${tableId}`);
    }
}




    // Fetch data and populate tables
    async function fetchDataAndPopulateTables() {
    console.log('Button clicked, fetching data...');
    try {
        const selectedApplicationName = localStorage.getItem('selectedApplicationname');
        const selectedCustomerName = localStorage.getItem('customerDropdown');
        const selectedServerName = localStorage.getItem('serverDropdown');
        console.log(selectedApplicationName);
        console.log(selectedCustomerName);
        console.log(selectedServerName);

        if (!selectedApplicationName || !selectedCustomerName || !selectedServerName) {
            alert('Please select all fields.');
            return;
        }

        showLoading(); // Show the loading screen

        const url = `http://localhost:8060/fetchComponentData?applicationName=${encodeURIComponent(selectedApplicationName)}&customerName=${encodeURIComponent(selectedCustomerName)}&serverName=${encodeURIComponent(selectedServerName)}`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.json();
        console.log('Data fetched:', data);

        // Populate tables if there is at least one entry in the respective data array
        if (data.appConfigData && data.appConfigData.length > 0) {
            populateTable('appConfigTable', data.appConfigData);
        } else {
            console.log('No data for appConfigTable');
        }

        if (data.appConfigThreadsData && data.appConfigThreadsData.length > 0) {
            populateTable('appConfigThreadsTable', data.appConfigThreadsData);
        } else {
            console.log('No data for appConfigThreadsTable');
        }

        if (data.appConfigCustomData && data.appConfigCustomData.length > 0) {
            populateTable('appConfigCustomTable', data.appConfigCustomData);
        } else {
            console.log('No data for appConfigCustomTable');
        }

    } catch (error) {
        console.error('There was a problem fetching the data:', error);
        alert('Error fetching data: ' + error.message);
    } finally {
        hideLoading(); // Hide the loading screen
    }
}



    // Add event listener to the check button
    if (checkButton) {
        checkButton.addEventListener('click', fetchDataAndPopulateTables);
    } else {
        console.error('Check button not found');
    }
});

// Function to handle row editing
function editRow(button) {
    var row = button.closest('tr'); // Find the closest row element
    var inputs = row.querySelectorAll('input'); // Select all input elements in the row
    var isReadOnly = inputs[0].hasAttribute('readonly'); // Check if inputs are readonly

    if (isReadOnly) {
        // Change to editable mode
        inputs.forEach(function(input) {
            input.removeAttribute('readonly'); // Remove readonly attribute
        });
        button.textContent = "Save"; // Change button text to "Save"
        row.setAttribute('data-updated', 'true'); // Mark the row as updated
        console.log("Editing mode activated.");
    } else {
        // Change to read-only mode
        var hasChanges = false;
        inputs.forEach(function(input) {
            if (input.value !== input.defaultValue) { // Check if value has changed
                hasChanges = true; // Set flag if any value has changed
            }
            input.setAttribute('readonly', true); // Add readonly attribute
        });
        button.textContent = "Edit"; // Change button text to "Edit"
        
        if (hasChanges) {
            row.setAttribute('data-updated', 'true'); // Mark the row as updated if there are changes
            console.log("Row marked as updated.");
        } else {
            row.removeAttribute('data-updated'); // Remove the data-updated attribute if no changes
            console.log("No changes detected. Row marked as not updated.");
        }
    }
}


// Function to delete a row with confirmation and send data to backend
function deleteRow(button) {
    if (confirm("Are you sure you want to delete this record?")) {
        var row = button.closest('tr'); // Find the closest row element
        var tableId = row.closest('table').id; // Get the table ID
        var tempIdInput = row.querySelector('input[name="tempId[]"]'); // Select the tempId input field
        var recordId = tempIdInput.value; // Get the value of the tempId input field
        
        console.log(recordId); // For debugging
        console.log(tableId);  // For debugging

        fetch('http://localhost:8060/deleteConfig', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                table: tableId,
                id: recordId
            })
        })
        .then(response => response.json())
        .then(result => {
            console.log('Success:', result);
            row.remove(); // Remove the row from the table
        })
        .catch(error => console.error('Error:', error));
    }
}



// Function to handle form submission and send updated data to backend
document.getElementById('submitButton').addEventListener('click', function() {
    var tables = document.querySelectorAll('table');

    tables.forEach(function(table) {
        var rows = table.querySelectorAll('tbody tr[data-updated="true"]'); // Select only updated rows
        console.log('Rows to be processed:', rows); // Debug statement

        var tableData = [];

        rows.forEach(function(row) {
            var inputs = row.querySelectorAll('input');
            var rowData = {};

            inputs.forEach(function(input) {
                if (input.name === 'referenceKey[]') {
                    // Handle referenceKey as an array
                    if (!rowData.referenceKey) {
                        rowData.referenceKey = [];
                    }
                    rowData.referenceKey.push(input.value); // Capture referenceKey values as an array
                } else {
                    rowData[input.name.replace('[]', '')] = input.value; // Capture other fields
                }
            });

            console.log('Row data:', rowData); // Debug statement

            tableData.push(rowData);
        });

        console.log('Table data:', tableData); // Debug statement

        if (tableData.length > 0) {
            fetch('http://localhost:8060/updateConfig', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    table: table.id,
                    data: tableData // Check tableData for each table before sending
                })
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(result => {
                if (result.status === 'success') {
                    alert('Success: Configuration updated successfully!');
                } else if (result.status === 'error') {
                    alert('Error: ' + result.message);
                }
            })
            .catch(error => {
                alert('Error: ' + error.message);
                console.error('Error:', error);
            });
        }
    });
});


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

//Reload page 
function fetchMemoryComponentForApp(selectedApplicationname, dropdown) {
    fetch(`http://localhost:8060/fetchComponentsMemory?selectedApplicationname=${selectedApplicationname}`)
        .then(response => response.json())
        .then(data => {
            if (data.components) {
                dropdown.innerHTML = '';  // Clear the dropdown before adding options
                data.components.forEach(component => {
                    var option = document.createElement('option');
                    option.value = component;
                    option.textContent = component;
                    dropdown.appendChild(option);
                });
            }
        })
        .catch(error => {
            console.error("Error fetching components:", error);
        });
}

// Populate memory components in the dropdown
function populateMemoryComponentDropdown(components) {
    const dropdowns = document.querySelectorAll('.memoryConfigDropdown');

    dropdowns.forEach(dropdown => {
        dropdown.innerHTML = ''; // Clear existing options

        // Add components as options in the dropdown
        components.forEach(component => {
            const option = document.createElement('option');
            option.value = component.toLowerCase(); // Use the component as lowercase for value
            option.textContent = component; // Display the component name
            dropdown.appendChild(option);
        });
        
        // Log to check if options are being added
        console.log("Dropdown options populated for:", dropdown);
    });
}

// Populate thread components in the dropdown
function populateThreadComponentDropdown(components) {
    const dropdowns = document.querySelectorAll('.threadConfigDropdown');
    dropdowns.forEach(dropdown => {
        dropdown.innerHTML = ''; 
        components.forEach(component => {
            const option = document.createElement('option');
            option.value = component.toLowerCase();
            option.textContent = component;
            dropdown.appendChild(option);
        });
    });
}

// Populate custom components in the dropdown
function populateCustomComponentDropdown(components) {
    const dropdowns = document.querySelectorAll('.customConfigDropdown');
    dropdowns.forEach(dropdown => {
        dropdown.innerHTML = ''; 
        components.forEach(component => {
            const option = document.createElement('option');
            option.value = component.toLowerCase();
            option.textContent = component;
            dropdown.appendChild(option);
        });
    });
}
