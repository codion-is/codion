/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.user;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UserTest {

  @Test
  void test() throws Exception {
    User user = User.parse("scott:tiger");
    assertEquals("scott", user.username());
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
    assertEquals(User.user("scott"), User.user("ScoTT"));
    assertEquals(Serializer.deserialize(Serializer.serialize(User.user("scott", "test".toCharArray()))), User.user("Scott"));
    char[] password = new char[] {'0', '1'};
    user.setPassword(password);
    assertNotSame(password, user.getPassword());
  }

  @Test
  void parse() {
    User user = User.parse("scott:tiger");
    assertEquals("scott", user.username());
    assertEquals("tiger", String.valueOf(user.getPassword()));
    user = User.parse(" scott:ti ger");
    assertEquals("scott", user.username());
    assertEquals("ti ger", String.valueOf(user.getPassword()));
    user = User.parse("pete");
    assertEquals("pete", user.username());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parse(" john ");
    assertEquals("john", user.username());
    assertTrue(String.valueOf(user.getPassword()).isEmpty());
    user = User.parse("scott:tiger:pet:e");
    assertEquals("scott", user.username());
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
