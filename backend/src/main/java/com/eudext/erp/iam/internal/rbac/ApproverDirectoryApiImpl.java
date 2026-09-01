package com.eudext.erp.iam.internal.rbac;

import com.eudext.erp.iam.ApproverDirectoryApi;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ApproverDirectoryApiImpl implements ApproverDirectoryApi {

    private final UserCompanyRoleRepository userCompanyRoleRepository;
    private final UserRepository userRepository;

    ApproverDirectoryApiImpl(UserCompanyRoleRepository userCompanyRoleRepository, UserRepository userRepository) {
        this.userCompanyRoleRepository = userCompanyRoleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> usersWithRole(UUID companyId, UUID roleId) {
        return userCompanyRoleRepository.findByCompanyIdAndRoleId(companyId, roleId).stream()
                .map(UserCompanyRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> managerOf(UUID userId) {
        return userRepository.findById(userId).map(User::getManagerId).map(Optional::ofNullable).orElse(Optional.empty());
    }
}
