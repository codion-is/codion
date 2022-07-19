/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UserTest {

  @Test
  void test() {
    User user = User.parse("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user.setPassword("mess".toCharArray());
    assertEquals("mess", String.valueOf(user.getPassword()));
    assertEquals("User: scott", user.toString());
    assertEquals("scott".hashCode(), user.hashCode());
    assertEquals(User.user("scott"), user);
    user.setPassword("test".toCharArray());
    assertEquals("test", String.valueOf(user.getPassword()));
    user.clearPassword();
    assertEquals("", String.valueOf(user.getPassword()));
    assertNotEquals(user, "scott");
    assertEquals(user, User.parse("scott:blabla"));
  }

  @Test
  void parse() {
    User user = User.parse("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user = User.parse(" scott:ti ger");
    assertEquals("scott", user.getUsername());
    assertEquals("ti ger", String.valueOf(user.getPassword()));
    user = User.parse("pete");
    assertEquals("pete", user.getUsername());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parse(" john ");
    assertEquals("john", user.getUsername());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parse("scott:tiger:pet:e");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger:pet:e", String.valueOf(user.getPassword()));
  }

  @Test
  void parseNoUsername() {
    assertThrows(IllegalArgumentException.class, () -> User.parse(":tiger"));
    assertThrows(IllegalArgumentException.class, () -> User.parse("::"));
    assertThrows(IllegalArgumentException.class, () -> User.parse("::tiger:"));
  }

  @Test
  void parseNoUserInfo() {
    assertThrows(IllegalArgumentException.class, () -> User.parse(""));
  }
}
