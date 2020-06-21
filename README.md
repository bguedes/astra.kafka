
##Create the Database

To register use this link :

[DataStax Astra Database-as-a-Service](https://astra.datastax.com/register)



![](https://raw.githubusercontent.com/bguedes/static/master/astraRegister.png)



![](https://raw.githubusercontent.com/bguedes/static/master/astraCreateDatabaseStep1.png)

Let parameters by default :

**Compute Size**
	Free tier
**Estimated Cost**
	Per Hour

Specify these parameters :

**Database name**			astrademo
**Keyspace name**			astrademo
**Database username**		datastax
**Database user password**	datastax


The database creation could take some minutes to acheive :

![](https://raw.githubusercontent.com/bguedes/static/master/astraCreateDatabaseProcessing.png)

Please wait !!

After creation, you will able to use it :

![](https://raw.githubusercontent.com/bguedes/static/master/astraDatabaseCreated.png)

Please download the bundle file needed for Authentification, you will need this zip file for be able to connect the database through your application.

![](https://raw.githubusercontent.com/bguedes/static/master/astraDatabaseBundleAuthenticationFile.png)

The file is called **secure-connect-astrademo.zip**

For testing Kafka ingestion,you will need to create some Cassandra tables.

```sql

create table if not exists astrademo.stocks_table_by_symbol (symbol text, datetime timestamp, exchange text, industry text, name text, value double, PRIMARY KEY (symbol, datetime));
create table if not exists astrademo.stocks_table_by_exchange (symbol text, datetime timestamp, exchange text, industry text, name text, value double, PRIMARY KEY (exchange, datetime));
create table if not exists astrademo.stocks_table_by_industry (symbol text, datetime timestamp, exchange text, industry text, name text, value double, PRIMARY KEY (industry, datetime));

```

![](https://raw.githubusercontent.com/bguedes/static/master/astraCreateTablesForKafkaTest.png)

For the database that's all Folks

##Kafka Setup and Running

### Prerequisites
- Docker: https://docs.docker.com/v17.09/engine/installation/
- Docker Compose: https://docs.docker.com/compose/install/
Clone this repository
```
git clone https://github.com/bguedes/astra.kafka.git
```

Go to the directory
```
cd kafka-connector-sink-json
```

Build the DataStax Kafka Connector image
```
docker build --no-cache -t datastax-connect -f Dockerfile-connector .
```

Build the JSON Java Producer image
```
docker build . -t kafka-producer -f Dockerfile-producer
```

Start Zookeeper, Kafka Brokers, Kafka Connect, Cassandra, and the producer containers
```
docker-compose up -d
```

### Docker instances Running

```
CONTAINER ID        IMAGE                              COMMAND                  CREATED             STATUS              PORTS                                         NAMES
7db0cffddbb9        datastax-connect:latest            "/etc/confluent/dock…"   37 hours ago        Up 37 hours         0.0.0.0:8083->8083/tcp, 9092/tcp              datastax-connect
9a9967a2d8d7        confluentinc/cp-kafka:latest       "/etc/confluent/dock…"   37 hours ago        Up 37 hours         0.0.0.0:9092->9092/tcp                        kafka-broker
d91eeb822b97        confluentinc/cp-zookeeper:latest   "/etc/confluent/dock…"   37 hours ago        Up 37 hours         2181/tcp, 2888/tcp, 3888/tcp                  zookeeper
2f0c114d78fc        kafka-producer:latest              "bash"                   37 hours ago        Up 37 hours                                                       kafka-producer
```

### Copy Astra credentials file to Docker Datastax Kafka Connector instances

Go to the datastax connector Docker image shell

```
docker exec -it datastax-connect bash
```

Create a datastax-connect-driver directory in /home/

```
cd /home/
mkdir datastax-connect-driver
exit
```

Copy the Astra bundle file downloaded previously

```
docker cp secure-connect-astrademo.zip datastax-connect:/home/datastax-connect-driver
```

### Running

#### Create the Kafka topic
Start a bash shell on the Kafka Broker
```
docker exec -it kafka-broker bash
```
Create the topic
```
kafka-topics --create --zookeeper zookeeper:2181 --replication-factor 1 --partitions 10 --topic json-stream --config retention.ms=-1
```

#### Load data into Kafka
Start a bash shell on the Kafka Producer
```
docker exec -it kafka-producer bash
```
Write 1000 records ( 10 stocks, 100 records per stock ) to Kafka using the JSON Java Producer in this project
```
mvn clean compile exec:java -Dexec.mainClass=json.JsonProducer -Dexec.args="json-stream 10 100 broker:29092"
```
There will be many lines of output in your console as Maven pulls down the dependencies. The following output means that it completed successfully
```
2020-03-09 18:01:34.268 [json.JsonProducer.main()] INFO  - Completed loading 1000/1000 records to Kafka in 1 seconds
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 20.254 s
[INFO] Finished at: 2020-03-09T18:01:34+00:00
[INFO] Final Memory: 31M/215M
[INFO] ------------------------------------------------------------------------
```

#### Start the DataStax Kafka Connector
Execute the following command from the machine where docker is running to start the connector using the Kafka Connect REST API
```
curl -X POST -H "Content-Type: application/json" -d @connector-config.json "http://localhost:8083/connectors"
```

#### Verifying the Kafka connector Status

Go to the Datastax Kafka Connector shell

```
docker exec -it datastax-connect bash
```

Use this Rest Api to chek the status, it has to be in RUNNING state

```
curl -s 127.0.0.1:8083/connectors/kafka-to-astra-sink/status | jq '.'

{
  "name": "kafka-to-astra-sink",
  "connector": {
    "state": "RUNNING",
    "worker_id": "datastax-connect:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "datastax-connect:8083"
    }
  ],
  "type": "sink"
}
```

if you don't have jq already installed :

```
apt update
apt-get install jq
```

#### Check data load in Astra tables

```sql
datastax@cqlsh> select * from astrademo.stocks_table_by_symbol limit 10;

 symbol | datetime                        | exchange | industry | name        | value
--------+---------------------------------+----------+----------+-------------+----------
    XOM | 2020-06-20 09:00:40.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 80.20207
    XOM | 2020-06-20 09:00:50.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.53933
    XOM | 2020-06-20 09:01:00.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.94806
    XOM | 2020-06-20 09:01:10.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.21969
    XOM | 2020-06-20 09:01:20.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.24684
    XOM | 2020-06-20 09:01:30.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.74106
    XOM | 2020-06-20 09:01:40.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 80.46026
    XOM | 2020-06-20 09:01:50.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.96613
    XOM | 2020-06-20 09:02:00.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 79.57981
    XOM | 2020-06-20 09:02:10.042000+0000 |     NYSE |   ENERGY | EXXON MOBIL | 80.22131

(10 rows)
datastax@cqlsh>
```
