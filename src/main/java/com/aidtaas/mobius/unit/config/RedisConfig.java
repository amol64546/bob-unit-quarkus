
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.ws.rs.Produces;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.client.RedisTimeoutException;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;

import static com.aidtaas.mobius.unit.constants.BobConstants.BOB_CACHE;
import static com.aidtaas.mobius.unit.constants.BobConstants.REDIS;
import static java.util.Collections.emptyList;


@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RedisConfig {

  private final ConfigProperties configProps;

  /**
   * Create a Redisson client.
   *
   * @return the cache
   */
  @Produces
  @Default
  public Cache<String, String> redissonClient() {

    try {
      log.info("In redissonClient Config");
      var config = new Config();
      if (configProps.redisClusterEnable()) {
        if (configProps.redisPasswordEnable()) {
          config.useClusterServers()
            .setPassword(configProps.redisPassword())
            .addNodeAddress(REDIS + configProps.redisHost());
        } else {
          config.useClusterServers()
            .addNodeAddress(REDIS + configProps.redisHost());
        }
      } else {
        if (configProps.redisPasswordEnable()) {
          config.useSingleServer()
            .setPassword(configProps.redisPassword())
            .setAddress(REDIS + configProps.redisHost());
        } else {
          config.useSingleServer()
            .setAddress(REDIS + configProps.redisHost());
        }
      }
      var redissonClient = Redisson.create(config);
      MutableConfiguration<String, String> jcacheConfig = new MutableConfiguration<>();
      Configuration<String, String> configuration = RedissonConfiguration.fromInstance(redissonClient, jcacheConfig);

      var manager = Caching.getCachingProvider().getCacheManager();
      return manager.createCache(BOB_CACHE, configuration);
    } catch (RedisTimeoutException e) {
      log.error("Error connecting to redis: ", e);
      return Cache.class.cast(emptyList());
    }
  }
}
