package edu.uci.ics.texera.web.resource.auth

import edu.uci.ics.amber.core.storage.StorageConfig
import edu.uci.ics.amber.engine.common.AmberConfig
import edu.uci.ics.texera.dao.SqlServer
import edu.uci.ics.texera.web.auth.JwtAuth._
import edu.uci.ics.texera.web.model.http.request.auth.{
  RefreshTokenRequest,
  UserLoginRequest,
  UserRegistrationRequest,
  LdapUserRegistrationRequest
}
import edu.uci.ics.texera.web.model.http.response.TokenIssueResponse
import edu.uci.ics.texera.dao.jooq.generated.Tables.USER
import edu.uci.ics.texera.dao.jooq.generated.enums.UserRole
import edu.uci.ics.texera.dao.jooq.generated.tables.daos.UserDao
import edu.uci.ics.texera.dao.jooq.generated.tables.pojos.User

import edu.uci.ics.texera.web.resource.auth.AuthResource._
import org.jasypt.util.password.StrongPasswordEncryptor

import javax.ws.rs._
import javax.ws.rs.core.MediaType


import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionOptions, Entry, ResultCode}
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.extensions._

object AuthResource {

  final private lazy val userDao = new UserDao(
    SqlServer
      .getInstance(StorageConfig.jdbcUrl, StorageConfig.jdbcUsername, StorageConfig.jdbcPassword)
      .createDSLContext()
      .configuration
  )

  /**
    * Retrieve exactly one User from databases with the given username and password.
    * The password is used to validate against the hashed password stored in the db.
    *
    * @param name     String

    * @param password String, plain text password
    * @return
    */
  def retrieveUserByUsernameAndPassword(name: String, password: String): Option[User] = {
    Option(
      SqlServer
        .getInstance(StorageConfig.jdbcUrl, StorageConfig.jdbcUsername, StorageConfig.jdbcPassword)
        .createDSLContext()
        .select()
        .from(USER)
        .where(USER.NAME.eq(name))
        .fetchOneInto(classOf[User])
    ).filter(user => new StrongPasswordEncryptor().checkPassword(password, user.getPassword))
  }


  def addUserToLdap(ldapUser: User) = {
    println("IN addUserToLdap")
    println(s"Adding user to LDAP: ${ldapUser.getScpUsername()}")

    // LDAP server configuration
    val ldapHost = "ldap://3.139.234.0"
    val ldapPort = 389
    val ldapBindDN = "cn=admin,dc=admap,dc=com"
    val ldapBindPassword = "23627"
    val baseDN = "dc=admap,dc=com"

    // user attributes
    val username = ldapUser.getScpUsername()
    val uid = username.split("_")(1)
    val password = ldapUser.getScpPassword()
    val userDN = s"uid=$username,ou=users,$baseDN"

    // Create an LDAP connection
    val connectionOptions = new LDAPConnectionOptions()
    connectionOptions.setConnectTimeoutMillis(5000)

    // Use SSL/TLS if connecting to LDAPS (port 636)
    val connection = new LDAPConnection(ldapHost, ldapPort)

    try {
      // Bind to the LDAP server as the admin
      connection.bind(ldapBindDN, ldapBindPassword)

      // Create the user entry
      val userEntry = new Entry(
        userDN,
        "objectClass: inetOrgPerson",
        "objectClass: posixAccount",
        "objectClass: top",
        s"uid: $username",
        s"cn: $username",
        s"sn: $username",
        "loginShell: /bin/bash",
        s"uidNumber: ${uid}",
        "gidNumber: 1000",
        s"homeDirectory: /home/users/$username"
      )

      // Add the user entry to LDAP
      connection.add(userEntry)
      println(s"User $username added to LDAP.")

      // Set the user's password
      val passwordModifyRequest = new PasswordModifyExtendedRequest(
        userDN, // User's DN
        null, // Old password (null for new user)
        password // New password
      )
      val passwordModifyResult = connection.processExtendedOperation(passwordModifyRequest)
      if (passwordModifyResult.getResultCode == ResultCode.SUCCESS) {
        println(s"Password set for user $username.")
      } else {
        println(s"Failed to set password for user $username: ${passwordModifyResult.getResultCode}")
      }

    } catch {
      case e: LDAPException =>
        println(s"LDAP error: ${e.getMessage}")
    } finally {
      // Close the connection
      connection.close()
    }
  }

  def createHomeDirectory(path: String): Boolean = {
    val file = new java.io.File(path)
    if (!file.exists()) {
      file.mkdirs()
      println(s"Home directory created at $path")
      true
    } else {
      println(s"Home directory already exists at $path")
      false
    }
  }


}

@Path("/auth/")
@Consumes(Array(MediaType.APPLICATION_JSON))
@Produces(Array(MediaType.APPLICATION_JSON))
class AuthResource {

  @POST
  @Path("/login")
  def login(request: UserLoginRequest): TokenIssueResponse = {
    if (!AmberConfig.isUserSystemEnabled)
      throw new NotAcceptableException("User System is disabled on the backend!")
    retrieveUserByUsernameAndPassword(request.username, request.password) match {
      case Some(user) =>
        TokenIssueResponse(jwtToken(jwtClaims(user, dayToMin(TOKEN_EXPIRE_TIME_IN_DAYS))))
      case None => throw new NotAuthorizedException("Login credentials are incorrect.")
    }
  }

  @POST
  @Path("/refresh")
  def refresh(request: RefreshTokenRequest): TokenIssueResponse = {
    val claims = jwtConsumer.process(request.accessToken).getJwtClaims
    claims.setExpirationTimeMinutesInTheFuture(dayToMin(TOKEN_EXPIRE_TIME_IN_DAYS).toFloat)
    TokenIssueResponse(jwtToken(claims))
  }

  @POST
  @Path("/register")
  def register(request: UserRegistrationRequest): TokenIssueResponse = {
    println("TESTING")
    if (!AmberConfig.isUserSystemEnabled)
      throw new NotAcceptableException("User System is disabled on the backend!")
    val username = request.username
    if (username == null) throw new NotAcceptableException("Username cannot be null.")
    if (username.trim.isEmpty) throw new NotAcceptableException("Username cannot be empty.")
    userDao.fetchByName(username).size() match {
      case 0 =>
        val user = new User
        user.setName(username)
        user.setEmail(username)
        user.setRole(UserRole.ADMIN)
        // hash the plain text password
        user.setPassword(new StrongPasswordEncryptor().encryptPassword(request.password))
        userDao.insert(user)
        TokenIssueResponse(jwtToken(jwtClaims(user, dayToMin(TOKEN_EXPIRE_TIME_IN_DAYS))))
      case _ =>
        // the username exists already
        throw new NotAcceptableException("Username exists already.")
    }
  }


  @POST
  @Path("/add-ldap-user")
  def addLdapUser(request: LdapUserRegistrationRequest): TokenIssueResponse = {
    println("IN ROUTE")
    val scpUsername = request.scpUsername
    val scpPassword = request.scpPassword

    val ldapUser = new User
    ldapUser.setScpUsername(scpUsername)
    ldapUser.setScpPassword(scpPassword)

    // add user to LDAP
    addUserToLdap(ldapUser)

    // Create home directory - not needed anymore because of pam
    // createHomeDirectory(s"/users/$scpUsername")

    TokenIssueResponse(jwtToken(jwtClaims(ldapUser, dayToMin(TOKEN_EXPIRE_TIME_IN_DAYS))))
  }


}
