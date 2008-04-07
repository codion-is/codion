/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.IdSource;

public class ModelTestDomain {

  public static final String T_TEST_MASTER = "test.master_entity";
  public static final String TEST_MASTER_ID = "id";
  public static final String TEST_MASTER_NAME = "name";

  public static final String T_TEST_DETAIL = "test.detail_entity";
  public static final String TEST_DETAIL_ID = "id";
  public static final String TEST_DETAIL_INT = "int";
  public static final String TEST_DETAIL_DOUBLE = "double";
  public static final String TEST_DETAIL_STRING = "string";
  public static final String TEST_DETAIL_SHORT_DATE = "short_date";
  public static final String TEST_DETAIL_LONG_DATE = "long_date";
  public static final String TEST_DETAIL_BOOLEAN = "boolean";
  public static final String TEST_DETAIL_ENTITY_ID = "entity_id";
  public static final String TEST_DETAIL_ENTITY_REF = "entity_ref";
  public static final String TEST_DETAIL_MASTER_NAME_DENORM = "denorm_prop";

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  static {
    EntityRepository.get().initialize(T_TEST_MASTER,
            new Property.PrimaryKeyProperty(TEST_MASTER_ID),
            new Property(TEST_MASTER_NAME, Type.STRING));

    EntityRepository.get().initialize(T_TEST_DETAIL, IdSource.ID_NONE, null,
            TEST_DETAIL_STRING, DETAIL_SELECT_TABLE_NAME, false,
            new Property.PrimaryKeyProperty(TEST_DETAIL_ID).setDefaultValue(420),
            new Property(TEST_DETAIL_INT, Type.INT, TEST_DETAIL_INT),
            new Property(TEST_DETAIL_DOUBLE, Type.DOUBLE, TEST_DETAIL_DOUBLE),
            new Property(TEST_DETAIL_STRING, Type.STRING, TEST_DETAIL_STRING),
            new Property(TEST_DETAIL_SHORT_DATE, Type.SHORT_DATE, TEST_DETAIL_SHORT_DATE),
            new Property(TEST_DETAIL_LONG_DATE, Type.LONG_DATE, TEST_DETAIL_LONG_DATE),
            new Property(TEST_DETAIL_BOOLEAN, Type.BOOLEAN, TEST_DETAIL_BOOLEAN).setDefaultValue(Type.Boolean.TRUE),
            new Property.EntityProperty(TEST_DETAIL_ENTITY_REF, TEST_DETAIL_ENTITY_REF, T_TEST_MASTER,
                    new Property(TEST_DETAIL_ENTITY_ID)),
            new Property.DenormalizedViewProperty(TEST_DETAIL_MASTER_NAME_DENORM, T_TEST_MASTER,
                    EntityRepository.get().getProperty(T_TEST_MASTER, TEST_MASTER_NAME), TEST_DETAIL_MASTER_NAME_DENORM));
  }
}
