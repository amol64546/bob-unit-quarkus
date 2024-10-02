
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.error;

import com.aidtaas.mobius.error.services.error.Error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BobError extends Error {

  private static final long serialVersionUID = 1L;

  private List<String> subErrors;

  public BobError(Error error, List<String> subErrors) {
    super(error.getHttpStatusCode(), error.getErrorCode(), error.getErrorMessage(), error.getActionRequired());
    this.subErrors = new ArrayList<>(subErrors);
  }

  public List<String> getSubErrors() {
    return Collections.unmodifiableList(subErrors);
  }

}
