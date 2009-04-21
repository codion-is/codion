create schema scott;

CREATE TABLE scott.dept (
  deptno INT NOT NULL,
  dname VARCHAR(14),
  loc VARCHAR(13)
);

alter table scott.dept
add constraint dept_pk
primary key (deptno);

CREATE TABLE scott.emp (
  empno INT NOT NULL,
  ename VARCHAR(10),
  job VARCHAR(9),
  mgr INT,
  hiredate DATE,
  sal DECIMAL(7, 2),
  comm DECIMAL(7, 2),
  deptno INT
);

alter table scott.emp
add constraint emp_pk
primary key (empno);

alter table scott.emp
add constraint emp_dept_fk
foreign key (deptno)
references scott.dept(deptno);