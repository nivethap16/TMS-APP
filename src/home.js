let selectedCustomer = '';
let selectedEnvironment = '';
let fetchedDetails = {};

async function fetchServer(selectedCustomer) {
    try {
        console.log("Selected Customer:", selectedCustomer);
        const url = `http://localhost:8060/fetchServers?selectedCustomer=${encodeURIComponent(selectedCustomer)}`;
        console.log(`Request URL: ${url}`);
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const responseData = await response.json();
        console.log("Response Data:", responseData);

        fetchedDetails = responseData;
        localStorage.setItem('fetchedDetails', JSON.stringify(responseData)); // Store in local storage
    } catch (error) {
        console.error('There has been a problem with your fetch operation:', error);
    }
}

function show(value, selector, buttonId) {
    document.querySelector(selector).value = value;
    document.querySelector(buttonId).innerText = value;

    if (selector === '.customer-textBox') {
        selectedCustomer = value;
        localStorage.setItem('selectedCustomer', value);
    }

    if (selector === '.environment-textBox') {
        selectedEnvironment = value;
    }

    console.clear();
    console.log(`Customer: ${selectedCustomer}`);
    console.log(`Environment: ${selectedEnvironment}`);
}

async function loadDropdownData() {
    try {
        const response = await fetch('http://localhost:8060/fetchCustomers');
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
		console.log(data);
        localStorage.setItem('dropdownData', JSON.stringify(data)); // Store dropdown data in localStorage
	

        const customerDropdown = document.getElementById('customerDropdown');
        customerDropdown.innerHTML = ''; // Clear existing options
		console.log(customerDropdown);

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

function toggleDropdown(event) {
    const dropdownContent = event.currentTarget.nextElementSibling;
    dropdownContent.style.display = dropdownContent.style.display === 'block' ? 'none' : 'block';
}

function redirectToNextPage() {
    window.location.href = 'Page2.html';
}

function goToAdmin() {
    window.location.href = 'Page5.html';
}

document.addEventListener('DOMContentLoaded', loadDropdownData);

// Loading screen functionality

document.addEventListener('DOMContentLoaded', function() {
    // Select the loading screen element
    const loadingScreen = document.querySelector('.loading-screen');

    // Function to show the loading screen
    function showLoading() {
        loadingScreen.style.display = 'flex';
    }

    // Function to hide the loading screen
    function hideLoading() {
        loadingScreen.style.display = 'none';
    }

    // Add these functions to the global scope
    window.showLoading = showLoading;
    window.hideLoading = hideLoading;

    // Handle form submission
    document.querySelector('.btn.btn-outline-light').addEventListener('click', handleSubmit);

    async function handleSubmit(event) {
        event.preventDefault(); // Prevent the default form submission

        console.log("Checking customer name...");
        if (selectedCustomer === '') {
            alert("Please Enter the Customer Name");
            console.log("Customer name missing, exiting function.");
            return; // Exit the function
        }

        console.log("Checking environment...");
        if (selectedEnvironment === '') {
            alert("Please Enter the Environment");
            console.log("Environment missing, exiting function.");
            return; // Exit the function
        }

        console.log("Showing loading screen...");
        showLoading(); // Show the loading screen

        console.log("Fetching server details...");
        await fetchServer(selectedCustomer); // Fetch server details

        // Wait until fetchedDetails is populated
        while (Object.keys(fetchedDetails).length === 0) {
            console.log("Waiting for fetched details...");
            await new Promise(resolve => setTimeout(resolve, 1000)); // Wait for 1 second
        }

        console.log("Hiding loading screen...");
        hideLoading(); // Hide the loading screen
        console.log("Redirecting to next page...");
        redirectToNextPage(); // Redirect to the next page
    }
});

function openNav() {
      document.getElementById("mySidenav").style.width = "300px";
      document.getElementById("main").style.marginLeft = "250px";
      document.body.style.backgroundColor = "rgba(0,0,0,0.7)";
    }
    
    function closeNav() {
      document.getElementById("mySidenav").style.width = "0";
      document.getElementById("main").style.marginLeft= "0";
      document.body.style.backgroundColor = "white";
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
document.querySelector('#sidebar__theme-switcher').addEventListener('click', switchTheme)

//Reload page 
function reloadPage() {
  location.reload();
}

setInterval(reloadPage, 3000); // 3000 milliseconds = 3 seconds

//set sidebar size automatically based on pagesize
window.addEventListener('resize', function() {
  const sidebar = document.getElementById('sidebar');
  const pageHeight = document.documentElement.clientHeight;
  sidebar.style.paddingBottom = `${pageHeight - sidebar.offsetTop}px`;
});