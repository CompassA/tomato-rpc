# mvn clean install -DskipTests -U -B -e && cd ./tomato-rpc-spring-sample-client && mvn clean package spring-boot:repackage -DskipTests -U -B -e && cd ..
# docker build -t compassa/rpc-sample-client:1.0.3 -f ./local_sample_client.Dockerfile .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181"  -d compassa/rpc-sample-client:1.0.3

FROM openjdk:17-jdk-slim
COPY ./tomato-rpc-spring-sample-client/target/*.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
ENTRYPOINT java -jar $JAVA_OPTIONS *.jar
