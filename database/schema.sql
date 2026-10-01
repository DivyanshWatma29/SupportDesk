CREATE DATABASE IF NOT EXISTS supportdesk
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE supportdesk;

-- 1. Departments table (eliminates department redundancy in tickets - 3NF)
CREATE TABLE IF NOT EXISTS departments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_department_name UNIQUE (name),
  CONSTRAINT uq_department_code UNIQUE (code)
);

-- 2. Users table (requesters such as students and staff, and support admins)
CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
  department_id BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_user_email UNIQUE (email),
  CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
  INDEX idx_user_department_id (department_id),
  INDEX idx_user_role (role)
);

-- 3. Tickets table (core entity referencing departments and users)
-- due_at = created_at + SLA hours for the priority (HIGH 4h, MEDIUM 24h, LOW 72h). resolved_at is set when status becomes RESOLVED.
CREATE TABLE IF NOT EXISTS tickets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ticket_number VARCHAR(20) NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  priority VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  department_id BIGINT NOT NULL,
  requester_id BIGINT NOT NULL,
  assigned_to BIGINT NULL,
  admin_note VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  due_at DATETIME(6) NULL,
  resolved_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_ticket_number UNIQUE (ticket_number),
  CONSTRAINT fk_ticket_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_ticket_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_ticket_department_id (department_id),
  INDEX idx_ticket_assigned_to (assigned_to),
  INDEX idx_ticket_status (status),
  INDEX idx_ticket_priority (priority),
  INDEX idx_ticket_created_at (created_at),
  INDEX idx_ticket_status_due_at (status, due_at)
);

-- 4. Audit Logs table (tracks lifecycle transitions and actions)
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  changed_by_id BIGINT NULL,
  action VARCHAR(50) NOT NULL,
  old_status VARCHAR(20) NULL,
  new_status VARCHAR(20) NULL,
  note VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
  CONSTRAINT fk_audit_changed_by FOREIGN KEY (changed_by_id) REFERENCES users (id) ON DELETE SET NULL,
  INDEX idx_audit_ticket_id (ticket_id),
  INDEX idx_audit_created_at (created_at)
);

-- Seed Initial Departments
INSERT INTO departments (name, code) VALUES
  ('IT Support', 'IT'),
  ('Staff Support', 'HR'),
  ('Fees & Finance', 'FIN'),
  ('Campus Facilities', 'FAC'),
  ('ID Cards & Access', 'SEC')
ON DUPLICATE KEY UPDATE name=VALUES(name);
