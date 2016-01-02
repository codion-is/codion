/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.testing.EntityTestUnit;

import org.junit.Test;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class PetstoreTest extends EntityTestUnit {

  @Test
  public void address() throws Exception {
    testEntity(T_ADDRESS);
  }

  @Test
  public void category() throws Exception {
    testEntity(T_CATEGORY);
  }

  @Test
  public void item() throws Exception {
    testEntity(T_ITEM);
  }

  @Test
  public void product() throws Exception {
    testEntity(T_PRODUCT);
  }

  @Test
  public void sellerInfo() throws Exception {
    testEntity(T_SELLER_CONTACT_INFO);
  }

  @Test
  public void tag() throws Exception {
    testEntity(T_TAG);
  }

  @Test
  public void tagItem() throws Exception {
    testEntity(T_TAG_ITEM);
  }

  @Override
  protected void loadDomainModel() {
    Petstore.init();
  }

  @Override
  protected User getTestUser() throws CancelException {
    return User.UNIT_TEST_USER;
  }
}
