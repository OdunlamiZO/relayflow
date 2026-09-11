package com.relayflow.api.workspace;

import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkspaceMapper {

    WorkspaceResponse toDto(Workspace workspace);
}
