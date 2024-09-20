let selectedCustomer = '';
let selectedServer = '';
let fetchedDetails = {};
let selectedTab = 'memory'; // Default tab

async function storeDetailsInDatabase(tab, customer, server, path, componentName, referenceKey, tag) {
  try {
    const url = 'http://localhost:8060/storeDetails';
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        customer: customer,
        serverName: server,
        path: path,
        componentName: componentName,
        referenceKey: referenceKey,
        Tag: tag
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
    alert(responseData.message || "Details stored successfully!");

  } catch (error) {
    console.error('Error storing details:', error);
    alert('Error storing details: ' + error.message);
  }
}

async function storethreadDetailsInDatabase(tab, customer, server, path, componentName, threadKey, tag) {
  try {
    const url = 'http://localhost:8060/storeThreadDetails';
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        customer: customer,
        serverName: server,
        path: path,
        componentName: componentName,
        threadKey: threadKey,
        Tag: tag
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
    alert(responseData.message || "Details stored successfully!");

  } catch (error) {
    console.error('Error storing details:', error);
    alert('Error storing details: ' + error.message);
  }
}

async function storecustomDetailsInDatabase(tab, customer, server, path, componentName, customKey, tag) {
  try {
    const url = 'http://localhost:8060/storeCustomDetails';
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        customer: customer,
        serverName: server,
        path: path,
        componentName: componentName,
        customKey: customKey,
        Tag: tag
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
    alert(responseData.message || "Details stored successfully!");

  } catch (error) {
    console.error('Error storing details:', error);
    alert('Error storing details: ' + error.message);
  }
}

function extractPathValue(path) {
  if (path.includes('#')) {
    const segments = path.split('#');
    return segments.length > 0 ? segments[segments.length - 1] : '';
  }
  return '';
}

function storeAdditionalDetails() {
  const tab = selectedTab;
  const customer = selectedCustomer;
  const server = selectedServer;
  const path = document.getElementById(`${tab}-path`).value;
  const componentName = document.getElementById(`${tab}-Component`).value;
  const referenceKey = document.getElementById(`${tab}-reference`).value.split(',').map(key => key.trim());

  let tag = '';
  if (path.includes('#')) {
    tag = extractPathValue(path);
  }

  localStorage.setItem(`${tab}-selectedCustomer`, customer);
  localStorage.setItem(`${tab}-selectedServer`, server);
  localStorage.setItem(`${tab}-path`, path);
  localStorage.setItem(`${tab}-componentName`, componentName);
  localStorage.setItem(`${tab}-referenceKey`, JSON.stringify(referenceKey));
  localStorage.setItem(`${tab}-Tag`, tag);

  console.log(`Customer: ${customer}`);
  console.log(`Server: ${server}`);
  console.log(`Path: ${path}`);
  console.log(`Component Name: ${componentName}`);
  console.log(`Reference Key: ${referenceKey}`);
  console.log(`Tag in file: ${tag}`);

  storeDetailsInDatabase('memory', customer, server, path, componentName, referenceKey, tag);
}

function storeAdditionalthreadDetails() {
  const tab = selectedTab;
  const customer = selectedCustomer;
  const server = selectedServer;
  const path = document.getElementById(`${tab}-path`).value;
  const componentName = document.getElementById(`${tab}-Component`).value;
  const threadKey = document.getElementById(`${tab}-reference`).value;

  let tag = '';
  if (path.includes('#')) {
    tag = extractPathValue(path);
  }

  localStorage.setItem(`${tab}-selectedCustomer`, customer);
  localStorage.setItem(`${tab}-selectedServer`, server);
  localStorage.setItem(`${tab}-path`, path);
  localStorage.setItem(`${tab}-componentName`, componentName);
  localStorage.setItem(`${tab}-referenceKey`, threadKey);
  localStorage.setItem(`${tab}-Tag`, tag);

  console.log(`Customer: ${customer}`);
  console.log(`Server: ${server}`);
  console.log(`Path: ${path}`);
  console.log(`Component Name: ${componentName}`);
  console.log(`Reference Key: ${threadKey}`);
  console.log(`Tag in file: ${tag}`);

  storethreadDetailsInDatabase('thread', customer, server, path, componentName, threadKey, tag);
}

function storeAdditionalCustomDetails() {
  const tab = selectedTab;
  const customer = selectedCustomer;
  const server = selectedServer;
  const path = document.getElementById(`${tab}-path`).value;
  const componentName = document.getElementById(`${tab}-Component`).value;
  const referenceKey = document.getElementById(`${tab}-reference`).value.split(',').map(key => key.trim());

  let tag = '';
  if (path.includes('#')) {
    tag = extractPathValue(path);
  }

  localStorage.setItem(`${tab}-selectedCustomer`, customer);
  localStorage.setItem(`${tab}-selectedServer`, server);
  localStorage.setItem(`${tab}-path`, path);
  localStorage.setItem(`${tab}-componentName`, componentName);
  localStorage.setItem(`${tab}-referenceKey`, JSON.stringify(referenceKey));
  localStorage.setItem(`${tab}-Tag`, tag);

  console.log(`Customer: ${customer}`);
  console.log(`Server: ${server}`);
  console.log(`Path: ${path}`);
  console.log(`Component Name: ${componentName}`);
  console.log(`Reference Key: ${referenceKey}`);
  console.log(`Tag in file: ${tag}`);

  storecustomDetailsInDatabase('custom', customer, server, path, componentName, referenceKey, tag);
}

