package com.company.I_SPICE.model;

/**
 * DTO used to bind the checkout HTML form.
 * firstName + lastName + phone are collected on the form but combined
 * into the shippingAddress string that is ultimately stored on the Order.
 */
public class CheckoutForm {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String shippingAddress;
    private String billingAddress;
    private boolean sameBilling = true;

    // ---------- getters / setters ----------

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public boolean isSameBilling() {
        return sameBilling;
    }

    public void setSameBilling(boolean sameBilling) {
        this.sameBilling = sameBilling;
    }

    /**
     * Returns a full formatted shipping address that includes the contact name
     * and phone number so it reads naturally on order/confirmation pages.
     */
    public String getFullShippingAddress() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
            if (lastName != null && !lastName.isBlank())
                sb.append(" ").append(lastName.trim());
            sb.append("\n");
        }
        if (phone != null && !phone.isBlank()) {
            sb.append(phone.trim()).append("\n");
        }
        if (shippingAddress != null && !shippingAddress.isBlank()) {
            sb.append(shippingAddress.trim());
        }
        return sb.toString().trim();
    }
}
