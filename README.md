# ready2go keycloak-authentication-stack


## features
- dedicated service for access requests
- keycloak instance to offer access management for other services
- utilize tokens to access protected services
- easy to build on 
- easy to modify for your needs

## quickstart
to start the instance with newly generated credentials, run:

    ./init.sh

or use the credentials defined in /configuration/default.env - those can be easily changed there

    sudo docker-compose --env-file ./configuration/default.env up

## secure your services with the authentication-stack

The project we used this for had only Services that also used Spring, so the configuration was pretty simple:

- add those services to the docker-compose file
- include them in the docker-network 
- configure their keycloak spring adapter through the application.properties file, like:              

-

    keycloak.auth-server-url = <keycloak_address:port/auth>
    keycloak.realm = <keycloak_realm>
    keycloak.resource = <keycloak_client>
    keycloak.public-client = true
    keycloak.security-constraints[0].authRoles[0] = offline_access    //this is a default role
    keycloak.security-constraints[0].securityCollections[0].patterns[0] = <your/protected/endpoint>
    

#### *and that's everything that is needed to secure your services with keycloak!*
