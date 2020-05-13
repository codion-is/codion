/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SerializerTest {

  @Test
  public void serializeDeserialize() throws IOException, ClassNotFoundException {
    assertNull(Serializer.deserialize(new byte[0]));
    assertEquals(0, Serializer.serialize(null).length);
    assertEquals(Integer.valueOf(4), Serializer.deserialize(Serializer.serialize(4)));
  }
}
