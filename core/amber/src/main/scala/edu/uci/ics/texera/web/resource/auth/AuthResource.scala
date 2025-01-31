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


import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionOptions, Entry, ResultCode, AddRequest}
import com.unboundid.ldap.sdk._


import com.jcraft.jsch._
import java.nio.file.{Paths, Files}

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
    val ldapHost = "3.129.210.205"
    val ldapPort = 389
    val ldapBindDN = "cn=admin,dc=admap,dc=com"
    val ldapBindPassword = "23627"
    val baseDN = "dc=admap,dc=com"

    // user attributes
    val username = ldapUser.getScpUsername()
    val uid = username.split("_")(1)
    val password = ldapUser.getScpPassword()

    var connection: LDAPConnection = null
    try {
      connection = new LDAPConnection(ldapHost, ldapPort, ldapBindDN, ldapBindPassword)
      val entry = new Entry(s"uid=$username,ou=users,$baseDN")


      entry.addAttribute("objectClass", "inetOrgPerson", "posixAccount", "shadowAccount")
      entry.addAttribute("uid", username)
      entry.addAttribute("cn", username)
      entry.addAttribute("sn", username)
      entry.addAttribute("userPassword", password)
      entry.addAttribute("loginShell", "/bin/bash")
      entry.addAttribute("uidNumber", uid)
      entry.addAttribute("gidNumber", "5000")
      entry.addAttribute("homeDirectory", s"/home/users/$username")

      // Use AddRequest explicitly
      val addRequest = new AddRequest(entry)
      connection.add(addRequest)

    } catch {
      case e: LDAPException =>
        println(s"LDAP error: ${e.getMessage}, Code: ${e.getResultCode}")
        throw e

    } finally {
      if (connection != null && connection.isConnected) {
        connection.close()
      }
    }
  }

  def createHomeDirectory(ldapUser: User): Boolean = {
    val username = ldapUser.getScpUsername()
    val path = s"/home/users/$username/"

    val sshHost = "3.129.210.205"
    val sshUser = "ubuntu"
    val privateKeyPath = "/Users/lanaramadan/Desktop/ADMAP/core/012624.pem"

    val command = s"sudo mkdir -p $path"

    var session: Session = null

    try {
      val jsch = new JSch()
      jsch.addIdentity(privateKeyPath)

      session = jsch.getSession(sshUser, sshHost, 22)
      session.setConfig("StrictHostKeyChecking", "no")

      session.connect()

      val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
      channel.setCommand(command)
      channel.setErrStream(System.err)
      channel.connect()

      if (channel.getExitStatus == 0) {
        println(s"Home directory created at $path")
        true
      } else {
        println(s"Failed to create home directory at $path")
        false
      }
    } catch {
      case e: Exception =>
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
        false
    } finally {
      // Ensure the session is disconnected
      if (session != null && session.isConnected) {
        session.disconnect()
      }
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
    val scpUsername = request.scpUsername
    val scpPassword = request.scpPassword

    val ldapUser = new User
    ldapUser.setScpUsername(scpUsername)
    ldapUser.setScpPassword(scpPassword)
    addUserToLdap(ldapUser)
    createHomeDirectory(ldapUser)

    TokenIssueResponse(jwtToken(jwtClaims(ldapUser, dayToMin(TOKEN_EXPIRE_TIME_IN_DAYS))))
  }


}
