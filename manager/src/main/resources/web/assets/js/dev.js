let reloadOnWsConnect = false;
let shouldWsReconnect = false;
let hasLoggedClose = false;
let wsTimeoutTimer = null;
function connectToWs() {
	if (true) return;
    window.wsCon = new WebSocket(`ws://${location.host}/_dev/ws`);

    window.wsCon.onopen = function() {
            console.log('Connected to dev WebSocket');
            clearTimeout(wsTimeoutTimer);
            if (reloadOnWsConnect) location.reload();
    }

    window.wsCon.onmessage = function(msg) {
        console.log(`Message from dev WebSocket: ${msg.data}`);
        if (msg.data === "close") {
            reloadOnWsConnect = true;
            shouldWsReconnect = true;

            function createTimer() {
                // If the page hasn't reloaded in 5 minutes,
                // chances are I took a break or finished work
                // for the day, so no need to keep retrying.
                wsTimeoutTimer = setTimeout(() => {
                    shouldWsReconnect = false;
                    console.warn("Failed to reconnect to dev WebSocket after 5 minutes");
                }, 5 * 60 * 1000);
            }

            if (wsTimeoutTimer) {
                clearTimeout(wsTimeoutTimer);
            }
            createTimer();
        }
    };

    window.wsCon.onclose = function(e) {
        if (!hasLoggedClose) {
            console.log(`Dev WebSocket closed with code ${e.code}`);
            hasLoggedClose = true;
        }
        if (shouldWsReconnect) connectToWs();
    };
}

window.doLoginWith = function (username, password) {
    const form = document.getElementsByTagName('form')[0];
    form.querySelector('input[name="username"]').value = username;
    form.querySelector('input[name="password"]').value = password;
    form.submit();
}

function createQuickLoginButton(text, fn) {
    const elem = document.createElement('button');
    elem.style.marginBottom = elem.style.marginTop = '4px';
    elem.style.width = 'fit-content';
    elem.setAttribute('onclick', fn);
    elem.innerText = text;

    return elem;
}

function createQuickLoginButtons() {
    const buttons = [
        createQuickLoginButton('Successful Login', 'doLoginWith(\'admin\', \'simofa\')'),
        createQuickLoginButton('Unsuccessful Login', 'doLoginWith(\'notanaccount\', \'1234\')')
    ];

    const elem = document.createElement('div');
    elem.style.flexDirection = 'column';
    elem.style.textAlign = elem.style.alignItems = 'center';
    const h3 = document.createElement('h3');
    h3.innerText = 'Dev Mode: Quick Login';
    elem.appendChild(h3);
    const warning = document.createElement('div');
    warning.innerText = 'Running in dev mode. Disable this in the config file.';
    warning.style.padding = '8px';
    warning.style.borderRadius = '4px';
    warning.style.backgroundColor = 'orangered';
    elem.appendChild(warning);
    for (button of buttons) {
        elem.appendChild(button);
    }

    return elem;
}

console.warn('Running in dev mode. Disable this in the config file.');
connectToWs();

switch (location.pathname) {
    case '/login':
        document.body.appendChild(createQuickLoginButtons())
        break;
}