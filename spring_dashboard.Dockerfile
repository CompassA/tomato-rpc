# mvn clean install -DskipTests -U -B -e && cd ./tomato-rpc-dashboard && mvn clean package spring-boot:repackage -DskipTests -U -B -e && cd ..
# docker build -t compassa/tomato-rpc-dashboard:1.0.3 -f ./spring_dashboard.Dockerfile  .
# docker run --env JAVA_OPTIONS="-Dtomato-rpc.name-service-uri=172.17.0.3:2181" -d compassa/tomato-rpc-dashboard:1.0.3

FROM openjdk:17-jdk-slim
COPY ./tomato-rpc-dashboard/target/tomato-rpc-dashboard-1.0-SNAPSHOT.jar /usr/src/myapp/
WORKDIR /usr/src/myapp/
EXPOSE 12222
EXPOSE
ENTRYPOINT java -jar $JAVA_OPTIONS tomato-rpc-dashboard-1.0-SNAPSHOT.jar