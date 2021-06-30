DROP DATABASE IF EXISTS docinabox;
CREATE DATABASE IF NOT EXISTS docinabox;

USE docinabox;

CREATE TABLE IF NOT EXISTS patients (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  address VARCHAR(255),
  city VARCHAR(80),
  telephone VARCHAR(20),
  covid_vaccinated VARCHAR(1),
  INDEX(last_name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS history (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  complaint VARCHAR(30),
  date DATE,
  patient_id INT(4) UNSIGNED NOT NULL,
  INDEX(patient_id),
  FOREIGN KEY (patient_id) REFERENCES patients(id)
) engine=InnoDB;
