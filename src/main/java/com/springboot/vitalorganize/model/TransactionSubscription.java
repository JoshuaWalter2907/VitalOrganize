package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionSubscription {

    private String status;
    private String id;

    @JsonProperty("amount_with_breakdown")
    private AmountWithBreakdown amountWithBreakdown;

    @JsonProperty("payer_name")
    private PayerName payerName;

    @JsonProperty("payer_email")
    private String payerEmail;

    private LocalDateTime time;

    // Getter und Setter
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AmountWithBreakdown getAmountWithBreakdown() {
        return amountWithBreakdown;
    }

    public void setAmountWithBreakdown(AmountWithBreakdown amountWithBreakdown) {
        this.amountWithBreakdown = amountWithBreakdown;
    }

    public PayerName getPayerName() {
        return payerName;
    }

    public void setPayerName(PayerName payerName) {
        this.payerName = payerName;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    // Innere statische Klasse: AmountWithBreakdown
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmountWithBreakdown {
        @JsonProperty("gross_amount")
        private CurrencyAmount grossAmount;

        @JsonProperty("fee_amount")
        private CurrencyAmount feeAmount;

        @JsonProperty("net_amount")
        private CurrencyAmount netAmount;

        // Getter und Setter
        public CurrencyAmount getGrossAmount() {
            return grossAmount;
        }

        public void setGrossAmount(CurrencyAmount grossAmount) {
            this.grossAmount = grossAmount;
        }

        public CurrencyAmount getFeeAmount() {
            return feeAmount;
        }

        public void setFeeAmount(CurrencyAmount feeAmount) {
            this.feeAmount = feeAmount;
        }

        public CurrencyAmount getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(CurrencyAmount netAmount) {
            this.netAmount = netAmount;
        }
    }

    // Innere statische Klasse: CurrencyAmount
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrencyAmount {
        @JsonProperty("currency_code")
        private String currencyCode;

        private String value;

        // Getter und Setter
        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // Innere statische Klasse: PayerName
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayerName {
        @JsonProperty("given_name")
        private String givenName;

        @JsonProperty("surname")
        private String surname;

        // Getter und Setter
        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }
}
