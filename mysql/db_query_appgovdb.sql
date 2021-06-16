select * from projects;
select * from accessrequests;
select * from appidentities;
select * from cybraccounts;

# Get access details w/ multi-way join
SELECT
proj.name, accreq.id, accreq.safe_name, appid.name, cybracct.name, cybracct.username, cybracct.db_name
FROM
projects proj, accessrequests accreq, appidentities appid, cybraccounts cybracct 
WHERE
accreq.project_id = proj.id
AND appid.accreq_id = accreq.id
AND appid.project_id = proj.id
AND cybracct.project_id = proj.id;
