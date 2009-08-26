/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.IdSource;

public class EntityTestDomain {

  public static final String T_MASTER = "test.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  public static final String T_DETAIL = "test.detail_entity";
  public static final String DETAIL_ID = "id";
  public static final String DETAIL_INT = "int";
  public static final String DETAIL_DOUBLE = "double";
  public static final String DETAIL_STRING = "string";
  public static final String DETAIL_DATE = "date";
  public static final String DETAIL_TIMESTAMP = "timestamp";
  public static final String DETAIL_BOOLEAN = "boolean";
  public static final String DETAIL_ENTITY_ID = "entity_id";
  public static final String DETAIL_ENTITY_FK = "entity_ref";
  public static final String DETAIL_MASTER_NAME = "master_name";
  public static final String DETAIL_MASTER_CODE = "master_code";

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  static {
    EntityRepository.initialize(T_MASTER,
            new Property.PrimaryKeyProperty(MASTER_ID),
            new Property(MASTER_NAME, Type.STRING),
            new Property(MASTER_CODE, Type.INT));

    EntityRepository.initialize(T_DETAIL, IdSource.NONE, null,
            DETAIL_STRING, DETAIL_SELECT_TABLE_NAME, false,
            new Property.PrimaryKeyProperty(DETAIL_ID).setDefaultValue(42),
            new Property(DETAIL_INT, Type.INT, DETAIL_INT),
            new Property(DETAIL_DOUBLE, Type.DOUBLE, DETAIL_DOUBLE),
            new Property(DETAIL_STRING, Type.STRING, DETAIL_STRING),
            new Property(DETAIL_DATE, Type.DATE, DETAIL_DATE),
            new Property(DETAIL_TIMESTAMP, Type.TIMESTAMP, DETAIL_TIMESTAMP),
            new Property(DETAIL_BOOLEAN, Type.BOOLEAN, DETAIL_BOOLEAN).setDefaultValue(Type.Boolean.TRUE),
            new Property.ForeignKeyProperty(DETAIL_ENTITY_FK, DETAIL_ENTITY_FK, T_MASTER,
                    new Property(DETAIL_ENTITY_ID)),
            new Property.DenormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_ENTITY_FK,
                    EntityRepository.getProperty(T_MASTER, MASTER_NAME), DETAIL_MASTER_NAME),
            new Property.DenormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_ENTITY_FK,
                    EntityRepository.getProperty(T_MASTER, MASTER_CODE), DETAIL_MASTER_CODE));
  }
}
