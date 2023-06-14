# TimeChain

## For test

in **Docker**:

1. mvn assembly:assembly get .jar
2. docker rmi name
3. docker build -t name .
4. docker-compose up -d (at dict of docker-compose.yaml)
5. docker-compose down

**debug**:
1. watch -n 60 ./query.sh
2. docker-compose stop; /export logs.sh
