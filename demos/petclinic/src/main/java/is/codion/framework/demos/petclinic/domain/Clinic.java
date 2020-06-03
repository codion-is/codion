/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.time.LocalDate;

import static is.codion.framework.domain.entity.Entities.type;

public interface Clinic {

  interface Vet {
    EntityType TYPE = type("petclinic.vet");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
  }

  interface Specialty {
    EntityType TYPE = type("petclinic.specialty");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  interface VetSpecialty {
    EntityType TYPE = type("petclinic.vet_specialty");
    Attribute<Integer> VET = TYPE.integerAttribute("vet");
    Attribute<Entity> VET_FK = TYPE.entityAttribute("vet_fk");
    Attribute<Integer> SPECIALTY = TYPE.integerAttribute("specialty");
    Attribute<Entity> SPECIALTY_FK = TYPE.entityAttribute("specialty_fk");
  }

  interface PetType {
    EntityType TYPE = type("petclinic.pet_type");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  interface Owner {
    EntityType TYPE = type("petclinic.owner");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    Attribute<String> ADDRESS = TYPE.stringAttribute("address");
    Attribute<String> CITY = TYPE.stringAttribute("city");
    Attribute<String> TELEPHONE = TYPE.stringAttribute("telephone");
  }

  interface Pet {
    EntityType TYPE = type("petclinic.pet");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<LocalDate> BIRTH_DATE = TYPE.localDateAttribute("birth_date");
    Attribute<Integer> PET_TYPE_ID = TYPE.integerAttribute("type_id");
    Attribute<Entity> PET_TYPE_FK = TYPE.entityAttribute("type_fk");
    Attribute<Integer> OWNER_ID = TYPE.integerAttribute("owner_id");
    Attribute<Entity> OWNER_FK = TYPE.entityAttribute("owner_fk");
  }

  interface Visit {
    EntityType TYPE = type("petclinic.visit");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<Integer> PET_ID = TYPE.integerAttribute("pet_id");
    Attribute<Entity> PET_FK = TYPE.entityAttribute("pet_fk");
    Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
    Attribute<String> DESCRIPTION = TYPE.stringAttribute("description");
  }
}
