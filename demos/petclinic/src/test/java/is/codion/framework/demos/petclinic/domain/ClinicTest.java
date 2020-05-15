/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.petclinic.domain.impl.ClinicImpl;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class ClinicTest extends EntityTestUnit {

  public ClinicTest() {
    super(ClinicImpl.class.getName());
  }

  @Test
  void vet() throws DatabaseException {
    test(Clinic.T_VET);
  }

  @Test
  void specialty() throws DatabaseException {
    test(Clinic.T_SPECIALTY);
  }

  @Test
  void vetSpecialty() throws DatabaseException {
    test(Clinic.T_VET_SPECIALTY);
  }

  @Test
  void petType() throws DatabaseException {
    test(Clinic.T_PET_TYPE);
  }

  @Test
  void owner() throws DatabaseException {
    test(Clinic.T_OWNER);
  }

  @Test
  void pet() throws DatabaseException {
    test(Clinic.T_PET);
  }

  @Test
  void visit() throws DatabaseException {
    test(Clinic.T_VISIT);
  }
}
