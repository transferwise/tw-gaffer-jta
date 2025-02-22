package com.transferwise.common.gaffer.test.complextest1.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Setter;

@Setter
@Entity
@Table(name = "clients")
public class Client implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
  private Long id;
  private String name;

  @Id
  public Long getId() {
    return id;
  }

  @Column
  public String getName() {
    return name;
  }

}
