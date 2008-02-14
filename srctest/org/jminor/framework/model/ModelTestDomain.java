/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.IdSource;

public class ModelTestDomain {

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String ID_PROP = "id";
  public static final String INT_PROP = "int";
  public static final String DOUBLE_PROP = "double";
  public static final String STRING_PROP = "string";
  public static final String SHORT_DATE_PROP = "short_date";
  public static final String LONG_DATE_PROP = "long_date";
  public static final String BOOLEAN_PROP = "boolean";
  public static final String ENTITY_ID_PROP = "entity_id";
  public static final String ENTITY_REF = "entity_ref";
  public static final String DENORM_PROP = "denorm_prop";
  public static final String SELECT_TABLE_NAME = "test.entity_test_select";

  public static final String T_TEST_DETAIL_ENTITY = "test.detail_entity";
  public static final String T_TEST_MASTER_ENTITY = "test.master_entity";

  static {
    EntityRepository.get().initialize(T_TEST_MASTER_ENTITY,
            new Property.PrimaryKeyProperty(ID),
            new Property(NAME, Type.STRING));

    EntityRepository.get().initialize(T_TEST_DETAIL_ENTITY, IdSource.ID_NONE, null,
            STRING_PROP, SELECT_TABLE_NAME, false,
            new Property.PrimaryKeyProperty(ID_PROP).setDefaultValue(420),
            new Property(INT_PROP, Type.INT, INT_PROP),
            new Property(DOUBLE_PROP, Type.DOUBLE, DOUBLE_PROP),
            new Property(STRING_PROP, Type.STRING, STRING_PROP),
            new Property(SHORT_DATE_PROP, Type.SHORT_DATE, SHORT_DATE_PROP),
            new Property(LONG_DATE_PROP, Type.LONG_DATE, LONG_DATE_PROP),
            new Property(BOOLEAN_PROP, Type.BOOLEAN, BOOLEAN_PROP).setDefaultValue(Type.Boolean.TRUE),
            new Property.EntityProperty(ENTITY_REF, ENTITY_REF, T_TEST_MASTER_ENTITY,
                    new Property(ENTITY_ID_PROP)),
            new Property.DenormalizedViewProperty(DENORM_PROP, T_TEST_MASTER_ENTITY,
                    EntityRepository.get().getProperty(T_TEST_MASTER_ENTITY, NAME), DENORM_PROP));
  }
}
