# Simofa

Simofa is a tool to help automate static website building and deployment. It is not a fully-featured alternative for \[insert PaaS here\].

It:
  - awaits GitHub commit webhooks
  - runs user-configured build commands in a secure Docker container
  - sends the built site to the server where it is to be deployed and runs the user-configured deployment script
  - handles failed builds/deployments

All other aspects of website building/deployment (such as SSL certificates and web server configuration) need to be configured by you. If you are looking for a fully automated solution, something like [staticdeploy.io](https://staticdeploy.io/) (not affiliated) may be better for you.

**Simofa is still in beta.**

## Requirements

### Build server
- Docker 19.03+
- Java 16+
- MySQL 8+
- Reverse proxy with SSL configured for Simofa domain (due to `crypto` API being used in JS)
- Linux recommended
- 2 CPU cores (minimum, recommended: ceil(`config.concurrent_builds` \* \[average build CPU usage (default 0.5)\]) + 1)
- 2GB memory (minimum, recommended: ceil(`config.concurrent_builds` \* \[average build memory usage (default 256MB)\]) + 1GB)
- 20GB disk space (minimum). You should have enough disk space for:
  - All containers and build data
  - All git repositories (once they are cloned, they are saved to `config.cache_directory` for future builds)
  - Build cache files
  - Processing files (copying, zipping, etc.)
- Modern web browser (Chrome 92+/Firefox 95+)

**Important: Install Simofa on its own server. It expects to be the only software managing Docker containers on the build server, and will delete all containers when it exits.**

### Deploy server
- Java 16+
- Linux
- unzip (or similar tool)
- 1 CPU core
- 1GB memory (recommended)
- Reverse proxy with SSL recommended if build server and deploy server are communicating over the internet

**Important:** Keep the build server and deploy server versions in sync

## Usage

### Login

When Simofa is first started, it will create a user with the username `admin` and password `simofa`. It is recommended to change this immediately after logging in.

### Git

Your website repositories should be hosted on GitHub. Only user/pass authentication is accepted at the moment. The git repository is cloned to a temporary directory before being copied to the Docker container, so the container never receives your git credentials.

Configure webhook to `{simofa_url}/public-api/deploy/website/{id}/github`, where `{id}` is the website ID and `{simofa_url}` is a publicly acessible URL for Simofa. Use the JSON content type, and the deploy token (generated when adding the website) as the secret.

### Docker

Simofa includes two Docker containers in the `docker` directory in the git repository. `simofa-alpine` contains Node.js v18.16 installed on Alpine Linux, and can be used as a base image. `simofa-jekyll` is based on `simofa-alpine`, and contains Ruby 3.1.2 and Bundler. Both images contain suitable build tools for installing native dependencies.

Docker containers must be Linux. The container name must start with `simofa-`, and can be up to 40 characters long (including the version).

Work is to be done in the `/simofa` directory (`/tmp/simofa-{build_id}` on deployment servers). The following sub-directories are created:
- in: The git repository (or `site.zip`, on the deployment server) is copied to this directory. It is also the default working directory.
- scripts: The build script is copied to `build.sh`. On the deployment server, the deployment and deployment failed scripts are copied to `deploy.sh` and `undeploy.sh`. You do not need to run any of these scripts, Simofa will automatically run the appropriate script.
- out: Your build script should output a compressed folder called `site.zip` here containing the built site. Using this directory is optional on the deployment server, but it is recommended to unzip the site here.
- cache: Used for build caching. Save a `cache.zip` file in this directory and it will be copied to the same location in future builds. Add `[no cache]` to a commit message to skip the build cache.

### Website Configuration

Build scripts must start with `#!/bin/bash` and it is recommended that `set -e` is put right below. This ensures that the script will exit immediately with a non-zero exit code if any of the commands fail, thereby failing the build.

Scripts configured in the Simofa dashboard are limited to 512 characters. If you have a longer build script, include it in the git repository and run that from the script configured in the dashboard. Keep the Docker container's file structure in mind when doing this.

There are multiple scripts that need to be configured:
- Build: Run on the Docker container to build the site and output it to `/simofa/out/site.zip`
- Deployment: Run on the deployment server. It should not only handle unzipping and copying the site to the correct location, but also making a backup of the original site.
- Deployment failed: Run on the deployment server. It should restore the backup made by the deployment script. When writing this script, keep in mind that the deployment can fail at any point in the deployment script so your code should check for the existance of files before attempting to delete/move them. Appending `|| :` to an unimportant command will also prevent an error in that command from forcing the script to exit with a non-zero error code.

To save time, a website's configuration can be saved as a template which can be loaded when creating another website. However, websites created from the same template will not have their configurations be kept in sync.

### Building and Deploying

Website builds will be cancelled after 20 minutes, and website deployments will be rolled back after 5 minutes. Rollbacks also have a 5 minute time limit.

While the build server can handle multiple concurrent builds, the deploy server can only deploy one website at a time.

The 5 latest build logs for each website are kept in memory and do not persist across restarts.

### JWT Authentication

JWT authentication is intended for cases where you'd like to give someone access to only part of the Simofa dashboard. It can also be used for API access. It is recommended to create tokens that only allow viewing data, not editing or adding data.

Sign the token with the secret defined in the config file and pass it as the `jwt` query parameter. Wherever possible, links and forms will include this query parameter to allow accessing different parts of the dashboard without receiving authentication errors.

**Important information:**
- The navbar will be hidden (and replaced with a warning not to bookmark the page) when using JWT authentication.
- Nothing else in the dashboard will be hidden when using JWT authentication. Attempting to access a page that isn't allowed by the token will result in an error.
- API responses will remain unchanged, meaning that information which is not viewable on the dashboard may still be viewable by users checking the API responses. While keys may be present in API responses, passwords are never returned by the API (except in cases where there are no alternative solutions).


The example below shows the recommended way to allow a client (with website ID `7`) to view build logs and stop a currently running build:
```json
{
	"exp": 1692362509, // Short-lived tokens are recommended
	"csrf": "wuzeMZJPxUfvXj7GJ4t7njO56tmkw16CQ_mUxUO2jF4", // Generate a long random string for each JWT
	"routes": [ // The user will be allowed to access any path that starts with any of these
		"/websites/7/logs",
		"/api/queue?website=7",
		"/websites/7/build",
		"/api/websites/7/build/"
	]
}
```
	
## Known Issues

- Stopping the Simofa Deploy server doesn't always roll back the running deployments. This is due to Java instantly killing sub-processes if Simofa Deploy is stopped instead of waiting for them to finish. This also prevents the logs and status from being submitted before shutting down.

## Plugins

Simofa allows the development of plugins that interact with it. Official plugins are released alongside Simofa with `-plugin` in the name (this is not required for custom plugins), and their source code is stored in the `officialplugins` directory.

Installing plugins is simple. Create a new directory called `plugins` in the same location as your `config.yml` and copy the plugin JAR file into this new directory. Then, start Simofa. If a plugin requires configuration, it should create a default configuration file in the `plugins` directory. After you configure the plugin, restart Simofa again.

Documentation for the plugin API is available [here](https://javadoc.jitpack.io/dev/truewinter/Simofa/SimofaAPI/latest/javadoc/).

**Important: Plugin support is still experimental and only a small portion of the API is currently available to plugins.**

## Development

In addition to the above requirements, you will need the following to develop Simofa:
- Node 16+
- 6-10GB memory (depending on IDE, Docker memory allocation, etc.)
- IDE with Maven support (IntelliJ is recommended)
- Visual Studio Code for developing JS and CSS (recommended, but any text editor will do)

Run `mvn clean install` from the `simofa-maven-plugin` module. Then run `mvn clean package` from the root module to generate TypeScript definitions.

Set `simofa_internals.dev` to `true` in `config.yml`. Run `npm run dev` and wait for it to finish. Ensure that `simofa/src/main/resources/web/assets/build/assets-manifest.json` contains entries for CSS and JavaScript. Then, run Simofa from your IDE.

With developer mode enabled, Simofa will automatically load the new asset manifest after Webpack builds the changes. Templates and assets will also be loaded from the disk instead of from the classpath, allowing you to change almost anything in those files without needing to restart Simofa.

To build, remove the contents of `simofa/src/main/resources/web/assets/build` and then run `mvn clean package`. This should output JAR files to `build/{version}/`. Ensure that the JAR file for the build server contains `web/assets/build/` as Maven occasionally doesn't copy this directory.

## Security

If you have discovered a security vulnerability in Simofa, please report it in accordance with the [TrueWinter Security Policy](https://truewinter.dev/legal/security)