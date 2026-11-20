package com.shopeasy.Domain;

import com.shopeasy.Domain.GS.GSModule;
import com.shopeasy.Domain.GS.StoreContextData;
import com.shopeasy.Domain.ORD.ORDModule;
import com.shopeasy.Domain.ORD.CourierDisplayData;
import com.shopeasy.Foundation.entities.Order;
import com.shopeasy.Foundation.entities.OrderStatus;
import com.shopeasy.Foundation.entities.StoreStatus;
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
    GSModule gsModule;
    ORDModule ordModule;
    EventManager eventManager;

    public OperatorRequestsModule(OrderRepository orderRepository, UserRepository userRepository,
                                  CourierRepository courierRepository, RoleValidator roleValidator,
                                  GSModule gsModule, ORDModule ordModule, EventManager eventManager) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.courierRepository = courierRepository;
        this.roleValidator = roleValidator;
        this.gsModule = gsModule;
        this.ordModule = ordModule;
        this.eventManager = eventManager;
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

    @Transactional
    public void resetSimulation(User user) {
        roleValidator.requireRole(user, Role.OPERATOR, "reset the simulation");

        List<StoreContextData> stores = gsModule.getAllStores(user);
        int closedStores = 0;
        for (StoreContextData store : stores) {
            if (!StoreStatus.CLOSED.name().equals(store.status())) {
                gsModule.closeStore(user, store.id());
                closedStores++;
            }
        }

        List<Order> activeOrders = orderRepository.findByStatusIn(List.of(
                OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
                OrderStatus.READY, OrderStatus.PICKED_UP));
        activeOrders.forEach(order -> ordModule.cancelOrder(user, order.getOrderId()));

        courierRepository.findAll().forEach(courier -> courier.setStatus(CourierStatus.OFFLINE));
        eventManager.logEvent("Simulation reset by operator " + user.getAccountId()
                + ": canceled " + activeOrders.size() + " orders, closed " + closedStores
                + " stores, and set all couriers offline");
    }
}
