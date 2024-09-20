document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM fully loaded and parsed.");
    
    // Retrieve selectedCustomer and updateInfo from localStorage
    const selectedCustomer = localStorage.getItem('selectedCustomer');
    console.log("Selected Customer:", selectedCustomer);

    const updateInfoStr = localStorage.getItem('updateInfo');
    console.log('Raw updateInfo from localStorage:', updateInfoStr);

    const updateInfo = JSON.parse(updateInfoStr);
    console.log('Parsed updateInfo on Page3:', updateInfo);

    let keyArray = []; // Declare keyArray here to ensure it's in scope

    if (updateInfo) {
        // Access properties from updateInfo
        const { modifiedName, key, path, serverName } = updateInfo;
        console.log('Modified Name on Page3:', modifiedName);
        console.log('Server Names on Page3:', serverName);
        console.log('Keys:', key); // Log the key to ensure it's being accessed correctly

        if (key.startsWith('[') && key.endsWith(']')) {
            // Remove the square brackets and split the string into an array
            const trimmedKeyString = key.slice(1, -1);
            keyArray = trimmedKeyString.split(',').map(item => item.trim());
        } else {
            // If no square brackets are found, just add the key as is to the array
            keyArray = key.split(',').map(item => item.trim());
        }

        // Ensure the key is properly parsed
        if (Array.isArray(keyArray) && keyArray.length > 0) {
            console.log('Valid keys found:', keyArray);
        } else {
            console.error('No valid keys found in updateInfo.');
            alert('No valid keys found. Please ensure the data is correct.');
            return;  // Terminate the function if no valid keys are found
        }
    } else {
        console.error('No updateInfo found in localStorage on Page3.');
        return;  // Terminate the function if updateInfo is not found
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
            const upgradedValue = document.getElementById('upgraded-value').value;

            // Validate input data
            if (!changeNumber || !jid || !upgradedValue) {
                console.error('All fields are required.');
                alert('Please fill in all fields.');
                return;  // Terminate the function if validation fails
            }

            // Create data object to send
            const dataToSend = {
                modifiedName: updateInfo.modifiedName,
                key: keyArray, // keyArray is now correctly referenced here
                path: updateInfo.path,
                serverNames: updateInfo.serverName,
                changeNumber,
                jid,
                upgradedValue,
                selectedCustomer,
            };

            console.log('Data to send:', dataToSend);

            try {
                // Show the loading screen only after validation has passed
                showLoading();
            
                console.log("Preparing to send request to server...");
                console.log("Data to send:", JSON.stringify(dataToSend));
            
                // Send POST request to Java backend
                const response = await fetch('http://localhost:8060/processCustomData', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dataToSend)
                });
            
                // Check if the response is ok (status code 200-299)
                if (!response.ok) {
                    console.error('Failed to process form data. Status:', response.status, 'StatusText:', response.statusText);
                    alert(`Failed to submit the form. Server responded with: ${response.status} - ${response.statusText}`);
                    throw new Error('Failed to process form data');
                }
            
                const responseData = await response.json();
                console.log('Server response:', responseData);
            
                // Check for potential errors in the server response
                if (responseData.error) {
                    console.error('Error in response:', responseData.error);
                    alert(`Error in server response: ${responseData.error}`);
                    throw new Error(`Error in server response: ${responseData.error}`);
                }
            
                // If successful, show the submit dialog
                showsubmitDialog();
            } catch (error) {
                // Catch any other errors, log them, and show an alert
                console.error('Error occurred:', error.message);
                alert(`Failed to submit the form. Error: ${error.message}`);
            } finally {
                // Hide the loading screen (only if not redirecting)
                hideLoading();
            }
            
        });
    } else {
        console.error('Submit button not found.');
    }

    // Loading screen functionality
    const loadingScreen = document.querySelector('.loading-screen');

    function showLoading() {
        if (loadingScreen) {
            loadingScreen.style.display = 'flex';
        }
    }

    function hideLoading() {
        if (loadingScreen) {
            loadingScreen.style.display = 'none';
        }
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

