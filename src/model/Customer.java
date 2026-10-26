package model;

import java.io.Serializable;

import model.annotation.Id;
import model.annotation.MaxLength;

/** Practicals 2, 3, 11: the person who owns accounts. */
public class Customer implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private static long customerCounter = 100;

    @Id
    private final String customerId;
    @MaxLength(30)
    private String name;
    private String email;
    private String mobile;
    private Address address;

    public Customer(String name, String email, String mobile, Address address) {
        this.customerId = generateCustomerId();
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
    }

    public Customer(String name, String email, String mobile) {
        this(name, email, mobile, null);
    }

    private static synchronized String generateCustomerId() {
        customerCounter++;
        return "CUST" + customerCounter;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return customerId + " | " + name + " | " + email + " | " + mobile;
    }

    /** Returns a copy of this customer (with its own copy of the address). */
    @Override
    public Customer clone() {
        try {
            Customer copy = (Customer) super.clone();
            if (address != null) {
                copy.address = new Address(address.getLine(), address.getCity(), address.getPincode());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /** Practical 3: static nested class. */
    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String line;
        private final String city;
        private final String pincode;

        public Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        public String getLine() {
            return line;
        }

        public String getCity() {
            return city;
        }

        public String getPincode() {
            return pincode;
        }

        @Override
        public String toString() {
            return line + ", " + city + " - " + pincode;
        }
    }
}
