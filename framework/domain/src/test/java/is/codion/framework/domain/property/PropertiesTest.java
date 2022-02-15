/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Serializer;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class PropertiesTest {

  private static final DomainType DOMAIN_TYPE = DomainType.domainType("domainType");
  private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType", PropertiesTest.class.getName());
  private static final EntityType ENTITY_TYPE2 = DOMAIN_TYPE.entityType("entityType2");

  @Test
  void derivedPropertyWithoutLinkedProperties() {
    assertThrows(IllegalArgumentException.class, () -> derivedProperty(ENTITY_TYPE.integerAttribute("attribute"), "caption", linkedValues -> null));
  }

  @Test
  void foreignKeyPropertyNonUniqueReferencedAttribute() {
    final Attribute<Integer> attribute = ENTITY_TYPE.integerAttribute("attr");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, attribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities() {
    final Attribute<Integer> integerAttribute = ENTITY_TYPE2.integerAttribute("test");
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, attribute2, integerAttribute));
  }

  @Test
  void foreignKeyPropertyDifferentReferenceEntities2() {
    final Attribute<Integer> integerAttribute = ENTITY_TYPE2.integerAttribute("test");
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute1, integerAttribute, attribute2));
  }

  @Test
  void foreignKeyPropertyAttributeFromOtherEntity() {
    final Attribute<Integer> attribute1 = ENTITY_TYPE2.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2));
  }

  @Test
  void foreignKeyPropertyDuplicateAttribute() {
    final Attribute<Integer> attribute1 = ENTITY_TYPE.integerAttribute("attribute1");
    final Attribute<Integer> attribute2 = ENTITY_TYPE.integerAttribute("attribute2");
    assertThrows(IllegalArgumentException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute1, attribute2, attribute1, attribute2));
  }

  @Test
  void foreignKeyWithoutReferencedAttribute() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", attribute, null));
  }

  @Test
  void foreignKeyWithoutReference() {
    final Attribute<Entity> attribute = ENTITY_TYPE.entityAttribute("attribute");
    assertThrows(NullPointerException.class, () -> ENTITY_TYPE.foreignKey("attribute", null, attribute));
  }

  @Test
  void intPropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().getDatePattern())));
  }

  @Test
  void doublePropertyWithDateFormat() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.doubleAttribute("attribute"))
            .format(new SimpleDateFormat(LocaleDateTimePattern.builder().yearTwoDigits().build().getDatePattern())));
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
  void getMaximumFractionDigitsNotNumerical() {
    assertEquals(-1, columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).get().getMaximumFractionDigits());
  }

  @Test
  void setNumberFormatGroupingNotNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).numberFormatGrouping(false));
  }

  @Test
  void setRangeNonNumerical() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.localDateAttribute("attribute")).range(5, 6));
  }

  @Test
  void setMaximumLengthNonString() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("attribute")).maximumFractionDigits(5));
  }

  @Test
  void minimumMaximumValue() {
    final ColumnProperty.Builder<Double, ?> builder = columnProperty(ENTITY_TYPE.doubleAttribute("attribute"));
    assertThrows(IllegalArgumentException.class, () -> builder.range(5, 4));
  }

  @Test
  void setColumnName() {
    assertEquals("hello", columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName("hello").get().getColumnName());
  }

  @Test
  void setColumnNameNull() {
    assertThrows(NullPointerException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("attribute")).columnName(null));
  }

  @Test
  void description() {
    final String description = "Here is a description";
    final Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).description(description).get();
    assertEquals(description, property.getDescription());
  }

  @Test
  void mnemonic() {
    final Character mnemonic = 'M';
    final Property<Integer> property = columnProperty(ENTITY_TYPE.integerAttribute("attribute")).mnemonic(mnemonic).get();
    assertEquals(mnemonic, property.getMnemonic());
  }

  @Test
  void subqueryPropertySetReadOnlyFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").readOnly());
  }

  @Test
  void subqueryPropertySetUpdatableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").updatable(false));
  }

  @Test
  void subqueryPropertySetInsertableFalse() {
    assertThrows(UnsupportedOperationException.class, () -> subqueryProperty(ENTITY_TYPE.integerAttribute("test"), "caption", "select").insertable(false));
  }

  @Test
  void stringPropertyNegativeMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.stringAttribute("property")).maximumLength(-4));
  }

  @Test
  void searchPropertyNonVarchar() {
    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("property")).searchProperty());
  }

  @Test
  void i18n() throws IOException, ClassNotFoundException {
    Property<Integer> property =
            columnProperty(ENTITY_TYPE.integerAttribute("i18n"))
                    .captionResourceKey("test").get();

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", property.getCaption());

    property = Serializer.deserialize(Serializer.serialize(property));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", property.getCaption());

    assertThrows(IllegalStateException.class, () -> columnProperty(ENTITY_TYPE2.integerAttribute("i18n"))
                    .captionResourceKey("key"));

    assertThrows(IllegalArgumentException.class, () -> columnProperty(ENTITY_TYPE.integerAttribute("i18n"))
                    .captionResourceKey("invalid_key"));
  }
}
