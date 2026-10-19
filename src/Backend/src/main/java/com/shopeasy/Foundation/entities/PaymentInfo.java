package com.shopeasy.Foundation.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_info")
public class PaymentInfo {

    @Id
    private Long customerId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    private String cardBrand;

    @Column(nullable = false)
    private String cardLast4Digits;

    @Column(nullable = false)
    private String cardExpMonth;

    @Column(nullable = false)
    private String cardExpYear;

    @Column(nullable = false)
    private String paymentProcessorToken;

    @Column(nullable = false)
    private String stripeCustomerId;

    protected PaymentInfo() {}

    public PaymentInfo(Customer customer, String cardBrand, String cardLast4Digits, String cardExpMonth, String cardExpYear, String paymentProcessorToken, String stripeCustomerId) {
        this.customer = customer;
        this.cardBrand = cardBrand;
        this.cardLast4Digits = cardLast4Digits;
        this.cardExpMonth = cardExpMonth;
        this.cardExpYear = cardExpYear;
        this.paymentProcessorToken = paymentProcessorToken;
        this.stripeCustomerId =  stripeCustomerId;
    }
}
