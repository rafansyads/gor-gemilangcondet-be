package io.mpruy.gor_gemilangcondet.backend_api.dto.transaksi.requests;

import io.mpruy.gor_gemilangcondet.backend_api.entities.payment.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {

    @NotNull(message = "Metode pembayaran wajib diisi")
    private PaymentMethod paymentMethod;

    @NotEmpty(message = "Keranjang tidak boleh kosong")
    @Valid
    private List<CartItemRequest> items;

    @Min(value = 0, message = "Diskon tidak boleh negatif")
    private double diskon;
}
