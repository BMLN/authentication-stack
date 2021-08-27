# authentication-stack
## ready2go authentication stack
- exposed API for access requests
- keycloak instance to offer access management for other services
- utilize tokens to access protected services

## quickstart
to start the instance with newly generated credentials, run:

    ./init.sh

or use the credentials defined in /credentials/keycloak.env - those can be easily changed there

    sudo docker-compose --env-file ./credentials/keycloak.env up

## add authentication to your application

your application needs to be able to connect to the keycloak instance, you can achieve that by adding it to the docker-network.

with spring the only thing we have to after that is to add the keycloak starter dependency: https://mvnrepository.com/artifact/org.keycloak/keycloak-spring-boot-starter

and the right configuration in our application.properties
like:

    keycloak.auth-server-url = <keycloak_address:port/auth>
    keycloak.realm = <keycloak_realm>
    keycloak.resource = <keycloak_client>
    keycloak.public-client = true
    keycloak.security-constraints[0].authRoles[0] = offline_access    //this is a default role
    keycloak.security-constraints[0].securityCollections[0].patterns[0] = <your/protected/endpoint>
