/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
    assertEquals("tiger", String.valueOf(user.password()));
    assertEquals("scott".hashCode(), user.hashCode());
    assertEquals(User.user("scott"), user);
    user.clearPassword();
    assertEquals("", String.valueOf(user.password()));
    assertNotEquals(user, "scott");
    assertEquals(user, User.parse("scott:blabla"));
    assertEquals(User.user("scott"), User.user("ScoTT"));
    assertEquals(Serializer.deserialize(Serializer.serialize(User.user("scott", "test".toCharArray()))), User.user("Scott"));
    User copy = user.copy();
    assertEquals(user.username(), copy.username());
    assertNotSame(user.password(), copy.password());
    assertEquals(String.valueOf(user.password()), String.valueOf(copy.password()));
    assertEquals("User: scott", user.toString());
    copy.clearPassword();
    assertNotNull(copy.password());
    assertEquals(0, copy.password().length);
  }

  @Test
  void parse() {
    User user = User.parse("scott:tiger");
    assertEquals("scott", user.username());
    assertEquals("tiger", String.valueOf(user.password()));
    user = User.parse(" scott:ti ger");
    assertEquals("scott", user.username());
    assertEquals("ti ger", String.valueOf(user.password()));
    user = User.parse("pete");
    assertEquals("pete", user.username());
    assertTrue(String.valueOf(user.password()).isEmpty());
    user = User.parse(" john ");
    assertEquals("john", user.username());
    assertTrue(String.valueOf(user.password()).isEmpty());
    user = User.parse("scott:tiger:pet:e");
    assertEquals("scott", user.username());
    assertEquals("tiger:pet:e", String.valueOf(user.password()));
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
