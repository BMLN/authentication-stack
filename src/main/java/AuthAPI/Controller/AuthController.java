package AuthAPI.Controller;


import AuthAPI.Model.Token;
import AuthAPI.Model.UserCredentials;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.client.util.HttpResponseException;
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

    private RealmResource realmController;
    private String authRealm;
    private String authClient;

    @Autowired
    private void connectRealmController(   @Value("${auth.user}") String username,
                                           @Value("${auth.password}") String password,
                                           @Value("${auth.realm}") String realm,
                                           @Value("${auth.client}") String client,
                                           @Value("${auth.use-new}") boolean uses_new,
                                           @Value("${auth.realm.new}") String new_realm,
                                           @Value("${auth.client.new}") String new_client){

        Keycloak keycloak = Keycloak.getInstance(
                keycloakURL,
                realm,
                username,
                password,
                client);

        this.authRealm = uses_new ? new_realm : realm;
        this.authClient = uses_new ? new_client : client;
        this.realmController = keycloak.realm(authRealm);

        try {
            keycloak.realms().realm(realm).clients().findAll();
            System.out.println("KEYCLOAK-CONFIGURATION: successfully connected to admin Controller!");
        }
        catch (Exception exception) {
            System.out.println("KEYCLOAK-CONFIGURATION: couldn't connect to keycloak: " + exception.getMessage());
            System.exit(-1);
        }

        try{
            //keycloak.realms().realm(authRealm).clients().findAll();
            keycloak.realm(authRealm).clients().findAll();
        }
        catch (NotFoundException noRealmException){
            System.out.println("KEYCLOAK-CONFIGURATION: creating...");

            ClientRepresentation authClient = new ClientRepresentation();
            authClient.setClientId(new_client);
            authClient.setPublicClient(true);
            authClient.setDirectAccessGrantsEnabled(true);
            authClient.setEnabled(true);

            RealmRepresentation newRealm = new RealmRepresentation();
            newRealm.setRealm(new_realm);
            newRealm.setAccessTokenLifespan(95);
            newRealm.setClients(List.of(authClient));
            newRealm.setEnabled(true);
            keycloak.realms().create(newRealm);
        }
        catch (Exception exception) {
            System.out.println("KEYCLOAK-CONFIGURATION: failed: " + exception.getMessage());
            System.exit(-1);
        }
        System.out.println("KEYCLOAK-CONFIGURATION: successfully connected to auth Resource!");
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

            response = new ResponseEntity<Token>(this.getToken(userData), HttpStatus.OK);
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
        catch (HttpResponseException exception){
            System.out.println("login failed");
            response = new ResponseEntity<>(HttpStatus.UNAUTHORIZED); //HTTPStatus:401
        }
        return response;
    }

    //keycloak adapters don't support refreshing tokens
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    //public ResponseEntity<Token> refresh(@RequestBody Token userToken) throws IOException, InterruptedException{
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
