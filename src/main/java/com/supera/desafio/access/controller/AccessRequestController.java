package com.supera.desafio.access.controller;

import com.supera.desafio.access.dto.AccessRequestCancelRequest;
import com.supera.desafio.access.dto.AccessRequestCreateRequest;
import com.supera.desafio.access.dto.AccessRequestResponse;
import com.supera.desafio.access.dto.PageResponse;
import com.supera.desafio.access.service.AccessRequestService;
import com.supera.desafio.core.domain.request.AccessRequestStatus;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;

    public AccessRequestController(AccessRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccessRequestResponse> create(@Valid @RequestBody AccessRequestCreateRequest payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(payload));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AccessRequestResponse>> list(@RequestParam(required = false) String q,
                                                                    @RequestParam(required = false) AccessRequestStatus status,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
                                                                    @RequestParam(required = false) Boolean urgent,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(required = false) Integer size) {
        PageResponse<AccessRequestResponse> response = service.list(q, status, startDate, endDate, urgent, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessRequestResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AccessRequestResponse> cancel(@PathVariable Long id,
                                                        @Valid @RequestBody AccessRequestCancelRequest payload) {
        return ResponseEntity.ok(service.cancel(id, payload));
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<AccessRequestResponse> renew(@PathVariable Long id) {
        return ResponseEntity.ok(service.renew(id));
    }
}
