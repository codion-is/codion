/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.DateFormats;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.KeyGenerators.queried;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public class TestDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    this(DOMAIN);
  }

  public TestDomain(final DomainType domain) {
    super(domain);
    compositeMaster();
    compositeDetail();
    master();
    detail();
    department();
    employee();
    noPKEntity();
    transientTest();
  }

  public static final EntityType<Entity> T_COMPOSITE_MASTER = DOMAIN.entityType("domain.composite_master");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID = T_COMPOSITE_MASTER.integerAttribute("id");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID_2 = T_COMPOSITE_MASTER.integerAttribute("id2");
  public static final Attribute<Integer> COMPOSITE_MASTER_ID_3 = T_COMPOSITE_MASTER.integerAttribute("id3");

  final void compositeMaster() {
    define(T_COMPOSITE_MASTER,
            columnProperty(COMPOSITE_MASTER_ID).primaryKeyIndex(0).nullable(true),
            columnProperty(COMPOSITE_MASTER_ID_2).primaryKeyIndex(1),
            columnProperty(COMPOSITE_MASTER_ID_3).primaryKeyIndex(2));
  }

  public static final EntityType<Entity> T_COMPOSITE_DETAIL = DOMAIN.entityType("domain.composite_detail");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID = T_COMPOSITE_DETAIL.integerAttribute("master_id");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = T_COMPOSITE_DETAIL.integerAttribute("master_id2");
  public static final Attribute<Integer> COMPOSITE_DETAIL_MASTER_ID_3 = T_COMPOSITE_DETAIL.integerAttribute("master_id3");
  public static final Attribute<Entity> COMPOSITE_DETAIL_MASTER_FK = T_COMPOSITE_DETAIL.entityAttribute("master_fk");

  final void compositeDetail() {
    define(T_COMPOSITE_DETAIL,
            columnProperty(COMPOSITE_DETAIL_MASTER_ID).primaryKeyIndex(0),
            columnProperty(COMPOSITE_DETAIL_MASTER_ID_2).primaryKeyIndex(1),
            columnProperty(COMPOSITE_DETAIL_MASTER_ID_3).primaryKeyIndex(2),
            foreignKeyProperty(COMPOSITE_DETAIL_MASTER_FK, "master")
                    .reference(COMPOSITE_DETAIL_MASTER_ID, COMPOSITE_MASTER_ID)
                    .reference(COMPOSITE_DETAIL_MASTER_ID_2, COMPOSITE_MASTER_ID_2)
                    .referenceReadOnly(COMPOSITE_DETAIL_MASTER_ID_3, COMPOSITE_MASTER_ID_3));
  }

  public interface Master extends Entity {
    EntityType<Master> TYPE = DOMAIN.entityType("domain.master_entity", Master.class);
    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Integer> CODE = TYPE.integerAttribute("code");

    Long getId();
    String getName();
  }

  final void master() {
    define(Master.TYPE,
            primaryKeyProperty(Master.ID)
                    .beanProperty("id"),
            columnProperty(Master.NAME)
                    .beanProperty("name"),
            columnProperty(Master.CODE))
            .comparator(new MasterComparator())
            .stringFactory(stringFactory(Master.NAME));
  }

  private static final class MasterComparator implements Comparator<Entity>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public int compare(final Entity o1, final Entity o2) {
      return o1.get(Master.CODE).compareTo(o2.get(Master.CODE));
    }
  }

  public interface Detail extends Entity {
    EntityType<Detail> TYPE = DOMAIN.entityType("domain.detail_entity", Detail.class);
    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<Integer> INT = TYPE.integerAttribute("int");
    Attribute<Double> DOUBLE = TYPE.doubleAttribute("double");
    Attribute<String> STRING = TYPE.stringAttribute("string");
    Attribute<LocalDate> DATE = TYPE.localDateAttribute("date");
    Attribute<LocalDateTime> TIMESTAMP = TYPE.localDateTimeAttribute("timestamp");
    Attribute<Boolean> BOOLEAN = TYPE.booleanAttribute("boolean");
    Attribute<Boolean> BOOLEAN_NULLABLE = TYPE.booleanAttribute("boolean_nullable");
    Attribute<Long> MASTER_ID = TYPE.longAttribute("master_id");
    Attribute<Entity> MASTER_FK = TYPE.entityAttribute("master_fk");
    Attribute<String> MASTER_NAME = TYPE.stringAttribute("master_name");
    Attribute<Integer> MASTER_CODE = TYPE.integerAttribute("master_code");
    Attribute<Integer> MASTER_CODE_NON_DENORM = TYPE.integerAttribute("master_code_non_denorm");
    Attribute<Entity> MASTER_VIA_CODE_FK = TYPE.entityAttribute("master_via_code_fk");
    Attribute<Integer> INT_VALUE_LIST = TYPE.integerAttribute("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
    Attribute<Integer> MASTER_CODE_DENORM = TYPE.integerAttribute("master_code_denorm");
    Attribute<byte[]> BYTES = TYPE.byteArrayAttribute("bytes");

    Optional<Long> getId();
    void setId(Long value);
    Optional<Double> getDouble();
    void setDouble(Double value);
    Optional<Master> getMaster();
    void setMaster(Master master);

    default void setAll(final Long id, final Double value, final Master master) {
      setId(id);
      setDouble(value);
      setMaster(master);
    }
  }

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  final void detail() {
    define(Detail.TYPE,
            primaryKeyProperty(Detail.ID)
                    .beanProperty("id"),
            columnProperty(Detail.INT, Detail.INT.getName()),
            columnProperty(Detail.DOUBLE, Detail.DOUBLE.getName())
                    .columnHasDefaultValue(true)
                    .beanProperty("double"),
            columnProperty(Detail.STRING, "Detail string")
                    .selectable(false),
            columnProperty(Detail.DATE, Detail.DATE.getName())
                    .columnHasDefaultValue(true),
            columnProperty(Detail.TIMESTAMP, Detail.TIMESTAMP.getName()),
            columnProperty(Detail.BOOLEAN, Detail.BOOLEAN.getName())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            columnProperty(Detail.BOOLEAN_NULLABLE, Detail.BOOLEAN_NULLABLE.getName())
                    .columnHasDefaultValue(true)
                    .defaultValue(true),
            columnProperty(Detail.MASTER_ID),
            foreignKeyProperty(Detail.MASTER_FK, Detail.MASTER_FK.getName())
                    .reference(Detail.MASTER_ID, Master.ID)
                    .beanProperty("master"),
            columnProperty(Detail.MASTER_CODE_NON_DENORM),
            foreignKeyProperty(Detail.MASTER_VIA_CODE_FK, Detail.MASTER_FK.getName())
                    .reference(Detail.MASTER_CODE_NON_DENORM, Master.CODE)
                    .beanProperty("master"),
            denormalizedViewProperty(Detail.MASTER_NAME, Detail.MASTER_NAME.getName(), Detail.MASTER_FK, Master.NAME),
            denormalizedViewProperty(Detail.MASTER_CODE, Detail.MASTER_CODE.getName(), Detail.MASTER_FK, Master.CODE),
            valueListProperty(Detail.INT_VALUE_LIST, Detail.INT_VALUE_LIST.getName(), ITEMS),
            derivedProperty(Detail.INT_DERIVED, Detail.INT_DERIVED.getName(), linkedValues -> {
              final Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, Detail.INT),
            denormalizedProperty(Detail.MASTER_CODE_DENORM, Detail.MASTER_FK, Master.CODE),
            columnProperty(Detail.BYTES)
                    .updatable(false))
            .keyGenerator(queried("select id from dual"))
            .orderBy(orderBy().ascending(Detail.STRING))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .smallDataset(true)
            .stringFactory(stringFactory(Detail.STRING));
  }

  public interface Department extends Entity {
    EntityType<Department> TYPE = DOMAIN.entityType("domain.scott.dept", Department.class);
    Attribute<Integer> NO = TYPE.integerAttribute("deptno");
    Attribute<String> NAME = TYPE.stringAttribute("dname");
    Attribute<String> LOCATION = TYPE.stringAttribute("loc");
    Attribute<Boolean> ACTIVE = TYPE.booleanAttribute("active");
    Attribute<byte[]> DATA = TYPE.byteArrayAttribute("data");

    Integer deptNo();
    String name();
    String location();
    Boolean active();
    void active(Boolean active);
  }

  final void department() {
    define(Department.TYPE, "scott.dept",
            primaryKeyProperty(Department.NO, Department.NO.getName())
                    .updatable(true).nullable(false)
                    .beanProperty("deptNo"),
            columnProperty(Department.NAME, Department.NAME.getName())
                    .searchProperty(true)
                    .preferredColumnWidth(120).maximumLength(14).nullable(false)
                    .beanProperty("name"),
            columnProperty(Department.LOCATION, Department.LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13)
                    .beanProperty("location"),
            booleanProperty(Department.ACTIVE, null, Integer.class, 1, 0)
                    .readOnly(true)
                    .beanProperty("active"),
            blobProperty(Department.DATA)
                    .eagerlyLoaded(false))
            .smallDataset(true)
            .orderBy(orderBy().ascending(Department.NAME))
            .stringFactory(stringFactory(Department.NAME))
            .caption("Department");
  }

  public interface Employee extends Entity {
    EntityType<Employee> TYPE = DOMAIN.entityType("domain.scott.emp", Employee.class);
    Attribute<Integer> ID = TYPE.integerAttribute("emp_id");
    Attribute<String> NAME = TYPE.stringAttribute("emp_name");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDateTime> HIREDATE = TYPE.localDateTimeAttribute("hiredate");
    Attribute<Double> SALARY = TYPE.doubleAttribute("sal");
    Attribute<Double> COMMISSION = TYPE.doubleAttribute("comm");
    Attribute<Integer> DEPARTMENT = TYPE.integerAttribute("deptno");
    Attribute<Entity> DEPARTMENT_FK = TYPE.entityAttribute("dept_fk");
    Attribute<Entity> MANAGER_FK = TYPE.entityAttribute("mgr_fk");
    Attribute<String> DEPARTMENT_LOCATION = TYPE.stringAttribute("location");
    Attribute<String> DEPARTMENT_NAME = TYPE.stringAttribute("department_name");
    Attribute<byte[]> DATA = TYPE.byteArrayAttribute("data");

    Integer getId();
    Double getCommission();
    Integer getDeptno();
    Department getDepartment();
    LocalDateTime getHiredate();
    String getJob();
    Integer getMgr();
    Employee getManager();
    String getName();
    Double getSalary();
  }

  final void employee() {
    define(Employee.TYPE, "scott.emp",
            primaryKeyProperty(Employee.ID, Employee.ID.getName())
                    .columnName("empno")
                    .beanProperty("id"),
            columnProperty(Employee.NAME, Employee.NAME.getName())
                    .searchProperty(true)
                    .columnName("ename").maximumLength(10).nullable(false)
                    .beanProperty("name"),
            columnProperty(Employee.DEPARTMENT)
                    .nullable(false)
                    .beanProperty("deptno"),
            foreignKeyProperty(Employee.DEPARTMENT_FK, Employee.DEPARTMENT_FK.getName())
                    .reference(Employee.DEPARTMENT, Department.NO)
                    .beanProperty("department"),
            valueListProperty(Employee.JOB, Employee.JOB.getName(),
                    asList(item("ANALYST"), item("CLERK"),
                            item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .searchProperty(true)
                    .beanProperty("job"),
            columnProperty(Employee.SALARY, Employee.SALARY.getName())
                    .nullable(false).minimumValue(1000).maximumValue(10000).maximumFractionDigits(2)
                    .beanProperty("salary"),
            columnProperty(Employee.COMMISSION, Employee.COMMISSION.getName())
                    .minimumValue(100).maximumValue(2000).maximumFractionDigits(2)
                    .beanProperty("commission"),
            columnProperty(Employee.MGR)
                    .beanProperty("mgr"),
            foreignKeyProperty(Employee.MANAGER_FK, Employee.MANAGER_FK.getName())
                    .reference(Employee.MGR, Employee.ID)
                    .beanProperty("manager"),
            columnProperty(Employee.HIREDATE, Employee.HIREDATE.getName())
                    .updatable(false)
                    .dateTimeFormatPattern(DateFormats.SHORT_DOT)
                    .nullable(false)
                    .beanProperty("hiredate"),
            denormalizedViewProperty(Employee.DEPARTMENT_LOCATION, Department.LOCATION.getName(), Employee.DEPARTMENT_FK, Department.LOCATION)
                    .preferredColumnWidth(100),
            derivedProperty(Employee.DEPARTMENT_NAME, null, new DepartmentNameProvider(), Employee.NAME, Employee.DEPARTMENT_FK),
            blobProperty(Employee.DATA, "Data")
                    .eagerlyLoaded(true))
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(orderBy().ascending(Employee.DEPARTMENT, Employee.NAME))
            .stringFactory(stringFactory(Employee.NAME))
            .caption("Employee");
  }

  private static final class DepartmentNameProvider implements DerivedProperty.Provider<String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String get(final DerivedProperty.SourceValues sourceValues) {
      final String name = sourceValues.get(Employee.NAME);
      final Entity department = sourceValues.get(Employee.DEPARTMENT_FK);
      if (name == null || department == null) {
        return null;
      }
      return name + " - " + department.get(Department.NAME);
    }
  }

  public static final EntityType<Entity> T_NO_PK = DOMAIN.entityType("no_pk");
  public static final Attribute<Integer> NO_PK_COL1 = T_NO_PK.integerAttribute("col1");
  public static final Attribute<Integer> NO_PK_COL2 = T_NO_PK.integerAttribute("col2");
  public static final Attribute<Integer> NO_PK_COL3 = T_NO_PK.integerAttribute("col3");

  final void noPKEntity() {
    define(T_NO_PK,
            columnProperty(NO_PK_COL1),
            columnProperty(NO_PK_COL2),
            columnProperty(NO_PK_COL3));
  }

  public static final EntityType<Entity> T_TRANS = DOMAIN.entityType("trans");

  public static final Attribute<Integer> TRANS_ID = T_TRANS.integerAttribute("id");
  public static final Attribute<Integer> TRANS_TRANS = T_TRANS.integerAttribute("trans");
  public static final TransientProperty.Builder<Integer> TRANS_BUILDER = Properties.transientProperty(TRANS_TRANS);

  final void transientTest() {
    define(T_TRANS,
            Properties.primaryKeyProperty(TRANS_ID),
            TRANS_BUILDER);
  }
}
