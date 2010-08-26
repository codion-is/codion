/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.valuemap.StringProvider;

import java.sql.Types;

public class EntityTestDomain {

  private EntityTestDomain() {}
  public static void init() {}

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
    Entities.define(T_MASTER,
            Properties.primaryKeyProperty(MASTER_ID),
            Properties.columnProperty(MASTER_NAME, Types.VARCHAR),
            Properties.columnProperty(MASTER_CODE, Types.INTEGER))
            .setStringProvider(new StringProvider<String>(MASTER_NAME))
            .setLargeDataset(true);

    Entities.define(T_DETAIL,
            Properties.primaryKeyProperty(DETAIL_ID),
            Properties.columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            Properties.columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE),
            Properties.columnProperty(DETAIL_STRING, Types.VARCHAR, DETAIL_STRING),
            Properties.columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE),
            Properties.columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            Properties.columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN).setDefaultValue(true),
            Properties.foreignKeyProperty(DETAIL_ENTITY_FK, DETAIL_ENTITY_FK, T_MASTER,
                    Properties.columnProperty(DETAIL_ENTITY_ID)),
            Properties.denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_NAME), DETAIL_MASTER_NAME),
            Properties.denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_CODE), DETAIL_MASTER_CODE))
            .setOrderByClause(DETAIL_STRING).setSelectTableName(DETAIL_SELECT_TABLE_NAME)
            .setStringProvider(new StringProvider<String>(DETAIL_STRING));
  }
}
