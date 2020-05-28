/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.DateFormats;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Department;
import is.codion.framework.domain.entity.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.KeyGenerators.queried;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class TestDomain extends Domain {

  public TestDomain() {
    compositeMaster();
    compositeDetail();
    master();
    detail();
    department();
    employee();
    noPKEntity();
  }

  public static final String T_COMPOSITE_MASTER = "domain.composite_master";
  public static final Attribute<Integer> COMPOSITE_MASTER_ID = attribute("id");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID_2 = attribute("id2");

  void compositeMaster() {
    define(T_COMPOSITE_MASTER,
            columnProperty(COMPOSITE_MASTER_ID, Types.INTEGER).primaryKeyIndex(0).nullable(true),
            columnProperty(COMPOSITE_MASTER_ID_2, Types.INTEGER).primaryKeyIndex(1));
  }

  public static final String T_COMPOSITE_DETAIL = "domain.composite_detail";
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID = attribute("master_id");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = attribute("master_id2");
  public static final Attribute<Entity> COMPOSITE_DETAIL_MASTER_FK = attribute("master_fk");

  void compositeDetail() {
    define(T_COMPOSITE_DETAIL,
            foreignKeyProperty(COMPOSITE_DETAIL_MASTER_FK, "master", T_COMPOSITE_MASTER,
                    asList(columnProperty(COMPOSITE_DETAIL_MASTER_ID, Types.INTEGER).primaryKeyIndex(0),
                            columnProperty(COMPOSITE_DETAIL_MASTER_ID_2, Types.INTEGER).primaryKeyIndex(1))));
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final Attribute<Long> MASTER_ID = attribute("id");
  public static final Attribute<String> MASTER_NAME = attribute("name");
  public static final Attribute<Integer> MASTER_CODE = attribute("code");

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID, Types.BIGINT),
            columnProperty(MASTER_NAME, Types.VARCHAR),
            columnProperty(MASTER_CODE, Types.INTEGER))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final String T_DETAIL = "domain.detail_entity";
  public static final Attribute<Long> DETAIL_ID = attribute("id");
  public static final Attribute<Integer> DETAIL_INT = attribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = attribute("double");
  public static final Attribute<String> DETAIL_STRING = attribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = attribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = attribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = attribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = attribute("boolean_nullable");
  public static final Attribute<Long> DETAIL_MASTER_ID = attribute("master_id");
  public static final Attribute<Entity> DETAIL_MASTER_FK = attribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = attribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = attribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = attribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = attribute("int_derived");
  public static final Attribute<Integer> DETAIL_MASTER_CODE_DENORM = attribute("master_code_denorm");

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    define(T_DETAIL,
            primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT.getId()),
            columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE.getId())
                    .columnHasDefaultValue(true),
            columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string")
                    .selectable(false),
            columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE.getId())
                    .columnHasDefaultValue(true),
            columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP.getId()),
            columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN.getId())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE.getId())
                    .columnHasDefaultValue(true)
                    .defaultValue(true),
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK.getId(), T_MASTER,
                    columnProperty(DETAIL_MASTER_ID, Types.BIGINT)),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_NAME), DETAIL_MASTER_NAME.getId()),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE), DETAIL_MASTER_CODE.getId()),
            valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST.getId(), ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED.getId(), linkedValues -> {
              final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, DETAIL_INT),
            denormalizedProperty(DETAIL_MASTER_CODE_DENORM, DETAIL_MASTER_FK,
                    getDefinition(T_MASTER).getProperty(MASTER_CODE)))
            .keyGenerator(queried("select id from dual"))
            .orderBy(orderBy().ascending(DETAIL_STRING))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .smallDataset(true)
            .stringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final Attribute<Integer> DEPARTMENT_ID = attribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = attribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = attribute("loc");
  public static final Attribute<Boolean> DEPARTMENT_ACTIVE = attribute("active");
  public static final Attribute<byte[]> DEPARTMENT_DATA = attribute("data");

  public static final String T_DEPARTMENT = "domain.scott.dept";

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID.getId())
                    .updatable(true).nullable(false)
                    .beanProperty("deptNo"),
            columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME.getId())
                    .searchProperty(true)
                    .preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION.getId())
                    .preferredColumnWidth(150).maximumLength(13)
                    .beanProperty("location"),
            booleanProperty(DEPARTMENT_ACTIVE, Types.INTEGER, null, 1, 0)
                    .readOnly(true)
                    .beanProperty("active"),
            blobProperty(DEPARTMENT_DATA)
                    .eagerlyLoaded(false))
            .smallDataset(true)
            .orderBy(orderBy().ascending(DEPARTMENT_NAME))
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .beanClass(Department.class)
            .caption("Department");
  }

  public static final String T_EMP = "domain.scott.emp";
  public static final Attribute<Integer> EMP_ID = attribute("emp_id");
  public static final Attribute<String> EMP_NAME = attribute("emp_name");
  public static final Attribute<String> EMP_JOB = attribute("job");
  public static final Attribute<Integer> EMP_MGR = attribute("mgr");
  public static final Attribute<LocalDateTime> EMP_HIREDATE = attribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = attribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = attribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = attribute("deptno");
  public static final Attribute<Entity> EMP_DEPARTMENT_FK = attribute("dept_fk");
  public static final Attribute<Entity> EMP_MGR_FK = attribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = attribute("location");
  public static final Attribute<String> EMP_NAME_DEPARTMENT = attribute("name_department");
  public static final Attribute<byte[]> EMP_DATA = attribute("data");

  void employee() {
    define(T_EMP, "scott.emp",
            primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID.getId())
                    .columnName("empno")
                    .beanProperty("id"),
            columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME.getId())
                    .searchProperty(true)
                    .columnName("ename").maximumLength(10).nullable(false)
                    .beanProperty("name"),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getId(), T_DEPARTMENT,
                    (ColumnProperty.Builder) columnProperty(EMP_DEPARTMENT, Types.INTEGER)
                            .beanProperty("deptno"))
                    .beanProperty("department")
                    .nullable(false),
            valueListProperty(EMP_JOB, Types.VARCHAR, EMP_JOB.getId(),
                    asList(item("ANALYST"), item("CLERK"),
                            item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true)
                    .beanProperty("job"),
            columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY.getId())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2)
                    .beanProperty("salary"),
            columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION.getId())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2)
                    .beanProperty("commission"),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getId(), T_EMP,
                    (ColumnProperty.Builder) columnProperty(EMP_MGR, Types.INTEGER)
                            .beanProperty("mgr"))
                    .beanProperty("manager"),
            columnProperty(EMP_HIREDATE, Types.TIMESTAMP, EMP_HIREDATE.getId())
                    .updatable(false)
                    .dateTimeFormatPattern(DateFormats.SHORT_DOT)
                    .nullable(false)
                    .beanProperty("hiredate"),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    getDefinition(T_DEPARTMENT).getProperty(DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION.getId()).preferredColumnWidth(100),
            derivedProperty(EMP_NAME_DEPARTMENT, Types.VARCHAR, null, linkedValues -> {
              final String name = (String) linkedValues.get(EMP_NAME);
              final Entity department = (Entity) linkedValues.get(EMP_DEPARTMENT_FK);
              if (name == null || department == null) {
                return null;
              }
              return name + " - " + department.get(DEPARTMENT_NAME);
            }, EMP_NAME, EMP_DEPARTMENT_FK),
            blobProperty(EMP_DATA, "Data")
                    .eagerlyLoaded(true))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(EMP_DEPARTMENT, EMP_NAME))
            .stringProvider(new StringProvider(EMP_NAME))
            .beanClass(Employee.class)
            .beanHelper(new EntityToEmployee())
            .caption("Employee");
  }

  public static final String T_NO_PK = "no_pk";
  public static final Attribute<Integer> NO_PK_COL1 = attribute("col1");
  public static final Attribute<Integer> NO_PK_COL2 = attribute("col2");
  public static final Attribute<Integer> NO_PK_COL3 = attribute("col3");

  void noPKEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1, Types.INTEGER),
            columnProperty(NO_PK_COL2, Types.INTEGER),
            columnProperty(NO_PK_COL3, Types.INTEGER));
  }

  private static final class EntityToEmployee implements EntityDefinition.BeanHelper<Employee> {

    private static final long serialVersionUID = 1;

    @Override
    public Employee toBean(final Entity entity, final Employee bean) {
      if (entity.isNotNull(EMP_HIREDATE)) {
        bean.setHiredate(entity.get(EMP_HIREDATE).truncatedTo(ChronoUnit.DAYS));
      }

      return bean;
    }
  }
}
