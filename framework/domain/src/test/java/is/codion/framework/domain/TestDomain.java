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
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.EntityAttribute;

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
  public static final Attribute<Integer> COMPOSITE_MASTER_ID = Attributes.attribute("id", Integer.class);
  public static final Attribute<Integer> COMPOSITE_MASTER_ID_2 = Attributes.attribute("id2", Integer.class);

  void compositeMaster() {
    define(T_COMPOSITE_MASTER,
            columnProperty(COMPOSITE_MASTER_ID).primaryKeyIndex(0).nullable(true),
            columnProperty(COMPOSITE_MASTER_ID_2).primaryKeyIndex(1));
  }

  public static final String T_COMPOSITE_DETAIL = "domain.composite_detail";
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID = Attributes.attribute("master_id", Integer.class);
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = Attributes.attribute("master_id2", Integer.class);
  public static final EntityAttribute COMPOSITE_DETAIL_MASTER_FK = Attributes.entityAttribute("master_fk");

  void compositeDetail() {
    define(T_COMPOSITE_DETAIL,
            foreignKeyProperty(COMPOSITE_DETAIL_MASTER_FK, "master", T_COMPOSITE_MASTER,
                    asList(columnProperty(COMPOSITE_DETAIL_MASTER_ID).primaryKeyIndex(0),
                            columnProperty(COMPOSITE_DETAIL_MASTER_ID_2).primaryKeyIndex(1))));
  }

  public static final String T_MASTER = "domain.master_entity";
  public static final Attribute<Long> MASTER_ID = Attributes.attribute("id", Long.class);
  public static final Attribute<String> MASTER_NAME = Attributes.attribute("name", String.class);
  public static final Attribute<Integer> MASTER_CODE = Attributes.attribute("code", Integer.class);

  void master() {
    define(T_MASTER,
            primaryKeyProperty(MASTER_ID),
            columnProperty(MASTER_NAME),
            columnProperty(MASTER_CODE))
            .comparator(Comparator.comparing(o -> o.get(MASTER_CODE)))
            .stringProvider(new StringProvider(MASTER_NAME));
  }

  public static final String T_DETAIL = "domain.detail_entity";
  public static final Attribute<Long> DETAIL_ID = Attributes.attribute("id", Long.class);
  public static final Attribute<Integer> DETAIL_INT = Attributes.attribute("int", Integer.class);
  public static final Attribute<Double> DETAIL_DOUBLE = Attributes.attribute("double", Double.class);
  public static final Attribute<String> DETAIL_STRING = Attributes.attribute("string", String.class);
  public static final Attribute<LocalDate> DETAIL_DATE = Attributes.attribute("date", LocalDate.class);
  public static final Attribute<LocalDateTime> DETAIL_TIMESTAMP = Attributes.attribute("timestamp", LocalDateTime.class);
  public static final Attribute<Boolean> DETAIL_BOOLEAN = Attributes.attribute("boolean", Boolean.class);
  public static final Attribute<Boolean> DETAIL_BOOLEAN_NULLABLE = Attributes.attribute("boolean_nullable", Boolean.class);
  public static final Attribute<Long> DETAIL_MASTER_ID = Attributes.attribute("master_id", Long.class);
  public static final EntityAttribute DETAIL_MASTER_FK = Attributes.entityAttribute("master_fk");
  public static final Attribute<String> DETAIL_MASTER_NAME = Attributes.attribute("master_name", String.class);
  public static final Attribute<Integer> DETAIL_MASTER_CODE = Attributes.attribute("master_code", Integer.class);
  public static final Attribute<Integer> DETAIL_INT_VALUE_LIST = Attributes.attribute("int_value_list", Integer.class);
  public static final Attribute<Integer> DETAIL_INT_DERIVED = Attributes.attribute("int_derived", Integer.class);
  public static final Attribute<Integer> DETAIL_MASTER_CODE_DENORM = Attributes.attribute("master_code_denorm", Integer.class);

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

  public static final Attribute<Integer> DEPARTMENT_ID = Attributes.attribute("deptno", Integer.class);
  public static final Attribute<String> DEPARTMENT_NAME = Attributes.attribute("dname", String.class);
  public static final Attribute<String> DEPARTMENT_LOCATION = Attributes.attribute("loc", String.class);
  public static final Attribute<Boolean> DEPARTMENT_ACTIVE = Attributes.attribute("active", Boolean.class);
  public static final BlobAttribute DEPARTMENT_DATA = Attributes.blobAttribute("data");

  public static final String T_DEPARTMENT = "domain.scott.dept";

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

  public static final String T_EMP = "domain.scott.emp";
  public static final Attribute<Integer> EMP_ID = Attributes.attribute("emp_id", Integer.class);
  public static final Attribute<String> EMP_NAME = Attributes.attribute("emp_name", String.class);
  public static final Attribute<String> EMP_JOB = Attributes.attribute("job", String.class);
  public static final Attribute<Integer> EMP_MGR = Attributes.attribute("mgr", Integer.class);
  public static final Attribute<LocalDateTime> EMP_HIREDATE = Attributes.attribute("hiredate", LocalDateTime.class);
  public static final Attribute<Double> EMP_SALARY = Attributes.attribute("sal", Double.class);
  public static final Attribute<Double> EMP_COMMISSION = Attributes.attribute("comm", Double.class);
  public static final Attribute<Integer> EMP_DEPARTMENT = Attributes.attribute("deptno", Integer.class);
  public static final EntityAttribute EMP_DEPARTMENT_FK = Attributes.entityAttribute("dept_fk");
  public static final EntityAttribute EMP_MGR_FK = Attributes.entityAttribute("mgr_fk");
  public static final Attribute<String> EMP_DEPARTMENT_LOCATION = Attributes.attribute("location", String.class);
  public static final Attribute<String> EMP_NAME_DEPARTMENT = Attributes.attribute("name_department", String.class);
  public static final BlobAttribute EMP_DATA = Attributes.blobAttribute("data");

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
                    (ColumnProperty.Builder) columnProperty(EMP_DEPARTMENT)
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
                    (ColumnProperty.Builder) columnProperty(EMP_MGR)
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

  public static final String T_NO_PK = "no_pk";
  public static final Attribute<Integer> NO_PK_COL1 = Attributes.attribute("col1", Integer.class);
  public static final Attribute<Integer> NO_PK_COL2 = Attributes.attribute("col2", Integer.class);
  public static final Attribute<Integer> NO_PK_COL3 = Attributes.attribute("col3", Integer.class);

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
