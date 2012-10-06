create schema scott;

CREATE TABLE scott.dept (
  deptno INT NOT NULL,
  dname VARCHAR(14) NOT NULL,
  loc VARCHAR(13),
  constraint dept_pk primary key (deptno)
);

CREATE TABLE scott.emp (
  empno INT NOT NULL,
  ename VARCHAR(10) NOT NULL,
  job VARCHAR(9),
  mgr INT,
  hiredate DATE,
  sal DECIMAL(7, 2) NOT NULL,
  comm DECIMAL(7, 2),
  deptno INT NOT NULL,
  constraint emp_pk primary key (empno),
  constraint emp_dept_fk foreign key (deptno) references scott.dept(deptno),
  constraint emp_mgr_fk foreign key (mgr) references scott.emp(empno)
);