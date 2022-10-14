# docker build -t compassa/sample-client:1.0.2 -f ~/code/java/tomato-rpc/sample_client.Dockerfile  .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181"  -d compassa/sample-client:1.0.2
FROM maven:3.8-openjdk-11 as builder
COPY . /tmp/
COPY settings.xml /usr/share/maven/ref/
WORKDIR /tmp/
RUN mvn clean install -DskipTests -U -B -e --settings /usr/share/maven/ref/settings.xml &&\
        cd ./tomato-rpc-spring-sample-client &&\
        mvn clean package spring-boot:repackage -DskipTests -U -B -e --settings /usr/share/maven/ref/settings.xml

FROM openjdk:11
COPY --from=builder /tmp/tomato-rpc-spring-sample-client/target/*.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
ENTRYPOINT java -jar $JAVA_OPTIONS *.jar