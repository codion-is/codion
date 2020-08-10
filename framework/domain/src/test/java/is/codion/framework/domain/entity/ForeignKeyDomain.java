package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;

import static is.codion.framework.domain.property.Properties.*;

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
    EntityType<Entity> TYPE = DOMAIN.entityType("species");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  void species() {
    define(Species.TYPE,
            primaryKeyProperty(Species.NO, "Number"),
            columnProperty(Species.NAME, "Name")
                    .maximumLength(50));
  }

  public interface Maturity {
    EntityType<Entity> TYPE = DOMAIN.entityType("species_maturity");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    Attribute<Entity> SPECIES_FK = TYPE.entityAttribute("species_fk");
  }

  void maturity() {
    define(Maturity.TYPE,
            columnProperty(Maturity.NO)
                    .primaryKeyIndex(0),
            columnProperty(Maturity.SPECIES_NO)
                    .primaryKeyIndex(1),
            foreignKeyProperty(Maturity.SPECIES_FK)
                    .reference(Maturity.SPECIES_NO, Species.NO));
  }

  public interface OtolithCategory {
    EntityType<Entity> TYPE = DOMAIN.entityType("otolith_category");
    Attribute<Integer> NO = TYPE.integerAttribute("no");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    Attribute<Entity> SPECIES_FK = TYPE.entityAttribute("species_fk");
  }

  void otolithCategory() {
    define(OtolithCategory.TYPE,
            columnProperty(OtolithCategory.NO)
                    .primaryKeyIndex(0),
            columnProperty(OtolithCategory.SPECIES_NO)
                    .primaryKeyIndex(1),
            foreignKeyProperty(OtolithCategory.SPECIES_FK)
                    .reference(OtolithCategory.SPECIES_NO, Species.NO));
  }

  public interface Otolith {
    EntityType<Entity> TYPE = DOMAIN.entityType("otolith");
    Attribute<Integer> STATION_ID = TYPE.integerAttribute("station_id");
    Attribute<Integer> SPECIES_NO = TYPE.integerAttribute("species_no");
    Attribute<Entity> SPECIES_FK = TYPE.entityAttribute("species_fk");
    Attribute<Integer> MATURITY_NO = TYPE.integerAttribute("maturity_no");
    Attribute<Entity> MATURITY_FK = TYPE.entityAttribute("maturity_fk");
    Attribute<Integer> OTOLITH_CATEGORY_NO = TYPE.integerAttribute("otolith_category_no");
    Attribute<Entity> OTOLITH_CATEGORY_FK = TYPE.entityAttribute("otolith_category_fk");
  }

  void otolith() {
    define(Otolith.TYPE,
            columnProperty(Otolith.STATION_ID)
                    .primaryKeyIndex(0),
            columnProperty(Otolith.SPECIES_NO)
                    .primaryKeyIndex(1)
                    .updatable(true)
                    .nullable(false),
            foreignKeyProperty(Otolith.SPECIES_FK)
                    .reference(Otolith.SPECIES_NO, Species.NO),
            columnProperty(Otolith.MATURITY_NO),
            foreignKeyProperty(Otolith.MATURITY_FK)
                    .reference(Otolith.MATURITY_NO, Maturity.NO)
                    .referenceReadOnly(Otolith.SPECIES_NO, Maturity.SPECIES_NO),
            columnProperty(Otolith.OTOLITH_CATEGORY_NO),
            foreignKeyProperty(Otolith.OTOLITH_CATEGORY_FK)
                    .reference(Otolith.OTOLITH_CATEGORY_NO, OtolithCategory.NO)
                    .referenceReadOnly(Otolith.SPECIES_NO, OtolithCategory.SPECIES_NO));
  }
}
