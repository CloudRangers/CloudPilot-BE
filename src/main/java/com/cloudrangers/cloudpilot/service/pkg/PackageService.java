package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.dto.request.InstallPackagesRequest;
import com.cloudrangers.cloudpilot.security.CustomUserDetails;

import java.util.List;

public interface PackageService {
    List<String> installPackages(CustomUserDetails user, InstallPackagesRequest request);
}
