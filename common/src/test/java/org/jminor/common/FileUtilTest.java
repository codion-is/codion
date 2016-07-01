package org.jminor.common;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FileUtilTest {

  @Test
  public void countLines() throws IOException {
    assertEquals(125, FileUtil.countLines("src/test/sql/create_h2_db.sql"));
    assertEquals(125, FileUtil.countLines(new File("src/test/sql/create_h2_db.sql")));
    assertEquals(116, FileUtil.countLines(new File("src/test/sql/create_h2_db.sql"), ");"));
  }
}
