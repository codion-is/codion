/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

/**
 * Encapsulates a username and password.
 */
public interface User {

  /**
   * @return the username
   */
  String getUsername();

  /**
   * @param password the password
   */
  void setPassword(char[] password);

  /**
   * @return the password
   */
  char[] getPassword();

  /**
   * Clears the password
   */
  void clearPassword();
}