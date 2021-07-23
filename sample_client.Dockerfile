# docker build -t compassa/sample-client:1.0.0 -f ~/code/java/tomato-rpc/sample_client.Dockerfile  .
FROM maven:3.8-openjdk-11 as builder
COPY . /tmp/
COPY settings.xml /usr/share/maven/ref/
WORKDIR /tmp/
RUN mvn clean package -DskipTests -U -B -e --settings /usr/share/maven/ref/settings.xml

FROM openjdk:11
COPY --from=builder /tmp/tomato-rpc-sample-client/target/*-jar-with-dependencies.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
ENV ZK_IP_PORT 127.0.0.1:2181
CMD ["/bin/sh", "-c", "java -jar *.jar"]