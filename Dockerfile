FROM openjdk:12
RUN mkdir -p /usr/app
COPY target/authapi-0.0.1-SNAPSHOT.jar /usr/app
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "authapi-0.0.1-SNAPSHOT.jar"]
