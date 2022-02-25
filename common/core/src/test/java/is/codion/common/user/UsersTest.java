/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UsersTest {

  @Test
  void test() {
    User user = User.parseUser("scott:tiger");
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
    assertEquals(user, User.parseUser("scott:blabla"));
  }

  @Test
  void parseUser() {
    User user = User.parseUser("scott:tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user = User.parseUser(" scott:ti ger");
    assertEquals("scott", user.getUsername());
    assertEquals("ti ger", String.valueOf(user.getPassword()));
    user = User.parseUser("pete");
    assertEquals("pete", user.getUsername());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parseUser(" john ");
    assertEquals("john", user.getUsername());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parseUser("scott:tiger:pet:e");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger:pet:e", String.valueOf(user.getPassword()));
  }

  @Test
  void parseUserNoUsername() {
    assertThrows(IllegalArgumentException.class, () -> User.parseUser(":tiger"));
    assertThrows(IllegalArgumentException.class, () -> User.parseUser("::"));
    assertThrows(IllegalArgumentException.class, () -> User.parseUser("::tiger:"));
  }

  @Test
  void parseUserNoUserInfo() {
    assertThrows(IllegalArgumentException.class, () -> User.parseUser(""));
  }
}
