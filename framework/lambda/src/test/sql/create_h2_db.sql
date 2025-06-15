-- Test database schema for Lambda module tests

-- Clean up if exists
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS department;

-- Create tables
CREATE TABLE department (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE employee (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id INTEGER,
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(id)
);

-- Create indexes
CREATE INDEX idx_employee_department ON employee(department_id);