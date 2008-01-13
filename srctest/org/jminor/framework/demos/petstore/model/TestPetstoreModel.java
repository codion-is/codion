/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.model.AbstractEntityTestFixture;
import org.jminor.framework.model.Entity;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashMap;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 13:20:26
 */
public class TestPetstoreModel extends TestCase {

  private static IEntityDb db;

  static {
    new Petstore();
    FrameworkSettings.get().useQueryRange = false;
    FrameworkSettings.get().useSmartRefresh = false;
    try {
      db = new AbstractEntityTestFixture() {
        public User getTestUser() throws UserCancelException {
          return new User("scott", "tiger");
        }

        public HashMap<String, Entity> initReferenceEntities(final Collection<String> classes) throws Exception {
          return null;
        }
      }.getIEntityDbProvider().getEntityDb();
    }
    catch (UserException e) {
      e.printStackTrace();
      throw e.getRuntimeException();
    }
  }

  public TestPetstoreModel(String name) {
    super(name);
  }

  public void testAddress() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ADDRESS));
  }

  public void testCategory() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_CATEGORY));
  }

  public void testItem() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ITEM));
  }

  public void testProduct() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_PRODUCT));
  }

  public void testSellerInfo() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_SELLER_CONTACT_INFO));
  }

  public void testTag() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_TAG));
  }

  public void testTagItem() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_TAG_ITEM));
  }

  public void testZipLocation() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ZIP_LOCATION));
  }
}
