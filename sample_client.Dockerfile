# docker build -t compassa/sample-client:1.0.0 -f ~/code/java/tomato-rpc/sample_client.Dockerfile  .
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
CMD ["/bin/sh", "-c", "java -jar *.jar"]