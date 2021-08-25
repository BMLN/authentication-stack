FROM maven:latest AS build
RUN mkdir -p /usr/apps
COPY src /usr/apps/authentication-stack/src
COPY pom.xml /usr/apps/authentication-stack
RUN mvn -f /usr/apps/authentication-stack/pom.xml clean package
RUN cd /usr/apps/authentication-stack/target && ls

FROM openjdk:12
RUN mkdir -p /usr/apps
COPY --from=build /usr/apps/authentication-stack/target/auth-services-0.0.1-SNAPSHOT.jar /usr/apps
WORKDIR /usr/apps
ENTRYPOINT ["java", "-jar", "auth-services-0.0.1-SNAPSHOT.jar"]
