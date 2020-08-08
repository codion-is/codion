/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.DateFormats;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class PropertiesTest {

  private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
  private static final EntityType<Entity> ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType");
  private static final EntityType<Entity> ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

  @Test
  public void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(ENTITY_TYPE.integerAttribute("attribute"), "caption", linkedValues -> null));
  }

  @Test
  public void foreignKeyPropertyNonUniqueReferencedAttribute() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(attribute, "caption").reference(attribute, attribute));
  }

  @Test
  public void foreignKeyPropertyDifferentReferenceEntities() {
    final Attribute<Entity> fkAttribute = ENTITY_TYPE.entityAttribute("attribute");
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(fkAttribute, "caption")
            .reference(attribute1, attribute1)
            .reference(attribute2, ENTITY_TYPE2.integerAttribute("test")));
  }

  @Test
  public void foreignKeyPropertyDifferentReferenceEntities2() {
    final Attribute<Entity> fkAttribute = ENTITY_TYPE.entityAttribute("attribute");
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(fkAttribute, "caption")
            .reference(attribute1, attribute1)
            .reference(ENTITY_TYPE2.integerAttribute("test"), attribute2));
  }

  @Test
  public void foreignKeyPropertyAttributeFromOtherEntity() {
    final Attribute<Entity> fkAttribute = ENTITY_TYPE.entityAttribute("attribute");
    final Attribute<Integer> attribute1 = ENTITY_TYPE2.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(fkAttribute, "caption")
            .reference(attribute1, attribute2));
  }

  @Test
  public void foreignKeyPropertyDuplicateAttribute() {
    final Attribute<Entity> fkAttribute = ENTITY_TYPE.entityAttribute("attribute");
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> foreignKeyProperty(fkAttribute, "caption")
            .reference(attribute1, attribute2)
            .reference(attribute1, attribute2));
  }

  @Test
  public void foreignKeyPropertyWithoutReferencedAttribute() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("attribute"), "caption").reference(attribute, null));
  }

  @Test
  public void foreignKeyPropertyWithoutAttribute() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> foreignKeyProperty(ENTITY_TYPE.entityAttribute("attribute"), "caption").reference(null, attribute));
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
