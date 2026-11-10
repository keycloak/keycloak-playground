#!/bin/bash -e

SPIRE_VERSION='1.13.3'

rm -rf tmp
mkdir -p tmp && cd tmp

if [ ! -d spire ]; then
  curl -s -N -L https://github.com/spiffe/spire/releases/download/v$SPIRE_VERSION/spire-$SPIRE_VERSION-linux-amd64-musl.tar.gz | tar xz
  mv spire-$SPIRE_VERSION spire
fi

cd spire

openssl req -x509 -newkey rsa:4096 -sha256 -days 3650   -nodes -keyout conf/localhost-unsigned.key -out conf/localhost-unsigned.pem -subj "/CN=localhost"   -addext "subjectAltName=DNS:localhost,DNS:*.localdomain,IP:127.0.0.1"

cp ../../spire-server-example.conf conf/server/
bin/spire-server run -config conf/server/spire-server-example.conf