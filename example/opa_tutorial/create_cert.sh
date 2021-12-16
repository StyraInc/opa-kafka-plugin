#!/usr/bin/env bash

mkdir -p cert/ca
mkdir -p cert/server
mkdir -p cert/client

# Create CA certificate
openssl req -new -x509 -days 3650 -keyout cert/ca/ca.key -out cert/ca/ca.crt -subj "/C=SE/L=Stockholm/CN=Kafka CA" -passout pass:1234

# Server certificate
keytool -genkey -keystore cert/server/server.keystore -alias broker -dname CN=broker -keyalg RSA -validity 3650 -ext san=dns:broker -storepass 123456
keytool -certreq -keystore cert/server/server.keystore -alias broker -file cert/server/server.unsigned.crt -storepass 123456
openssl x509 -req -sha256 -CA cert/ca/ca.crt -CAkey cert/ca/ca.key -in cert/server/server.unsigned.crt -out cert/server/server.crt -days 3650 -CAcreateserial -passin pass:1234

# Broker truststore
keytool -import -keystore cert/server/server.truststore -alias ca -file cert/ca/ca.crt -storepass 123456 -noprompt

# Broker keystore
keytool -import -file cert/ca/ca.crt -keystore cert/server/server.keystore -alias ca -storepass 123456 -noprompt
keytool -import -file cert/server/server.crt -keystore cert/server/server.keystore -alias broker -storepass 123456 -noprompt

echo "123456" > cert/server/credentials.txt

# Client truststore
keytool -import -file cert/ca/ca.crt -keystore cert/client/client.truststore -alias ca -storepass 123456 -noprompt

declare -a clients=("pii_consumer" "anon_producer" "anon_consumer" "fanout_producer")
for client in "${clients[@]}" ; do
  keytool -genkey -keystore cert/client/"${client}".keystore -alias "${client}" -dname "CN=${client}, OU=Developers" -keyalg RSA -validity 3650 -storepass 123456
  keytool -certreq -keystore cert/client/"${client}".keystore -alias "${client}" -file cert/client/"${client}".unsigned.crt -storepass 123456
  openssl x509 -req -sha256 -CA cert/ca/ca.crt -CAkey cert/ca/ca.key -in cert/client/"${client}".unsigned.crt -out cert/client/"${client}".crt -days 3650 -CAcreateserial -passin pass:1234
  keytool -import -file cert/ca/ca.crt -keystore cert/client/"${client}".keystore -alias ca -storepass 123456 -noprompt
  keytool -import -file cert/client/"${client}".crt -keystore cert/client/"${client}".keystore -alias "${client}" -storepass 123456 -noprompt

  cat << EOF > cert/client/"${client}".properties
security.protocol=SSL
ssl.truststore.location=/tmp/client/client.truststore
ssl.truststore.password=123456
ssl.keystore.location=/tmp/client/${client}.keystore
ssl.keystore.password=123456
ssl.key.password=123456
EOF

  rm cert/client/"${client}".unsigned.crt cert/client/"${client}".crt
done

# Cleanup
rm cert/server/server.unsigned.crt cert/ca/ca.srl
