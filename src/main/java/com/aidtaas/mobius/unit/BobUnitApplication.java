
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * This class represents the main entry point for the Mobius consumer application.
 * It is annotated with @QuarkusMain, which indicates that this is a Quarkus application.
 * The main method runs the Quarkus application.
 */
@QuarkusMain
public class BobUnitApplication {

  /**
   * The main method for the Mobius consumer application.
   * It runs the Quarkus application with the provided arguments.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    Quarkus.run(args);
  }

}
