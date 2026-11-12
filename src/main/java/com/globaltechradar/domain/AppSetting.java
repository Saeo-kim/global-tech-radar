package com.globaltechradar.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 64)
    private String key;

    @Column(name = "setting_value", length = 500)
    private String value;

    protected AppSetting() { }

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void setValue(String value) { this.value = value; }

    public String getKey() { return key; }
    public String getValue() { return value; }
}
