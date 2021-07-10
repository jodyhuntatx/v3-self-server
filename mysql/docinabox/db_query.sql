SELECT patients.first_name, patients.last_name, history.complaint
FROM patients, history
WHERE history.patient_id = patients.id;
