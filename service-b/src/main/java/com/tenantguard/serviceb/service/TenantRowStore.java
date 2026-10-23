package com.tenantguard.serviceb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class TenantRowStore {

    public record Row(String ownerTenant, String value) {}

    private final List<Row> rows = new ArrayList<>();
    private final boolean missingFilter;

    public TenantRowStore(@Value("${tenantguard.fault.mode:NONE}") String mode) {
        this.missingFilter = FaultMode.valueOf(mode) == FaultMode.F4_MISSING_FILTER;
        for (String tenant : List.of("tenant-1", "tenant-2", "tenant-3")) {
            for (int i = 1; i <= 4; i++) {
                rows.add(new Row(tenant, tenant + "-row-" + i));
            }
        }
    }

    /** Query executed under the thread's current tenant context, as the code path does. */
    public List<Row> visibleRows(MessageObservation obs) {
        String scope = TenantContext.current();
        obs.checkStatement(scope);
        List<Row> result = rows.stream()
                .filter(r -> missingFilter || Objects.equals(r.ownerTenant(), scope))
                .toList();
        result.forEach(r -> obs.checkRow(r.ownerTenant()));
        return result;
    }
}