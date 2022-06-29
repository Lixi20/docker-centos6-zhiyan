package com.cdgeekcamp.zhiyan.webserver.utils;

import happyjava.HappyLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@SuppressWarnings("unused")
@Component
public class RedisUtils {
    private final HappyLog log = new HappyLog(RedisUtils.class);

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    public Long getZhiYanConfigListSize(String host) {
        return redisTemplate.opsForList().size("ZhiYanConfig_" + host);
    }

    public void setZhiYanConfigListForIndex(String host, long index, String config) {
        redisTemplate.opsForList().set("ZhiYanConfig_" + host, index, config);
    }

    public List<String> getRedisHostList() {
        Set<String> keys = redisTemplate.keys("*");
        List<String> configs = new ArrayList<>();
        assert keys != null;
        for (String key : keys) {
            if (key.startsWith("ZhiYanConfig")) {
                String[] arr = key.split("_");
                if (arr.length >= 3) {
                    configs.add(arr[1]);
                }
            }
        }
        return configs;
    }

    public void sendMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    public String getHostLinuxKernelVersion(String host) {
        Set<String> keys = redisTemplate.keys("*");
        List<String> configs = new ArrayList<>();
        String version = "";
        assert keys != null;
        for (String key : keys) {
            if (key.startsWith("ZhiYanConfig_"+host)) {
                String[] arr = key.split("_");
                version = arr[2];
            }
        }
        return version;
    }
    public String getHostFullKey(String host) {
        Set<String> keys = redisTemplate.keys("*");
        List<String> configs = new ArrayList<>();
        String fullKeyName = "";
        assert keys != null;
        for (String key : keys) {
            if (key.startsWith("ZhiYanConfig_"+host)) {
                fullKeyName = key;
            }
        }
        return fullKeyName;
    }

    public Map<String, String> hgetall(String key) {
        return redisTemplate.execute((RedisCallback<Map<String, String>>) con -> {
            Map<byte[], byte[]> result = con.hGetAll(key.getBytes());
            if (CollectionUtils.isEmpty(result)) {
                return new HashMap<>(0);
            }

            Map<String, String> ans = new HashMap<>(result.size());
            for (Map.Entry<byte[], byte[]> entry : result.entrySet()) {
                ans.put(new String(entry.getKey()), new String(entry.getValue()));
            }
            return ans;
        });
    }
}
