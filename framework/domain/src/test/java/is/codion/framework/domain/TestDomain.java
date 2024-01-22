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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.DerivedAttribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static is.codion.common.item.Item.item;
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
    superEntity();
    master2();
    detail2();
    department();
    employee();
    keyTest();
    noPKEntity();
    transientModifies();
    transientModifiesNot();
    nullString();
    invalidDerived();
    foreignKeyLazyColumn();
  }

  public interface CompositeMaster {
    EntityType TYPE = DOMAIN.entityType("domain.composite_master");

    Column<Integer> COMPOSITE_MASTER_ID_3 = TYPE.integerColumn("id3");
    Column<Integer> COMPOSITE_MASTER_ID_2 = TYPE.integerColumn("id2");
    Column<Integer> COMPOSITE_MASTER_ID = TYPE.integerColumn("id");
  }

  void compositeMaster() {
    add(CompositeMaster.TYPE.define(
            CompositeMaster.COMPOSITE_MASTER_ID.define()
                    .primaryKey(0).nullable(true),
            CompositeMaster.COMPOSITE_MASTER_ID_2.define()
                    .primaryKey(1),
            CompositeMaster.COMPOSITE_MASTER_ID_3.define()
                    .primaryKey(2)));
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
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID.define()
                    .primaryKey(0),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_2.define()
                    .primaryKey(1),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3.define()
                    .primaryKey(2),
            CompositeDetail.COMPOSITE_DETAIL_MASTER_FK.define()
                    .foreignKey()
                    .caption("master")
                    .readOnly(CompositeDetail.COMPOSITE_DETAIL_MASTER_ID_3)));
  }

  public interface Super {
    EntityType TYPE = DOMAIN.entityType("db.super_entity");

    Column<Integer> ID = TYPE.integerColumn("id");
  }

  void superEntity() {
    add(Super.TYPE.define(Super.ID.define().primaryKey()));
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
            Master.ID.define()
                    .primaryKey()
                    .beanProperty("id"),
            Master.NAME.define()
                    .column()
                    .beanProperty("name"),
            Master.CODE.define()
                    .column(),
            Master.READ_ONLY.define()
                    .column()
                    .readOnly(true))
            .comparator(new MasterComparator())
            .stringFactory(Master.NAME));
  }

  public interface Master2 {
    EntityType TYPE = DOMAIN.entityType("db.master_entity");

    Column<Integer> ID_1 = TYPE.integerColumn("id");
    Column<Integer> ID_2 = TYPE.integerColumn("id2");
    Column<Integer> SUPER_ID = TYPE.integerColumn("super_id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> CODE = TYPE.integerColumn("code");

    ForeignKey SUPER_FK = TYPE.foreignKey("super_fk", SUPER_ID, Super.ID);
  }

  void master2() {
    add(Master2.TYPE.define(
            Master2.ID_1.define().primaryKey(0),
            Master2.ID_2.define().primaryKey(1),
            Master2.SUPER_ID.define().column(),
            Master2.SUPER_FK.define()
                    .foreignKey().caption("Super"),
            Master2.NAME.define().column(),
            Master2.CODE.define().column())
            .comparator(Comparator.comparing(o -> o.get(Master2.CODE)))
            .stringFactory(Master2.NAME));
  }

  public interface Detail2 {
    EntityType TYPE = DOMAIN.entityType("db.detail_entity2");

    Column<Long> ID = TYPE.longColumn("id");
    Column<Integer> INT = TYPE.integerColumn("int");
    Column<Double> DOUBLE = TYPE.doubleColumn("double");
    Column<String> STRING = TYPE.stringColumn("string");
    Column<LocalDate> DATE = TYPE.localDateColumn("date");
    Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
    Column<Boolean> BOOLEAN = TYPE.booleanColumn("boolean");
    Column<Boolean> BOOLEAN_NULLABLE = TYPE.booleanColumn("boolean_nullable");
    Column<Integer> MASTER_ID_1 = TYPE.integerColumn("master_id");
    Column<Integer> MASTER_ID_2 = TYPE.integerColumn("master_id_2");
    Column<String> MASTER_NAME = TYPE.stringColumn("master_name");
    Column<Integer> MASTER_CODE = TYPE.integerColumn("master_code");
    Column<Integer> INT_VALUE_LIST = TYPE.integerColumn("int_value_list");
    Attribute<Integer> INT_DERIVED = TYPE.integerAttribute("int_derived");

    ForeignKey MASTER_FK = TYPE.foreignKey("master_fk",
            MASTER_ID_1, Master2.ID_1,
            MASTER_ID_2, Master2.ID_2);
    ForeignKey MASTER_VIA_CODE_FK = TYPE.foreignKey("master_via_code_fk", MASTER_CODE, Master2.CODE);
  }

  private static final EntityType DETAIL_SELECT_TABLE_NAME = DOMAIN.entityType("db.entity_test_select");

  private static final List<Item<Integer>> ITEMS = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));

  void detail2() {
    add(Detail2.TYPE.define(
            Detail2.ID.define()
                    .primaryKey(),
            Detail2.INT.define()
                    .column()
                    .caption(Detail2.INT.name()),
            Detail2.DOUBLE.define()
                    .column()
                    .caption(Detail2.DOUBLE.name()),
            Detail2.STRING.define()
                    .column()
                    .caption("Detail2 string"),
            Detail2.DATE.define()
                    .column()
                    .caption(Detail2.DATE.name()),
            Detail2.TIMESTAMP.define()
                    .column()
                    .caption(Detail2.TIMESTAMP.name()),
            Detail2.BOOLEAN.define()
                    .column()
                    .caption(Detail2.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean column"),
            Detail2.BOOLEAN_NULLABLE.define()
                    .column()
                    .caption(Detail2.BOOLEAN_NULLABLE.name())
                    .defaultValue(true),
            Detail2.MASTER_ID_1.define()
                    .column(),
            Detail2.MASTER_ID_2.define()
                    .column(),
            Detail2.MASTER_FK.define()
                    .foreignKey()
                    .caption(Detail2.MASTER_FK.name()),
            Detail2.MASTER_VIA_CODE_FK.define()
                    .foreignKey()
                    .caption(Detail2.MASTER_FK.name()),
            Detail2.MASTER_NAME.define()
                    .denormalized(Detail2.MASTER_FK, Master.NAME)
                    .caption(Detail2.MASTER_NAME.name()),
            Detail2.MASTER_CODE.define()
                    .column()
                    .caption(Detail2.MASTER_CODE.name()),
            Detail2.INT_VALUE_LIST.define()
                    .column()
                    .items(ITEMS)
                    .caption(Detail2.INT_VALUE_LIST.name()),
            Detail2.INT_DERIVED.define()
                    .derived(linkedValues -> {
              Integer intValue = linkedValues.get(Detail2.INT);
              if (intValue == null) {
                return null;
              }

              return intValue * 10;
            }, Detail2.INT)
                    .caption(Detail2.INT_DERIVED.name()))
            .selectTableName(DETAIL_SELECT_TABLE_NAME.name())
            .orderBy(ascending(Detail2.STRING))
            .smallDataset(true)
            .stringFactory(Detail2.STRING));
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

  void detail() {
    add(Detail.TYPE.define(
            Detail.ID.define()
                    .primaryKey()
                    .beanProperty("id"),
            Detail.SHORT.define()
                    .column()
                    .caption(Detail.SHORT.name()),
            Detail.INT.define()
                    .column()
                    .caption(Detail.INT.name()),
            Detail.DOUBLE.define()
                    .column()
                    .caption(Detail.DOUBLE.name())
                    .columnHasDefaultValue(true)
                    .beanProperty("double"),
            Detail.STRING.define()
                    .column()
                    .caption("Detail string")
                    .selectable(false),
            Detail.DATE.define()
                    .column()
                    .caption(Detail.DATE.name())
                    .columnHasDefaultValue(true),
            Detail.TIMESTAMP.define()
                    .column()
                    .caption(Detail.TIMESTAMP.name()),
            Detail.BOOLEAN.define()
                    .column()
                    .caption(Detail.BOOLEAN.name())
                    .nullable(false)
                    .defaultValue(true)
                    .description("A boolean property"),
            Detail.BOOLEAN_NULLABLE.define()
                    .column()
                    .caption(Detail.BOOLEAN_NULLABLE.name())
                    .columnHasDefaultValue(true)
                    .defaultValue(true),
            Detail.MASTER_ID.define()
                    .column(),
            Detail.MASTER_FK.define()
                    .foreignKey()
                    .caption(Detail.MASTER_FK.name())
                    .beanProperty("master"),
            Detail.MASTER_CODE_NON_DENORM.define()
                    .column(),
            Detail.MASTER_VIA_CODE_FK.define()
                    .foreignKey()
                    .caption(Detail.MASTER_FK.name())
                    .beanProperty("master"),
            Detail.MASTER_NAME.define()
                    .denormalized(Detail.MASTER_FK, Master.NAME)
                    .caption(Detail.MASTER_NAME.name()),
            Detail.MASTER_CODE.define()
                    .denormalized(Detail.MASTER_FK, Master.CODE)
                    .caption(Detail.MASTER_CODE.name()),
            Detail.INT_VALUE_LIST.define()
                    .column()
                    .items(ITEMS)
                    .caption(Detail.INT_VALUE_LIST.name()),
            Detail.INT_DERIVED.define()
                    .derived(linkedValues -> {
              Integer intValue = linkedValues.get(Detail.INT);
              if (intValue == null) {

                return null;
              }

              return intValue * 10;
            }, Detail.INT)
                    .caption(Detail.INT_DERIVED.name()),
            Detail.BYTES.define()
                    .column()
                    .updatable(false)
                    .lazy(true))
            .keyGenerator(queried("select id from dual"))
            .orderBy(ascending(Detail.STRING))
            .selectTableName(DETAIL_SELECT_TABLE_NAME.name())
            .smallDataset(true)
            .stringFactory(Detail.STRING));
  }

  public interface Department extends Entity {
    EntityType TYPE = DOMAIN.entityType("domain.scott.dept", Department.class);
    Column<Integer> ID = TYPE.integerColumn("deptno");
    Column<String> NAME = TYPE.stringColumn("dname");
    Column<String> LOCATION = TYPE.stringColumn("loc");
    Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
    Column<byte[]> DATA = TYPE.byteArrayColumn("data");
    Column<Character> CODE = TYPE.characterColumn("code");

    int deptNo();

    String name();

    String location();

    Boolean active();

    void active(Boolean active);

    void setDeptNo(int deptNo);

    ConditionType CONDITION = TYPE.conditionType("condition");
    ConditionType NAME_NOT_NULL_CONDITION = TYPE.conditionType("conditionNameNotNull");
  }

  void department() {
    add(Department.TYPE.define(
            Department.ID.define()
                    .primaryKey()
                    .caption(Department.ID.name())
                    .updatable(true).nullable(false)
                    .beanProperty("deptNo"),
            Department.NAME.define()
                    .column()
                    .caption(Department.NAME.name())
                    .searchable(true)
                    .maximumLength(14)
                    .nullable(false)
                    .beanProperty("name"),
            Department.LOCATION.define()
                    .column()
                    .caption(Department.LOCATION.name())
                    .maximumLength(13)
                    .beanProperty("location"),
            Department.ACTIVE.define()
                    .booleanColumn(Integer.class, 1, 0)
                    .readOnly(true)
                    .beanProperty("active"),
            Department.DATA.define()
                    .column()
                    .lazy(true),
            Department.CODE.define()
                    .column())
            .tableName("scott.dept")
            .smallDataset(true)
            .orderBy(ascending(Department.NAME))
            .stringFactory(Department.NAME)
            .conditionProvider(Department.CONDITION, (columns, values) -> {
              StringBuilder builder = new StringBuilder("deptno in (");
              values.forEach(value -> builder.append("?,"));
              builder.deleteCharAt(builder.length() - 1);

              return builder.append(")").toString();
            })
            .conditionProvider(Department.NAME_NOT_NULL_CONDITION, (columns, values) -> "department name is not null")
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
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT_NO, Department.ID);
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
            Employee.ID.define()
                    .primaryKey()
                    .caption(Employee.ID.name())
                    .name("empno")
                    .beanProperty("id"),
            Employee.NAME.define()
                    .column()
                    .caption(Employee.NAME.name())
                    .searchable(true)
                    .name("ename")
                    .maximumLength(10)
                    .nullable(false)
                    .beanProperty("name"),
            Employee.DEPARTMENT_NO.define()
                    .column()
                    .nullable(false)
                    .beanProperty("deptno"),
            Employee.DEPARTMENT_FK.define()
                    .foreignKey()
                    .caption(Employee.DEPARTMENT_FK.name())
                    .beanProperty("department"),
            Employee.JOB.define()
                    .column()
                    .items(asList(item("ANALYST"), item("CLERK"),
                            item("MANAGER"), item("PRESIDENT"), item("SALESMAN")))
                    .caption(Employee.JOB.name())
                    .searchable(true)
                    .beanProperty("job"),
            Employee.SALARY.define()
                    .column()
                    .caption(Employee.SALARY.name())
                    .nullable(false).valueRange(1000, 10000).maximumFractionDigits(2)
                    .beanProperty("salary"),
            Employee.COMMISSION.define()
                    .column()
                    .caption(Employee.COMMISSION.name())
                    .valueRange(100, 2000).maximumFractionDigits(2)
                    .beanProperty("commission"),
            Employee.MGR.define()
                    .column()
                    .beanProperty("mgr"),
            Employee.MANAGER_FK.define()
                    .foreignKey()
                    .caption(Employee.MANAGER_FK.name())
                    .beanProperty("manager"),
            Employee.HIREDATE.define()
                    .column()
                    .caption(Employee.HIREDATE.name())
                    .updatable(false)
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDot()
                            .yearFourDigits()
                            .build())
                    .nullable(false)
                    .beanProperty("hiredate"),
            Employee.DEPARTMENT_LOCATION.define()
                    .denormalized(Employee.DEPARTMENT_FK, Department.LOCATION)
                    .caption(Department.LOCATION.name()),
            Employee.DEPARTMENT_NAME.define()
                    .derived(new DepartmentNameProvider(), Employee.NAME, Employee.DEPARTMENT_FK),
            Employee.DATA.define()
                    .column()
                    .caption("Data"))
            .tableName("scott.emp")
            .keyGenerator(KeyGenerator.sequence("scott.emp_seq"))
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
            KeyTest.ID1.define()
                    .primaryKey(0),
            KeyTest.ID2.define()
                    .primaryKey(1),
            KeyTest.ID3.define()
                    .primaryKey(2)
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
            NoPk.COL1.define()
                    .column(),
            NoPk.COL2.define()
                    .column(),
            NoPk.COL3.define()
                    .column()));
  }

  public interface TransModifies {
    EntityType TYPE = DOMAIN.entityType("trans_modifies");

    Column<Integer> ID = TYPE.integerColumn("id");
    Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
  }

  void transientModifies() {
    add(TransModifies.TYPE.define(
            TransModifies.ID.define()
                    .primaryKey(),
            TransModifies.TRANS.define()
                    .attribute()));
  }

  public interface TransModifiesNot {
    EntityType TYPE = DOMAIN.entityType("trans_modifies_not");

    Column<Integer> ID = TYPE.integerColumn("id");
    Attribute<Integer> TRANS = TYPE.integerAttribute("trans");
  }

  void transientModifiesNot() {
    add(TransModifiesNot.TYPE.define(
            TransModifiesNot.ID.define()
                    .primaryKey(),
            TransModifiesNot.TRANS.define()
                    .attribute()
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
            NullString.ID.define()
                    .primaryKey(),
            NullString.ATTR.define()
                    .column(),
            NullString.ATTR2.define()
                    .column())
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
            InvalidDerived.ID.define()
                    .primaryKey(),
            InvalidDerived.INT.define()
                    .column(),
            InvalidDerived.INVALID_DERIVED.define()
                    .derived(linkedValues -> linkedValues.get(InvalidDerived.INT).intValue(), InvalidDerived.ID))
            .caption(InvalidDerived.INVALID_DERIVED.name())//incorrect source value, trigger exception
            .stringFactory(entity -> null));
  }

  public interface ForeignKeyLazyColumn {
    EntityType TYPE = DOMAIN.entityType("foreign_key_lazy_column");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<Integer> DEPARTMENT_ID = TYPE.integerColumn("department_id");
    
    ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_fk", DEPARTMENT_ID, Department.ID);
  }

  void foreignKeyLazyColumn() {
    add(ForeignKeyLazyColumn.TYPE.define(
            ForeignKeyLazyColumn.ID.define()
                    .primaryKey(),
            ForeignKeyLazyColumn.DEPARTMENT_ID.define()
                    .column()
                    .lazy(true),
            ForeignKeyLazyColumn.DEPARTMENT_FK.define()
                    .foreignKey()));
  }
}
