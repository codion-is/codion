/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.sql.Types;

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
    EntityRepository.add(new EntityDefinition(T_MASTER,
            new Property.PrimaryKeyProperty(MASTER_ID),
            new Property(MASTER_NAME, Types.VARCHAR),
            new Property(MASTER_CODE, Types.INTEGER)));

    EntityRepository.add(new EntityDefinition(T_DETAIL,
            new Property.PrimaryKeyProperty(DETAIL_ID),
            new Property(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            new Property(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE),
            new Property(DETAIL_STRING, Types.VARCHAR, DETAIL_STRING),
            new Property(DETAIL_DATE, Types.DATE, DETAIL_DATE),
            new Property(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            new Property(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN).setDefaultValue(true),
            new Property.ForeignKeyProperty(DETAIL_ENTITY_FK, DETAIL_ENTITY_FK, T_MASTER,
                    new Property(DETAIL_ENTITY_ID)),
            new Property.DenormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_ENTITY_FK,
                    EntityRepository.getProperty(T_MASTER, MASTER_NAME), DETAIL_MASTER_NAME),
            new Property.DenormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_ENTITY_FK,
                    EntityRepository.getProperty(T_MASTER, MASTER_CODE), DETAIL_MASTER_CODE))
            .setOrderByClause(DETAIL_STRING).setSelectTableName(DETAIL_SELECT_TABLE_NAME));
  }
}
