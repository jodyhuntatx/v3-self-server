SELECT pr.name, appid.name
FROM projects pr, appidentities appid 
WHERE appid.accreq_id = 3 AND appid.project_id = pr.id;

SELECT pr.name, appid.name 
FROM projects pr, appidentities appid, accessrequests ar
WHERE ar.id = 3 AND appid.accreq_id = ar.id AND ar.project_id = pr.id;
