/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class ChinookTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public ChinookTest() {
    super(ChinookImpl.class.getName());
  }

  @Test
  public void album() throws Exception {
    testEntity(T_ALBUM);
  }

  @Test
  public void artist() throws Exception {
    testEntity(T_ARTIST);
  }

  @Test
  public void customer() throws Exception {
    testEntity(T_CUSTOMER);
  }

  @Test
  public void employee() throws Exception {
    testEntity(T_EMPLOYEE);
  }

  @Test
  public void genre() throws Exception {
    testEntity(T_GENRE);
  }

  @Test
  public void invoce() throws Exception {
    testEntity(T_INVOICE);
  }

  @Test
  public void invoiceLine() throws Exception {
    testEntity(T_INVOICELINE);
  }

  @Test
  public void mediaType() throws Exception {
    testEntity(T_MEDIATYPE);
  }

  @Test
  public void playlist() throws Exception {
    testEntity(T_PLAYLIST);
  }

  @Test
  public void playlistTrack() throws Exception {
    testEntity(T_PLAYLISTTRACK);
  }

  @Test
  public void track() throws Exception {
    testEntity(T_TRACK);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return UNIT_TEST_USER;
  }
}
