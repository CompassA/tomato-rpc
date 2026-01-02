# mvn clean install -DskipTests -U -B -e && cd ./tomato-rpc-spring-sample-server && mvn clean package spring-boot:repackage -DskipTests -U -B -e && cd ..
# docker build -t compassa/rpc-sample-server:1.0.3 -f ./spring_client_sample.Dockerfile  .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181 -Dspring.profiles.active=prod"  -d compassa/rpc-sample-server:1.0.3

FROM openjdk:17-jdk-slim
COPY ./tomato-rpc-spring-sample-server/target/tomato-rpc-spring-sample-server-1.0-SNAPSHOT.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
EXPOSE 9090
EXPOSE
ENTRYPOINT java -jar $JAVA_OPTIONS tomato-rpc-spring-sample-server-1.0-SNAPSHOT.jar
#--------for debug-----------
#RUN ls >> log_
#CMD ["tail", "-f", "log_"]
