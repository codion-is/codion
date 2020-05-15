/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ItemsTest {

  @Test
  public void test() throws IOException, ClassNotFoundException {
    final Item<String> item = Items.item("hello", "world");
    assertEquals("hello", item.getValue());
    assertEquals("world", item.getCaption());
    assertEquals("world", item.toString());

    final Item<String> newItem = Items.item("hello", "bla");
    assertEquals(item, newItem);
    assertEquals("hello".hashCode(), item.hashCode());
    assertEquals(1, item.compareTo(newItem));

    final Item<String> thirdItem = Items.item("hello");
    assertEquals("hello".hashCode(), thirdItem.hashCode());
    assertEquals("hello", thirdItem.getCaption());

    assertEquals(newItem, Items.item("hello"));
    assertNotEquals(newItem, "hello");

    assertEquals(0, Items.item(null).hashCode());

    //just make sure its ok post serialization
    final Item<String> deser = Serializer.deserialize(Serializer.serialize(item));
    deser.compareTo(item);
  }
}
