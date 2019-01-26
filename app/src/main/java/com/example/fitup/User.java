package com.example.fitup;

public class User {
    private String group;
    private String steps;
    private String top;

    public User() {
    }

    public User(String group, String steps, String top) {
        this.group = group;
        this.steps = steps;
        this.top = top;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getTop() {
        return top;
    }

    public void setTop(String top) {
        this.top = top;
    }
}
