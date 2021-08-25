package AuthAPI.Model;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Arrays;

public class Token {

    private String accessToken;
    private String refreshToken;
    private int expiresIn;


    //needed for objectmapper
    public  Token(){
    }

    public Token(AccessTokenResponse keycloakToken){
        this.accessToken = keycloakToken.getToken();
        this.refreshToken = keycloakToken.getRefreshToken();
        this.expiresIn = (int) keycloakToken.getExpiresIn();
    }

    public Token(String str){

        int i = str.indexOf("access_token");
        str = str.substring(i + "access_token".length() + 2 + 1); //Subtract "access_token" - "header" : field name + name length + 2x symbols + 1x the symbol marking the value field
        int end = str.indexOf("\",");
        this.setAccessToken(str.substring(0, end));

        i = str.indexOf("expires_in");
        str = str.substring(i + "expires_in".length() + 2); //no +1, because there are no symbols marking the int value of the field
        end = str.indexOf(",");
        this.setExpiresIn(Integer.valueOf(str.substring(0, end)));

        i = str.indexOf("refresh_token");
        str = str.substring(i + "refresh_token".length() + 2 + 1);
        end = str.indexOf("\",");
        this.setRefreshToken(str.substring(0, end));

    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }



}
