/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.testing;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.tools.testing.EntityTestUnit;

import org.junit.Test;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 13:20:26
 */
public class PetstoreTest extends EntityTestUnit {

  @Test
  public void address() throws Exception {
    testEntity(Petstore.T_ADDRESS);
  }

  @Test
  public void category() throws Exception {
    testEntity(Petstore.T_CATEGORY);
  }

  @Test
  public void item() throws Exception {
    testEntity(Petstore.T_ITEM);
  }

  @Test
  public void product() throws Exception {
    testEntity(Petstore.T_PRODUCT);
  }

  @Test
  public void sellerInfo() throws Exception {
    testEntity(Petstore.T_SELLER_CONTACT_INFO);
  }

  @Test
  public void tag() throws Exception {
    testEntity(Petstore.T_TAG);
  }

  @Test
  public void tagItem() throws Exception {
    testEntity(Petstore.T_TAG_ITEM);
  }

  @Override
  protected void loadDomainModel() {
    new Petstore();
  }

  @Override
  protected User getTestUser() throws CancelException {
    return new User("scott", "tiger");
  }
}
