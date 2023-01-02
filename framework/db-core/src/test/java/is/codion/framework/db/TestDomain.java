/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.item.Item;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.property.Property.*;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    superEntity();
    master();
    detail();
    department();
    employee();
  }

  public interface Super {
    EntityType TYPE = DOMAIN.entityType("db.super_entity");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
  }

  void superEntity() {
    add(definition(primaryKeyProperty(Super.ID)));
  }

  public interface Master {
    EntityType TYPE = DOMAIN.entityType("db.master_entity");

    Attribute<Integer> ID_1 = TYPE.integerAttribute("id");
    Attribute<Integer> ID_2 = TYPE.integerAttribute("id2");
    Attribute<Integer> SUPER_ID = TYPE.integerAttribute("super_id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Integer> CODE = TYPE.integerAttribute("code");

    ForeignKey SUPER_FK = TYPE.foreignKey("super_fk", SUPER_ID, Super.ID);
  }

  void master() {
    add(definition(
            columnProperty(Master.ID_1).primaryKeyIndex(0),
            columnProperty(Master.ID_2).primaryKeyIndex(1),
            columnProperty(Master.SUPER_ID),
            foreignKeyProperty(Master.SUPER_FK, "Super"),
            columnProperty(Master.NAME),
            columnProperty(Master.CODE))
            .comparator(Comparator.comparing(o -> o.get(Master.CODE)))
            .stringFactory(Master.NAME));
  }

  public interface Detail {
    EntityType TYPE = DOMAIN.entityType("db.detail_entity");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<Integer> INT = TYPE.integerAttribute("int");
    Attribute<Double> DOUBLE = TYPE.doubleAttribute("double");
    Attribute<String> STRING = TYPE.stringAttribute("string");
    Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
    Attribute<LocalDateTime> TIMESTAMP = TYPE.localDateTimeAttribute("timestamp");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Boolean> BOOLEAN_NULLABLE = TYPE.booleanAttribute("boolean_nullable");
    Attribute<Integer> MASTER_ID_1 = TYPE.integerAttribute("master_id");
    Attribute<Integer> MASTER_ID_2 = TYPE.integerAttribute("master_id_2");
    Attribute<String> MASTER_NAME = TYPE.stringAttribute("master_name");
    Attribute<Integer> MASTER_CODE = TYPE.integerAttribute("master_code");
    Attribute<Integer> INT_VALUE_LIST = TYPE.integerAttribute("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk",
            MASTER_ID_1, Master.ID_1,
            MASTER_ID_2, Master.ID_2);
    ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE, Master.CODE);
  }

  private static final EntityType DETAIL_SELECT_TABLE_NAME = DOMAIN.entityType("db.entity_test_select");

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(definition(
            primaryKeyProperty(Detail.ID),
            columnProperty(Detail.INT, Detail.INT.name()),
            columnProperty(Detail.DOUBLE, Detail.DOUBLE.name()),
            columnProperty(Detail.STRING, "Detail string"),
            columnProperty(Detail.DATE, Detail.DATE.name()),
            columnProperty(Detail.TIMESTAMP, Detail.TIMESTAMP.name()),
            columnProperty(Detail.BOOLEAN, Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(Detail.BOOLEAN_NULLABLE, Detail.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            columnProperty(Detail.MASTER_ID_1),
            columnProperty(Detail.MASTER_ID_2),
            foreignKeyProperty(Detail.MASTER_FK, Detail.MASTER_FK.name()),
            foreignKeyProperty(Detail.MASTER_VIA_CODE_FK, Detail.MASTER_FK.name()),
            denormalizedViewProperty(Detail.MASTER_NAME, Detail.MASTER_NAME.name(), Detail.MASTER_FK, Master.NAME),
            columnProperty(Detail.MASTER_CODE, Detail.MASTER_CODE.name()),
            itemProperty(Detail.INT_VALUE_LIST, Detail.INT_VALUE_LIST.name(), ITEMS),
            derivedProperty(Detail.INT_DERIVED, Detail.INT_DERIVED.name(), linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail.INT))
            .selectTableName(DETAIL_SELECT_TABLE_NAME.name())
            .orderBy(ascending(Detail.STRING))
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department {
    EntityType TYPE = DOMAIN.entityType("db.scott.dept");

    Attribute<Integer> ID = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");

    ConditionType CONDITION_ID = TYPE.conditionType("condition");
    ConditionType NAME_NOT_NULL_CONDITION_ID = TYPE.conditionType("departmentNameNotNull");
  }

  void department() {
    add(definition(
            primaryKeyProperty(Department.ID, Department.ID.name())
                    .updatable(true).nullable(false),
            columnProperty(Department.NAME, Department.NAME.name())
                    .searchProperty(true)
                    .preferredColumnWidth(120)
                    .maximumLength(14)
                    .nullable(false),
            columnProperty(Department.LOCATION, Department.LOCATION.name())
                    .preferredColumnWidth(150)
                    .maximumLength(13))
            .tableName("scott.dept")
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .conditionProvider(Department.CONDITION_ID, (attributes, values) -> {
              StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.NAME_NOT_NULL_CONDITION_ID, (attributes, values) -> "department name is not null")
            .caption("Department"));
  }

  public interface Employee {
    EntityType TYPE = DOMAIN.entityType("db.scott.emp");

    Attribute<Integer> ID = TYPE.integerAttribute("emp_id");
    Attribute<String> NAME = TYPE.stringAttribute("emp_name");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDateTime> HIREDATE = TYPE.localDateTimeAttribute("hiredate");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");

    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, ID);
  }

  void employee() {
    add(definition(
            primaryKeyProperty(Employee.ID, Employee.ID.name()).columnName("empno"),
            columnProperty(Employee.NAME, Employee.NAME.name())
                    .searchProperty(true)
                    .columnName("ename")
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
            .tableName("scott.emp")
            .orderBy(ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringFactory(Employee.NAME)
            .caption("Employee"));
  }
}
