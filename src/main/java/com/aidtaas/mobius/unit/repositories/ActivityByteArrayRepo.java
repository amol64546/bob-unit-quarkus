
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.repositories;

import com.aidtaas.mobius.unit.constants.SqlQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class ActivityByteArrayRepo {

  private final EntityManager entityManager;

  /**
   * Find bytes by byte array id byte [ ].
   *
   * @param id the id
   * @return the byte [ ]
   */
  public byte[] findBytesByByteArrayId(String id) {
    return (byte[]) entityManager.createNativeQuery(String.format(SqlQueries.BYTES_BY_BYTEARRAY_ID_QUERY, id))
      .getSingleResult();
  }


}
