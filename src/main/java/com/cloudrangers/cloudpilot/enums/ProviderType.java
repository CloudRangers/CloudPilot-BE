package com.cloudrangers.cloudpilot.enums;

import lombok.Getter;

@Getter
public enum ProviderType {
    AWS("AWS", "Amazon Web Services"),
    VSPHERE("vSphere", "VMware vSphere"),
    AZURE("Azure", "Microsoft Azure"),
    GCP("GCP", "Google Cloud Platform");

    private final String displayName;
    private final String description;

    ProviderType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getTerraformProvider() {
        return switch (this) {
            case AWS -> "aws";
            case VSPHERE -> "vsphere";
            case AZURE -> "azure";
            case GCP -> "google";
        };
    }
}
