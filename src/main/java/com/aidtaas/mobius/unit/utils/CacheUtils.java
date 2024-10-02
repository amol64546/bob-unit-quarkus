
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.utils;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.cache.Cache;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CacheUtils {

  private final Cache<String, String> cache;

  /**
   * Adds the key value pair to cache.
   *
   * @param cacheKey   the cache key
   * @param cacheValue the cache value
   */
  public void addToCache(String cacheKey, String cacheValue) {
    cache.put(cacheKey, cacheValue);
  }

  /**
   * Gets the value from cache.
   *
   * @param cacheKey the cache key
   * @return the from cache
   */
  public String getFromCache(String cacheKey) {
    return cache.get(cacheKey);
  }

}
