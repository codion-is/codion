/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UserTest {

  @Test
  public void test() {
    final User user = new User("scott", "tiger");
    assertEquals("scott", user.getUsername());
    assertEquals("tiger", user.getPassword());
  }
}
