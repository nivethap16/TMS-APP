// ****************** SCRIPT1.JS ************************** 
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
/*function reloadPage() {
  location.reload();
}

setInterval(reloadPage, 3000);*/// 3000 milliseconds = 3 seconds


function populateRestartHistoryTable(restartData) {
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
		
		const statusInput = document.createElement('input');
        statusInput.type = 'text';
        statusInput.name = 'status[]';
        statusInput.classList.add('form-control');
        statusInput.value = data.status;
		
		const resultMessageInput = document.createElement('input');
        resultMessageInput.type = 'text';
        resultMessageInput.name = 'resultMessage[]';
        resultMessageInput.classList.add('form-control');
        resultMessageInput.value = data.resultMessage;
 
        // Create row structure
        row.appendChild(createTableCell(customerNameInput));
        row.appendChild(createTableCell(serverNameInput));
        row.appendChild(createTableCell(processNameInput));
        row.appendChild(createTableCell(commandLineInput));  // Safely insert commandline input
        row.appendChild(createTableCell(restartDateInput));
        row.appendChild(createTableCell(restartTimeInput));
		row.appendChild(createTableCell(statusInput));
		row.appendChild(createTableCell(resultMessageInput));
 
        tableBody.appendChild(row);
    });
}
 
// Helper function to create a table cell and append an element to it
function createTableCell(element) {
    const cell = document.createElement('td');
    cell.appendChild(element);
    return cell;
}

function fetchRestartHistoryDetails() {
    fetch('http://localhost:8060/fetchRestartHistoryDetails')  // Adjust API path
        .then(response => response.json())
        .then(data => {
            populateRestartHistoryTable(data);
        })
        .catch(error => console.error('Error fetching restart details:', error));
}

document.addEventListener('DOMContentLoaded', () => {
    fetchRestartHistoryDetails();
});