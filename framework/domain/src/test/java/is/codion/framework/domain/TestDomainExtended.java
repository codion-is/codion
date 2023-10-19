/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.AbstractReport;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.sql.Connection;

public final class TestDomainExtended extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomainExtended.class);

  public static final EntityType T_EXTENDED = DOMAIN.entityType("extended.entity");
  public static final Column<Integer> EXTENDED_ID = T_EXTENDED.integerColumn("id");
  public static final Column<String> EXTENDED_NAME = T_EXTENDED.stringColumn("name");
  public static final Column<Integer> EXTENDED_DEPT_ID = T_EXTENDED.integerColumn("dept_id");
  public static final ForeignKey EXTENDED_DEPT_FK = T_EXTENDED.foreignKey("dept_fk", EXTENDED_DEPT_ID, TestDomain.Department.ID);

  public static final ProcedureType<?, ?> PROC_TYPE = ProcedureType.procedureType("proc");
  public static final FunctionType<Object, Object, Object> FUNC_TYPE = FunctionType.functionType("func");
  public static final ReportType<Object, Object, Object> REP_TYPE = ReportType.reportType("rep");

  public TestDomainExtended() {
    this(DOMAIN);
  }

  public TestDomainExtended(DomainType domainType) {
    super(domainType);
    addEntities(new TestDomain());
    extended();
    procedure();
    report();
    function();
  }

  void extended() {
    add(T_EXTENDED.define(
            EXTENDED_ID.define().primaryKey(),
            EXTENDED_NAME.define().column(),
            EXTENDED_DEPT_ID.define().column(),
            EXTENDED_DEPT_FK.define().foreignKey()));
  }

  void procedure() {
    add(PROC_TYPE, (connection, arguments) -> {});
  }

  void function() {
    add(FUNC_TYPE, (connection, arguments) -> null);
  }

  void report() {
    add(REP_TYPE, new AbstractReport<Object, Object, Object>("", false) {
      @Override
      public Object fillReport(Connection connection, Object parameters) {
        return null;
      }

      @Override
      public Object loadReport() {
        return null;
      }
    });
  }

  public static final class TestDomainSecondExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainSecondExtension.class);

    public static final EntityType T_SECOND_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Column<Integer> EXTENDED_ID = T_SECOND_EXTENDED.integerColumn("id");
    public static final Column<String> EXTENDED_NAME = T_SECOND_EXTENDED.stringColumn("name");

    public TestDomainSecondExtension() {
      super(DOMAIN);
      addAll(new TestDomainExtended());
      extendedSecond();
    }

    void extendedSecond() {
      add(T_SECOND_EXTENDED.define(
              EXTENDED_ID.define().primaryKey(),
              EXTENDED_NAME.define().column()));
    }
  }

  public static final class TestDomainThirdExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainThirdExtension.class);

    public static final EntityType T_THIRD_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Column<Integer> EXTENDED_ID = T_THIRD_EXTENDED.integerColumn("id");
    public static final Column<String> EXTENDED_NAME = T_THIRD_EXTENDED.stringColumn("name");

    public TestDomainThirdExtension() {
      super(DOMAIN);
      addEntities(new TestDomainSecondExtension());
      extendedThird();
    }

    void extendedThird() {
      add(T_THIRD_EXTENDED.define(
              EXTENDED_ID.define().primaryKey(),
              EXTENDED_NAME.define().column()));
    }
  }
}
