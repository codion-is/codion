/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.testing;

import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.tools.testing.EntityTestUnit;
import org.jminor.common.model.User;
import org.jminor.common.model.CancelException;

import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 19:32:45
 */
public class ChinookTest extends EntityTestUnit {

  @Test
  public void album() throws Exception{
    testEntity(Chinook.T_ALBUM);
  }

  @Test
  public void artist() throws Exception{
    testEntity(Chinook.T_ARTIST);
  }

  @Test
  public void customer() throws Exception{
    testEntity(Chinook.T_CUSTOMER);
  }

  @Test
  public void employee() throws Exception{
    testEntity(Chinook.T_EMPLOYEE);
  }

  @Test
  public void genre() throws Exception{
    testEntity(Chinook.T_GENRE);
  }

  @Test
  public void invoce() throws Exception{
    testEntity(Chinook.T_INVOICE);
  }

  @Test
  public void invoiceLine() throws Exception{
    testEntity(Chinook.T_INVOICELINE);
  }

  @Test
  public void mediaType() throws Exception{
    testEntity(Chinook.T_MEDIATYPE);
  }

  @Test
  public void playlist() throws Exception{
    testEntity(Chinook.T_PLAYLIST);
  }

  @Test
  public void playlistTrack() throws Exception{
    testEntity(Chinook.T_PLAYLISTTRACK);
  }

  @Test
  public void track() throws Exception{
    testEntity(Chinook.T_TRACK);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return User.UNIT_TEST_USER;
  }

  protected void loadDomainModel() {
    new Chinook();
  }
}
