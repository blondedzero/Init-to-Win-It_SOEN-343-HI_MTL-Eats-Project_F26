package com.shopeasy.Domain;

import com.shopeasy.Domain.exceptions.PermissionDeniedException;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleValidator {
    protected RoleValidator() {}
    public void requireRole(User user, Role required, String action) {
        if (user.getRole() != required) {
            throw new PermissionDeniedException("A " + user.getRole().name().toLowerCase()
                    + " account cannot " + action);
        }
    }

    public void requireRole(User user, List<Role> required, String action) {
        if (!required.contains(user.getRole())) {
            throw new PermissionDeniedException("A " + user.getRole().name().toLowerCase()
                    + " account cannot " + action);
        }
    }
}
