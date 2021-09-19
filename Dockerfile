FROM maven:latest AS build
RUN mkdir -p /usr/apps
COPY src /usr/apps/authentication-stack/src
COPY pom.xml /usr/apps/authentication-stack
RUN mvn -f /usr/apps/authentication-stack/pom.xml clean package

FROM openjdk:12
RUN mkdir -p /usr/apps
COPY --from=build /usr/apps/authentication-stack/target/auth-service-0.0.1.jar /usr/apps
WORKDIR /usr/apps
ENTRYPOINT ["java", "-jar", "auth-service-0.0.1.jar"]
