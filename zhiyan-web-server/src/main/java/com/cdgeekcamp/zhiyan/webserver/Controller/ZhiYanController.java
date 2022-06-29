package com.cdgeekcamp.zhiyan.webserver.Controller;

import com.cdgeekcamp.zhiyan.webserver.Checker.URLChecker;
import com.cdgeekcamp.zhiyan.webserver.Controller.json.ResponseMessageJson;
import com.cdgeekcamp.zhiyan.webserver.Model.TokenDate;
import com.cdgeekcamp.zhiyan.webserver.Model.TokenRepository;
import com.cdgeekcamp.zhiyan.webserver.utils.EncryptionAlgorithm;
import com.cdgeekcamp.zhiyan.webserver.utils.GreetingRequestParam;
import com.cdgeekcamp.zhiyan.webserver.utils.RedisUtils;
import com.google.gson.JsonObject;
import happyjava.HappyLog;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/ZhiYan")

public class ZhiYanController {
    private static final HappyLog log = new HappyLog(ZhiYanController.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    RedisUtils redisUtils;

    public JsonObject buildMessage(int code, String message) {
        JsonObject jsonContainer = new JsonObject();
        jsonContainer.addProperty("code", code);
        jsonContainer.addProperty("message", message);
        return jsonContainer;
    }

    public Boolean checkRedisKeyLife(String token) {
        Optional<TokenDate> optionalTestEntity = tokenRepository.findById(token);
        return optionalTestEntity.isPresent();
    }

    @CrossOrigin
    @PostMapping(value = "/login/userinfo")
    public String getOneUserInfo(@RequestParam("username") String username,
                                 @RequestParam("password") String password,
                                 GreetingRequestParam requestParam) throws NoSuchAlgorithmException, InvalidKeyException, JSONException {
        final String expectedAccessKeySecret = "zhiyan";

        try {
            URLChecker.checkUsername(username);
            URLChecker.checkPassword(password);
            URLChecker.checkTimestamp(requestParam.getTimestamp());
            URLChecker.checkSignatureMethod(requestParam.getSignatureMethod());
            URLChecker.checkSignatureNonce(requestParam.getSignatureNonce());
            URLChecker.checkSignature(requestParam.getSignature());
        } catch (Exception e) {
            log.error("login-userinfo：" + " " + "获取用户登录信息失败：" + " " + e.getMessage() + " " + "参数校验未通过");
            return buildMessage(405, e.getMessage()).toString();
        }

        String[] sortedParams = requestParam.toSortedList();
        final String Algorithm = requestParam.getSignatureMethod();
        String ExpectedSignature = EncryptionAlgorithm.Signature(Algorithm, expectedAccessKeySecret,
                new StringBuilder(requestParam.getUsername() + requestParam.getPassword()));

        if (!ExpectedSignature.equals(requestParam.getSignature())) {
            log.error("Signature错误！！！");
            return buildMessage(405, "Signature错误！！！").toString();
        }

        UUID Token = UUID.randomUUID();
        TokenDate saveToken = new TokenDate();
        saveToken.setUsername(requestParam.getUsername());
        saveToken.setPassword(requestParam.getPassword());
        saveToken.setToken(Token.toString());
        saveToken.setTime(3600L);
        tokenRepository.save(saveToken);

        Map<String, Object> map;
        map = jdbcTemplate.queryForMap("select password from user_info where username='" + username + "' limit 1 ");
        JSONObject getUserinfo = new JSONObject(map);

        String expectedPassword = getUserinfo.getString("password");

        if (expectedPassword.equals(password)) {
            JsonObject responseMessage = buildMessage(200, "登录成功");
            responseMessage.addProperty("token", Token.toString());
            log.info("登录成功");
            return responseMessage.toString();
        } else {
            log.error("默认用户名或密码错误,请重新输入");
            return buildMessage(405, "默认用户名或密码错误,请重新输入").toString();
        }
    }

    @CrossOrigin
    @PostMapping(value = "/update/userinfo")
    public ResponseMessageJson updateUserinfo(@RequestParam("old") String oldPassword,
                                              @RequestParam("new") String newPassword,
                                              @RequestHeader("token") String userToken) throws JSONException {
        try {
            URLChecker.checkPassword(oldPassword);
            URLChecker.checkPassword(newPassword);
        } catch (Exception e) {
            log.error("update-userinfo：" + " " + "获取用户信息失败：" + " " + e.getMessage() + " " + "参数校验未通过");
            return new ResponseMessageJson(405, e.getMessage(), "");
        }

        Map<String, Object> map = jdbcTemplate.queryForMap("select password from user_info where username ='admin'");

        JSONObject getUserinfo = new JSONObject(map);

        String dbOldPassword = getUserinfo.getString("password");

        if (checkRedisKeyLife(userToken)) {
            if (dbOldPassword.equals(oldPassword)) {
                jdbcTemplate.update("update user_info set password ='" + newPassword + "' where username = 'admin'");
                log.info("用户修改密码成功");
                return new ResponseMessageJson(200, "用户修改密码成功", "");
            } else {
                log.info("密码错误,请重新输入");
                return new ResponseMessageJson(405, "原密码错误,请重新输入", "");
            }
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @PostMapping(value = "/add/token")
    public ResponseMessageJson addUserToken(@RequestParam("datetime") String datetime,
                                            @RequestParam("host") String host,
                                            @RequestParam("token") String token,
                                            @RequestHeader("token") String userToken) {
        try {
            URLChecker.checkTime(datetime);
            URLChecker.checkHost(host);
            URLChecker.checkToken(token);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.error("add-token：" + " " + "获取token信息失败：" + " " + e.getMessage() + " " + "参数校验未通过");
            return new ResponseMessageJson(405, e.getMessage(), "");
        }

        if (checkRedisKeyLife(userToken)) {
            String existSql = "select 1 as exist from user_token where host = '" + host + "'";
            List<String> existCode = jdbcTemplate.queryForList(existSql, String.class);
            if (existCode.size() == 0) {
                String sql = "insert into user_token (datetime,host,token) values ('" + datetime + "','" + host +
                        "','" + token + "')";
                jdbcTemplate.update(sql);
                log.info("令牌保存成功");
                return new ResponseMessageJson(200, "令牌保存成功", "");
            } else {
                String sql = "update user_token set token ='" + token + "',datetime ='" + datetime + "' where host ='" + host + "';";
                jdbcTemplate.update(sql);
                log.info("令牌更新成功");
                return new ResponseMessageJson(200, "令牌更新成功", "");
            }
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/token-list")
    public ResponseMessageJson getTokenList(@RequestHeader("token") String userToken) {

        if (checkRedisKeyLife(userToken)) {
            List<Map<String, Object>> map;
            map = jdbcTemplate.queryForList("select datetime ,host ,token from user_token;");
            return new ResponseMessageJson(200, "数据获取成功", map);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/{name}/new")
    public ResponseMessageJson getOneValue(@PathVariable("name") String name,
                                           @RequestParam("host") String host,
                                           @RequestHeader("token") String userToken) throws Exception {
        try {
            URLChecker.checkName(name);
            URLChecker.checkHost(host);
        } catch (Exception e) {
            log.error("获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
        }

        if (checkRedisKeyLife(userToken)) {
            Map<String, Object> map;
            map = jdbcTemplate.queryForMap("select * from ts_" + name + " where host ='" + host +
                    "' order by datetime desc limit 1");
            return new ResponseMessageJson(200, "数据获取成功", map);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/list/{name}")
    public ResponseMessageJson getProcessesValue(@PathVariable("name") String name,
                                                 @RequestParam("host") String host,
                                                 @RequestHeader("token") String userToken) throws Exception {
        try {
            URLChecker.checkName(name);
            URLChecker.checkHost(host);
        } catch (Exception e) {
            log.error("list：" + " " + "获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("list：" + " " + "获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
        }

        String formatName = name.replace("-", "_");

        if (checkRedisKeyLife(userToken)) {
            List<Map<String, Object>> map;
            map = jdbcTemplate.queryForList("select * from ts_" + formatName + " where host ='" + host +
                    "' and datetime = (select datetime from ts_" + formatName + " where host ='" + host +
                    "' ORDER By datetime desc limit 1 )order by datetime asc;");
            return new ResponseMessageJson(200, "数据获取成功", map);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/chart-list/{name}")
    public ResponseMessageJson ChartList(@PathVariable("name") String name,
                                         @RequestParam("time") String time,
                                         @RequestParam("host") String host,
                                         @RequestHeader("token") String userToken) throws Exception {
        try {
            URLChecker.checkName(name);
//            URLChecker.checkTime(String.valueOf(time));
            URLChecker.checkHost(host);
        } catch (Exception e) {
            log.error("chart-list：" + " " + "获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("chart-list：" + " " + "获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
        }
        String tableName = "ts_" + name;

        if (Objects.equals(name, "io")){
            String version = redisUtils.getHostLinuxKernelVersion(host);
            String[] arr = version.split("\\.");
            int bigVersionCode = Integer.parseInt(arr[0]);
            int smallVersionCode = Integer.parseInt(arr[1]);

            if (bigVersionCode <= 4 && smallVersionCode < 18) {
                tableName = "ts_io_4_18_down";
            } else if (bigVersionCode >= 5 && smallVersionCode > 5) {
                tableName = "ts_io_5_5_up";
            } else {
                tableName = "ts_io_4_18_up";
            }
        }

        String key;
        String keyword = null;
        ArrayList<Object> tablesList = new ArrayList<>();
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from " + tableName + " where host = '" + host + "' limit 1;");

        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd");// a为am/pm的标记
        Date date = new Date();// 获取当前时间

        JSONObject jsonObject = new JSONObject(map);
        Iterator<?> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            key = (String) iterator.next();
            if (jsonObject.get(key) instanceof String) {
                if (!Objects.equals(key, "host") && !Objects.equals(key, "datetime")) {
                    keyword = key;
                }
            }
        }

        List<String> keywordMap = jdbcTemplate.queryForList(
                "select distinct " + keyword + " from " + tableName + " where host = '" + host +
                        "' and datetime = (select datetime from " + tableName + " where host = '" + host +
                        "' ORDER By datetime desc limit 1);", String.class
        );

        for (String s : keywordMap) {
            tablesList.add(s);

            String sql = "select * from " + tableName + " where host = '" + host +
                    "' and datetime > (select datetime from " + tableName + " where host = '" + host +
                    "' ORDER By datetime desc limit 1 )  -  INTERVAL '" + time + "' and " + keyword + " ='" + s +
                    "' order by datetime asc;";

            List<Map<String, Object>> keywordInfo = jdbcTemplate.queryForList(sql);
            tablesList.add(keywordInfo);
        }

        if (checkRedisKeyLife(userToken)) {
            return new ResponseMessageJson(200, "数据获取成功", tablesList);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/module-list")
    public ResponseMessageJson getModuleList(@RequestParam("host") String host,
                                             @RequestHeader("token") String userToken) throws Exception {
        try {
            URLChecker.checkHost(host);
        } catch (Exception e) {
            log.error("module-list：" + " " + "获取信息失败：" + host + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("module-list：" + " " + "获取信息失败：" + host + " " + e.getMessage() + " " + "参数校验未通过");
        }

        if (checkRedisKeyLife(userToken)) {
            List<String> list = new ArrayList<>(redisUtils.hgetall(redisUtils.getHostFullKey(host)).values());

            List<String> configList = new ArrayList<>();
            for (String s : list) {
                if (s.startsWith("{")) {
                    configList.add(s.replace("\"{", "{").replace("}\",", "},").replaceAll("\\\\", ""));
                }
            }

            return new ResponseMessageJson(200, "数据获取成功", configList);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/host-list")
    public ResponseMessageJson getHostList(@RequestHeader("token") String userToken) {

        if (checkRedisKeyLife(userToken)) {
            List<String> hostList = redisUtils.getRedisHostList();

            return new ResponseMessageJson(200, "数据获取成功", hostList);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @GetMapping(value = "/get/{name}")
    public ResponseMessageJson getValueBetweenDatetime(@PathVariable("name") String name,
                                                       @RequestParam("time") String time,
                                                       @RequestParam("host") String host,
                                                       @RequestHeader("token") String userToken) throws Exception {
        try {
            URLChecker.checkName(name);
//            URLChecker.checkTime(String.valueOf(time));
            URLChecker.checkHost(host);
        } catch (Exception e) {
            log.error("获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("获取模块信息失败：" + name + " " + e.getMessage() + " " + "参数校验未通过");
        }

        if (checkRedisKeyLife(userToken)) {
            List<Map<String, Object>> map;
            map = jdbcTemplate.queryForList("select * from ts_" + name + " where host = '" + host + "' and  datetime > (select datetime  from ts_" + name + " where host = '" + host + "' ORDER By datetime desc limit 1) -  INTERVAL '" + time + "' order by datetime asc");
            return new ResponseMessageJson(200, "数据获取成功", map);
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }

    @CrossOrigin
    @PostMapping(value = "/update/config")
    public ResponseMessageJson MessageSendTest(@RequestHeader("token") String userToken,
                                               @RequestParam("config") String config,
                                               @RequestParam("host") String host,
                                               @RequestParam("name") String name) throws Exception {

        try {
            URLChecker.checkName(name);
        } catch (Exception e) {
            log.error("获取" + name + " " + "config信息失败：" + " " + e.getMessage() + " " + "参数校验未通过");
            throw new Exception("获取" + name + " " + "config信息失败：" + " " + e.getMessage() + " " + "参数校验未通过");
        }
        if (checkRedisKeyLife(userToken)) {
            JSONObject contentJson = new JSONObject();

            contentJson.put("ModuleName", name);

            contentJson.put("Host", host);

            contentJson.put("Config", config);

            redisUtils.sendMessage("UpdateConfig", contentJson.toString());

            return new ResponseMessageJson(200, "新配置接受成功", "");
        } else {
            return new ResponseMessageJson(400, "Token验证失败", "");
        }
    }
}