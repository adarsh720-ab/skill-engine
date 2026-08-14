package com.wexa.skillengine.entity;

/**
 * Domain representation of a (:User) node in CognoDB.
 * The password field always holds a BCrypt hash once persisted — never plaintext.
 */
public class UserNode {

    private String id;
    private String email;
    private String password; // BCrypt hash
    private String role;     // e.g. "ROLE_USER" or "ROLE_ADMIN"

    public UserNode() {
    }

    public UserNode(String id, String email, String password, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
