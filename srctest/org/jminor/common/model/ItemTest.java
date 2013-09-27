package org.jminor.common.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemTest {

  @Test
  public void test() {
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
  }
}
