create schema employees;

CREATE TABLE employees.department (
  deptno INT NOT NULL,
  dname VARCHAR(14) NOT NULL,
  loc VARCHAR(13),
  constraint dept_pk primary key (deptno)
);

CREATE TABLE employees.employee (
  empno INT NOT NULL,
  ename VARCHAR(10) NOT NULL,
  job VARCHAR(9),
  mgr INT,
  hiredate DATE,
  sal DECIMAL(7, 2) NOT NULL,
  comm DECIMAL(7, 2),
  deptno INT NOT NULL,
  data blob,
  constraint emp_pk primary key (empno),
  constraint emp_dept_fk foreign key (deptno) references employees.department(deptno),
  constraint emp_mgr_fk foreign key (mgr) references employees.employee(empno)
);

CREATE SEQUENCE employees.employee_seq START WITH 17;

INSERT INTO employees.department(deptno, dname, loc)
VALUES (10, 'ACCOUNTING', 'NEW YORK'),
  (20, 'RESEARCH', 'DALLAS'),
  (30, 'SALES', 'CHICAGO'),
  (40, 'OPERATIONS', 'BOSTON');

INSERT INTO employees.employee(empno, ename, job, mgr, hiredate, sal, comm, deptno, data)
VALUES (8, 'KING', 'PRESIDENT', NULL, '1981-11-17', 5000, NULL, 10, 'LONG CHAR DATA'),
  (3, 'JONES', 'MANAGER', 8, '1981-04-02', 2975, NULL, 20, 'LONG CHAR DATA'),
  (5, 'BLAKE', 'MANAGER', 3, '1981-05-01', 2850, NULL, 10, 'LONG CHAR DATA'),
  (1, 'ALLEN', 'SALESMAN', 5, '1981-02-20', 1600, 300, 30, 'LONG CHAR DATA'),
  (2, 'WARD', 'SALESMAN', 8, '1981-02-22', 1250, 500, 30, 'LONG CHAR DATA'),
  (4, 'MARTIN', 'SALESMAN', 3, '1981-09-28', 1250, 1400, 30, 'LONG CHAR DATA'),
  (6, 'CLARK', 'MANAGER', 8, '1981-06-09', 2450, 1500, 10, 'LONG CHAR DATA'),
  (7, 'SCOTT', 'ANALYST', 3, '1987-04-19', 3000, 1500, 20, 'LONG CHAR DATA'),
  (9, 'TURNER', 'SALESMAN', 5, '1981-09-08', 1500, 0, 30, 'LONG CHAR DATA'),
  (10, 'ADAMS', 'CLERK', 3, '1988-05-15', 1100, NULL, 20, 'LONG CHAR DATA'),
  (11, 'JAMES', 'CLERK', 5, '1996-10-03', 950, 1500, 10, 'LONG CHAR DATA'),
  (12, 'FORD', 'CLERK', 3, '1988-12-12', 3200, 1200, 20, 'LONG CHAR DATA'),
  (13, 'MILLER', 'CLERK', 6, '1983-01-23', 1300, 1200, 10, 'LONG CHAR DATA'),
  (14, 'PAUL', 'CLERK', 3, '1989-12-12', 1600, 1200, 20, 'LONG CHAR DATA'),
  (15, 'BAKER', 'CLERK', 5, '2007-01-01', 1000, NULL, 10, 'LONG CHAR DATA'),
  (16, 'TREVOR', 'CLERK', 5, '2007-01-01', 1000, NULL, 10, 'LONG CHAR DATA');

commit;

create user scott password 'tiger';
alter user scott admin true;