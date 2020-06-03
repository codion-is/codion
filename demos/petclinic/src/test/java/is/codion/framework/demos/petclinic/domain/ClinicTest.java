/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.petclinic.domain.Clinic.Owner;
import is.codion.framework.demos.petclinic.domain.Clinic.Pet;
import is.codion.framework.demos.petclinic.domain.Clinic.PetType;
import is.codion.framework.demos.petclinic.domain.Clinic.Specialty;
import is.codion.framework.demos.petclinic.domain.Clinic.Vet;
import is.codion.framework.demos.petclinic.domain.Clinic.VetSpecialty;
import is.codion.framework.demos.petclinic.domain.Clinic.Visit;
import is.codion.framework.demos.petclinic.domain.impl.ClinicImpl;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class ClinicTest extends EntityTestUnit {

  public ClinicTest() {
    super(ClinicImpl.class.getName());
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
