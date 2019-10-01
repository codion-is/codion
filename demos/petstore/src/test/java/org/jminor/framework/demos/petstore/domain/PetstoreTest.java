/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static org.jminor.framework.demos.petstore.domain.Petstore.*;

public class PetstoreTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public PetstoreTest() {
    super(Petstore.class.getName());
  }

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
  protected User getTestUser() throws CancelException {
    return UNIT_TEST_USER;
  }
}
