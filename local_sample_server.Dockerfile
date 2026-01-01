# mvn clean install -DskipTests -U -B -e && cd ./tomato-rpc-spring-sample-server && mvn clean package spring-boot:repackage -DskipTests -U -B -e && cd ..
# docker build -t compassa/rpc-sample-server:1.0.3 -f ./local_sample_server.Dockerfile  .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181"  -d compassa/rpc-sample-server:1.0.3

FROM openjdk:11.0.11-jre-slim
COPY ./tomato-rpc-spring-sample-server/target/tomato-rpc-spring-sample-server-1.0-SNAPSHOT.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
EXPOSE 1535
ENTRYPOINT java -jar $JAVA_OPTIONS tomato-rpc-spring-sample-server-1.0-SNAPSHOT.jar
#--------for debug-----------
#RUN ls >> log_
#CMD ["tail", "-f", "log_"]
