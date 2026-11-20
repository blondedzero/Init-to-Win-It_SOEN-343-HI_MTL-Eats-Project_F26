ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_status_check;

ALTER TABLE orders
    ADD CONSTRAINT orders_status_check
    CHECK (status IN (
        'PLACED',
        'ACCEPTED',
        'PREPARING',
        'REJECTED',
        'CANCELED',
        'READY',
        'PICKED_UP',
        'DELIVERED',
        'REFUNDED'
    ));

ALTER TABLE couriers
DROP CONSTRAINT IF EXISTS couriers_status_check;

ALTER TABLE couriers
    ADD CONSTRAINT couriers_status_check
    CHECK (status IN (
    'IDLE',
    'CARRYING',
    'OFFLINE',
    'ASSIGNED'
    ));
