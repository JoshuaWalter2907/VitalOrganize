package com.springboot.vitalorganize.model;

public class RoleAssignment {
    private String week;
    private String personName;
    private String role;

    // Getters and Setters
    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
