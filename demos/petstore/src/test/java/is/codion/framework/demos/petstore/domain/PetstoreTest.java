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
    test(Address.TYPE);
  }

  @Test
  public void category() throws Exception {
    test(Category.TYPE);
  }

  @Test
  public void item() throws Exception {
    test(Item.TYPE);
  }

  @Test
  public void product() throws Exception {
    test(Product.TYPE);
  }

  @Test
  public void sellerInfo() throws Exception {
    test(SellerContactInfo.TYPE);
  }

  @Test
  public void tag() throws Exception {
    test(Tag.TYPE);
  }

  @Test
  public void tagItem() throws Exception {
    test(TagItem.TYPE);
  }
}
