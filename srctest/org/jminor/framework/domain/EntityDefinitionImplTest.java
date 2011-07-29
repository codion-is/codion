package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class EntityDefinitionImplTest {

  @Test
  public void test() {
    final Entity.Definition definition = new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR)).setIdSource(IdSource.NONE).setIdValueSource("idValueSource")
            .setSelectQuery("select * from dual").setOrderByClause("order by name")
            .setReadOnly(true).setSelectTableName("selectTableName").setGroupByClause("name");

    assertEquals("entityID", definition.getEntityID());
    assertEquals("tableName", definition.getTableName());
    assertEquals(IdSource.NONE, definition.getIdSource());
    assertEquals("idValueSource", definition.getIdValueSource());
    assertEquals("select * from dual", definition.getSelectQuery());
    assertEquals(false, definition.isSmallDataset());
    assertEquals("order by name", definition.getOrderByClause());
    assertEquals(true, definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("id, name", definition.getSelectColumnsString());
    assertEquals("name", definition.getGroupByClause());
  }
}
