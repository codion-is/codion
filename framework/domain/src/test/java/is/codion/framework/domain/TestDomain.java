/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.DerivedAttribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static is.codion.common.item.Item.item;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.KeyGenerator.queried;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;

public final class TestDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    this(DOMAIN);
  }

  public TestDomain(DomainType domain) {
    super(domain);
    compositeMaster();
    compositeDetail();
    master();
    detail();
    department();
    employee();
    keyTest();
    noPKEntity();
    transientModifies();
    transientModifiesNot();
    nullString();
    invalidDerived();
  }

  public interface CompositeMaster {
    EntityType TYPE = DOMAIN.entityType("domain.composite_master");

    Column<Integer> COMPOSITE_MASTER_ID_3 = TYPE.integerColumn("id3");
    Column<Integer> COMPOSITE_MASTER_ID_2 = TYPE.integerColumn("id2");
    Column<Integer> COMPOSITE_MASTER_ID = TYPE.integerColumn("id");
  }

  void compositeMaster() {
    add(CompositeMaster.TYPE.define(
            CompositeMaster.COMPOSITE_MASTER_ID.column().primaryKeyIndex(0).nullable(true),
            CompositeMaster.COMPOSITE_MASTER_ID_2.column().primaryKeyIndex(1),
            CompositeMaster.COMPOSITE_MASTER_ID_3.column().primaryKeyIndex(2)));
  }

  public interface CompositeDetail {
    EntityType TYPE = DOMAIN.entityType("domain.composite_detail");

    Column<Integer> COMPOSITE_DETAIL_MASTER_ID = TYPE.integerColumn("master_id");
    Column<Integer> COMPOSITE_DETAIL_MASTER_ID_2 = TYPE.integerColumn("master_id2");
    Column<Integer> COMPOSITE_DETAIL_MASTER_ID_3 = TYPE.integerColumn("master_id3");
    ForeignKey COMPOSITE_DETAIL_MASTER_FK = TYPE.foreignKey("master_fk",
            COMPOSITE_DETAIL_MASTER_ID, CompositeMaster.COMPOSITE_MASTER_ID,
            COMPOSITE_DETAIL_MASTER_ID_2, CompositeMaster.COMPOSITE_MASTER_ID_2,
            COMPOSITE_DETAIL_MASTER_ID_3, CompositeMaster.COMPOSITE_MASTER_ID_3);
  }

  void compositeDetail() {
    add(CompositeDetail.TYPE.define(
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID.column().primaryKeyIndex(0),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2.column().primaryKeyIndex(1),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3.column().primaryKeyIndex(2),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_FK.foreignKey()
                    .caption("master")
                    .readOnly(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3)));
  }

  public interface Master extends Entity {
    EntityType TYPE = DOMAIN.entityType("domain.master_entity", Master.class);
    Column<Long> ID = TYPE.longColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> CODE = TYPE.integerColumn("code");
    Column<Integer> READ_ONLY = TYPE.integerColumn("read_only");

    Long getId();

    String getName();
  }

  void master() {
    add(Master.TYPE.define(
            Master.ID.primaryKeyColumn()
                    .beanProperty("id"),
            Master.NAME.column()
                    .beanProperty("name"),
            Master.CODE.column(),
            Master.READ_ONLY.column()
                    .readOnly(true))
            .comparator(new MasterComparator())
            .stringFactory(Master.NAME));
  }

  private static final class MasterComparator implements Comparator<Entity>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public int compare(Entity o1, Entity o2) {
      return o1.get(Master.CODE).compareTo(o2.get(Master.CODE));
    }
  }

  public interface Detail extends Entity {
    EntityType TYPE = DOMAIN.entityType("domain.detail_entity", Detail.class);
    Column<Long> ID = TYPE.longColumn("id");
    Column<Short> SHORT = TYPE.shortColumn("short");
    Column<Integer> INT = TYPE.integerColumn("int");
    Column<Double> DOUBLE = TYPE.doubleColumn("double");
    Column<String> STRING = TYPE.stringColumn("string");
    Column<LocalDate> DATE = TYPE.localDateColumn("date");
    Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
    Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
    Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
    Column<Long> MASTER_ID = TYPE.longColumn("master_id");
    ForeignKey MASTER_FK = TYPE.foreignKey("master2_fk", MASTER_ID, Master.ID);
    Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
    Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
    Column<Integer> MASTER_CODE_NON_DENORM = TYPE.integerColumn("master_code_non_denorm");
    ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE_NON_DENORM, Master.CODE);
    Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");
    Column<byte[]> BYTES = TYPE.byteArrayColumn("bytes");

    Optional<Long> getId();

    void setId(Long value);

    Optional<Double> getDouble();

    void setDouble(Double value);

    Master master();

    Optional<Master> getMaster();

    void setMaster(Master master);

    default void setAll(Long id, Double value, Master master) {
      setId(id);
      setDouble(value);
      setMaster(master);
    }
  }

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail() {
    add(Detail.TYPE.define(
            Detail.ID.primaryKeyColumn()
                    .beanProperty("id"),
            Detail.SHORT.column()
                    .caption(Detail.SHORT.name()),
            Detail.INT.column()
                    .caption(Detail.INT.name()),
            Detail.DOUBLE.column()
                    .caption(Detail.DOUBLE.name())
                    .columnHasDefaultValue(true)
                    .beanProperty("double"),
            Detail.STRING.column()
                    .caption("Detail string")
                    .selectable(false),
            Detail.DATE.column()
                    .caption(Detail.DATE.name())
                    .columnHasDefaultValue(true),
            Detail.TIMESTAMP.column()
                    .caption(Detail.TIMESTAMP.name()),
            Detail.BOOLEAN.column()
                    .caption(Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            Detail.BOOLEAN_NULLABLE.column()
                    .caption(Detail.BOOLEAN_NULLABLE.name())
                    .columnHasDefaultValue(true)
                    .defaultValue(true),
            Detail.MASTER_ID.column(),
            Detail.MASTER_FK.foreignKey()
                    .caption(Detail.MASTER_FK.name())
                    .beanProperty("master"),
            Detail.MASTER_CODE_NON_DENORM.column(),
            Detail.MASTER_VIA_CODE_FK.foreignKey()
                    .caption(Detail.MASTER_FK.name())
                    .beanProperty("master"),
            Detail.MASTER_NAME.denormalizedAttribute(Detail.MASTER_FK, Master.NAME)
                    .caption(Detail.MASTER_NAME.name()),
            Detail.MASTER_CODE.denormalizedAttribute(Detail.MASTER_FK, Master.CODE)
                    .caption(Detail.MASTER_CODE.name()),
            Detail.INT_VALUE_LIST.itemColumn(ITEMS)
                    .caption(Detail.INT_VALUE_LIST.name()),
            Detail.INT_DERIVED.derivedAttribute(linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, Detail.INT)
                    .caption(Detail.INT_DERIVED.name()),
            Detail.BYTES.column()
                    .updatable(false))
            .keyGenerator(queried("select id from dual"))
            .orderBy(ascending(Detail.STRING))
            .selectTableName(DETAIL_SELECT_TABLE_NAME)
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("domain.scott.dept", Department.class);
    Column<Integer> NO = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
    Column<byte[]> DATA = TYPE.byteArrayColumn("data");

    int deptNo();

    String name();

    String location();

    Boolean active();

    void active(Boolean active);

    void setDeptNo(int deptNo);
  }

  void department() {
    add(Department.TYPE.define(
            Department.NO.primaryKeyColumn()
                    .caption(Department.NO.name())
                    .updatable(true).nullable(false)
                    .beanProperty("deptNo"),
            Department.NAME.column()
                    .caption(Department.NAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            Department.LOCATION.column()
                    .caption(Department.LOCATION.name())
                    .maximumLength(13)
                    .beanProperty("location"),
            Department.ACTIVE.booleanColumn(Integer.class, 1, 0)
                    .readOnly(true)
                    .beanProperty("active"),
            Department.DATA.blobColumn())
            .tableName("scott.dept")
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .caption("Department"));
  }

  public interface Employee extends Entity {
    EntityType TYPE = DOMAIN.entityType("domain.scott.emp", Employee.class);
    Column<Integer> ID = TYPE.integerColumn("emp_id");
    Column<String> NAME = TYPE.stringColumn("emp_name");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDateTime> HIREDATE = TYPE.localDateTimeColumn("hiredate");
    Column<Double> SALARY = TYPE.doubleColumn("sal");
    Column<Double> COMMISSION = TYPE.doubleColumn("comm");
    Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("deptno");
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT_NO, Department.NO);
    ForeignKey MANAGER_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
    Column<String> DEPARTMENT_LOCATION = TYPE.stringColumn("location");
    Attribute<String> DEPARTMENT_NAME = TYPE.stringAttribute("department_name");
    Column<byte[]> DATA = TYPE.byteArrayColumn("data");

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

  void employee() {
    add(Employee.TYPE.define(
            Employee.ID.primaryKeyColumn()
                    .caption(Employee.ID.name())
                    .columnName("empno")
                    .beanProperty("id"),
            Employee.NAME.column()
                    .caption(Employee.NAME.name())
                    .searchColumn(true)
                    .columnName("ename").maximumLength(10).nullable(false)
                    .beanProperty("name"),
            Employee.DEPARTMENT_NO.column()
                    .nullable(false)
                    .beanProperty("deptno"),
            Employee.DEPARTMENT_FK.foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name())
                    .beanProperty("department"),
            Employee.JOB.itemColumn(
                    asList(item("ANALYST"), item("CLERK"),
                            item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchColumn(true)
                    .beanProperty("job"),
            Employee.SALARY.column()
                    .caption(Employee.SALARY.name())
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2)
                    .beanProperty("salary"),
            Employee.COMMISSION.column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000).maximumFractionDigits(2)
                    .beanProperty("commission"),
            Employee.MGR.column()
                    .beanProperty("mgr"),
            Employee.MANAGER_FK.foreignKey()
                    .caption(Employee.MANAGER_FK.name())
                    .beanProperty("manager"),
            Employee.HIREDATE.column()
                    .caption(Employee.HIREDATE.name())
                    .updatable(false)
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDot()
                            .yearFourDigits()
                            .build())
                    .nullable(false)
                    .beanProperty("hiredate"),
            Employee.DEPARTMENT_LOCATION.denormalizedAttribute(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption(Department.LOCATION.name()),
            Employee.DEPARTMENT_NAME.derivedAttribute(new DepartmentNameProvider(), Employee.NAME, Employee.DEPARTMENT_FK),
            Employee.DATA.blobColumn()
                    .caption("Data")
                    .eagerlyLoaded(true))
            .tableName("scott.emp")
            .keyGenerator(increment("scott.emp", "empno"))
            .orderBy(ascending(Employee.DEPARTMENT_NO, Employee.NAME))
            .stringFactory(Employee.NAME)
            .caption("Employee"));
  }

  private static final class DepartmentNameProvider implements DerivedAttribute.Provider<String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String get(DerivedAttribute.SourceValues sourceValues) {
      String name = sourceValues.get(Employee.NAME);
      Entity department = sourceValues.get(Employee.DEPARTMENT_FK);
      if (name == null || department == null) {
        return null;
      }
      return name + " - " + department.get(Department.NAME);
    }
  }

  public interface KeyTest {
    EntityType TYPE = DOMAIN.entityType("KeyTest");

    Column<Integer> ID1 = TYPE.integerColumn("id1");
    Column<Integer> ID2 = TYPE.integerColumn("id2");
    Column<Integer> ID3 = TYPE.integerColumn("id3");
  }

  void keyTest() {
    add(KeyTest.TYPE.define(
            KeyTest.ID1.primaryKeyColumn()
                    .primaryKeyIndex(0),
            KeyTest.ID2.primaryKeyColumn()
                    .primaryKeyIndex(1),
            KeyTest.ID3.primaryKeyColumn()
                    .primaryKeyIndex(2)
                    .nullable(true)));
  }

  public interface NoPk {
    EntityType TYPE = DOMAIN.entityType("no_pk");
    Column<Integer> COL1 = TYPE.integerColumn("col1");
    Column<Integer> COL2 = TYPE.integerColumn("col2");
    Column<Integer> COL3 = TYPE.integerColumn("col3");
  }

  void noPKEntity() {
    add(NoPk.TYPE.define(
            NoPk.COL1.column(),
            NoPk.COL2.column(),
            NoPk.COL3.column()));
  }

  public interface TransModifies {
    EntityType TYPE = DOMAIN.entityType("trans_modifies");

    Column<Integer> ID = TYPE.integerColumn("id");
    Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
  }

  void transientModifies() {
    add(TransModifies.TYPE.define(
            TransModifies.ID.primaryKeyColumn(),
            TransModifies.TRANS.attribute()));
  }

  public interface TransModifiesNot {
    EntityType TYPE = DOMAIN.entityType("trans_modifies_not");

    Column<Integer> ID = TYPE.integerColumn("id");
    Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
  }

  void transientModifiesNot() {
    add(TransModifiesNot.TYPE.define(
            TransModifiesNot.ID.primaryKeyColumn(),
            TransModifiesNot.TRANS.attribute()
                    .modifiesEntity(false)));
  }

  public interface NullString {
    EntityType TYPE = DOMAIN.entityType("null_string");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<Integer> ATTR = TYPE.integerColumn("attr");
    Column<Integer> ATTR2 = TYPE.integerColumn("attr2");
  }

  void nullString() {
    add(NullString.TYPE.define(
            NullString.ID.primaryKeyColumn(),
            NullString.ATTR.column(),
            NullString.ATTR2.column())
            .stringFactory(entity -> null));
  }

  public interface InvalidDerived {
    EntityType TYPE = DOMAIN.entityType("invalid_derived");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<Integer> INT = TYPE.integerColumn("int");
    Attribute<Integer> INVALID_DERIVED = TYPE.integerAttribute("invalid_derived");
  }

  void invalidDerived() {
    add(InvalidDerived.TYPE.define(
            InvalidDerived.ID.primaryKeyColumn(),
            InvalidDerived.INT.column(),
            InvalidDerived.INVALID_DERIVED.derivedAttribute(linkedValues -> linkedValues.get(InvalidDerived.INT).intValue(), InvalidDerived.ID))
            .caption(InvalidDerived.INVALID_DERIVED.name())//incorrect source value, trigger exception
            .stringFactory(entity -> null));
  }
}
