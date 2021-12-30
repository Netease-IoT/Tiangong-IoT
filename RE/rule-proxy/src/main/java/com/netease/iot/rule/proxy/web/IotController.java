package com.netease.iot.rule.proxy.web;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.netease.iot.rule.proxy.message.ErrorResultMessage;
import com.netease.iot.rule.proxy.message.IotBaseResultMessage;
import com.netease.iot.rule.proxy.service.IotService;
import com.netease.iot.rule.proxy.util.IotConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@Controller
@RequestMapping(value = "/rule", params = "Version=2018-11-14")
//@RequestMapping(value = "/rule")
public class IotController {

    @Autowired
    private IotService iotService;

    @ResponseBody
    @RequestMapping(value = "/httpTest", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity httpTest(@RequestBody JSONObject jsonParam,
                                   @RequestHeader HttpHeaders headers) {
        System.out.println(jsonParam);
        System.out.println(headers.size());
        return new ResponseEntity("ok", null, HttpStatus.valueOf(200));
    }

    @ResponseBody
    @RequestMapping(params = {"Action=CreateRule"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/CreateRule", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity createRule(@RequestBody JSONObject jsonParam,
                                     @RequestHeader HttpHeaders headers) {
        return updateOrCreateRule(jsonParam, headers);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=UpdateRule"}, method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/UpdateRule", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity updateRule(@RequestBody JSONObject jsonParam,
                                     @RequestHeader HttpHeaders headers) {
        return updateOrCreateRule(jsonParam, headers);
    }

    private ResponseEntity updateOrCreateRule(JSONObject jsonParam, HttpHeaders headers) {
        String requestId = headers.get(IotConstants.HEAD_REQUESTID).get(0);
        String tenantId = headers.get(IotConstants.HEAD_PRODUCTID).get(0);
        String language = headers.get(IotConstants.HEAD_LANGUAGE).get(0);
        IotBaseResultMessage resultMessage = iotService.createOrUpdateRule(jsonParam.toJSONString(), requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetSql"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/GetSql", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getSql(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getSql(map, requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=SubmitSql"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/SubmitSql", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity submitRule(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.submitSql(map, requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetRule"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/GetRule", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getRule(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getRule(map, requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetRuleList"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/GetRuleList", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getRuleList(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getRuleList(map, requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=StartRule"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/StartRule", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity startRule(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.startRule(map, requestId, tenantId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=StopRule"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/StopRule", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity stopRule(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.stopRule(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=DeleteRule"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/DeleteRule", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity deleteRule(HttpServletRequest request) {

        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.deleteRule(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetDatabases"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/getDatabases", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getDB(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getDB(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetTables"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/getTables", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getTables(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getTable(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=GetRuleAlarmInstances"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/getTables", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity getAlarmInstances(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.getRuleAlarmInstances( tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=CheckRuleName"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/checkRuleName", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity checkRuleName(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.checkName(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    @ResponseBody
    @RequestMapping(params = {"Action=CheckSql"}, method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @RequestMapping(value = "/checkSql", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity checkSql(HttpServletRequest request) {
        String requestId = request.getHeader(IotConstants.HEAD_REQUESTID);
        String language = request.getHeader(IotConstants.HEAD_LANGUAGE);
        String tenantId = request.getHeader(IotConstants.HEAD_PRODUCTID);
        Map<String, String> map = getMap(request.getParameterMap());
        IotBaseResultMessage resultMessage = iotService.checkSql(map, tenantId, requestId, language);
        return getReturn(resultMessage);
    }

    private ResponseEntity getReturn(IotBaseResultMessage resultMessage) {
        if (resultMessage instanceof ErrorResultMessage) {
            return new ResponseEntity(resultMessage, null, HttpStatus.valueOf(400));
        } else {
            return new ResponseEntity(JSONObject.toJSONString(resultMessage), null, HttpStatus.valueOf(200));
        }
    }

    private Map<String, String> getMap(Map<String, String[]> map) {
        Map<String, String> returnMap = Maps.newHashMap();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            try {
                returnMap.put(entry.getKey(), entry.getValue()[0]);
            } catch (Exception e) {
            }
        }
        return returnMap;
    }
}