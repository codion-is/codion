/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain;

import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class ChinookTest extends EntityTestUnit {

  public ChinookTest() {
    super(ChinookImpl.class.getName());
  }

  @Test
  public void album() throws Exception {
    test(T_ALBUM);
  }

  @Test
  public void artist() throws Exception {
    test(T_ARTIST);
  }

  @Test
  public void customer() throws Exception {
    test(T_CUSTOMER);
  }

  @Test
  public void employee() throws Exception {
    test(T_EMPLOYEE);
  }

  @Test
  public void genre() throws Exception {
    test(T_GENRE);
  }

  @Test
  public void invoce() throws Exception {
    test(T_INVOICE);
  }

  @Test
  public void invoiceLine() throws Exception {
    test(T_INVOICELINE);
  }

  @Test
  public void mediaType() throws Exception {
    test(T_MEDIATYPE);
  }

  @Test
  public void playlist() throws Exception {
    test(T_PLAYLIST);
  }

  @Test
  public void playlistTrack() throws Exception {
    test(T_PLAYLISTTRACK);
  }

  @Test
  public void track() throws Exception {
    test(T_TRACK);
  }
}
