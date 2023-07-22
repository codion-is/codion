/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static is.codion.framework.demos.petstore.domain.Petstore.*;

public class PetstoreTest extends EntityTestUnit {

  public PetstoreTest() {
    super(new Petstore());
  }

  @Test
  void address() throws Exception {
    test(Address.TYPE);
  }

  @Test
  void category() throws Exception {
    test(Category.TYPE);
  }

  @Test
  void item() throws Exception {
    test(Item.TYPE);
  }

  @Test
  void product() throws Exception {
    test(Product.TYPE);
  }

  @Test
  void sellerInfo() throws Exception {
    test(SellerContactInfo.TYPE);
  }

  @Test
  void tag() throws Exception {
    test(Tag.TYPE);
  }

  @Test
  void tagItem() throws Exception {
    test(TagItem.TYPE);
  }
}
