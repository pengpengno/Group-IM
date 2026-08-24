package com.github.im.dto.workbench.approval;
public record ApprovalDefinitionDTO(Long definitionId, String code, String name,
                                    String formSchemaJson, int version) { }
