/**
 * Running this script will send a request to the web server
 * to request reloading the asset manifest.
 * Any changes to other files will require a restart.
 */

/** @type {import('axios').default} */
const axios = require('axios');
const fs = require('fs');
const path = require('path');
const yaml = require('yaml');
const config = yaml.parse(fs.readFileSync(path.join(__dirname, 'config.yml'), {
    encoding: 'utf-8'
}));

function reload() {
    if (!config.simofa_internals.dev) {
        return console.error('Simofa is not in dev mode');
    }
	axios.get(`http://localhost:${config.port}/_dev/reload`).then((response) => {
		console.log('Reloaded asset manifest. Response:', response.data);
	}).catch((err) => {
		if (err.code === 'ECONNREFUSED') {
			return console.error('Failed to reload asset manifest. Is Simofa online?');
		}

		console.error('Failed to reload asset manifest. Response:', err.response.data);
	});
}

// Make it executable from the command line
if (require.main === module) {
	reload();
}
module.exports.reload = reload;