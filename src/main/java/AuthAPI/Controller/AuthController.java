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
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/auth")
public class AuthController {

    //@Autowired
    //private RealmConfig realmConfig;


    private Keycloak adminInstance =

            Keycloak.getInstance("http://localhost:8082/auth",
            "master",
            "admin",
            "Pa55w0rd",
            "mercator-services");
    private RealmResource realmController = adminInstance.realm("master");



    /**
    private RealmResource realm = Keycloak.getInstance(realmConfig.getServerurl(),
            realmConfig.getRealm(),
            realmConfig.getUsername(),
            realmConfig.getPassword(),
            realmConfig.getResource()).realm(realmConfig.getResource());

    **/


    private AuthzClient az = AuthzClient.create();

    private Token getToken(UserCredentials userData){
        return new Token(this.az.obtainAccessToken(userData.getUsername(), userData.getPassword()));
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

    //temporary
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    //public ResponseEntity<Token> refresh(@RequestBody Token userToken) throws IOException, InterruptedException{
    public ResponseEntity<Token> refresh(@RequestBody Token userToken) throws IOException {

        String refreshToken = userToken.getRefreshToken();
        ResponseEntity<Token> response;


        //request new accesskey
        //requestbody
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("client_id", "mercator-services"));
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));

        //send post request
        HttpPost request = new HttpPost("http://localhost:8082/auth/realms/master/protocol/openid-connect/token");
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
        System.out.println(resp.toString());

        return response;
    }


}
