INSERT INTO provider (provider_type, name, account_id) VALUES
                                                           ('AWS', 'AWS Prod Account', '123456789012'),
                                                           ('VSPHERE', 'OnPrem DC1', 'vsphere-dc1');

INSERT INTO os_image (provider_id, provider_type, name, image_id) VALUES
                                                                      (1, 'AWS', 'Amazon Linux 2023', 'ami-0123456789abcdef0'),
                                                                      (2, 'VSPHERE', 'Ubuntu 22.04 Template', 'tmpl-ubuntu-2204');

INSERT INTO instance_type (provider_id, provider_type, name, vcpu, memory_mb) VALUES
                                                                                  (1, 'AWS', 't3.small', 2, 2048),
                                                                                  (2, 'VSPHERE', 'Small-2C2G', 2, 2048);
