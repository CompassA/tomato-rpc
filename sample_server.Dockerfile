# docker build -t sample-server:1.0.0 -f ~/code/java/tomato-rpc/sample_server.Dockerfile  .
FROM maven:3.8-openjdk-11 as builder
COPY . /tmp/
COPY settings.xml /usr/share/maven/ref/
WORKDIR /tmp/
RUN mvn clean package -DskipTests -U -B -e

FROM openjdk:11
COPY --from=builder /tmp/tomato-rpc-sample-server/target/*-jar-with-dependencies.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
EXPOSE 1535
CMD ["/bin/sh", "-c", "java -jar *.jar"]
#--------for debug-----------
#RUN ls >> log_
#CMD ["tail", "-f", "log_"]