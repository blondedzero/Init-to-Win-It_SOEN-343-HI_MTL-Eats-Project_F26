package com.shopeasy.Domain.DLV;

import com.shopeasy.Foundation.entities.Courier;

public interface CourierStatusObserver {
    void onCourierIdle(Courier c);
}
