package com.shopeasy.Domain;

import com.shopeasy.Domain.ORD.CourierDisplayData;
import com.shopeasy.Foundation.entities.CourierStatus;
import com.shopeasy.Foundation.entities.Role;
import com.shopeasy.Foundation.entities.User;
import com.shopeasy.Foundation.repositories.CourierRepository;
import com.shopeasy.Foundation.repositories.OrderRepository;
import com.shopeasy.Foundation.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class OperatorRequestsModule {
    OrderRepository orderRepository;
    UserRepository userRepository;
    CourierRepository courierRepository;
    RoleValidator roleValidator;

    public OperatorRequestsModule(OrderRepository orderRepository, UserRepository userRepository, CourierRepository courierRepository, RoleValidator roleValidator) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.courierRepository = courierRepository;
        this.roleValidator = roleValidator;
    }

    @Transactional(readOnly = true)
    public List<CourierDisplayData> getAllCouriersOnDuty(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all couriers");
        return courierRepository.findByStatusIn(List.of(CourierStatus.IDLE, CourierStatus.CARRYING, CourierStatus.ASSIGNED)).stream()
                .map(CourierDisplayData::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourierDisplayData> getAllCouriersIdle(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "view all couriers on duty");
        return courierRepository.findByStatusIn(List.of(CourierStatus.IDLE)).stream()
                .map(CourierDisplayData::from)
                .toList();
    }
}
