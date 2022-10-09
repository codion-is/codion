/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.property.Property.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
    employee();
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.ID, Department.ID.name())
                    .updatable(true)
                    .nullable(false),
            columnProperty(Department.NAME, Department.NAME.name())
                    .searchProperty(true)
                    .preferredColumnWidth(120)
                    .maximumLength(14)
                    .nullable(false),
            columnProperty(Department.LOCATION, Department.LOCATION.name())
                    .preferredColumnWidth(150)
                    .maximumLength(13))
            .smallDataset(true)
            .stringFactory(Department.NAME)
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");

    Attribute<Integer> ID = TYPE.integerAttribute("empno");
    Attribute<String> NAME = TYPE.stringAttribute("ename");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);

    ConditionType MGR_CONDITION_TYPE = TYPE.conditionType("mgrConditionId");
  }

  void employee() {
    add(definition(
            primaryKeyProperty(Employee.ID, Employee.ID.name()),
            columnProperty(Employee.NAME, Employee.NAME.name())
                    .searchProperty(true)
                    .maximumLength(10)
                    .nullable(false),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.name()),
            itemProperty(Employee.JOB, Employee.JOB.name(),
                    asList(item("ANALYST"), item("CLERK"), item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true),
            columnProperty(Employee.SALARY, Employee.SALARY.name())
                    .nullable(false)
                    .valueRange(1000, 10000)
                    .maximumFractionDigits(2),
            columnProperty(Employee.COMMISSION, Employee.COMMISSION.name())
                    .valueRange(100, 2000)
                    .maximumFractionDigits(2),
            columnProperty(Employee.MGR),
            foreignKeyProperty(Employee.MGR_FK, Employee.MGR_FK.name()),
            columnProperty(Employee.HIREDATE, Employee.HIREDATE.name())
                    .nullable(false),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Department.LOCATION.name(), Employee.DEPARTMENT_FK, Department.LOCATION)
                    .preferredColumnWidth(100))
            .stringFactory(Employee.NAME)
            .keyGenerator(increment("scott.emp", "empno"))
            .conditionProvider(Employee.MGR_CONDITION_TYPE, (attributes, values) -> "mgr > ?")
            .caption("Employee"));
  }
}
