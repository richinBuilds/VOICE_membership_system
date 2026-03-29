package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.dtos.ApiResponse;
import org.voice.membership.dtos.CapturePayPalOrderRequest;
import org.voice.membership.dtos.CreatePayPalOrderRequest;
import org.voice.membership.dtos.PayPalOrderResponse;
import org.voice.membership.dtos.RedirectResponse;
import org.voice.membership.dtos.WebhookStatusResponse;
import org.voice.membership.services.PayPalMembershipService;

import java.security.Principal;
@RestController
@RequiredArgsConstructor
public class PayPalController {

    private final PayPalMembershipService payPalMembershipService;

    @PostMapping("/register/paypal/create-order")
    public ResponseEntity<ApiResponse<PayPalOrderResponse>> createOrder(@Valid @RequestBody CreatePayPalOrderRequest request,
            Principal principal) {
        PayPalOrderResponse response = payPalMembershipService.createOrder(request.membershipId(), principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Order created", response));
    }

    @PostMapping("/register/paypal/capture-order")
    public ResponseEntity<ApiResponse<RedirectResponse>> captureOrder(@Valid @RequestBody CapturePayPalOrderRequest request,
            Principal principal) {
        RedirectResponse response = payPalMembershipService.captureOrder(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Payment finalized", response));
    }

    @PostMapping("/api/paypal/webhook")
    public ResponseEntity<ApiResponse<WebhookStatusResponse>> webhook(@RequestHeader HttpHeaders headers,
            @RequestBody String payload) {
        WebhookStatusResponse response = payPalMembershipService.handleWebhook(headers.toSingleValueMap(), payload);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", response));
    }
}
