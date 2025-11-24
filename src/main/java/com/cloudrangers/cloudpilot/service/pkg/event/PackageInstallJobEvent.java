package com.cloudrangers.cloudpilot.service.pkg.event;

import com.cloudrangers.cloudpilot.dto.message.InstallPackageJobMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class PackageInstallJobEvent extends ApplicationEvent {

    private final List<InstallPackageJobMessage> messages;

    public PackageInstallJobEvent(List<InstallPackageJobMessage> messages) {
        super(messages);
        this.messages = messages;
    }
}
