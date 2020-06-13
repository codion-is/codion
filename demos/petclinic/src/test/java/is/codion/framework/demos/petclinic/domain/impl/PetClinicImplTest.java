/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.impl;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.petclinic.domain.Owner;
import is.codion.framework.demos.petclinic.domain.Pet;
import is.codion.framework.demos.petclinic.domain.PetType;
import is.codion.framework.demos.petclinic.domain.Specialty;
import is.codion.framework.demos.petclinic.domain.Vet;
import is.codion.framework.demos.petclinic.domain.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.Visit;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class PetClinicImplTest extends EntityTestUnit {

  public PetClinicImplTest() {
    super(PetClinicImpl.class.getName());
  }

  @Test
  void vet() throws DatabaseException {
    test(Vet.TYPE);
  }

  @Test
  void specialty() throws DatabaseException {
    test(Specialty.TYPE);
  }

  @Test
  void vetSpecialty() throws DatabaseException {
    test(VetSpecialty.TYPE);
  }

  @Test
  void petType() throws DatabaseException {
    test(PetType.TYPE);
  }

  @Test
  void owner() throws DatabaseException {
    test(Owner.TYPE);
  }

  @Test
  void pet() throws DatabaseException {
    test(Pet.TYPE);
  }

  @Test
  void visit() throws DatabaseException {
    test(Visit.TYPE);
  }
}
