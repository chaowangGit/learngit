package com.mobvoi.overseas.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import com.mashape.unirest.http.utils.Base64Coder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LDAPUtil {

  private static DirContext context;

  private static String SERVER = "ldap://ldap.mobvoi.com:389/";

  private static String BASEDN = "ou=users,dc=ldap,dc=mobvoi,dc=com";

  private static String USERNAME = "cn=admin,dc=ldap,dc=mobvoi,dc=com";

  private static String PASSWORD = "Mobvoi!297";

  private static String PASSWORDATTR = "userPassword";

  private static Pattern pattern = Pattern.compile("\\{(.*)\\}(.*)");

  private static DirContext getContext() {
    if (context == null) {
      synchronized (LDAPUtil.class) {
        try {
          if (context == null) {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, SERVER + BASEDN);
            env.put(Context.SECURITY_PRINCIPAL, USERNAME);
            env.put(Context.SECURITY_CREDENTIALS, PASSWORD);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            // See http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/pool.html
            // http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.pool.timeout", "300000");
            env.put("com.sun.jndi.ldap.connect.pool.maxsize", "5");
            env.put("com.sun.jndi.ldap.read.timeout", "8000");
            env.put("com.sun.jndi.ldap.connect.timeout", "8000");

            context = new InitialDirContext(env);
          }
        } catch (NamingException e) {
          log.error(e.toString());
        }
      }
    }
    return context;
  }

  private static String encoded(String algorithm, String password) {
    // Calculate hash value
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance(algorithm);
      md.update(password.getBytes());
      byte[] bytes = md.digest();
      // Print out value in Base64 encoding
      char[] hash = Base64Coder.encode(bytes);
      password = String.format("{%s}%s", algorithm, String.valueOf(hash));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return password;
  }

  private static void closeContext() {
    try {
      if (context != null) {
        context.close();
        context = null;
      }
    } catch (NamingException namingException) {
      log.info(namingException.toString());
    }
  }

  /**
   * 判断是否存在.
   */
  public static boolean exist(String userName) {
    boolean success = false;
    try {
      getContext();
      SearchControls constrants = new SearchControls();
      constrants.setSearchScope(SearchControls.SUBTREE_SCOPE);
      NamingEnumeration<SearchResult> ne = context.search("", "cn=" + userName, constrants);

      return ne.hasMoreElements();
    } catch (NamingException ex) {
      log.error(ex.toString());

      closeContext();
    }
    return success;
  }

  /**
   * 验证授权.
   */
  public static boolean authorize(String userName, String password) {
    boolean success = false;

    try {
      getContext();
      SearchControls constrants = new SearchControls();
      constrants.setSearchScope(SearchControls.SUBTREE_SCOPE);
      NamingEnumeration<SearchResult> ne = context.search("", "cn=" + userName, constrants);
      while (ne != null & ne.hasMoreElements()) {
        Object obj = ne.nextElement();
        if (obj instanceof SearchResult) {
          SearchResult sr = (SearchResult) obj;
          Attributes attrs = sr.getAttributes();
          if (attrs == null) {
            log.info("user has not attribute, userName: " + userName);
          } else {
            Attribute attr = attrs.get(PASSWORDATTR);
            Object o = attr.get();
            byte[] s = (byte[]) o;
            String pwd2 = new String(s);
            Matcher matcher = pattern.matcher(pwd2);
            if (matcher.find()) {
              password = encoded(matcher.group(1), password);
            }
            success = pwd2.equals(password);
          }
        }
      }
    } catch (NamingException ex) {
      log.error(ex.toString());

      closeContext();
    }

    return success;
  }
}