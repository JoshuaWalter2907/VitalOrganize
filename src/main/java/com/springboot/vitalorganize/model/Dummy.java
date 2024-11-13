package com.springboot.vitalorganize.model;

import java.time.LocalDate;

public class Dummy {
    private int id;
    private String name;
    private LocalDate date;

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Dummy{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", date=" + date +
               '}';
    }
}
