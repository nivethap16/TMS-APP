let selectedCustomer = '';
let Servers = [];

document.addEventListener('DOMContentLoaded', () => {
    console.log("DOM fully loaded and parsed.");

    selectedCustomer = localStorage.getItem('selectedCustomer');
    console.log("Selected Customer:", selectedCustomer);

    const fetchedDetails = JSON.parse(localStorage.getItem('fetchedDetails'));
    console.log("Fetched Details from Local Storage:", fetchedDetails);

    if (fetchedDetails && fetchedDetails.Properties) {
        Servers = fetchedDetails.Servers;
        console.log("Servers:", Servers);

        function separateProperties(properties) {
            const appServers = [];
            const webServers = [];

            for (const key in properties) {
                if (properties.hasOwnProperty(key)) {
                    if (key[9] === '2') { // Assuming this suffix indicates app servers
                        appServers.push(...properties[key]);
                    } else if (key[9] === '1') { // Assuming this suffix indicates web servers
                        webServers.push(...properties[key]);
                    }
                }
            }

            return { appServers, webServers };
        }

        function processMemoryEntries(entries) {
            const memoryEntries = {};

            entries.forEach(entry => {
                const modifiedFileName = entry.modifiedFileName.trim().toLowerCase();

                if (!memoryEntries[modifiedFileName]) {
                    memoryEntries[modifiedFileName] = {
                        modifiedFileName: entry.modifiedFileName,
                        node1Xms: '',
                        node1Xmx: '',
                        node2Xms: '',
                        node2Xmx: '',
                        path: entry.path,
                        key: [],
                        serverName: entry.serverName,
                        tag: entry.Tags || '' // Add the tag property here
                    };
                }

                const isNode1 = entry.serverName && entry.serverName.endsWith('1');
                const isNode2 = entry.serverName && entry.serverName.endsWith('2');

                if (entry.key === '-Xms') {
                    if (isNode1) {
                        memoryEntries[modifiedFileName].node1Xms = entry.xmsmemory;
                    } else if (isNode2) {
                        memoryEntries[modifiedFileName].node2Xms = entry.xmsmemory;
                    }
                } else if (entry.key === '-Xmx') {
                    if (isNode1) {
                        memoryEntries[modifiedFileName].node1Xmx = entry.xmxmemory;
                    } else if (isNode2) {
                        memoryEntries[modifiedFileName].node2Xmx = entry.xmxmemory;
                    }
                }

                if (!memoryEntries[modifiedFileName].key.includes(entry.key)) {
                    memoryEntries[modifiedFileName].key.push(entry.key);
                }
            });

            return Object.values(memoryEntries);
        }

        function processThreadEntries(entries) {
            const threadEntries = {};

            entries.forEach(entry => {
                const modifiedFileName = entry.modifiedFileName.trim().toLowerCase();

                if (!threadEntries[modifiedFileName]) {
                    threadEntries[modifiedFileName] = {
                        modifiedFileName: entry.modifiedFileName,
                        node1Thread: '',
                        node2Thread: '',
                        path: entry.path,
                        key: entry.key,
                        serverName: entry.serverName,
                        tag: entry.Tags || '' // Add the tag property here
                    };
                }

                const isNode1 = entry.serverName && entry.serverName.endsWith('1');
                const isNode2 = entry.serverName && entry.serverName.endsWith('2');

                if (entry.key === 'numThreads') {
                    if (isNode1) {
                        threadEntries[modifiedFileName].node1Thread = entry.Thread;
                    } else if (isNode2) {
                        threadEntries[modifiedFileName].node2Thread = entry.Thread;
                    }
                }

                if (!threadEntries[modifiedFileName].key.includes(entry.key)) {
                    threadEntries[modifiedFileName].key.push(entry.key);
                }
            });

            return Object.values(threadEntries);
        }
		
		function processCustomEntries(entries) {
    const customEntries = {};

    entries.forEach(entry => {
        // Check if entry and its required properties are defined
        if (entry && entry.type === 'custom' && entry.modifiedFileName) {
            const modifiedFileName = entry.modifiedFileName.trim().toLowerCase();

            if (!customEntries[modifiedFileName]) {
                customEntries[modifiedFileName] = {
                    modifiedFileName: entry.modifiedFileName,
                    node1Value: '', // Initialize node1Value
                    node2Value: '', // Initialize node2Value
                    path: entry.path || '', // Ensure path is defined
                    type: [], // Initialize type as an array
					serverName: entry.serverName,
                    key: entry.key || '' // Assign the key property correctly
                };
            }

            const isNode1 = entry.serverName && entry.serverName.endsWith('1');
            const isNode2 = entry.serverName && entry.serverName.endsWith('2');

            // Set node1Value or node2Value based on serverName
            if (entry.type === 'custom') {
                if (isNode1) {
                    customEntries[modifiedFileName].node1Value = entry.value || '';
                } else if (isNode2) {
                    customEntries[modifiedFileName].node2Value = entry.value || '';
                }
            }

            // Accumulate types if entry.type is 'custom'
            if (entry.type === 'custom' && !customEntries[modifiedFileName].type.includes(entry.type)) {
                customEntries[modifiedFileName].type.push(entry.type);
            }
        }
    });

    return Object.values(customEntries);
}





        function extractDistinctServers(Servers) {
            // Initialize variables to hold the server names
            let appNode1 = '';
            let appNode2 = '';
            let webNode1 = '';
            let webNode2 = '';

            // Iterate over the server list
            Servers.forEach(Server => {
                // Extract the index for classification
                const serverName = Server.serverName;
                const index = serverName[9]; // Assumed to be the index for classification

                if (index === '2') {
                    if (serverName.endsWith('1')) {
                        appNode1 = serverName;
                    } else if (serverName.endsWith('2')) {
                        appNode2 = serverName;
                    }
                } else if (index === '1') {
                    if (serverName.endsWith('1')) {
                        webNode1 = serverName;
                    } else if (serverName.endsWith('2')) {
                        webNode2 = serverName;
                    }
                }
            });

            // Return the server names in an object for easy access
            return {
                appNode1,
                appNode2,
                webNode1,
                webNode2
            };
        }

        const { appServers, webServers } = separateProperties(fetchedDetails.Properties);
        const memoryEntries = appServers.concat(webServers).filter(entry => entry.key === '-Xms' || entry.key === '-Xmx');
        const appMemoryEntries = processMemoryEntries(memoryEntries.filter(entry => appServers.includes(entry)));
        const webMemoryEntries = processMemoryEntries(memoryEntries.filter(entry => webServers.includes(entry)));

        const threadEntries = appServers.concat(webServers).filter(entry => entry.key === 'numThreads');
        const appThreadEntries = processThreadEntries(threadEntries.filter(entry => appServers.includes(entry)));
        const webThreadEntries = processThreadEntries(threadEntries.filter(entry => webServers.includes(entry)));
		
		const customEntries = appServers.concat(webServers).filter(entry => entry.type === 'custom');
		const appCustomEntries = processCustomEntries(customEntries.filter(entry => appServers.includes(entry)));
		const webCustomEntries = processCustomEntries(customEntries.filter(entry => webServers.includes(entry)));


        console.log("Merged App Memory Entries:", appMemoryEntries);
        console.log("Merged Web Memory Entries:", webMemoryEntries);
        console.log("Merged App Thread Entries:", appThreadEntries);
        console.log("Merged Web Thread Entries:", webThreadEntries);
		console.log("Merged App Custom Entries:", appCustomEntries);
        console.log("Merged Web Custom Entries:", webCustomEntries);

        const webMemoryTable = document.getElementById('details-table');
        const appMemoryTable = document.getElementById('details-table-2');
        const webThreadTable = document.getElementById('details-table-3');
        const appThreadTable = document.getElementById('details-table-4');
		const webCustomTable = document.getElementById('details-table-5');
        const appCustomTable = document.getElementById('details-table-6');

        const webMemoryTableBody = webMemoryTable.querySelector('tbody');
        const appMemoryTableBody = appMemoryTable.querySelector('tbody');
        const webThreadTableBody = webThreadTable.querySelector('tbody');
        const appThreadTableBody = appThreadTable.querySelector('tbody');
		const webCustomTableBody = webCustomTable.querySelector('tbody');
        const appCustomTableBody = appCustomTable.querySelector('tbody');

        webMemoryTableBody.innerHTML = '';
        appMemoryTableBody.innerHTML = '';
        webThreadTableBody.innerHTML = '';
        appThreadTableBody.innerHTML = '';
		webCustomTableBody.innerHTML = '';
		appCustomTableBody.innerHTML = '';

        const createAndAppendRows = (tableBody, entries, type, valueType) => {
            entries.forEach(detail => {
                const row = document.createElement('tr');

                if (valueType === 'memory') {
                    row.innerHTML = `
                        <td>${detail.modifiedFileName}</td>
                        <td>${formatMemory(detail.node1Xms, detail.node1Xmx)}</td>
                        <td>${formatMemory(detail.node2Xms, detail.node2Xmx)}</td>
                        <td><button type="button" class="update-button" data-modified-name="${detail.modifiedFileName}" data-path="${detail.path}" data-key='${JSON.stringify(detail.key)}' data-type="${type}" data-tag="${detail.tag}" data-value-type="memory">Update</button></td>
                    `;
                } else if (valueType === 'thread') {
                    row.innerHTML = `
                        <td>${detail.modifiedFileName}</td>
                        <td>${formatThread(detail.node1Thread)}</td>
                        <td>${formatThread(detail.node2Thread)}</td>
                        <td><button type="button" class="update-button" data-modified-name="${detail.modifiedFileName}" data-path="${detail.path}" data-key='${JSON.stringify(detail.key)}' data-type="${type}" data-tag="${detail.tag}" data-value-type="thread">Update</button></td>
                    `;
                } else if (valueType === 'custom') {
                    row.innerHTML = `
                        <td>${detail.modifiedFileName}</td>
                        <td>${formatCustom(detail.node1Value)}</td>
                        <td>${formatCustom(detail.node2Value)}</td>
                        <td><button type="button" class="update-button" data-modified-name="${detail.modifiedFileName}" data-path="${detail.path}" data-key='${JSON.stringify(detail.key)}' data-type="${type}"  data-value-type="custom">Update</button></td>
                    `;
                }

                tableBody.appendChild(row);
            });
        };

        const formatMemory = (xms, xmx) => {
            if (!xms && !xmx) {
                return 'undefined';
            } else if (!xms) {
                return `${xmx}`;
            } else if (!xmx) {
                return `${xms}`;
            } else {
                return `${xms}, ${xmx}`;
            }
        };

        const formatThread = (thread) => {
            return thread ? thread : 'undefined';
        };
		
		const formatCustom = (custom) => {
            return custom ? custom : 'undefined';
        };

        const updateTableHeaders = (table, serverName1, serverName2, isWebTable = false) => {
            const headerRow = table.querySelector('thead tr');
            if (headerRow) {
                if (isWebTable) {
                    headerRow.cells[1].textContent = `(${serverName1})`;
                    headerRow.cells[2].textContent = `(${serverName2})`;
                } else {
                    headerRow.cells[1].textContent = `${serverName1}`;
                    headerRow.cells[2].textContent = `${serverName2}`;
                }
            }
        };

        const { appNode1, appNode2, webNode1, webNode2 } = extractDistinctServers(Servers);

        updateTableHeaders(appMemoryTable, appNode1, appNode2);
        updateTableHeaders(webMemoryTable, webNode1, webNode2, true);
        updateTableHeaders(appThreadTable, appNode1, appNode2);
        updateTableHeaders(webThreadTable, webNode1, webNode2, true);
		updateTableHeaders(appCustomTable, appNode1, appNode2);
        updateTableHeaders(webCustomTable, webNode1, webNode2, true);


        createAndAppendRows(webMemoryTableBody, webMemoryEntries, 'memory', 'memory');
        createAndAppendRows(appMemoryTableBody, appMemoryEntries, 'memory', 'memory');
        createAndAppendRows(webThreadTableBody, webThreadEntries, 'thread', 'thread');
        createAndAppendRows(appThreadTableBody, appThreadEntries, 'thread', 'thread');
		createAndAppendRows(webCustomTableBody, webCustomEntries, 'custom', 'custom');
        createAndAppendRows(appCustomTableBody, appCustomEntries, 'custom', 'custom');

       const addClickListener = (tableBody, entries) => {
    tableBody.addEventListener('click', (event) => {
        const target = event.target;
        if (target.classList.contains('update-button')) {
            event.preventDefault();
            console.log('Button clicked:', target);

            const modifiedName = target.getAttribute('data-modified-name');
            const path = target.getAttribute('data-path');
            const key = JSON.parse(target.getAttribute('data-key')); // Parse JSON string to array
            const type = target.getAttribute('data-type');
            const tag = target.getAttribute('data-tag');
            const valueType = target.getAttribute('data-value-type');

            const entry = entries.find(e => e.modifiedFileName === modifiedName);
            const node = entry && entry.serverName && entry.serverName.endsWith('1') ? '1' : '2';
            const serverName = entry ? entry.serverName : '';

            console.log('Modified Name:', modifiedName);
            console.log('Path:', path);
            console.log('Key:', key);
            console.log('Server Name:', serverName);
            console.log('Type:', type);
            console.log('Tag:', tag);
            console.log('Node:', node);
            console.log('Value Type:', valueType);

            const updateInfo = {
                modifiedName: modifiedName,
                path: path,
                key: key,
                serverName: serverName,
                type: type,
                node: node,
                tag: tag,
                valueType: valueType
            };
            localStorage.setItem('updateInfo', JSON.stringify(updateInfo));

            if (valueType === 'memory') {
                window.location.href = 'Page3.html';
            } else if (valueType === 'thread') {
                window.location.href = 'Page4.html';
            } else if (valueType === 'custom') {
                window.location.href = 'custconfig.html';
            }
        }
    });
};

addClickListener(appMemoryTableBody, appMemoryEntries);
addClickListener(webMemoryTableBody, webMemoryEntries);
addClickListener(appThreadTableBody, appThreadEntries);
addClickListener(webThreadTableBody, webThreadEntries);
addClickListener(appCustomTableBody, appCustomEntries);
addClickListener(webCustomTableBody, webCustomEntries);
};
	
});




