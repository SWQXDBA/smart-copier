package test.model;

import lombok.Data;

@Data
public class Bank {
    public Bank() {
    }

    String name;

    public Bank(String name) {
        this.name = name;
    }
}
