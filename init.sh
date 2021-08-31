#################use if database start up isn't normal##################
sudo docker-compose rm -f -v postgres                                 #
sudo docker-compose rm -f -v keycloak                                 #
sudo docker-compose down                                              #
########################################################################

########################################################################
rm -f ./configuration/generated.env

printf "DB_NAME=urbanrescueranch\n\
DB_USER=$(openssl rand -hex 24)\n\
DB_PASS=$(openssl rand -hex 24)\n\
\n\
ADMIN_REALM=master\n\
ADMIN_CLIENT=admin-cli\n\
ADMIN_USER=$(openssl rand -hex 24)\n\
ADMIN_PASS=$(openssl rand -hex 24)\n\
\n\
KEYCLOAK_URL=http://localhost:8080/auth\n\
KEYCLOAK_REALM=auth-realm\n\
KEYCLOAK_CLIENT=auth-client\n\
\n\
AUTH_PORT=1337" >> ./configuration/generated.env
########################################################################

########################################################################
sudo docker-compose --env-file ./configuration/generated_auth.env up        #
########################################################################
