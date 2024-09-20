document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM fully loaded and parsed.");
    const selectedCustomer = localStorage.getItem('selectedCustomer');
    console.log("Selected Customer:", selectedCustomer);

    // Retrieve updateInfo from localStorage
    const updateInfo = JSON.parse(localStorage.getItem('updateInfo'));
    console.log('Retrieved updateInfo on Page3:', updateInfo);

    if (updateInfo) {
        // Example: Accessing properties from updateInfo
        const { modifiedName, key, path, serverName, tag } = updateInfo;
        console.log('Modified Name on Page3:', modifiedName);
        console.log('Server Names on Page3:', serverName);
    } else {
        console.error('No updateInfo found in localStorage on Page3.');
    }

    // Check if form element exists
    const submitButton = document.getElementById('submit-button');
    if (submitButton) {
        // Handle form submission
        submitButton.addEventListener('click', async (event) => {
            event.preventDefault();

            // Get values from input fields
            const changeNumber = document.getElementById('change-number').value;
            const jid = document.getElementById('jid').value;
            const upgradedThread = document.getElementById('upgraded-thread').value;

            // Validate input data
            if (!changeNumber || !jid || !upgradedThread) {
                console.error('All fields are required.');
                alert('Please fill in all fields.');
                return;
            }

            // Create data object to send
            const dataToSend = {
                modifiedName: updateInfo.modifiedName,
                key: updateInfo.key,
                path: updateInfo.path,
                serverNames: updateInfo.serverName,
                changeNumber,
                jid,
                upgradedThread: upgradedThread,
                selectedCustomer,
                tag: updateInfo.tag
            };

            console.log('Data to send (stored locally):', dataToSend);

            try {
                showLoading();
            
                // Send POST request to Java backend
                const response = await fetch('http://localhost:8060/processThreadData', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dataToSend)
                });
            
                if (!response.ok) {
                    throw new Error('Failed to process thread data. Server returned: ' + response.status);
                }
            
                const responseData = await response.json();
            
                if (responseData.error) {
                    // If there's an error in the response
                    throw new Error(responseData.error);
                }
            
                console.log('Server response:', responseData);
                showsubmitDialog();
            } catch (error) {
                console.error('Error:', error.message);
                alert('Error: ' + error.message);
            } finally {
                hideLoading();
            }
            
        });
    } else {
        console.error('Submit button not found.');
    }
});
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
});

function showsubmitDialog() {
            // Show the dialog and overlay
            document.getElementById('submitDialog').style.display = 'block';
            document.getElementById('overlay').style.display = 'block';
        }

       function closesubmitDialog() {
    // Hide the confirm dialog and overlay
    document.getElementById('submitDialog').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
}

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
