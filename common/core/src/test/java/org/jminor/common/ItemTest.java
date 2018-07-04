/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemTest {

  @Test
  public void test() throws IOException, ClassNotFoundException {
    final Item<String> item = new Item<>("hello", "world");
    assertEquals("hello", item.getItem());
    assertEquals("world", item.getCaption());
    assertEquals("world", item.toString());

    final Item<String> newItem = new Item<>("hello", "bla");
    assertEquals(item, newItem);
    assertEquals("hello".hashCode(), item.hashCode());
    assertEquals(1, item.compareTo(newItem));

    final Item<String> thirdItem = new Item<>("hello");
    assertEquals("hello".hashCode(), thirdItem.hashCode());
    assertEquals("hello", thirdItem.getCaption());

    assertEquals(0, new Item<String>(null).hashCode());

    //just make sure its ok post serialization
    final Item<String> deser = Util.deserialize(Util.serialize(item));
    deser.compareTo(item);
  }
}
