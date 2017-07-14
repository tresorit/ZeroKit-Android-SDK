package com.tresorit.zerokit.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TenantIdResolver {
    private static Pattern HostedTenantUrlPattern = Pattern.compile("\\Ahttps?://[^/,^?,^#]*/tenant-([a-z][a-z0-9]{7,9})/?\\z");
    private static Pattern ProductionTenantUrlPattern = Pattern.compile("\\Ahttps?://([a-z][a-z0-9]{7,9})\\.[^/,^?,^#]*/?\\z");
    private static Pattern TenantIdRegex = Pattern.compile("\\A[a-z][a-z0-9]{7,9}\\z");

    public static String getTenantId(String baseUrl){
        Matcher matcher = ProductionTenantUrlPattern.matcher(baseUrl);
        String tenantId = null;
        if (matcher.matches()){
            tenantId = matcher.group(1);
        }
        else{
            matcher = HostedTenantUrlPattern.matcher(baseUrl);
            if (matcher.matches()){
                tenantId = matcher.group(1);
            }
        }
        if (tenantId == null || !TenantIdRegex.matcher(tenantId).matches())
            throw new IllegalArgumentException("The given or parsed tenant ID is invalid.");
        return tenantId;
    }
}
