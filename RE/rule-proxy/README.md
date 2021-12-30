# RULE PROXY

The rule-proxy allows you to manage rules, such as create, show, drop, describe, start, stop and restart rules，Enables users to quickly build rules and simplify operations on rules

## start rule-proxy
rule-daemon.sh START ProxyApplication

## stop rule-proxy
rule-daemon.sh STOP ProxyApplication

### Some job operation

#### create a job

The API accepts a JSON content and create a job.  
`POST http://localhost:8080/rule/v1/createOrSaveJob`  
Request Sample  
`{  
"product": "product",
"userId": "userId",  
"sql": "sql",
"jobId": "jobId",
"jobName": "jobName",
"clusterId": "clusterId",
"serverId": "serverId",
"versionStatus": "1"
}`

#### get job

The API accepts a JSON content  
`POST http://localhost:8080/rule/v1/getJobInfo`  
Request Sample  
`{"jobIdList": "jobIdList",
"userId":"userId",
"product":"product",
"type":"type"
}`  