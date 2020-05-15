/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class PetstoreTest extends EntityTestUnit {

  public PetstoreTest() {
    super(Petstore.class.getName());
  }

  @Test
  public void address() throws Exception {
    test(T_ADDRESS);
  }

  @Test
  public void category() throws Exception {
    test(T_CATEGORY);
  }

  @Test
  public void item() throws Exception {
    test(T_ITEM);
  }

  @Test
  public void product() throws Exception {
    test(T_PRODUCT);
  }

  @Test
  public void sellerInfo() throws Exception {
    test(T_SELLER_CONTACT_INFO);
  }

  @Test
  public void tag() throws Exception {
    test(T_TAG);
  }

  @Test
  public void tagItem() throws Exception {
    test(T_TAG_ITEM);
  }
}
