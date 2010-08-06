package org.jminor.common.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ItemTest {

  @Test
  public void test() {
    final Item<String> item = new Item<String>("hello", "world");
    assertEquals("hello", item.getItem());
    assertEquals("world", item.getCaption());
    assertEquals("world", item.toString());

    final Item<String> newItem = new Item<String>("hello", "bla");
    assertEquals(item, newItem);
    assertEquals("hello".hashCode(), item.hashCode());
    assertEquals(1, item.compareTo(newItem));
  }
}
