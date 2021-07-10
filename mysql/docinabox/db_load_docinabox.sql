USE docinabox;

INSERT INTO patients(first_name, last_name, address, city, telephone, covid_vaccinated)
VALUES
("Sally", "Fields", "3 Sunset Blvd.", "Los Angeles", "713-555-1212", "Y"),
("Joe", "Montana", "123 Sandhill Rd.", "San Francisco", "999-555-1212", "N"),
("Bob", "Ross", "123 Happy Valley Dr.", "San Francisco", "999-555-1212", "N")
;
INSERT INTO history(complaint, date, patient_id)
VALUES
("Cancer", '2014-01-01', 1),
("Tuberculosis", '2013-01-01', 2),
("Painful swelling", '2020-10-25', 3)
;


CREATE TABLE IF NOT EXISTS history (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  complaint VARCHAR(30),
  date DATE,
  patient_id INT(4) UNSIGNED NOT NULL,
  INDEX(patient_id),
  FOREIGN KEY (patient_id) REFERENCES patients(id)
) engine=InnoDB;
