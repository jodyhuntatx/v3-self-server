# Get access details w/ multi-way join
SELECT appid.name, cybracct.username, cybracct.db_name
FROM accessrequests accreq, appidentities appid, cybraccounts cybracct 
WHERE appid.project_id = accreq.project_id
AND appid.accreq_id = accreq.id
AND appid.project_id = cybracct.project_id;
