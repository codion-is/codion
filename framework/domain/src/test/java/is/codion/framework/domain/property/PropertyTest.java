/*
 * Copyright (c) 2011 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Serializer;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.domain.property.Property.*;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertyTest {

  private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
  private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType", PropertyTest.class.getName());
  private static final EntityType ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

  @Test
  void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(ENTITY_TYPE.integerAttribute("attribute"), "caption", linkedValues -> null));
  }

  @Test
  void foreignKeyPropertyNonUniqueReferencedAttribute() {
    Attribute<Integer> attribute = ENTITY_TYPE.integerAttribute("attr");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, attribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities() {
    Attribute<Integer> integerAttribute = ENTITY_TYPE2.integerAttribute("test");
    Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, attribute2, integerAttribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities2() {
    Attribute<Integer> integerAttribute = ENTITY_TYPE2.integerAttribute("test");
    Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, integerAttribute, attribute2));
  }

  @Test
  void foreignKeyPropertyAttributeFromOtherEntity() {
    Attribute<Integer> attribute1 = ENTITY_TYPE2.integerAttribute("attribute1");
    Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2));
  }

  @Test
  void foreignKeyPropertyDuplicateAttribute() {
    Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2, attribute1, attribute2));
  }

  @Test
  void foreignKeyWithoutReferencedAttribute() {
    Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, null));
  }

  @Test
  void foreignKeyWithoutReference() {
    Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", null, attribute));
  }

  @Test
  void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.doubleAttribute("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().datePattern())));
  }

  @Test
  void nonTemporalPropertyWithFormatPatter() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).dateTimePattern("dd-MM-yy"));
  }

  @Test
  void nonDecimalWithRoundingMode() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).decimalRoundingMode(RoundingMode.CEILING));
  }

  @Test
  void datePropertyWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void timestampPropertyWithNumberFormat() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateTimeAttribute("attribute")).format(NumberFormat.getIntegerInstance()));
  }

  @Test
  void setMaximumFractionDigitsNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  void maximumFractionDigitsNotNumerical() {
    assertEquals(-1, columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).build().maximumFractionDigits());
  }

  @Test
  void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).numberFormatGrouping(false));
  }

  @Test
  void setRangeNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).valueRange(5, 6));
  }

  @Test
  void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  void minimumMaximumValue() {
    ColumnProperty.Builder<Double, ?> builder = columnProperty(ENTITY_TYPE.doubleAttribute("attribute"));
    assertThrows(IllegalArgumentException.class, () -> builder.valueRange(5, 4));
  }

  @Test
  void setColumnName() {
    assertEquals("hello", ((ColumnProperty<?>) columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName("hello").build()).columnName());
  }

  @Test
  void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName(null));
  }

  @Test
  void description() {
    final String description = "Here is a description";
    Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).description(description).build();
    assertEquals(description, property.description());
  }

  @Test
  void mnemonic() {
    final Character mnemonic = 'M';
    Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).mnemonic(mnemonic).build();
    assertEquals(mnemonic, property.mnemonic());
  }

  @Test
  void subqueryProperties() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "select").readOnly(true));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "select").readOnly(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "select").updatable(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "select").insertable(false));
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "select").columnExpression("expression"));
  }

  @Test
  void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("property")).maximumLength(-4));
  }

  @Test
  void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("property")).searchProperty(true));
  }

  @Test
  void i18n() throws IOException, ClassNotFoundException {
    Property<Integer> property =
            columnProperty(ENTITY_TYPE.integerAttribute("i18n"))
                    .captionResourceKey("test").build();

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", property.caption());

    property = Serializer.deserialize(Serializer.serialize(property));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", property.caption());

    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE2.integerAttribute("i18n"))
                    .captionResourceKey("key"));

    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("i18n"))
                    .captionResourceKey("invalid_key"));
  }

  @Test
  void itemProperty() {
    List<Item<Integer>> itemsDuplicate = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(1));
    assertThrows(IllegalArgumentException.class, () -> Property.itemProperty(ENTITY_TYPE.integerAttribute("item"), itemsDuplicate));

    List<Item<Integer>> items = Arrays.asList(Item.item(null), Item.item(1), Item.item(2), Item.item(3));
    ItemProperty<Integer> property = (ItemProperty<Integer>) Property.itemProperty(ENTITY_TYPE.integerAttribute("item"), items).build();
    assertFalse(property.isValid(4));
    assertThrows(IllegalArgumentException.class, () -> property.item(4));
    assertTrue(property.isValid(null));
    assertTrue(property.isValid(2));
    assertNotNull(property.item(null));
    assertNotNull(property.item(1));
  }
}