function downloadCSV() {
    const table1 = document.getElementById("details-table");
    const table2 = document.getElementById("details-table-2");
    const table3 = document.getElementById("details-table-3");
    const table4 = document.getElementById("details-table-4");
    const table5 = document.getElementById("details-table-5");
    const table6 = document.getElementById("details-table-6");

    const rows1 = table1.querySelectorAll("tr");
    const rows2 = table2.querySelectorAll("tr");
    const rows3 = table3.querySelectorAll("tr");
    const rows4 = table4.querySelectorAll("tr");
    const rows5 = table5.querySelectorAll("tr");
    const rows6 = table6.querySelectorAll("tr");

    let csvContent = "";

    // Add selectedCustomer to the CSV content
    csvContent += `"Selected Customer","${selectedCustomer}"\n\n`;

    // Add Servers details to the CSV content as an array
    csvContent += `"Servers"\n`;
    Servers.forEach(server => {
        csvContent += `"${server.serverName}"\n`;
    });
    csvContent += `\n`;

    // Add heading for the Web Memory table
    csvContent += `"Web Memory"\n`;
    rows1.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });
    csvContent += `\n`;

    // Add heading for the App Memory table
    csvContent += `"App Memory"\n`;
    rows2.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });
    csvContent += `\n`;

    // Add heading for the Web Thread table
    csvContent += `"Web Thread"\n`;
    rows3.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });
    csvContent += `\n`;

    // Add heading for the App Thread table
    csvContent += `"App Thread"\n`;
    rows4.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });
    csvContent += `\n`;

    // Add heading for the Table 5
    csvContent += `"Table 5"\n`;
    rows5.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });
    csvContent += `\n`;

    // Add heading for the Table 6
    csvContent += `"Table 6"\n`;
    rows6.forEach(row => {
        let rowData = [];
        row.querySelectorAll("th, td").forEach(cell => {
            rowData.push(`"${cell.textContent.replace(/"/g, '""')}"`);
        });
        csvContent += rowData.join(",") + "\n";
    });

    const blob = new Blob([csvContent], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.setAttribute("hidden", "");
    a.setAttribute("href", url);
    a.setAttribute("download", "report.csv");
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}



document.addEventListener("DOMContentLoaded", function() {
    openTab(null, 'memory');
  });

  function openTab(evt, tabName) {
    var i, tabcontent, tablinks;

    tabcontent = document.getElementsByClassName("tabcontent");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }

    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }

    document.getElementById(tabName).style.display = "block";
    if (evt) {
        evt.currentTarget.className += " active";
    } else {
        document.querySelector(".tablinks").className += " active";
    }
  }
