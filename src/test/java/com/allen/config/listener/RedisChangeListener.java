package com.allen.config.listener;

import com.allen.config.utils.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RedisChangeListener implements ChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisChangeListener.class);

    private static final String KEY_PREFIX = "redis.";

    private static final String REDIS_SERVER_HOST = "redis.server.host";

    private static final String REDIS_SERVER_PORT = "redis.server.port";

    // Redis数据源连接池动态刷新的配置名
    private static final List<String> REFRESH_KEYS = Arrays.asList(REDIS_SERVER_HOST, REDIS_SERVER_PORT);

    @Override
    public void onChange(ChangeEvent event) {
        Map<String, ChangeEvent.Change> allChanges = event.getChanges();
        if(CollectionUtils.isEmpty(allChanges))
            return;

        StringBuilder redisChangesLog = new StringBuilder("[");
        Map<String, ChangeEvent.Change> redisChanges = new HashMap<>();
        for (String key : allChanges.keySet()) {
            if (key.startsWith(KEY_PREFIX)) {
                ChangeEvent.Change change = allChanges.get(key);
                if(null == change)
                    continue;

                String oldValue = change.getOldValue();
                String newValue = change.getNewValue();
                if(StringUtils.hasText(oldValue) && StringUtils.hasText(newValue) && oldValue.equals(newValue))
                    continue;

                redisChangesLog.append(change).append(",");
                redisChanges.put(key, change);
            }
        }
        redisChangesLog.deleteCharAt(redisChangesLog.length() - 1).append(",");
        if(redisChanges.isEmpty())
            return;

        boolean refresh = false;
        for(String refreshKey : REFRESH_KEYS) {
            // 只要出现需要刷新的配置，就要刷新
            if(redisChanges.containsKey(refreshKey)) {
                refresh = true;
                break;
            }
        }

        if(!refresh)
            return;

        // 刷新配置
        Map<String, JedisConnectionFactory> connectionFactories = SpringContextHolder.getBeans(JedisConnectionFactory.class);
        if(CollectionUtils.isEmpty(connectionFactories))
            return;

        LOGGER.info("开始变更Redis Client配置。" + redisChangesLog.toString());
        long start = System.currentTimeMillis();
        for (String beanName : connectionFactories.keySet()) {
            JedisConnectionFactory factory = connectionFactories.get(beanName);
            // 销毁现存的连接池
            factory.destroy();

            for (String key : redisChanges.keySet()) {
                String newValue = redisChanges.get(key).getNewValue();
                if (REDIS_SERVER_HOST.equals(key)) {
                    factory.setHostName(newValue);
                } else if(REDIS_SERVER_PORT.equals(key)) {
                    factory.setPort(Integer.parseInt(newValue));
                }
            }
            //JedisShardInfo置空，稍后重建
            factory.setShardInfo(null);
            factory.afterPropertiesSet();
        }
        LOGGER.info("变更Redis Client配置完成，耗时{}ms", System.currentTimeMillis() - start);
    }

}
