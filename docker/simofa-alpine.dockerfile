FROM alpine:3.18.2
ENV NODE_PACKAGE_URL https://unofficial-builds.nodejs.org/download/release/v18.16.0/node-v18.16.0-linux-x64-musl.tar.gz

# Required by Node
RUN apk add libstdc++
# These may be useful for website build scripts
RUN apk add curl bash ca-certificates python3 grep zip unzip
WORKDIR /opt
RUN wget $NODE_PACKAGE_URL
RUN mkdir -p /opt/nodejs
RUN tar -zxvf *.tar.gz --directory /opt/nodejs --strip-components=1
RUN rm *.tar.gz
RUN ln -s /opt/nodejs/bin/node /usr/local/bin/node
RUN ln -s /opt/nodejs/bin/npm /usr/local/bin/npm
RUN ln -s /opt/nodejs/bin/npx /usr/local/bin/npx
RUN mkdir /simofa
RUN mkdir /simofa/in
RUN mkdir /simofa/out
RUN mkdir /simofa/scripts
WORKDIR /simofa/in