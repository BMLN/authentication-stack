package AuthAPI.Model;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Arrays;

public class UserCredentials {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRepresentation getUserRepresentation(){

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(this.getPassword());

        UserRepresentation userRepr = new UserRepresentation();
        userRepr.setUsername(this.getUsername());
        userRepr.setCredentials(Arrays.asList(password));
        userRepr.setEnabled(true);

        return userRepr;
    }
}
