# authentication-stack
## not ready yet!
toDo
- [ ] add key generation for init script
- [ ] realm isn't configured correctly after startup (needs to be public and have direct access grant enabled) for authentication flow
- [ ] remove remaining constants 


## ready2go authentication stack
- exposed API for access requests
- keycloak instance to offer access management for other services
- utilize tokens to access protected services

## quickstart
to start the instance with newly generated credentials, run:

    ./init.sh

or use the credentials defined in /credentials/keycloak.env - those can be easily changed there

    sudo docker-compose --env-file ./credentials/keycloak.env up

## secure your application with the authentication-stack

Your application needs to be able to connect to the keycloak instance, you can achieve that by adding it to the docker-network. With spring the only thing we have to do after that is to add the keycloak starter dependency: https://mvnrepository.com/artifact/org.keycloak/keycloak-spring-boot-starter

and the right configuration in your application.properties
like:

    keycloak.auth-server-url = <keycloak_address:port/auth>
    keycloak.realm = <keycloak_realm>
    keycloak.resource = <keycloak_client>
    keycloak.public-client = true
    keycloak.security-constraints[0].authRoles[0] = offline_access    //this is a default role
    keycloak.security-constraints[0].securityCollections[0].patterns[0] = <your/protected/endpoint>
    
#### *and those endpoints are now only accessible if you provide a valid access token!*