async function fetchServersForCustomer(selectedCustomer) {
  try {
    console.log("Selected Customer:", selectedCustomer);
    const url = `http://localhost:8060/fetchServers?selectedCustomer=${encodeURIComponent(selectedCustomer)}`;
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error('Network response was not ok');
    }

    const serverData = await response.json();
    console.log(serverData);

    // Parse the serverData correctly
    const servers = serverData.Servers || [];
    const dropdown = 
      (selectedTab === 'memory' ? document.getElementById('serverDropdown') : 
      selectedTab === 'thread' ? document.getElementById('thrdserverDropdown') : 
      document.getElementById('custserverDropdown'));

    dropdown.innerHTML = '';

    servers.forEach(server => {
      const li = document.createElement('li');
      li.textContent = server.serverName; // Access the serverName property
      li.classList.add('dropdown-item');
      li.addEventListener('click', () => {
        selectedServer = server.serverName; // Use serverName instead of server
        const serverDropdownButton = 
          (selectedTab === 'memory' ? document.getElementById('custserverDropdownButton') : 
          selectedTab === 'thrd' ? document.getElementById('thrdserverDropdownButton') : 
          document.getElementById('custserverDropdownButton'));

        serverDropdownButton.textContent = selectedServer;
      });
      dropdown.appendChild(li);
    });

  } catch (error) {
    console.error('Error fetching server details:', error);
  }
}

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

async function fetchCustomerDetails() {
  try {
    const url = 'http://localhost:8060/fetchCustomers';
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error('Network response was not ok');
    }

    const customerData = await response.json();
    const customers = customerData.Customers || [];

    const customerDropdown = document.getElementById('customerDropdown');
    const thrdCustomerDropdown = document.getElementById('thrdcustomerDropdown');
    const custCustomerDropdown = document.getElementById('custcustomerDropdown');

    customerDropdown.innerHTML = '';
    thrdCustomerDropdown.innerHTML = '';
    custCustomerDropdown.innerHTML = '';

    customers.forEach((customer) => {
      const li = document.createElement('li');
      li.textContent = customer;
      li.classList.add('dropdown-item');
      li.addEventListener('click', async () => {
        selectedCustomer = customer;
        document.getElementById(
          selectedTab === 'memory' ? 'customerDropdownButton' : 
          selectedTab === 'thrd' ? 'thrdcustomerDropdownButton' : 
          'custcustomerDropdownButton'
        ).textContent = customer;
        await handleCustomerSelection(); // Call the handleCustomerSelection function
      });
      customerDropdown.appendChild(li);

      const thrdLi = li.cloneNode(true);
      thrdLi.addEventListener('click', async () => {
        selectedCustomer = customer;
        document.getElementById(
          selectedTab === 'memory' ? 'customerDropdownButton' : 
          selectedTab === 'thrd' ? 'thrdcustomerDropdownButton' : 
          'custcustomerDropdownButton'
        ).textContent = customer;
        await handleCustomerSelection(); // Call the handleCustomerSelection function
      });
      thrdCustomerDropdown.appendChild(thrdLi);
      
      const custLi = li.cloneNode(true);
      custLi.addEventListener('click', async () => {
        selectedCustomer = customer;
        document.getElementById(
          selectedTab === 'memory' ? 'customerDropdownButton' : 
          selectedTab === 'thrd' ? 'thrdcustomerDropdownButton' : 
          'custcustomerDropdownButton'
        ).textContent = customer;
        await handleCustomerSelection(); // Call the handleCustomerSelection function
      });
      custCustomerDropdown.appendChild(custLi);
    });

    fetchedDetails.Customers = customers;
    localStorage.setItem('fetchedDetails', JSON.stringify(fetchedDetails));

  } catch (error) {
    console.error('Error fetching customer details:', error);
  }
}

function openTab(evt, tabName) {
  let i, tabcontent, tablinks;

  // Hide all tab content
  tabcontent = document.getElementsByClassName('tabcontent');
  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.display = 'none';
  }

  // Remove the "active" class from all tab links
  tablinks = document.getElementsByClassName('tablinks');
  for (i = 0; i < tablinks.length; i++) {
    tablinks[i].className = tablinks[i].className.replace(' active', '');
  }

  // Show the current tab content and add the "active" class to the clicked tab link
  document.getElementById(tabName).style.display = 'block';
  if (evt) evt.currentTarget.className += ' active';

  selectedTab = tabName.toLowerCase();

  // Fetch customer details for the selected tab
  fetchCustomerDetails();
}

// Event Listeners for form submission
document.getElementById('SubmitButton').addEventListener('click', storeAdditionalDetails);
document.getElementById('thrdSubmitButton').addEventListener('click', storeAdditionalthreadDetails);
document.getElementById('custSubmitButton').addEventListener('click', storeAdditionalCustomDetails);

// Initialize
document.addEventListener('DOMContentLoaded', function() {
  fetchCustomerDetails();
  openTab(null, 'memory'); // Open the memory tab by default
});
