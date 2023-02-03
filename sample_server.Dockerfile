# docker build -t compassa/rpc-sample-server:1.0.2 -f ~/code/java/tomato-rpc/sample_server.Dockerfile  .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181"  -d compassa/rpc-sample-server:1.0.2
FROM maven:3.8-openjdk-11 as builder
COPY . /tmp/
COPY settings.xml /usr/share/maven/ref/
WORKDIR /tmp/
RUN mvn clean install -DskipTests -U -B -e --settings /usr/share/maven/ref/settings.xml &&\
    cd ./tomato-rpc-spring-sample-server &&\
    mvn clean package spring-boot:repackage -DskipTests -U -B -e --settings /usr/share/maven/ref/settings.xml


FROM openjdk:11
COPY --from=builder /tmp/tomato-rpc-spring-sample-server/target/*.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
EXPOSE 1535
ENTRYPOINT java -jar $JAVA_OPTIONS *.jar
#--------for debug-----------
#RUN ls >> log_
#CMD ["tail", "-f", "log_"]