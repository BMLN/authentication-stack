package AuthAPI.Controller;


import AuthAPI.Model.Token;
import AuthAPI.Model.UserCredentials;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${keycloak.auth-server-url}")
    private String keycloakURL;
    @Value("${keycloak.realm}")
    private String authRealm;
    @Value("${keycloak.resource}")
    private String authClient;


    private RealmResource realmController;


    @Autowired
    private void connectRealmController(   @Value("${auth.user}") String username,
                                           @Value("${auth.password}") String password,
                                           @Value("${auth.realm}") String realm,
                                           @Value("${auth.client}") String client) throws InterruptedException {

        Keycloak keycloak = Keycloak.getInstance(
                keycloakURL,
                realm,
                username,
                password,
                client);

        this.realmController = keycloak.realm(realm);

        try {
            for (int i = 0; i <= 5; i++) {
                System.out.println("KEYCLOAK-CONFIGURATION: connecting...");
                try {
                    realmController.clients().findAll();
                    i = 5;
                    System.out.println("KEYCLOAK-CONFIGURATION: connected the admin Controller");
                } catch (Exception exception) {
                    if (i == 5) {
                        System.out.println("KEYCLOAK-CONFIGURATION: couldn't connect to keycloak: " + exception.getMessage());
                        throw new ConnectTimeoutException(exception.getMessage());
                    }
                    Thread.sleep(30 * 1000);
                }
            }


            try {
                System.out.println("KEYCLOAK-CONFIGURATION: connecting to realm...");

                this.realmController = keycloak.realm(authRealm);
                realmController.clients().findAll();

            } catch (NotFoundException noRealmException) {
                System.out.println("KEYCLOAK-CONFIGURATION: creating realm...");

                RealmRepresentation newRealm = new RealmRepresentation();
                newRealm.setRealm(authRealm);
                newRealm.setAccessTokenLifespan(3 * 60 * 24);
                newRealm.setEnabled(true);
                keycloak.realms().create(newRealm);
            }


            if(this.realmController.clients().findByClientId(authClient).isEmpty()){

                System.out.println("KEYCLOAK-CONFIGURATION: creating client...");

                ClientRepresentation clientRepresentation  = new ClientRepresentation();
                clientRepresentation.setClientId(authClient);
                clientRepresentation.setEnabled(true);
                this.realmController.clients().create(clientRepresentation);
            }


            System.out.println("KEYCLOAK-CONFIGURATION: adjusting client settings...");

            ClientResource clientController = this.realmController.clients().get(this.realmController.clients().findByClientId(authClient).get(0).getId());
            ClientRepresentation cc = clientController.toRepresentation();
            cc.setPublicClient(true);
            cc.setDirectAccessGrantsEnabled(true);
            cc.setEnabled(true);
            clientController.update(cc);


            this.realmController.clients().findAll();
            System.out.println("KEYCLOAK-CONFIGURATION: successfully connected to auth Resource!");

        }
        catch (Exception keycloakConException){
            System.out.println("KEYCLOAK-CONFIGURATION: failed: " + keycloakConException.getMessage());
            System.exit(-1);
        }

    }



    private Token getToken(UserCredentials userData){

        AccessTokenResponse session = Keycloak.getInstance(
                keycloakURL,
                authRealm,
                userData.getUsername(),
                userData.getPassword(),
                authClient).tokenManager().getAccessToken();

        return new Token(session.getToken(), session.getRefreshToken(), (int) session.getExpiresIn());
    }


    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<Token> addUser(@RequestBody UserCredentials userData) {

        ResponseEntity<Token> response;

        //addUser() if username doesnt exist yet
        if (this.realmController.users().search(userData.getUsername()).isEmpty()){

            this.realmController.users().create(userData.getUserRepresentation());
            System.out.println("created new user!");

            response = this.login(userData);
        }
        else {
            response = new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);  //HTTPStatus:406
        }

        return response;
    }

    @RequestMapping(value = "/deactivate", method = RequestMethod.POST)
    public void disableUser(KeycloakPrincipal principal) {

        if(principal != null) {
            String userID = principal.getName();

            UsersResource users = this.realmController.users();
            UserRepresentation userRepr = users.get(userID).toRepresentation();
            userRepr.setEnabled(false);

            users.get(userID).update(userRepr);
        }
        System.out.println("set user status to inactive!");
    }


    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<Token> login(@RequestBody UserCredentials userData){

        ResponseEntity<Token> response;

        try {
            Token userToken = this.getToken(userData);
            response  = new ResponseEntity<Token>(userToken, HttpStatus.OK);
        }
        catch (NotAuthorizedException exception){
            System.out.println("login failed");
            response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED); //HTTPStatus:401
        }
        catch (Exception exception){
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    //keycloak adapters don't support refreshing tokens
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    public ResponseEntity<Token> refresh(@RequestBody Token userToken) throws IOException {

        String refreshToken = userToken.getRefreshToken();
        ResponseEntity<Token> response;


        //request new accesskey
        //requestbody
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", authClient));
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));

        //send post request
        HttpPost request = new HttpPost(keycloakURL + "/realms/" + authRealm + "/protocol/openid-connect/token");
        request.setEntity(new UrlEncodedFormEntity(params));
        HttpClient client = new DefaultHttpClient();
        org.apache.http.HttpResponse resp = client.execute(request);

        //process response
        if (resp.getStatusLine().getStatusCode() == 200) {
            InputStreamReader inputReader = new InputStreamReader(resp.getEntity().getContent());
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            StringBuffer sb = new StringBuffer();
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                sb.append(str);
            }
            str = sb.toString();
            response = new ResponseEntity<Token>(new Token(str), HttpStatus.OK);
        }
        else {
            response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return response;
    }

}
