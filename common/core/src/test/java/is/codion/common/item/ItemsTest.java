/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class ItemsTest {

  @Test
  void item() throws IOException, ClassNotFoundException {
    Item<String> item = Item.item("hello", "world");
    assertEquals("hello", item.getValue());
    assertEquals("world", item.getCaption());
    assertEquals("world", item.toString());

    Item<String> newItem = Item.item("hello", "bla");
    assertEquals(item, newItem);
    assertEquals("hello".hashCode(), item.hashCode());
    assertEquals(1, item.compareTo(newItem));

    Item<String> thirdItem = Item.item("hello");
    assertEquals("hello".hashCode(), thirdItem.hashCode());
    assertEquals("hello", thirdItem.getCaption());

    assertEquals(newItem, Item.item("hello"));
    assertNotEquals(newItem, "hello");

    assertEquals(0, Item.item(null).hashCode());

    //just make sure it's ok post serialization
    Item<String> deser = Serializer.deserialize(Serializer.serialize(item));
    deser.compareTo(item);
  }

  @Test
  void itemI18n() {
    assertThrows(NullPointerException.class, () -> Item.itemI18n("value", null, "item"));
    assertThrows(NullPointerException.class, () -> Item.itemI18n("value", ItemsTest.class.getName(), null));

    Item<String> item = Item.itemI18n("value", ItemsTest.class.getName(), "item");
    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Gildi", item.getCaption());

    item = Item.itemI18n("value", ItemsTest.class.getName(), "item");
    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Item", item.getCaption());
  }
}
