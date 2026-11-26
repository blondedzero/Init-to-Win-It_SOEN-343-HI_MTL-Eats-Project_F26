package com.shopeasy.Domain;

import com.shopeasy.ApplicationSeeder;
import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.ORD.CourierDisplayData;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
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
    ORDModule ordModule;
    EventManager eventManager;
    ApplicationSeeder applicationSeeder;

    public OperatorRequestsModule(OrderRepository orderRepository, UserRepository userRepository,
                                  CourierRepository courierRepository, RoleValidator roleValidator,
                                  ORDModule ordModule, EventManager eventManager,
                                  ApplicationSeeder applicationSeeder) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.courierRepository = courierRepository;
        this.roleValidator = roleValidator;
        this.ordModule = ordModule;
        this.eventManager = eventManager;
        this.applicationSeeder = applicationSeeder;
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
                .filter(courier -> !orderRepository.existsByCourier_AccountIdAndStatus(
                        courier.getAccountId(), OrderStatus.READY))
                .map(CourierDisplayData::from)
                .toList();
    }

    @Transactional
    public void resetSimulation(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "reset the simulation");

        int canceledOrders = 0;
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            if (order.getSeedInitialStatus() != null) {
                order.restoreSeedInitialState();
            } else if (List.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
                    OrderStatus.READY, OrderStatus.PICKED_UP).contains(order.getStatus())) {
                order.markCancelled();
                orderRepository.save(order);
                canceledOrders++;
            }
        }

        applicationSeeder.restoreInitialEntityStates();
        ordModule.resetSimulationQueues();
        eventManager.logEvent("Simulation reset by operator " + user.getAccountId()
                + ": canceled " + canceledOrders
                + " new active orders and restored the initial store, courier, and product states");
    }
}
