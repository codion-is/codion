/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SerializerTest {

  @Test
  public void serializeDeserialize() throws IOException, ClassNotFoundException {
    assertNull(Serializer.deserialize(new byte[0]));
    assertEquals(0, Serializer.serialize(null).length);
    assertEquals(Integer.valueOf(4), Serializer.deserialize(Serializer.serialize(4)));
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final List<Integer> ints = asList(1, 2, 3, 4);
    final File file = File.createTempFile("FileUtilTest.serialize", ".txt");
    file.deleteOnExit();

    Serializer.serializeToFile(ints, file);

    final List<Integer> readInts = Serializer.deserializeFromFile(file);

    assertEquals(ints, readInts);

    file.delete();
  }
}
