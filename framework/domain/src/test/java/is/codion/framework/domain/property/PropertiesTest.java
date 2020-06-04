/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.DateFormats;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import static is.codion.framework.domain.entity.EntityType.entityType;
import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertiesTest {

  private static final EntityType ENTITY_TYPE = entityType("entityType");
  private static final EntityType REFERENCED_ENTITY_TYPE = entityType("referencedEntityType");

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(ENTITY_TYPE.integerAttribute("attribute"), "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferenceAttribute() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute, "caption", REFERENCED_ENTITY_TYPE, columnProperty(attribute)));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("attribute"), "caption", REFERENCED_ENTITY_TYPE, (ColumnProperty.Builder<?>) null));
  }

  @Test
  public void foreignKeyPropertyWithoutReferenceEntityType() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("attribute"), "caption", null, columnProperty(ENTITY_TYPE.integerAttribute("col"))));
  }

  @Test
  public void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.doubleAttribute("attribute")).format(new SimpleDateFormat(DateFormats.COMPACT)));
  }

  @Test
  public void datePropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.localDateTimeAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  public void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  public void getMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).get().getMaximumFractionDigits());
  }

  @Test
  public void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).numberFormatGrouping(false));
  }

  @Test
  public void setMinimumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).minimumValue(5));
  }

  @Test
  public void setMaximumValueNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).maximumValue(5));
  }

  @Test
  public void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  public void minimumMaximumValue() {
    final ColumnProperty.Builder<Double> builder = columnProperty(ENTITY_TYPE.doubleAttribute("attribute"));
    builder.minimumValue(5);
    assertThrows(IllegalArgumentException.class, () -> builder.maximumValue(4));
    builder.maximumValue(6);
    assertThrows(IllegalArgumentException.class, () -> builder.minimumValue(7));
  }

  @Test
  public void setColumnName() {
    assertEquals("hello", columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName("hello").get().getColumnName());
  }

  @Test
  public void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName(null));
  }

  @Test
  public void description() {
    final String description = "Here is a description";
    final Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  public void mnemonic() {
    final Character mnemonic = 'M';
    final Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  public void foreignKeyPropertyNullable() {
    final ColumnProperty.Builder<Integer> columnProperty = columnProperty(ENTITY_TYPE.integerAttribute("attribute"));
    final ColumnProperty.Builder<Integer> columnProperty2 = columnProperty(ENTITY_TYPE.integerAttribute("attribute2"));
    final ForeignKeyProperty.Builder foreignKeyProperty =
            foreignKeyProperty(ENTITY_TYPE.entityAttribute("fkAttribute"), "fk", REFERENCED_ENTITY_TYPE,
                    Arrays.asList(columnProperty, columnProperty2));
    foreignKeyProperty.nullable(false);
    assertFalse(columnProperty.get().isNullable());
    assertFalse(columnProperty2.get().isNullable());
    assertFalse(foreignKeyProperty.get().isNullable());
  }

  @Test
  public void foreignKeyPropertyUpdatable() {
    final ColumnProperty.Builder<Integer> updatableReferenceProperty = columnProperty(ENTITY_TYPE.integerAttribute("attribute"));
    final ColumnProperty.Builder<Integer> nonUpdatableReferenceProperty = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).updatable(false);

    final ForeignKeyProperty.Builder updatableForeignKeyProperty = foreignKeyProperty(ENTITY_TYPE.entityAttribute(
            "fkProperty"), "test",
            REFERENCED_ENTITY_TYPE, updatableReferenceProperty);
    assertTrue(updatableForeignKeyProperty.get().isUpdatable());

    final ForeignKeyProperty nonUpdatableForeignKeyProperty = foreignKeyProperty(ENTITY_TYPE.entityAttribute(
            "fkProperty"), "test",
            REFERENCED_ENTITY_TYPE, nonUpdatableReferenceProperty).get();

    assertFalse(nonUpdatableForeignKeyProperty.isUpdatable());

    final ForeignKeyProperty nonUpdatableCompositeForeignKeyProperty =
            foreignKeyProperty(ENTITY_TYPE.entityAttribute("fkProperty"), "test", REFERENCED_ENTITY_TYPE,
                    Arrays.asList(updatableReferenceProperty, nonUpdatableReferenceProperty)).get();
    assertFalse(nonUpdatableCompositeForeignKeyProperty.isUpdatable());
  }

  @Test
  public void foreignKeyPropertyNullProperty() {
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("id"), "caption", ENTITY_TYPE, (ColumnProperty.Builder<?>) null));
  }

  @Test
  public void foreignKeyPropertyNoProperties() {
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("id")
            , "caption", ENTITY_TYPE,
            Collections.emptyList()));
  }

  @Test
  public void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").readOnly(false));
  }

  @Test
  public void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").updatable(false));
  }

  @Test
  public void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").insertable(false));
  }

  @Test
  public void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("property")).maximumLength(-4));
  }

  @Test
  public void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("property")).searchProperty(true));
  }
}
