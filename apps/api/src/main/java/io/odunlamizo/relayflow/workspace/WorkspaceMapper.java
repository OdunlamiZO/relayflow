package io.odunlamizo.relayflow.workspace;

// Docs: docs/codebase-components.md#workspacemapper

import io.odunlamizo.relayflow.workspace.domain.Workspace;
import io.odunlamizo.relayflow.workspace.dto.WorkspaceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkspaceMapper {

    WorkspaceResponse toDto(Workspace workspace);
}
