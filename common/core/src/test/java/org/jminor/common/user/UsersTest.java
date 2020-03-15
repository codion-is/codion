/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UsersTest {

  @Test
  public void test() {
    final User user = Users.parseUser("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user.setPassword("mess".toCharArray());
    assertEquals("mess", String.valueOf(user.getPassword()));
    assertEquals("User: scott", user.toString());
    assertEquals("scott".hashCode(), user.hashCode());
    assertEquals(Users.user("scott", null), user);
    user.setPassword("test".toCharArray());
    assertEquals("test", String.valueOf(user.getPassword()));
    user.clearPassword();
    assertEquals("", String.valueOf(user.getPassword()));
    assertNotEquals(user, "scott");
    assertEquals(user, Users.parseUser("scott:blabla"));
  }

  @Test
  public void parseUser() {
    final User user = Users.parseUser("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
  }

  @Test
  public void parseUserNoUsername() {
    assertThrows(IllegalArgumentException.class, () -> Users.parseUser(":tiger"));
  }

  @Test
  public void parseUserNoPassword() {
    assertThrows(IllegalArgumentException.class, () -> Users.parseUser("scott:"));
  }

  @Test
  public void parseUserNoUserInfo() {
    assertThrows(IllegalArgumentException.class, () -> Users.parseUser(""));
  }
}
