package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.Types;

public class EntityDefinitionTest {

  @Test
  public void test() {
    final EntityDefinition definition = new EntityDefinition("entityID", "tableName",
            new Property.PrimaryKeyProperty("id"),
            new Property("name", Types.VARCHAR)).setIdSource(IdSource.NONE).setIdValueSource("idValueSource")
            .setSelectQuery("select * from dual").setLargeDataset(true).setOrderByClause("order by name")
            .setReadOnly(true).setSelectTableName("selectTableName");

    assertEquals("entityID", definition.getEntityID());
    assertEquals("tableName", definition.getTableName());
    assertEquals(IdSource.NONE, definition.getIdSource());
    assertEquals("idValueSource", definition.getIdValueSource());
    assertEquals("select * from dual", definition.getSelectQuery());
    assertEquals(true, definition.isLargeDataset());
    assertEquals("order by name", definition.getOrderByClause());
    assertEquals(true, definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("id, name", definition.getSelectColumnsString());
  }
}
