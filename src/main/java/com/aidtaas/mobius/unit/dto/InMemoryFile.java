
/*
 * Copyright (c) 2024.
 * Gaian Solutions Pvt. Ltd.
 * All rights reserved.
 */
package com.aidtaas.mobius.unit.dto;

import com.aidtaas.mobius.error.services.exception.ObjectMappingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.camunda.bpm.engine.variable.value.FileValue;

import static com.google.common.io.ByteStreams.toByteArray;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InMemoryFile implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String filename;
  private final byte[] fileData;

  /**
   * Constructor for the InMemoryFile class.
   * It initializes the filename and file data fields.
   *
   * @param name      the name of the file
   * @param byteArray the data of the file
   */
  public InMemoryFile(String name, byte[] byteArray) {
    this.fileData = Arrays.copyOf(byteArray, byteArray.length);
    this.filename = name;
  }

  /**
   * Constructor for the InMemoryFile class.
   * It initializes the filename and file data fields from a FileValue object.
   * It throws an ObjectMappingException if an error occurs while writing to the byte array.
   *
   * @param file the FileValue object
   */
  public InMemoryFile(FileValue file) {
    byte[] byteData;
    try {
      byteData = toByteArray(file.getValue());
    } catch (IOException e) {
      throw new ObjectMappingException("error while writing to byteArray", e);
    }

    this.filename = file.getFilename();
    this.fileData = Arrays.copyOf(byteData, byteData.length);
  }

  /**
   * Checks if this object is equal to another object.
   * It returns true if the other object is an InMemoryFile and has the same filename and file data.
   *
   * @param object the other object
   * @return true if the other object is an InMemoryFile and has the same filename and file data, false otherwise
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    InMemoryFile that = (InMemoryFile) object;
    return Objects.equals(filename, that.filename)
      && Arrays.equals(fileData, that.fileData);
  }

  /**
   * Returns a hash code for this object.
   * The hash code is computed based on the filename and file data.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return Objects.hash(filename, Arrays.hashCode(fileData));
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }
}
