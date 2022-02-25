/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.sql.Connection;

import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.foreignKeyProperty;

public final class TestDomainExtended extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomainExtended.class);

  public static final EntityType T_EXTENDED = DOMAIN.entityType("extended.entity");
  public static final Attribute<Integer> EXTENDED_ID = T_EXTENDED.integerAttribute("id");
  public static final Attribute<String> EXTENDED_NAME = T_EXTENDED.stringAttribute("name");
  public static final Attribute<Integer> EXTENDED_DEPT_ID = T_EXTENDED.integerAttribute("dept_id");
  public static final ForeignKey EXTENDED_DEPT_FK = T_EXTENDED.foreignKey("dept_fk", EXTENDED_DEPT_ID, TestDomain.Department.NO);

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
    define(T_EXTENDED,
            columnProperty(EXTENDED_ID).primaryKeyIndex(0),
            columnProperty(EXTENDED_NAME),
            columnProperty(EXTENDED_DEPT_ID),
            foreignKeyProperty(EXTENDED_DEPT_FK));
  }

  void procedure() {
    defineProcedure(PROC_TYPE, (connection, arguments) -> {});
  }

  void function() {
    defineFunction(FUNC_TYPE, (connection, arguments) -> null);
  }

  void report() {
    defineReport(REP_TYPE, new Report<Object, Object, Object>() {
      @Override
      public Object fillReport(Connection connection, Object parameters) throws ReportException {
        return null;
      }

      @Override
      public Object loadReport() throws ReportException {
        return null;
      }
    });
  }

  public static final class TestDomainSecondExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainSecondExtension.class);

    public static final EntityType T_SECOND_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Attribute<Integer> EXTENDED_ID = T_SECOND_EXTENDED.integerAttribute("id");
    public static final Attribute<String> EXTENDED_NAME = T_SECOND_EXTENDED.stringAttribute("name");

    public TestDomainSecondExtension() {
      super(DOMAIN);
      copyFrom(new TestDomainExtended());
      extendedSecond();
    }

    void extendedSecond() {
      define(T_SECOND_EXTENDED,
              columnProperty(EXTENDED_ID).primaryKeyIndex(0),
              columnProperty(EXTENDED_NAME));
    }
  }

  public static final class TestDomainThirdExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainThirdExtension.class);

    public static final EntityType T_THIRD_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Attribute<Integer> EXTENDED_ID = T_THIRD_EXTENDED.integerAttribute("id");
    public static final Attribute<String> EXTENDED_NAME = T_THIRD_EXTENDED.stringAttribute("name");

    public TestDomainThirdExtension() {
      super(DOMAIN);
      addEntities(new TestDomainSecondExtension());
      extendedThird();
    }

    void extendedThird() {
      define(T_THIRD_EXTENDED,
              columnProperty(EXTENDED_ID).primaryKeyIndex(0),
              columnProperty(EXTENDED_NAME));
    }
  }
}
