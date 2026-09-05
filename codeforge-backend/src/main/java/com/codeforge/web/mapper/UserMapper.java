package com.codeforge.web.mapper;

import com.codeforge.domain.User;
import com.codeforge.web.dto.auth.RegisterRequest;
import com.codeforge.web.dto.auth.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * DTO ⇄ entity conversion for users.
 *
 * <p>Mapping happens in the controller so the service layer only ever deals in
 * entities. The password is not mapped: the raw value is passed to the service
 * separately and hashed there, so a plaintext password never sits on an entity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    UserResponse toResponse(User user);
}
