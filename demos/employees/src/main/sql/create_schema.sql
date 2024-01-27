create user if not exists scott password 'tiger';
alter user scott admin true;

create schema employees;

CREATE TABLE employees.department (
  department_no INT NOT NULL,
  name VARCHAR(14) NOT NULL,
  location VARCHAR(13),
  constraint department_pk primary key (department_no)
);

CREATE TABLE employees.employee(
  id INT NOT NULL,
  name VARCHAR(10) NOT NULL,
  job VARCHAR(9),
  manager_id INT,
  hiredate DATE,
  salary DECIMAL(7, 2) NOT NULL,
  commission DECIMAL(7, 2),
  department_no INT NOT NULL,
  constraint employee_pk primary key (id),
  constraint employee_department_fk foreign key (department_no) references employees.department(department_no),
  constraint employee_manager_fk foreign key (manager_id) references employees.employee(id)
);

CREATE SEQUENCE employees.employee_seq START WITH 17;

INSERT INTO employees.department(department_no, name, location)
VALUES (10, 'Accounting', 'New York'),
  (20, 'Research', 'Dallas'),
  (30, 'Sales', 'Chicaco'),
  (40, 'Operations', 'Boston');

INSERT INTO employees.employee(id, name, job, manager_id, hiredate, salary, commission, department_no)
VALUES (8, 'King', 'President', NULL, '1981-11-17', 5000, NULL, 10),
  (3, 'Jones', 'Manager', 8, '1981-04-02', 2975, NULL, 20),
  (5, 'Blake', 'Manager', 3, '1981-05-01', 2850, NULL, 10),
  (1, 'Allen', 'Salesman', 5, '1981-02-20', 1600, 300, 30),
  (2, 'Ward', 'Salesman', 8, '1981-02-22', 1250, 500, 30),
  (4, 'Martin', 'Salesman', 3, '1981-09-28', 1250, 1400, 30),
  (6, 'Clark', 'Manager', 8, '1981-06-09', 2450, 1500, 10),
  (7, 'Scott', 'Analyst', 3, '1987-04-19', 3000, 1500, 20),
  (9, 'Turner', 'Salesman', 5, '1981-09-08', 1500, 0, 30),
  (10, 'Adams', 'Clerk', 3, '1988-05-15', 1100, NULL, 20),
  (11, 'James', 'Clerk', 5, '1996-10-03', 950, 1500, 10),
  (12, 'Ford', 'Clerk', 3, '1988-12-12', 3200, 1200, 20),
  (13, 'Miller', 'Clerk', 6, '1983-01-23', 1300, 1200, 10),
  (14, 'Ben', 'Clerk', 3, '1989-12-12', 1600, 1200, 20),
  (15, 'Baker', 'Clerk', 5, '2007-01-01', 1000, NULL, 10),
  (16, 'Trevor', 'Clerk', 5, '2007-01-01', 1000, NULL, 10);

commit;