/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UserTest {

  @Test
  public void test() {
    final User user = new User("scott", "tiger".toCharArray());
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user.setPassword("mess".toCharArray());
    assertEquals("mess", String.valueOf(user.getPassword()));
    assertEquals("User: scott", user.toString());
    assertEquals("scott".hashCode(), user.hashCode());
    assertEquals(new User("scott", null), user);
    user.setPassword("test".toCharArray());
    assertEquals("test", String.valueOf(user.getPassword()));
    user.clearPassword();
    assertEquals("", String.valueOf(user.getPassword()));
  }

  @Test
  public void parseUser() {
    final User user = User.parseUser("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
  }

  @Test
  public void parseUserNoUsername() {
    assertThrows(IllegalArgumentException.class, () -> User.parseUser(":tiger"));
  }

  @Test
  public void parseUserNoPassword() {
    assertThrows(IllegalArgumentException.class, () -> User.parseUser("scott:"));
  }

  @Test
  public void parseUserNoUserInfo() {
    assertThrows(IllegalArgumentException.class, () -> User.parseUser(""));
  }
}
