package com.shopeasy.Domain.DLV;

import com.shopeasy.Domain.ORD.OrderData;
import com.shopeasy.Foundation.entities.CourierStatus;

import java.util.List;

public record CourierDashboardData(CourierStatus status, List<OrderData> orders) {}
