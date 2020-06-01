/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.DateFormats;
import is.codion.common.item.Item;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.attribute.BlobAttribute;
import is.codion.framework.domain.attribute.EntityAttribute;
import is.codion.framework.domain.entity.Department;
import is.codion.framework.domain.entity.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.Entities.entityIdentity;
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

  public static final EntityIdentity T_COMPOSITE_MASTER = entityIdentity("domain.composite_master");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID = T_COMPOSITE_MASTER.integerAttribute("id");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID_2 = T_COMPOSITE_MASTER.integerAttribute("id2");

  void compositeMaster() {
    define(T_COMPOSITE_MASTER,
            columnProperty(COMPOSITE_MASTER_ID).primaryKeyIndex(0).nullable(true),
            columnProperty(COMPOSITE_MASTER_ID_2).primaryKeyIndex(1));
  }

  public static final EntityIdentity T_COMPOSITE_DETAIL = entityIdentity("domain.composite_detail");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID = T_COMPOSITE_DETAIL.integerAttribute("master_id");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = T_COMPOSITE_DETAIL.integerAttribute("master_id2");
  public static final EntityAttribute COMPOSITE_DETAIL_MASTER_FK = T_COMPOSITE_DETAIL.entityAttribute("master_fk");

  void compositeDetail() {
    define(T_COMPOSITE_DETAIL,
            foreignKeyProperty(COMPOSITE_DETAIL_MASTER_FK, "master", T_COMPOSITE_MASTER,
                    asList(columnProperty(COMPOSITE_DETAIL_MASTER_ID).primaryKeyIndex(0),
                            columnProperty(COMPOSITE_DETAIL_MASTER_ID_2).primaryKeyIndex(1))));
  }

  public static final EntityIdentity T_MASTER = entityIdentity("domain.master_entity");
  public static final Attribute<Long> MASTER_ID = T_MASTER.longAttribute("id");
  public static final Attribute<String> MASTER_NAME = T_MASTER.stringAttribute("name");
  public static final Attribute<Integer> MASTER_CODE = T_MASTER.integerAttribute("code");

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID),
            columnProperty(MASTER_NAME),
            columnProperty(MASTER_CODE))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final EntityIdentity T_DETAIL = entityIdentity("domain.detail_entity");
  public static final Attribute<Long> DETAIL_ID = T_DETAIL.longAttribute("id");
  public static final Attribute<Integer> DETAIL_INT = T_DETAIL.integerAttribute("int");
  public static final Attribute<Double> DETAIL_DOUBLE = T_DETAIL.doubleAttribute("double");
  public static final Attribute<String> DETAIL_STRING = T_DETAIL.stringAttribute("string");
  public static final Attribute<LocalDate> DETAIL_DATE = T_DETAIL.localDateAttribute("date");
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = T_DETAIL.localDateTimeAttribute("timestamp");
  public static final Attribute<Boolean> DETAIL_BOOLEAN = T_DETAIL.booleanAttribute("boolean");
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = T_DETAIL.booleanAttribute("boolean_nullable");
  public static final Attribute<Long> DETAIL_MASTER_ID = T_DETAIL.longAttribute("master_id");
  public static final EntityAttribute DETAIL_MASTER_FK = T_DETAIL.entityAttribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = T_DETAIL.stringAttribute("master_name");
  public static final Attribute<Integer> DETAIL_MASTER_CODE = T_DETAIL.integerAttribute("master_code");
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = T_DETAIL.integerAttribute("int_value_list");
  public static final Attribute<Integer> DETAIL_INT_DERIVED = T_DETAIL.integerAttribute("int_derived");
  public static final Attribute<Integer> DETAIL_MASTER_CODE_DENORM = T_DETAIL.integerAttribute("master_code_denorm");

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    define(T_DETAIL,
            primaryKeyProperty(DETAIL_ID),
            columnProperty(DETAIL_INT, DETAIL_INT.getName()),
            columnProperty(DETAIL_DOUBLE, DETAIL_DOUBLE.getName())
                    .columnHasDefaultValue(true),
            columnProperty(DETAIL_STRING, "Detail string")
                    .selectable(false),
            columnProperty(DETAIL_DATE, DETAIL_DATE.getName())
                    .columnHasDefaultValue(true),
            columnProperty(DETAIL_TIMESTAMP, DETAIL_TIMESTAMP.getName()),
            columnProperty(DETAIL_BOOLEAN, DETAIL_BOOLEAN.getName())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(DETAIL_BOOLEAN_NULLABLE, DETAIL_BOOLEAN_NULLABLE.getName())
                    .columnHasDefaultValue(true)
                    .defaultValue(true),
            foreignKeyProperty(DETAIL_MASTER_FK, DETAIL_MASTER_FK.getName(), T_MASTER,
                    columnProperty(DETAIL_MASTER_ID)),
            denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_MASTER_FK, MASTER_NAME, DETAIL_MASTER_NAME.getName()),
            denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_MASTER_FK, MASTER_CODE, DETAIL_MASTER_CODE.getName()),
            valueListProperty(DETAIL_INT_VALUE_LIST, DETAIL_INT_VALUE_LIST.getName(), ITEMS),
            derivedProperty(DETAIL_INT_DERIVED, DETAIL_INT_DERIVED.getName(), linkedValues -> {
              final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, DETAIL_INT),
            denormalizedProperty(DETAIL_MASTER_CODE_DENORM, DETAIL_MASTER_FK, MASTER_CODE))
            .keyGenerator(queried("select id from dual"))
            .orderBy(orderBy().ascending(DETAIL_STRING))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .smallDataset(true)
            .stringProvider(new StringProvider(DETAIL_STRING));
  }

  public static final EntityIdentity T_DEPARTMENT = entityIdentity("domain.scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");
  public static final Attribute<Boolean> DEPARTMENT_ACTIVE = T_DEPARTMENT.booleanAttribute("active");
  public static final BlobAttribute DEPARTMENT_DATA = T_DEPARTMENT.blobAttribute("data");

  void department() {
    define(T_DEPARTMENT, "scott.dept",
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false)
                    .beanProperty("deptNo"),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true)
                    .preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
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

  public static final EntityIdentity T_EMP = entityIdentity("domain.scott.emp");
  public static final Attribute<Integer> EMP_ID = T_EMP.integerAttribute("emp_id");
  public static final Attribute<String> EMP_NAME = T_EMP.stringAttribute("emp_name");
  public static final Attribute<String> EMP_JOB = T_EMP.stringAttribute("job");
  public static final Attribute<Integer> EMP_MGR = T_EMP.integerAttribute("mgr");
  public static final Attribute<LocalDateTime> EMP_HIREDATE = T_EMP.localDateTimeAttribute("hiredate");
  public static final Attribute<Double> EMP_SALARY = T_EMP.doubleAttribute("sal");
  public static final Attribute<Double> EMP_COMMISSION = T_EMP.doubleAttribute("comm");
  public static final Attribute<Integer> EMP_DEPARTMENT = T_EMP.integerAttribute("deptno");
  public static final EntityAttribute EMP_DEPARTMENT_FK = T_EMP.entityAttribute("dept_fk");
  public static final EntityAttribute EMP_MGR_FK = T_EMP.entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = T_EMP.stringAttribute("location");
  public static final Attribute<String> EMP_NAME_DEPARTMENT = T_EMP.stringAttribute("name_department");
  public static final BlobAttribute EMP_DATA = T_EMP.blobAttribute("data");

  void employee() {
    define(T_EMP, "scott.emp",
            primaryKeyProperty(EMP_ID, EMP_ID.getName())
                    .columnName("empno")
                    .beanProperty("id"),
            columnProperty(EMP_NAME, EMP_NAME.getName())
                    .searchProperty(true)
                    .columnName("ename").maximumLength(10).nullable(false)
                    .beanProperty("name"),
            foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK.getName(), T_DEPARTMENT,
                    (ColumnProperty.Builder<?>) columnProperty(EMP_DEPARTMENT)
                            .beanProperty("deptno"))
                    .beanProperty("department")
                    .nullable(false),
            valueListProperty(EMP_JOB, EMP_JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"),
                            item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true)
                    .beanProperty("job"),
            columnProperty(EMP_SALARY, EMP_SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2)
                    .beanProperty("salary"),
            columnProperty(EMP_COMMISSION, EMP_COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2)
                    .beanProperty("commission"),
            foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK.getName(), T_EMP,
                    (ColumnProperty.Builder<?>) columnProperty(EMP_MGR)
                            .beanProperty("mgr"))
                    .beanProperty("manager"),
            columnProperty(EMP_HIREDATE, EMP_HIREDATE.getName())
                    .updatable(false)
                    .dateTimeFormatPattern(DateFormats.SHORT_DOT)
                    .nullable(false)
                    .beanProperty("hiredate"),
            denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK, DEPARTMENT_LOCATION,
                    DEPARTMENT_LOCATION.getName()).preferredColumnWidth(100),
            derivedProperty(EMP_NAME_DEPARTMENT, null, linkedValues -> {
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

  public static final EntityIdentity T_NO_PK = entityIdentity("no_pk");
  public static final Attribute<Integer> NO_PK_COL1 = T_NO_PK.integerAttribute("col1");
  public static final Attribute<Integer> NO_PK_COL2 = T_NO_PK.integerAttribute("col2");
  public static final Attribute<Integer> NO_PK_COL3 = T_NO_PK.integerAttribute("col3");

  void noPKEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1),
            columnProperty(NO_PK_COL2),
            columnProperty(NO_PK_COL3));
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
