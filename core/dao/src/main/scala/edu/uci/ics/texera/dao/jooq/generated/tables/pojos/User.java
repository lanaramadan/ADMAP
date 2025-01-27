/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.dao.jooq.generated.tables.pojos;


import edu.uci.ics.texera.dao.jooq.generated.enums.UserRole;
import edu.uci.ics.texera.dao.jooq.generated.tables.interfaces.IUser;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class User implements IUser {

    private static final long serialVersionUID = -51594379;

    private UInteger uid;
    private String   name;
    private String   email;
    private String   password;

    private String   scpUsername;

    private String   scpPassword;

    private String   googleId;
    private UserRole role;
    private String   googleAvatar;

    public User() {}

    public User(IUser value) {
        this.uid = value.getUid();
        this.name = value.getName();
        this.email = value.getEmail();
        this.password = value.getPassword();
        this.googleId = value.getGoogleId();
        this.role = value.getRole();
        this.googleAvatar = value.getGoogleAvatar();
        this.scpUsername = getScpUsername();
        this.scpPassword = getScpPassword();
    }

    public User(
        UInteger uid,
        String   name,
        String   email,
        String   password,
        String   googleId,
        UserRole role,
        String   googleAvatar,
        String   scpUsername,
        String   scpPassword
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.googleId = googleId;
        this.role = role;
        this.googleAvatar = googleAvatar;
        this.scpUsername = scpUsername != null ? scpUsername : "";
        this.scpPassword = scpPassword != null ? scpPassword : "";
    }

    @Override
    public UInteger getUid() {
        return this.uid;
    }

    @Override
    public void setUid(UInteger uid) {
        this.uid = uid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    public String getScpUsername() {return this.scpUsername;}

    public void setScpUsername(String scpUsername) {
        this.scpUsername = scpUsername;
    }

    public String getScpPassword() {return this.scpPassword;}

    public void setScpPassword(String scpPassword) {
        this.scpPassword = scpPassword;
    }

    @Override
    public String getGoogleId() {
        return this.googleId;
    }

    @Override
    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    @Override
    public UserRole getRole() {
        return this.role;
    }

    @Override
    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String getGoogleAvatar() {
        return this.googleAvatar;
    }

    @Override
    public void setGoogleAvatar(String googleAvatar) {
        this.googleAvatar = googleAvatar;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("User (");

        sb.append(uid);
        sb.append(", ").append(name);
        sb.append(", ").append(email);
        sb.append(", ").append(password);
        sb.append(", ").append(googleId);
        sb.append(", ").append(role);
        sb.append(", ").append(googleAvatar);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IUser from) {
        setUid(from.getUid());
        setName(from.getName());
        setEmail(from.getEmail());
        setPassword(from.getPassword());
        setGoogleId(from.getGoogleId());
        setRole(from.getRole());
        setGoogleAvatar(from.getGoogleAvatar());
    }

    @Override
    public <E extends IUser> E into(E into) {
        into.from(this);
        return into;
    }
}
