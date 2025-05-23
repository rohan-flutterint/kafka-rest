# Testing Environment: `mtls`

## Description

Starts up a Kafka cluster, where each broker has a REST Proxy instance running as a Kafka HTTP
servlet.

Components:

  * 3 Kafka brokers:
    1. `kafka-1:9291`, `http://kafka-1:9193`
    2. `kafka-2:9292`, `http://kafka-2:9293`
    3. `kafka-3:9293`, `http://kafka-3:9393`

Kafka is configured with PLAINTEXT security, and REST Proxy is configured with mTLS authentication.

## Usage

The command below starts up all containers. Use `-d` to start on detached mode.

```shell script
$ ./run.sh [-d] 
```

To make a request to REST Proxy, make sure to specify the end-user key pair, as well as the CA
certificate.

```shell script
$ curl --cacert testing/secrets/kafka-ca.crt \
    --key testing/secrets/alice.key \
    --cert testing/secrets/alice.crt \
    https://127.0.0.1:9391/v3/clusters
```

## Configuration

### REST Proxy

REST Proxy is configured to use mTLS authentication by setting:

```yaml
KAFKA_REST_SSL_KEYSTORE_LOCATION: "/secrets/kafka-rest.jks"
KAFKA_REST_SSL_KEYSTORE_PASSWORD: "kafka-rest-pass"
KAFKA_REST_SSL_TRUSTSTORE_LOCATION: "/secrets/kafka-ca.jks"
KAFKA_REST_SSL_TRUSTSTORE_PASSWORD: "kafka-ca-pass"
KAFKA_REST_SSL_KEY_PASSWORD: "kafka-rest-pass"
KAFKA_REST_SSL_CLIENT_AUTH: "true"
```

These certificates are generated by `../../secrets/configure.sh`.
