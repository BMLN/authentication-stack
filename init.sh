#################use if database start up isn't normal##################
sudo docker-compose rm -f -v postgres
sudo docker-compose rm -f -v keycloak
sudo docker-compose down
########################################################################


sudo docker-compose --env-file ./credentials/keycloak.env up