#!/usr/bin/env bash

SCRIPT_DIR=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd)

mkdir -p cert/ca
mkdir -p cert/server
mkdir -p cert/client

# Create CA certificate
openssl req -new -x509 -days 3650 -keyout cert/ca/ca.key -out cert/ca/ca.crt -subj "/C=SE/L=Stockholm/CN=Kafka CA" -passout pass:1234

# Server certificate
keytool -genkey -keystore cert/server/server.keystore -alias localhost -dname CN=localhost -keyalg RSA -validity 3650 -ext san=dns:localhost -storepass 123456
keytool -certreq -keystore cert/server/server.keystore -alias localhost -file cert/server/server.unsigned.crt -storepass 123456
openssl x509 -req -CA cert/ca/ca.crt -CAkey cert/ca/ca.key -in cert/server/server.unsigned.crt -out cert/server/server.crt -days 3650 -CAcreateserial -passin pass:1234

# Broker truststore
keytool -import -keystore cert/server/server.truststore -alias ca -file cert/ca/ca.crt -storepass 123456 -noprompt

# Broker keystore
keytool -import -file cert/ca/ca.crt -keystore cert/server/server.keystore -alias ca -storepass 123456 -noprompt
keytool -import -file cert/server/server.crt -keystore cert/server/server.keystore -alias localhost -storepass 123456 -noprompt

echo "123456" > cert/server/credentials.txt

# Client truststore
keytool -import -file cert/ca/ca.crt -keystore cert/client/client.truststore -alias ca -storepass 123456 -noprompt

# Client keystore - repeat per client
keytool -genkey -keystore cert/client/alice.keystore -alias alice -dname CN=alice-mgmt -keyalg RSA -validity 3650 -storepass 123456
keytool -certreq -keystore cert/client/alice.keystore -alias alice -file cert/client/alice.unsigned.crt -storepass 123456
openssl x509 -req -CA cert/ca/ca.crt -CAkey cert/ca/ca.key -in cert/client/alice.unsigned.crt -out cert/client/alice.crt -days 3650 -CAcreateserial -passin pass:1234

keytool -import -file cert/ca/ca.crt -keystore cert/client/alice.keystore -alias ca -storepass 123456 -noprompt
keytool -import -file cert/client/alice.crt -keystore cert/client/alice.keystore -alias alice -storepass 123456 -noprompt

cat << EOF > cert/client/alice.properties
security.protocol=SSL
ssl.truststore.location=${SCRIPT_DIR}/cert/client/client.truststore
ssl.truststore.password=123456
ssl.keystore.location=${SCRIPT_DIR}/cert/client/alice.keystore
ssl.keystore.password=123456
ssl.key.password=123456
EOF

# Cleanup
rm cert/server/server.unsigned.crt cert/ca/ca.srl cert/client/alice.unsigned.crt
