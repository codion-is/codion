/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.*;

class ForeignKeyDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType("fkDomain");

  ForeignKeyDomain() {
    super(DOMAIN);
    species();
    maturity();
    otolithCategory();
    otolith();
  }

  public interface Species {
    EntityType TYPE = DOMAIN.entityType("species");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  void species() {
    add(definition(
            primaryKeyProperty(Species.NO, "Number"),
            columnProperty(Species.NAME, "Name")
                    .maximumLength(50)));
  }

  public interface Maturity {
    EntityType TYPE = DOMAIN.entityType("species_maturity");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", Maturity.SPECIES_NO, Species.NO);
  }

  void maturity() {
    add(definition(
            columnProperty(Maturity.NO)
                    .primaryKeyIndex(0),
            columnProperty(Maturity.SPECIES_NO)
                    .primaryKeyIndex(1),
            foreignKeyProperty(Maturity.SPECIES_FK)));
  }

  public interface OtolithCategory {
    EntityType TYPE = DOMAIN.entityType("otolith_category");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", OtolithCategory.SPECIES_NO, Species.NO);
  }

  void otolithCategory() {
    add(definition(
            columnProperty(OtolithCategory.NO)
                    .primaryKeyIndex(0),
            columnProperty(OtolithCategory.SPECIES_NO)
                    .primaryKeyIndex(1),
            foreignKeyProperty(OtolithCategory.SPECIES_FK)));
  }

  public interface Otolith {
    EntityType TYPE = DOMAIN.entityType("otolith");
    Attribute<Integer> STATION_ID = TYPE.integerAttribute("station_id");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", Otolith.SPECIES_NO, Species.NO);
    Attribute<Integer> MATURITY_NO = TYPE.integerAttribute("maturity_no");
    ForeignKey MATURITY_FK = TYPE.foreignKey("maturity_fk",
            Otolith.MATURITY_NO, Maturity.NO,
            Otolith.SPECIES_NO, Maturity.SPECIES_NO);
    Attribute<Integer> OTOLITH_CATEGORY_NO = TYPE.integerAttribute("otolith_category_no");
    ForeignKey OTOLITH_CATEGORY_FK = TYPE.foreignKey("otolith_category_fk",
            Otolith.OTOLITH_CATEGORY_NO, OtolithCategory.NO,
            Otolith.SPECIES_NO, OtolithCategory.SPECIES_NO);
  }

  void otolith() {
    add(definition(
            columnProperty(Otolith.STATION_ID)
                    .primaryKeyIndex(0),
            columnProperty(Otolith.SPECIES_NO)
                    .primaryKeyIndex(1)
                    .updatable(true)
                    .nullable(false),
            foreignKeyProperty(Otolith.SPECIES_FK),
            columnProperty(Otolith.MATURITY_NO),
            foreignKeyProperty(Otolith.MATURITY_FK)
                    .readOnly(Otolith.SPECIES_NO),
            columnProperty(Otolith.OTOLITH_CATEGORY_NO),
            foreignKeyProperty(Otolith.OTOLITH_CATEGORY_FK)
                    .readOnly(Otolith.SPECIES_NO)));
  }
}
