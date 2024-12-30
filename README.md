# Simofa

Simofa is a tool to help automate static website building and deployment. It is not a fully-featured alternative for \[insert PaaS here\].

It:
  - awaits GitHub commit webhooks
  - runs user-configured build commands in a secure Docker container
  - sends the built site to the server where it is to be deployed and runs the user-configured deploy script
  - handles failed builds/deploys

All other aspects of website building/deployment (such as SSL certificates and web server configuration) need to be configured by you. If you are looking for a fully automated solution, something like [staticdeploy.io](https://staticdeploy.io/) (not affiliated) may be better for you.

**Simofa is still in beta.**

## Requirements

### Manager server
- Java 16+
- MySQL 8+
- Reverse proxy with WebSocket support
  - Your reverse proxy should be configured to support large enough POST bodies (recommended: 100MB) for the build output and cache files
- Linux recommended
- 1 CPU core
- 1GB memory (recommended)
- 20GB disk space (minimum). You should have enough disk space for:
  - All git repositories (once they are cloned, they are saved to `config.cache_directory` for future builds)
  - Build cache files
- Modern web browser (Chrome 92+/Firefox 95+)

### Build server
- Docker 19.03+
- Java 16+
- Linux recommended
- 2 CPU cores (minimum, recommended: ceil(`config.concurrent_builds` \* \[average configured build CPU (default 0.5)\]) + 1)
- 2GB memory (minimum, recommended: ceil(`config.concurrent_builds` \* \[average configured build memory (default 256MB)\]) + 1GB)
- 20GB disk space (minimum). You should have enough disk space for:
  - All containers and build data
  - Processing files (copying, zipping, storing temporary files, etc.)

### Deploy server
- Java 16+
- Linux
- unzip (or similar tool)
- 1 CPU core
- 1GB memory (recommended)
- Reverse proxy with SSL recommended if build server and deploy server are communicating over the internet

**Important:** Keep the build server and deploy server versions in sync

## Usage

### Configuration

After starting Simofa for the first time, a `config.yml` file will be created. Edit this file as needed and then restart Simofa.

### Login

When Simofa is first started, it will create a user with the username `admin` and password `simofa`. It is recommended to change this immediately after logging in.

### Git

Your website repositories should be hosted on GitHub. Only user/pass authentication is accepted at the moment. The git repository is cloned to a temporary directory before being copied to the Docker container, so the container never receives your git credentials.

When adding a website in Simofa, you can choose whether to build on each commit, tag, or release.

Add `[no ci]` to a commit message to skip the build for that commit.

There are multiple ways to trigger builds:

#### GitHub App

Go to the GitHub App page in the dashboard to create a GitHub App. Remember to install it to your GitHub account after creating it.

#### GitHub Webhook

Configure webhook to `{simofa_url}/public-api/deploy/website/{id}/github`, where `{id}` is the website ID and `{simofa_url}` is a publicly acessible URL for Simofa. Use the JSON content type, and the deploy token (generated when adding the website) as the secret.

#### Deploy Hook

After creating a website, the deploy hook URL will be shown under the deploy token field. Send a POST request to this URL to trigger a build (optionally with a URL-encoded `commit` query parameter to set the commit message).

### Docker

Simofa includes two Docker containers in the `docker` directory in the git repository. `simofa-alpine` contains Node.js v18.16 installed on Alpine Linux, and can be used as a base image. `simofa-jekyll` is based on `simofa-alpine`, and contains Ruby 3.1.2 and Bundler. Both images contain suitable build tools for installing native dependencies. These images are also published to Docker Hub as `truewinter/simofa-alpine` and `truewinter/simofa-jekyll`.

Docker containers must be Linux. The container name can be up to 40 characters long (including the version).

Work is to be done in the `/simofa` directory (`/tmp/simofa-{build_id}` on deploy servers). The following sub-directories are created:
- in: The git repository (or `site.zip`, on the deploy server) is copied to this directory. It is also the default working directory.
- scripts: The build script is copied to `build.sh`. On the deploy server, the deploy and deploy failed scripts are copied to `deploy.sh` and `undeploy.sh`. You do not need to run any of these scripts, Simofa will automatically run the appropriate script.
- out: Your build script should output a compressed folder called `site.zip` here containing the built site. Using this directory is optional on the deploy server, but it is recommended to unzip the site here.
- cache: Used for build caching. Save a `cache.zip` file in this directory and it will be copied to the same location in future builds. Add `[no cache]` to a commit message to skip the build cache.

### Website Configuration

Build scripts must start with `#!/bin/bash` and it is recommended that `set -e` is put right below. This ensures that the script will exit immediately with a non-zero exit code if any of the commands fail, thereby failing the build.

Scripts configured in the Simofa dashboard are limited to 512 characters. If you have a longer build script, include it in the git repository and run that from the script configured in the dashboard. Keep the Docker container's file structure in mind when doing this.

There are multiple scripts that need to be configured:
- Build: Run on the Docker container to build the site and output it to `/simofa/out/site.zip`
- Deploy: Run on the deploy server. It should not only handle unzipping and copying the site to the correct location, but also making a backup of the original site.
- Deploy failed: Run on the deploy server. It should restore the backup made by the deploy script. When writing this script, keep in mind that the deploy can fail at any point in the deploy script so your code should check for the existance of files before attempting to delete/move them. Appending `|| :` to an unimportant command will also prevent an error in that command from forcing the script to exit with a non-zero error code.

To save time, a website's configuration can be saved as a template which can be loaded when creating another website. However, websites created from the same template will not have their configurations be kept in sync.

### Building and Deploying

Website builds will be cancelled after 20 minutes, and website deploys will be rolled back after 5 minutes. Rollbacks also have a 5 minute time limit.

While the build server can handle multiple concurrent builds, the deploy server can only deploy one website at a time.

The 5 latest build logs for each website are kept in memory and do not persist across restarts.
	
## Known Issues

- Stopping the Simofa Deploy server doesn't always roll back the running deploys. This is due to Java instantly killing sub-processes if Simofa Deploy is stopped instead of waiting for them to finish. This also prevents the logs and status from being submitted before shutting down.

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

Run `mvn clean package` from the root module to generate TypeScript definitions. Then, run Simofa from your IDE and run `npm run dev` to start Webpack.

To build, run `mvn clean package`. This should output JAR files to `build/{version}/`.

## Security

If you have discovered a security vulnerability in Simofa, please report it in accordance with the [TrueWinter Security Policy](https://truewinter.dev/legal/security)