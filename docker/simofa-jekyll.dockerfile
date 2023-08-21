FROM simofa-alpine:latest

SHELL ["/bin/bash", "--login", "-c"]
RUN apk update && apk add alpine-sdk gcc gnupg curl ruby procps musl-dev make linux-headers zlib zlib-dev openssl openssl-dev libssl1.1 shadow && cd /tmp && \
	curl -sSL https://github.com/rvm/rvm/tarball/stable -o rvm-stable.tar.gz && \
	echo 'export rvm_prefix="$HOME"' > /root/.rvmrc && \
	echo 'export rvm_path="$HOME/.rvm"' >> /root/.rvmrc && \
	mkdir rvm && cd rvm && \
	tar --strip-components=1 -xzf ../rvm-stable.tar.gz && \
	./install --auto-dotfiles --autolibs=0 && \
	source /root/.rvm/scripts/rvm && \
	rvm install 3.1.2 && rvm use --default 3.1.2 && \
	cd ../ && rm -rf rvm-stable rvm-stable.tar.gz rvm && \
	gem install bundler && \
	bundle config silence_root_warning true && \
	echo 'source /root/.rvm/scripts/rvm' >> /root/.bashrc && \
	rm -rf /var/cache/apk/*

WORKDIR /simofa/in