# authentication-stack
## not ready yet!
toDo
- [x] add key generation for init script
- [x] realm isn't configured correctly after startup (needs to be public and have direct access grant enabled) for authentication flow
- [ ] finish realm configuration through startup
- [ ] restarting only works after comletely detaching old containers and daza
- [x] remove remaining constants 
- [x] catch errors (to make logs cleaner)
- [x] module name/structure
- [x] improve instructions
- [ ] move to public repository


## ready2go keycloak authentication stack
- exposed API for access requests
- keycloak instance to offer access management for other services
- utilize tokens to access protected services

## quickstart
to start the instance with newly generated credentials, run:

    ./init.sh

or use the credentials defined in /configuration/auth.env - those can be easily changed there

    sudo docker-compose --env-file ./configuration/auth.env up

## secure your application with the authentication-stack

The project we used this for had only Services that also used Spring. 
So all we had to do was to add those services to the docker-compose file, 
include them in the docker-network and configure
their keycloak spring adapter through the application.properties file, like:              

    keycloak.auth-server-url = <keycloak_address:port/auth>
    keycloak.realm = <keycloak_realm>
    keycloak.resource = <keycloak_client>
    keycloak.public-client = true
    keycloak.security-constraints[0].authRoles[0] = offline_access    //this is a default role
    keycloak.security-constraints[0].securityCollections[0].patterns[0] = <your/protected/endpoint>
    

#### *and that's everything that is needed to secure your services with keycloak!*
